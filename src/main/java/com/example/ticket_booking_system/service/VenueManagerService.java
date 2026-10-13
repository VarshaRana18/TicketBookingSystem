package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.TheatreRequestDto;
import com.example.ticket_booking_system.dto.TheatreResponseDto;
import com.example.ticket_booking_system.entity.Theatre;
import com.example.ticket_booking_system.entity.User;
import com.example.ticket_booking_system.repository.TheatreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VenueManagerService{
    private final TheatreRepository theatreRepository;

    public TheatreResponseDto createTheatre(TheatreRequestDto dto, User currentManager) {
        Theatre theatre = new Theatre();
        theatre.setName(dto.getName());
        theatre.setLocation(dto.getLocation());
        theatre.setManager(currentManager);

        Theatre savedTheatre = theatreRepository.save(theatre);

        TheatreResponseDto response = new TheatreResponseDto();
        response.setId(savedTheatre.getId());
        response.setName(savedTheatre.getName());
        response.setLocation(savedTheatre.getLocation());
        response.setManagerId(savedTheatre.getManager().getId());
        response.setManagerName(savedTheatre.getManager().getName());

        return response;
    }
}
