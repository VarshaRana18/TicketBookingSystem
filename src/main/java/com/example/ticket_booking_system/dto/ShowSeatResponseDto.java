package com.example.ticket_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ShowSeatResponseDto {
    private Long showSeatId;
    private int seatNumber;
    private String seatRow;
    private String category;
    private Double price;
    private String status;
}
