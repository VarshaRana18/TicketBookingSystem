package com.example.ticket_booking_system.controller;

import com.example.ticket_booking_system.dto.CreateUserRequestDto;
import com.example.ticket_booking_system.dto.CreateUserResponseDto;
import com.example.ticket_booking_system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService){
        this.userService =  userService;
    }

    @PostMapping
    public ResponseEntity<CreateUserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto dto){
        return ResponseEntity.ok(userService.addUser(dto));
    }
}
