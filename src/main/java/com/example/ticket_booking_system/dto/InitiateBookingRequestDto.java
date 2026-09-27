package com.example.ticket_booking_system.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InitiateBookingRequestDto {
    @NotNull
    private Long userId;
    @NotNull
    private Long showId;
    @NotEmpty(message = "You must select at least one seat")
    private List<Long> showSeatIds;
}
