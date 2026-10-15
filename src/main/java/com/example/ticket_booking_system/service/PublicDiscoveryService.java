package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.EventResponseDto;
import com.example.ticket_booking_system.dto.ShowResponseDto;
import com.example.ticket_booking_system.dto.ShowSeatResponseDto;
import com.example.ticket_booking_system.entity.Show;
import com.example.ticket_booking_system.entity.ShowSeat;
import com.example.ticket_booking_system.exception.ResourceNotFoundException;
import com.example.ticket_booking_system.repository.EventRepository;
import com.example.ticket_booking_system.repository.ShowRepository;
import com.example.ticket_booking_system.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicDiscoveryService {
    private final EventRepository eventRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

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
            dto.setEndTime(show.getEndTime());
            dto.setPrice(show.getTicketPrice());
            return dto;
        }).toList();
    }

    public List<ShowSeatResponseDto> getSeatMapForShow(Long showId) {
        List<ShowSeat> seats = showSeatRepository.findByShowId(showId);

        if (seats.isEmpty()) {
            throw new ResourceNotFoundException("No seats found or show does not exist.");
        }

        return seats.stream().map(seat -> {
            ShowSeatResponseDto dto = new ShowSeatResponseDto();
            dto.setShowSeatId(seat.getId());
            dto.setSeatRow(seat.getSeat().getSeatRow());
            dto.setSeatNumber(seat.getSeat().getSeatNumber());
            dto.setCategory(seat.getSeat().getCategory());
            dto.setPrice(seat.getPrice());
            dto.setStatus(seat.getStatus().name());
            return dto;
        }).toList();
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

    @Transactional(readOnly = true)
    public List<ShowSeatResponseDto> getSeatsForShow(Long showId) {
        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);

        return showSeats.stream()
                .map((showSeat)->{
                    ShowSeatResponseDto dto = new ShowSeatResponseDto();
                    dto.setShowSeatId(showSeat.getId());
                    dto.setPrice(showSeat.getPrice());

                    dto.setStatus(showSeat.getStatus().name());

                    dto.setSeatRow(showSeat.getSeat().getSeatRow());
                    dto.setSeatNumber(showSeat.getSeat().getSeatNumber());
                    dto.setCategory(showSeat.getSeat().getCategory());

                    return dto;
                })
                .toList();
    }

}
