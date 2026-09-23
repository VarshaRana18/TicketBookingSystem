package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.entity.SeatStatus;
import com.example.ticket_booking_system.entity.ShowSeat;
import com.example.ticket_booking_system.repository.ShowSeatRepository;
import com.example.ticket_booking_system.exception.ShowSeatNotAvailableException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final ShowSeatRepository showSeatRepository;

    @Transactional
    public void lockSeatsForBooking(List<Long> showSeatIds){
        List<ShowSeat> selectedSeats = showSeatRepository.findByIdsForUpdate(showSeatIds);

        if (selectedSeats.size() != showSeatIds.size()) {
            throw new IllegalArgumentException("One or more invalid seat IDs provided.");
        }

        for(ShowSeat seat: selectedSeats){
            if(seat.getStatus() != SeatStatus.AVAILABLE){
                throw new ShowSeatNotAvailableException("Seat ID " + seat.getId() + " is no longer available.");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for(ShowSeat seat: selectedSeats){
            seat.setStatus(SeatStatus.HELD);
            seat.setLockedAt(now);
        }

        showSeatRepository.saveAll(selectedSeats);
    }


}
