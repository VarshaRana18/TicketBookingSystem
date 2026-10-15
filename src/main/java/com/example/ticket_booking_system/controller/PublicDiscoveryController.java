package com.example.ticket_booking_system.controller;

import com.example.ticket_booking_system.dto.EventResponseDto;
import com.example.ticket_booking_system.dto.ShowResponseDto;
import com.example.ticket_booking_system.dto.ShowSeatResponseDto;
import com.example.ticket_booking_system.service.PublicDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicDiscoveryController {
    private final PublicDiscoveryService discoveryService;

    @GetMapping("/events")
    public ResponseEntity<List<EventResponseDto>> getAllEvents() {
        return ResponseEntity.ok(discoveryService.getAllEvents());
    }

    @GetMapping("/events/{eventId}/shows")
    public ResponseEntity<List<ShowResponseDto>> getUpcomingShows(@PathVariable Long eventId) {
        return ResponseEntity.ok(discoveryService.getUpcomingShowsForEvent(eventId));
    }

    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<List<ShowSeatResponseDto>> getSeatMap(@PathVariable Long showId) {
        return ResponseEntity.ok(discoveryService.getSeatMapForShow(showId));
    }

}
