package com.example.ticket_booking_system.controller;

import com.example.ticket_booking_system.dto.EventRequestDto;
import com.example.ticket_booking_system.dto.EventResponseDto;
import com.example.ticket_booking_system.entity.Event;
import com.example.ticket_booking_system.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    @PostMapping("/events")
    public ResponseEntity<EventResponseDto> createEvent(@Valid @RequestBody EventRequestDto dto){
        EventResponseDto savedEvent = adminService.createEvent(dto);
        return new ResponseEntity<>(savedEvent, HttpStatus.CREATED);
    }
}
