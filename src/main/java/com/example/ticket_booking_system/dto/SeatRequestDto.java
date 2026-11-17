package com.example.ticket_booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SeatRequestDto {
    private String seatRow;
    private Integer seatNumber;
}