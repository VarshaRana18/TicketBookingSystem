package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.BookingHistoryDto;
import com.example.ticket_booking_system.dto.InitiateBookingRequestDto;
import com.example.ticket_booking_system.dto.InitiateBookingResponseDto;
import com.example.ticket_booking_system.entity.*;
import com.example.ticket_booking_system.exception.ResourceNotFoundException;
import com.example.ticket_booking_system.repository.BookingRepository;
import com.example.ticket_booking_system.repository.ShowRepository;
import com.example.ticket_booking_system.repository.ShowSeatRepository;
import com.example.ticket_booking_system.exception.ShowSeatNotAvailableException;
import com.example.ticket_booking_system.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public InitiateBookingResponseDto initiateBooking(InitiateBookingRequestDto dto, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        Show show = showRepository.findById(dto.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show Not Found"));

        List<ShowSeat> selectedSeats = showSeatRepository.findByIdsForUpdate(dto.getShowSeatIds());

        if (selectedSeats.size() != dto.getShowSeatIds().size()) {
            throw new IllegalArgumentException("One or more seats does not exist. ");
        }

        double totalAmount = 0.0;

        for(ShowSeat seat: selectedSeats){
            if (!seat.getShow().getId().equals(show.getId())) {
                throw new IllegalArgumentException("Seat ID " + seat.getId() + " does not belong to this show.");
            }
            if(seat.getStatus() != SeatStatus.AVAILABLE){
                throw new ShowSeatNotAvailableException("Seat ID " + seat.getSeat().getSeatRow() + seat.getSeat().getSeatNumber() + " is currently unavailable.");
            }

            totalAmount += seat.getPrice();
        }

        LocalDateTime now = LocalDateTime.now();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setTotalAmount(totalAmount);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookingTime(now);

        Booking savedBooking = bookingRepository.save(booking);

        for(ShowSeat seat: selectedSeats){
            seat.setStatus(SeatStatus.HELD);
            seat.setLockedAt(now);
            seat.setBooking(savedBooking);
        }

        showSeatRepository.saveAll(selectedSeats);

        InitiateBookingResponseDto responseDto = new InitiateBookingResponseDto();
        responseDto.setBookingId(savedBooking.getId());
        responseDto.setTotalAmount(totalAmount);
        responseDto.setPaymentStatus(savedBooking.getPaymentStatus().name());
        responseDto.setLockExpiresAt(now.plusMinutes(10));

        return responseDto;
    }


    @Transactional
    public String confirmPayment(Long bookingId, Long userId){
        Booking booking = getAndValidateBooking(bookingId, userId);

        // If the background sweeper already marked this as FAILED due to a 10-minute timeout, reject it.
        if (booking.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Booking is no longer pending. It may have expired.");
        }

        List<ShowSeat> seats = showSeatRepository.findByBookingId(bookingId);

        if (seats.isEmpty()) {
            booking.setPaymentStatus(PaymentStatus.FAILED);
            bookingRepository.save(booking);
            throw new IllegalStateException("Your session expired and the seats were released or booked by someone else.");
        }

        for (ShowSeat seat : seats) {
            // Double-check the seat wasn't swept away by the background task
            if (seat.getStatus() != SeatStatus.HELD) {
                throw new IllegalStateException("Seat " + seat.getId() + " is no longer held.");
            }

            seat.setStatus(SeatStatus.BOOKED);
            seat.setLockedAt(null); // Clear the temporary lock timer
        }
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        bookingRepository.save(booking);
        showSeatRepository.saveAll(seats);

        return "Payment successful! Seats have been booked.";
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
        List<ShowSeat> seats = showSeatRepository.findByBookingId(bookingId);
        for (ShowSeat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setLockedAt(null);
            seat.setBooking(null); // Unlink the foreign key so a new booking can claim this seat!
        }

        showSeatRepository.saveAll(seats);

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
        dto.setEventName(booking.getShow().getEvent().getTitle());
        dto.setTheatreName(booking.getShow().getScreen().getTheatre().getName());
        dto.setScreenName(booking.getShow().getScreen().getName());
        dto.setShowTime(booking.getShow().getStartTime());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setPaymentStatus(booking.getPaymentStatus().name());
        dto.setBookingTime(booking.getBookingTime());

        List<ShowSeat> seats = showSeatRepository.findByBookingId(booking.getId());
        List<String> seatNames = seats.stream()
                .map(showSeat -> showSeat.getSeat().getSeatRow() + showSeat.getSeat().getSeatNumber())
                .toList();

        dto.setSeatNumbers(seatNames);
        return dto;
    }
}
