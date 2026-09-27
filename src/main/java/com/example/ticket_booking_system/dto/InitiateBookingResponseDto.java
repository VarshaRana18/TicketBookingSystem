package com.example.ticket_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InitiateBookingResponseDto {
    private Long bookingId;
    private Double totalAmount;
    private String paymentStatus;
    private LocalDateTime lockExpiresAt;
}
