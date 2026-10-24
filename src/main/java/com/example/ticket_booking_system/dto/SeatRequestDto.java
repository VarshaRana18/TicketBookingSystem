package com.example.ticket_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatRequestDto {
    private String seatRow;
    private Integer seatNumber;
}