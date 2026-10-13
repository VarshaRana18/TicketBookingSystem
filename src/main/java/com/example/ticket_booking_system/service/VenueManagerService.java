package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.ScreenRequestDto;
import com.example.ticket_booking_system.dto.ScreenResponseDto;
import com.example.ticket_booking_system.dto.TheatreRequestDto;
import com.example.ticket_booking_system.dto.TheatreResponseDto;
import com.example.ticket_booking_system.entity.Screen;
import com.example.ticket_booking_system.entity.Seat;
import com.example.ticket_booking_system.entity.Theatre;
import com.example.ticket_booking_system.entity.User;
import com.example.ticket_booking_system.exception.ResourceNotFoundException;
import com.example.ticket_booking_system.repository.ScreenRepository;
import com.example.ticket_booking_system.repository.SeatRepository;
import com.example.ticket_booking_system.repository.TheatreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueManagerService{
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;

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

        List<Seat> seatsToSave = new ArrayList<>();

        for (int i = 0; i < dto.getNumRows(); i++) {
            char rowLetter = (char) ('A' + i);

            // Loop through seat numbers (1, 2, 3...)
            for (int j = 1; j <= dto.getSeatsPerRow(); j++) {
                Seat seat = new Seat();
                seat.setSeatRow(String.valueOf(rowLetter));
                seat.setSeatNumber(j);
                seat.setScreen(saved);

                if (i >= dto.getNumRows() - 2) {
                    seat.setCategory("PREMIUM");
                } else {
                    seat.setCategory("STANDARD");
                }

                seatsToSave.add(seat);
            }
        }

        // Save all generated seats to the database
        seatRepository.saveAll(seatsToSave);

        ScreenResponseDto res = new ScreenResponseDto();
        res.setId(saved.getId());
        res.setName(saved.getName());
        res.setTheatreId(saved.getTheatre().getId());
        res.setTheatreName(saved.getTheatre().getName());

        return res;
    }
}
