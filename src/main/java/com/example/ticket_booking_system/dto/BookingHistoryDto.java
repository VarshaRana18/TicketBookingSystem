package com.example.ticket_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class BookingHistoryDto {
    private Long bookingId;
    private String eventName;
    private String theatreName;
    private String screenName;
    private LocalDateTime showTime;
    private List<String> seatNumbers;
    private double totalAmount;
    private String paymentStatus;
    private LocalDateTime bookingTime;
}
