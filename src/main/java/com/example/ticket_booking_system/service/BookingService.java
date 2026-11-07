package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.*;
import com.example.ticket_booking_system.entity.*;
import com.example.ticket_booking_system.exception.ResourceNotFoundException;
import com.example.ticket_booking_system.repository.BookingRepository;
import com.example.ticket_booking_system.repository.ShowRepository;
import com.example.ticket_booking_system.exception.ShowSeatNotAvailableException;
import com.example.ticket_booking_system.repository.TicketRepository;
import com.example.ticket_booking_system.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final SeatMapCacheEvictor seatMapCacheEvictor;

    private final Clock clock;
    @Value("${booking.hold-duration-minutes}")
    private int HoldDurationMinutes;
    @Value("${booking.max-seats-per-booking}")
    private int MaxSeatsPerBooking;

    @Transactional
    public InitiateBookingResponseDto initiateBooking(InitiateBookingRequestDto dto, Long userId){
        if (dto.getSeats() == null || dto.getSeats().isEmpty()) {
            throw new IllegalArgumentException("You must select at least one seat.");
        }
        if (dto.getSeats().size() > MaxSeatsPerBooking) {
            throw new IllegalArgumentException("You cannot book more than " + MaxSeatsPerBooking + " seats at a time.");
        }

        long uniqueSeats = dto.getSeats().stream()
                .map(seat -> seat.getSeatRow().toUpperCase() + seat.getSeatNumber())
                .distinct()
                .count();
        if (uniqueSeats != dto.getSeats().size()) {
            throw new IllegalArgumentException("Duplicate seats found in the request payload.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        Show show = showRepository.findById(dto.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show Not Found"));

        if (show.getStartTime().isBefore(LocalDateTime.now(clock))) {
            throw new IllegalStateException("Tickets can no longer be booked for a show that has already started.");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setTotalAmount(0.0);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookingTime(LocalDateTime.now(clock));

        booking.setExpiresAt(LocalDateTime.now(clock).plusMinutes(HoldDurationMinutes));

        Booking savedBooking = bookingRepository.save(booking);

        List<Ticket> tickets = new ArrayList<>();
        double totalAmount = 0.0;

        int maxRows = show.getScreen().getNumRows();
        int maxSeatsPerRow = show.getScreen().getSeatsPerRow();

        for (SeatRequestDto seatReq : dto.getSeats()) {
            int rowIndex = seatReq.getSeatRow().toUpperCase().charAt(0) - 'A' + 1;

            if (rowIndex < 1 || rowIndex > maxRows || seatReq.getSeatNumber() < 1 || seatReq.getSeatNumber() > maxSeatsPerRow) {
                throw new IllegalArgumentException("Seat " + seatReq.getSeatRow() + seatReq.getSeatNumber() + " does not exist in this screen.");
            }

            Ticket ticket = new Ticket();
            ticket.setShow(show);
            ticket.setSeatRow(seatReq.getSeatRow());
            ticket.setSeatNumber(seatReq.getSeatNumber());
            ticket.setBooking(savedBooking);
            ticket.setPrice(show.getTicketPrice());
            ticket.setStatus(SeatStatus.HELD);
            ticket.setLockedAt(LocalDateTime.now(clock));

            tickets.add(ticket);
            totalAmount += ticket.getPrice();
        }
        savedBooking.setTotalAmount(totalAmount);
//        bookingRepository.save(savedBooking);

        try {
            ticketRepository.saveAll(tickets);
            ticketRepository.flush();

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ShowSeatNotAvailableException("One or more selected seats were just booked by someone else.");
        }

        seatMapCacheEvictor.evictAfterCommit(List.of(show.getId()));

        return mapToBookingResponseDto(booking, tickets, PaymentStatus.PENDING);
    }


    @Transactional
    public InitiateBookingResponseDto confirmPayment(Long bookingId, Long userId){
        Booking booking = getAndValidateBooking(bookingId, userId);
        LocalDateTime now = LocalDateTime.now(clock);

        int updatedRows = bookingRepository.confirmIfStillValid(bookingId, now);

        if (updatedRows == 0) {
            throw new IllegalStateException("Your 10-minute hold has expired or this booking was already processed.");
        }

        ticketRepository.updateStatusByBookingId(bookingId,SeatStatus.BOOKED);

        seatMapCacheEvictor.evictAfterCommit(List.of(booking.getShow().getId()));

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);

//        ticketRepository.saveAll(tickets);
//        ticketRepository.flush();

        return mapToBookingResponseDto(booking, tickets,PaymentStatus.SUCCESS);
    }

    @Transactional
    public BookingCancellationResponseDto cancelPayment(Long bookingId, Long userId){
        Booking booking = getAndValidateBooking(bookingId, userId);

        int updatedRows = bookingRepository.cancelIfStillPending(bookingId);

        if (updatedRows == 0) {
            // The database blocked the cancellation because it is no longer PENDING
            throw new IllegalStateException("Only pending bookings can be cancelled. This booking may have already been processed.");
        }

        ticketRepository.deleteByBookingId(bookingId);
        seatMapCacheEvictor.evictAfterCommit(List.of(booking.getShow().getId()));

        BookingCancellationResponseDto response = new BookingCancellationResponseDto();
        response.setBookingId(booking.getId());
        response.setPaymentStatus(PaymentStatus.FAILED.name());
        response.setMessage("Payment cancelled. Seats have been released.");
        return response;
    }

    private Booking getAndValidateBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Security Check: Prevent User A from confirming/cancelling User B's booking
        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You do not have permission to modify this booking.");
        }

        return booking;
    }

    @Transactional(readOnly = true)
    public List<BookingHistoryDto> getUserBookingHistory(Long userId, int page, int size){
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));

        List<Booking> bookings = bookingRepository.findHistory(userId, pageable);
        if (bookings.isEmpty()) return List.of();

        List<Long> ids = bookings.stream().map(Booking::getId).toList();
        Map<Long, List<Ticket>> ticketsByBooking = ticketRepository.findByBookingIdIn(ids)
                .stream()
                .collect(Collectors.groupingBy(t -> t.getBooking().getId()));

        return bookings.stream()
                .map(b -> mapToBookingHistoryDto(b, ticketsByBooking.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    private BookingHistoryDto mapToBookingHistoryDto(Booking booking, List<Ticket> tickets) {
        Show show = booking.getShow();

        BookingHistoryDto dto = new BookingHistoryDto();
        dto.setBookingId(booking.getId());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setPaymentStatus(booking.getPaymentStatus().name());
        dto.setBookingTime(booking.getBookingTime());
        dto.setEventName(show.getEvent().getTitle());
        dto.setTheatreName(show.getScreen().getTheatre().getName());
        dto.setScreenName(show.getScreen().getName());
        dto.setShowTime(show.getStartTime());
        dto.setSeatNumbers(tickets.stream()
                .map(t -> t.getSeatRow() + t.getSeatNumber())
                .toList());
        return dto;
    }

    private InitiateBookingResponseDto mapToBookingResponseDto(Booking booking, List<Ticket> tickets,PaymentStatus status) {
        InitiateBookingResponseDto dto = new InitiateBookingResponseDto();
        dto.setBookingId(booking.getId());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setPaymentStatus(status.name());
        dto.setLockExpiresAt(booking.getExpiresAt());

        List<String> seatNames = tickets.stream()
                .map(ticket -> ticket.getSeatRow() + ticket.getSeatNumber())
                .toList();

        dto.setSeatNumbers(seatNames);

        return dto;
    }
}
