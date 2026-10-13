package com.example.ticket_booking_system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ShowRequestDto {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Ticket price is required")
    private Double ticketPrice;
}
