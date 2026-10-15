package com.example.ticket_booking_system.controller;

import com.example.ticket_booking_system.dto.*;
import com.example.ticket_booking_system.entity.Theatre;
import com.example.ticket_booking_system.entity.User;
import com.example.ticket_booking_system.service.VenueManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasRole;

@RestController
@RequestMapping("api/venue-manager")
@PreAuthorize("hasRole('VENUE_MANAGER')")
@RequiredArgsConstructor
public class VenueManagerController {
    private final VenueManagerService venueService;

    @PostMapping("/theatres")
    public ResponseEntity<TheatreResponseDto> createTheatre(@Valid @RequestBody TheatreRequestDto dto, @AuthenticationPrincipal User user){
        TheatreResponseDto savedTheatre = venueService.createTheatre(dto,user);
        return new  ResponseEntity<>(savedTheatre, HttpStatus.CREATED);
    }

    @PostMapping("/theatres/{theatreId}/screens")
    public ResponseEntity<ScreenResponseDto> addScreen(
            @PathVariable Long theatreId,
            @Valid @RequestBody ScreenRequestDto requestDto,
            @AuthenticationPrincipal User currentUser
    ) {
        return new ResponseEntity<>(venueService.addScreenToTheatre(theatreId,requestDto,currentUser), HttpStatus.CREATED);
    }

    @PostMapping("/screens/{screenId}/shows")
    public ResponseEntity<ShowResponseDto> scheduleShow(@PathVariable Long screenId, @Valid @RequestBody ShowRequestDto dto, @AuthenticationPrincipal User user){
        return new ResponseEntity<>(venueService.scheduleShow(screenId,dto,user), HttpStatus.CREATED);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<VenueDashboardResponseDto> getDashboardStats(@AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(venueService.getManagerDashboard(currentUser));
    }
}
