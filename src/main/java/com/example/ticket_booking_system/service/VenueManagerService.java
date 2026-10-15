package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.*;
import com.example.ticket_booking_system.entity.*;
import com.example.ticket_booking_system.exception.ResourceNotFoundException;
import com.example.ticket_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueManagerService{
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

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

    @Transactional
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

    @Transactional
    public ShowResponseDto scheduleShow(Long screenId, ShowRequestDto dto, User manager){
        Screen screen = screenRepository.findById(screenId).orElseThrow(()-> new ResourceNotFoundException("Screen not found"));
        if(!screen.getTheatre().getManager().getId().equals(manager.getId())){
            throw new AccessDeniedException("You do not own this screen");
        }
        Event event = eventRepository.findById(dto.getEventId()).orElseThrow(()-> new ResourceNotFoundException("Event not found"));

        Show show = new Show();
        show.setScreen(screen);
        show.setEvent(event);
        show.setStartTime(dto.getStartTime());
        show.setEndTime(dto.getEndTime());
        show.setTicketPrice(dto.getTicketPrice());

        Show savedShow = showRepository.save(show);

        List<Seat> physicalSeats = seatRepository.findByScreenId(screenId);
        List<ShowSeat> showSeatsToSave = new ArrayList<>();

        for(Seat physicalSeat : physicalSeats){
            ShowSeat showSeat = new ShowSeat();
            showSeat.setShow(savedShow);
            showSeat.setSeat(physicalSeat);
            showSeat.setStatus(SeatStatus.AVAILABLE);

            if ("PREMIUM".equals(physicalSeat.getCategory())) {
                showSeat.setPrice(dto.getTicketPrice() + 50.0); // Premium markup
            } else {
                showSeat.setPrice(dto.getTicketPrice());
            }

            showSeatsToSave.add(showSeat);
        }

        showSeatRepository.saveAll(showSeatsToSave);

        ShowResponseDto res = new ShowResponseDto();
        res.setShowId(savedShow.getId());
        res.setMovieTitle(savedShow.getEvent().getTitle());
        res.setPrice(savedShow.getTicketPrice());
        res.setTheatreName(savedShow.getScreen().getTheatre().getName());
        res.setScreenName(savedShow.getScreen().getName());
        res.setStartTime(savedShow.getStartTime());

        return res;
    }

    public VenueDashboardResponseDto getManagerDashboard(User manager){
        List<Theatre> managerTheatres = theatreRepository.findByManagerId(manager.getId());

        double grandTotalRevenue = 0.0;
        long grandTotalTickets = 0L;
        List<TheatreStatsDto> theatreStatsList = new ArrayList<>();

        for(Theatre theatre : managerTheatres){
            Double theatreRevenue = showSeatRepository.calculateTotalRevenueByTheatreId(theatre.getId());
            Long theatreTickets = showSeatRepository.countTicketsSoldByTheatreId(theatre.getId());

            grandTotalRevenue += theatreRevenue;
            grandTotalTickets += theatreTickets;

            theatreStatsList.add(new TheatreStatsDto(
                    theatre.getId(),
                    theatre.getName(),
                    theatreRevenue,
                    theatreTickets
            ));
        }

        VenueDashboardResponseDto dashboard = new VenueDashboardResponseDto();
        dashboard.setTotalPlatformRevenue(grandTotalRevenue);
        dashboard.setTotalTicketsSold(grandTotalTickets);
        dashboard.setTheatreBreakdown(theatreStatsList);

        return dashboard;
    }
}
