package com.example.ticket_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class ShowSeatResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private int seatNumber;
    private String seatRow;
    private String category;
    private Double price;
    private String status;
}
