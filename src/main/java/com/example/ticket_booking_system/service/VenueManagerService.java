package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.ScreenRequestDto;
import com.example.ticket_booking_system.dto.ScreenResponseDto;
import com.example.ticket_booking_system.dto.TheatreRequestDto;
import com.example.ticket_booking_system.dto.TheatreResponseDto;
import com.example.ticket_booking_system.entity.Screen;
import com.example.ticket_booking_system.entity.Theatre;
import com.example.ticket_booking_system.entity.User;
import com.example.ticket_booking_system.exception.ResourceNotFoundException;
import com.example.ticket_booking_system.repository.ScreenRepository;
import com.example.ticket_booking_system.repository.TheatreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class VenueManagerService{
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;

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

    public ScreenResponseDto addScreenToTheatre(Long theatreId,ScreenRequestDto dto,User manager){
        Theatre theatre = theatreRepository.findById(theatreId).orElseThrow(() -> new ResourceNotFoundException("Theatre not found"));
        if(!theatre.getManager().getId().equals(manager.getId())){
            throw new AccessDeniedException("You do not have permission to manage this theatre");
        }
        Screen screen = new Screen();
        screen.setName(dto.getName());
        screen.setTheatre(theatre);

        Screen saved = screenRepository.save(screen);

        ScreenResponseDto res = new ScreenResponseDto();
        res.setId(saved.getId());
        res.setName(saved.getName());
        res.setTheatreId(saved.getTheatre().getId());
        res.setTheatreName(saved.getTheatre().getName());

        return res;
    }
}
