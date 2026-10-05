package com.example.ticket_booking_system.controller;

import com.example.ticket_booking_system.dto.AuthenticationRequestDto;
import com.example.ticket_booking_system.dto.AuthenticationResponseDto;
import com.example.ticket_booking_system.dto.CreateUserRequestDto;
import com.example.ticket_booking_system.dto.CreateUserResponseDto;
import com.example.ticket_booking_system.service.AuthenticationService;
import com.example.ticket_booking_system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService service;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseDto> authenticate(
            @RequestBody AuthenticationRequestDto request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("/register")
    public ResponseEntity<CreateUserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto dto){
        return ResponseEntity.ok(userService.addUser(dto));
    }
}
