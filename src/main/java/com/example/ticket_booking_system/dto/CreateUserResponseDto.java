package com.example.ticket_booking_system.dto;

import com.example.ticket_booking_system.entity.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserResponseDto {
    private Long id;

    private String name;

    private String email;

    private Role role;
}
