package com.example.ticket_booking_system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TheatreRequestDto {
    @NotBlank(message = "Theatre name is required")
    private String name;

    @NotBlank(message = "Location/City is required")
    private String location;
}
