package com.example.ticket_booking_system.service;

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

}
