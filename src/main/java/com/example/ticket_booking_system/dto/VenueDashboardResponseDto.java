package com.example.ticket_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VenueDashboardResponseDto {
    private double totalPlatformRevenue;
    private long totalTicketsSold;
    private List<TheatreStatsDto> theatreBreakdown;
}
