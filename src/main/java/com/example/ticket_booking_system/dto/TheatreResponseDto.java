package com.example.ticket_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TheatreResponseDto {
    private Long id;
    private String name;
    private String location;
    private Long managerId;
    private String managerName;
}
