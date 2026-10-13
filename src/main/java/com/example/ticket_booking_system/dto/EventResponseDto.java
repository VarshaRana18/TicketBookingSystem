package com.example.ticket_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventResponseDto {
    private Long id;
    private String name;
    private String description;
    private Integer durationMinutes;
}