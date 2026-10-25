package com.example.ticket_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingCancellationResponseDto {
    private Long bookingId;
    private String paymentStatus;
    private String message;
}