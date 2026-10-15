package com.example.ticket_booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TheatreStatsDto {
    private Long theatreId;
    private String theatreName;
    private double revenue;
    private long ticketsSold;
}