package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.BookingHistoryDto;
import com.example.ticket_booking_system.dto.InitiateBookingRequestDto;
import com.example.ticket_booking_system.dto.InitiateBookingResponseDto;
import com.example.ticket_booking_system.dto.SeatRequestDto;
import com.example.ticket_booking_system.entity.*;
import com.example.ticket_booking_system.exception.ResourceNotFoundException;
import com.example.ticket_booking_system.repository.BookingRepository;
import com.example.ticket_booking_system.repository.ShowRepository;
import com.example.ticket_booking_system.exception.ShowSeatNotAvailableException;
import com.example.ticket_booking_system.repository.TicketRepository;
import com.example.ticket_booking_system.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public InitiateBookingResponseDto initiateBooking(InitiateBookingRequestDto dto, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        Show show = showRepository.findById(dto.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show Not Found"));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setTotalAmount(0.0);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookingTime(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        List<Ticket> tickets = new ArrayList<>();
        double totalAmount = 0.0;

        for (SeatRequestDto seatReq : dto.getSeats()) {

            Ticket ticket = new Ticket();
            ticket.setShow(show);
            ticket.setSeatRow(seatReq.getSeatRow());
            ticket.setSeatNumber(seatReq.getSeatNumber());
            ticket.setBooking(savedBooking);
            ticket.setPrice(show.getTicketPrice());
            ticket.setStatus(SeatStatus.HELD);
            ticket.setLockedAt(LocalDateTime.now());

            tickets.add(ticket);
            totalAmount += ticket.getPrice();
        }
        savedBooking.setTotalAmount(totalAmount);
        bookingRepository.save(savedBooking);

        try {
            ticketRepository.saveAll(tickets);
            ticketRepository.flush();

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ShowSeatNotAvailableException("One or more selected seats were just booked by someone else.");
        }

        return mapToBookingResponseDto(booking, tickets);
    }


    @Transactional
    public InitiateBookingResponseDto confirmPayment(Long bookingId, Long userId){
        Booking booking = getAndValidateBooking(bookingId, userId);

        // If the background sweeper already marked this as FAILED due to a 10-minute timeout, reject it.
        if (booking.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Booking is no longer pending. It may have expired.");
        }

        booking.setPaymentStatus(PaymentStatus.SUCCESS);

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);

        for (Ticket ticket : tickets) {
            ticket.setStatus(SeatStatus.BOOKED);
            ticket.setLockedAt(null);
        }

        ticketRepository.saveAll(tickets);

        return mapToBookingResponseDto(booking, tickets);
    }

    @Transactional
    public String cancelPayment(Long bookingId, Long userId){
        Booking booking = getAndValidateBooking(bookingId, userId);

        if (booking.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending bookings can be cancelled.");
        }

        // 1. Mark Booking as Failed/Cancelled
        booking.setPaymentStatus(PaymentStatus.FAILED);
        bookingRepository.save(booking);

        // 2. Immediately Release the Seats for other users
        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);

        ticketRepository.deleteAll(tickets);

        return "Payment cancelled. Seats have been released.";
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

    public List<BookingHistoryDto> getUserBookingHistory(Long userId){
        List<Booking> bookings = bookingRepository.findByUserIdOrderByBookingTimeDesc(userId);

        return bookings.stream()
                .map(this::mapToBookingHistoryDto)
                .toList();
    }

    private BookingHistoryDto mapToBookingHistoryDto(Booking booking){
        BookingHistoryDto dto = new BookingHistoryDto();

        dto.setBookingId(booking.getId());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setPaymentStatus(booking.getPaymentStatus().name());
        dto.setBookingTime(booking.getBookingTime());

        // 1. Fetch tickets using your new sparse repository
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());

        if (!tickets.isEmpty()) {
            // 2. All tickets in a booking share the same show, so grab info from the first one
            Show show = tickets.get(0).getShow();

            dto.setEventName(show.getEvent().getTitle()); // Or getName(), depending on your Event entity
            dto.setTheatreName(show.getScreen().getTheatre().getName());
            dto.setScreenName(show.getScreen().getName());
            dto.setShowTime(show.getStartTime());

            // 3. Map the coordinates directly from the Ticket entity
            List<String> seatNames = tickets.stream()
                    .map(ticket -> ticket.getSeatRow() + ticket.getSeatNumber())
                    .toList();

            dto.setSeatNumbers(seatNames);
        } else {
            // Fallback for empty or completely cancelled/deleted bookings
            dto.setSeatNumbers(new ArrayList<>());
        }

        return dto;
    }

    private InitiateBookingResponseDto mapToBookingResponseDto(Booking booking, List<Ticket> tickets) {
        InitiateBookingResponseDto dto = new InitiateBookingResponseDto();
        dto.setBookingId(booking.getId());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setPaymentStatus(booking.getPaymentStatus().name());
        dto.setLockExpiresAt(booking.getBookingTime().plusMinutes(10));

        return dto;
    }
}
