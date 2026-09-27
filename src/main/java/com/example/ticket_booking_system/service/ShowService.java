package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.ShowResponseDto;
import com.example.ticket_booking_system.dto.ShowSeatResponseDto;
import com.example.ticket_booking_system.entity.Show;
import com.example.ticket_booking_system.entity.ShowSeat;
import com.example.ticket_booking_system.repository.ShowRepository;
import com.example.ticket_booking_system.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;

    // 1. Get all shows for a specific movie
    @Transactional(readOnly = true)
    public List<ShowResponseDto> getShowsByMovieId(Long movieId) {
        List<Show> shows = showRepository.findByEventId(movieId);

        return shows.stream()
                .map(this::mapToShowDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowSeatResponseDto> getSeatsForShow(Long showId) {
        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);

        return showSeats.stream()
                .map(this::mapToShowSeatDto)
                .toList();
    }


    private ShowResponseDto mapToShowDto(Show show){
        ShowResponseDto dto = new ShowResponseDto();
        dto.setShowId(show.getId());
        dto.setPrice(show.getTicketPrice());
        dto.setStartTime(show.getStartTime());

        dto.setMovieTitle(show.getEvent().getTitle());
        dto.setScreenName(show.getScreen().getName());
        dto.setTheatreName(show.getScreen().getTheatre().getName());

        return dto;
    }
    private ShowSeatResponseDto mapToShowSeatDto(ShowSeat showSeat){
        ShowSeatResponseDto dto = new ShowSeatResponseDto();
        dto.setShowSeatId(showSeat.getId());
        dto.setPrice(showSeat.getPrice());

        dto.setStatus(showSeat.getStatus().name());

        dto.setSeatRow(showSeat.getSeat().getSeatRow());
        dto.setSeatNumber(showSeat.getSeat().getSeatNumber());
        dto.setCategory(showSeat.getSeat().getCategory());

        return dto;
    }
}
