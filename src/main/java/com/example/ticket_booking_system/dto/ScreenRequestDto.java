package com.example.ticket_booking_system.dto;

import com.example.ticket_booking_system.entity.Theatre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenRequestDto {
    @NotBlank(message = "Screen name is required")
    private String name;

    @NotNull(message = "Total seats capacity is required")
    private Integer totalSeats;

}
