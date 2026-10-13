package com.example.ticket_booking_system.dto;

import com.example.ticket_booking_system.entity.Seat;
import com.example.ticket_booking_system.entity.Theatre;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ScreenResponseDto {
    private Long id;
    private String name;
    private Long theatreId;
    private String theatreName;
}
