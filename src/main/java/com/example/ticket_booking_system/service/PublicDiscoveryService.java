package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.EventResponseDto;
import com.example.ticket_booking_system.dto.ShowResponseDto;
import com.example.ticket_booking_system.dto.ShowSeatResponseDto;
import com.example.ticket_booking_system.entity.Show;
import com.example.ticket_booking_system.entity.Ticket;
import com.example.ticket_booking_system.exception.ResourceNotFoundException;
import com.example.ticket_booking_system.repository.EventRepository;
import com.example.ticket_booking_system.repository.ShowRepository;
import com.example.ticket_booking_system.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicDiscoveryService {
    private final EventRepository eventRepository;
    private final ShowRepository showRepository;
    private final TicketRepository ticketRepository;

    public List<EventResponseDto> getAllEvents() {
        return eventRepository.findAll().stream().map(event -> {
            EventResponseDto dto = new EventResponseDto();
            dto.setId(event.getId());
            dto.setName(event.getTitle());
            dto.setDescription(event.getDescription());
            dto.setDurationMinutes(event.getDurationInMinutes());
            return dto;
        }).toList();
    }

    public List<ShowResponseDto> getUpcomingShowsForEvent(Long eventId) {
        List<Show> shows = showRepository.findByEventIdAndStartTimeAfterOrderByStartTimeAsc(eventId, LocalDateTime.now());

        return shows.stream().map(show -> {
            ShowResponseDto dto = new ShowResponseDto();
            dto.setShowId(show.getId());
            dto.setMovieTitle(show.getEvent().getTitle());
            dto.setTheatreName(show.getScreen().getTheatre().getName());
            dto.setScreenName(show.getScreen().getName());
            dto.setStartTime(show.getStartTime());
            dto.setPrice(show.getTicketPrice());
            return dto;
        }).toList();
    }

    public List<ShowSeatResponseDto> getSeatMapForShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found."));

        int numRows = show.getScreen().getNumRows();
        int seatsPerRow = show.getScreen().getSeatsPerRow();
        Double basePrice = show.getTicketPrice();

        List<Ticket> existingTickets = ticketRepository.findByShowId(showId);

        Map<String, Ticket> ticketMap = existingTickets.stream()
                .collect(Collectors.toMap(
                        t -> t.getSeatRow() + "-" + t.getSeatNumber(),
                        t -> t
                ));

        List<ShowSeatResponseDto> seatMap = new ArrayList<>();

        for (int r = 1; r <= numRows; r++) {
            // Converts 1 -> A, 2 -> B, 3 -> C...
            String rowLetter = String.valueOf((char) ('A' + (r - 1)));

            for (int s = 1; s <= seatsPerRow; s++) {
                String coordinateKey = rowLetter + "-" + s;
                Ticket ticket = ticketMap.get(coordinateKey);

                ShowSeatResponseDto dto = new ShowSeatResponseDto();
                dto.setSeatRow(rowLetter);
                dto.setSeatNumber(s);
                dto.setCategory("STANDARD");

                if (ticket != null) {
                    // Seat is taken: map the database values
                    dto.setPrice(ticket.getPrice());
                    dto.setStatus(ticket.getStatus().name());
                } else {
                    // Seat is empty: map the virtual available state
                    dto.setPrice(basePrice);
                    dto.setStatus("AVAILABLE");
                }

                seatMap.add(dto);
            }
        }

        return seatMap;
    }

    @Transactional(readOnly = true)
    public List<ShowResponseDto> getShowsByMovieId(Long movieId) {
        List<Show> shows = showRepository.findByEventId(movieId);


        return shows.stream()
                .map((show) -> {
                    ShowResponseDto dto = new ShowResponseDto();
                    dto.setShowId(show.getId());
                    dto.setPrice(show.getTicketPrice());
                    dto.setStartTime(show.getStartTime());

                    dto.setMovieTitle(show.getEvent().getTitle());
                    dto.setScreenName(show.getScreen().getName());
                    dto.setTheatreName(show.getScreen().getTheatre().getName());

                    return dto;
                })
                .toList();
    }

}
