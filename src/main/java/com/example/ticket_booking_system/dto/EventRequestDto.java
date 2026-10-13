package com.example.ticket_booking_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EventRequestDto {
    @NotBlank(message = "Event name cannot be blank")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Duration in minutes is required")
    private Integer durationMinutes;

    @NotNull(message = "Language is required")
    private String language;
}
