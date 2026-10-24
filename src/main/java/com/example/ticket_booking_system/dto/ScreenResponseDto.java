package com.example.ticket_booking_system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenResponseDto {
    private Long id;
    private String name;
    private Long theatreId;
    private String theatreName;
    private Integer numRows;
    private Integer seatsPerRow;
}
