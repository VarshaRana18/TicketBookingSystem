package com.example.ticket_booking_system.controller;

import com.example.ticket_booking_system.dto.*;
import com.example.ticket_booking_system.entity.User;
import com.example.ticket_booking_system.service.BookingService;
import com.example.ticket_booking_system.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {
    private final ShowService showService;
    private final BookingService bookingService;

    // 1. Get all shows for a movie
    @GetMapping("/movies/{movieId}/shows")
    public ResponseEntity<List<ShowResponseDto>> getShowsForMovie(@PathVariable Long movieId){
        List<ShowResponseDto> shows = showService.getShowsByMovieId(movieId);
        return ResponseEntity.ok(shows);
    }

    // 2. Get all seats for a specific show
    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<List<ShowSeatResponseDto>>  getSeatsForShow(@PathVariable Long showId){
        List<ShowSeatResponseDto> seats = showService.getSeatsForShow(showId);
        return ResponseEntity.ok(seats);
    }

    // 3. Initiate the booking (Lock the seats)
    @PostMapping("bookings/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<InitiateBookingResponseDto> initiateBooking(@Valid @RequestBody InitiateBookingRequestDto requestDto,
                                                                      @AuthenticationPrincipal User currentUser){

        InitiateBookingResponseDto response = bookingService.initiateBooking(requestDto, currentUser.getId());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> confirmBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal User currentUser
    ) {
        String response = bookingService.confirmPayment(bookingId, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> cancelBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal User currentUser
    ) {
        String response = bookingService.cancelPayment(bookingId, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<BookingHistoryDto>> getBookingHistory(
            @AuthenticationPrincipal User currentUser
    ) {
        List<BookingHistoryDto> history = bookingService.getUserBookingHistory(currentUser.getId());
        return ResponseEntity.ok(history);
    }
}
