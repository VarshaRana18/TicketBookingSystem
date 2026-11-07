package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.entity.PaymentStatus;
import com.example.ticket_booking_system.entity.SeatStatus;
import com.example.ticket_booking_system.repository.BookingRepository;
import com.example.ticket_booking_system.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatUnlockScheduler {
    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final SeatMapCacheEvictor seatMapCacheEvictor;
    private final Clock clock;

    @Scheduled(fixedDelay = 60000) //Changing fixedRate to fixedDelay ensures the next 60-second countdown doesn't start until the current execution finishes, preventing overlapping sweeps.
    @Transactional
    public void unlockExpiredSeats() {
        LocalDateTime now = LocalDateTime.now(clock);

        int failedBookings = bookingRepository.failExpiredBookings(now);
        List<Long> affectedShowIds = ticketRepository.findShowIdsOfFailedBookings(SeatStatus.HELD,PaymentStatus.FAILED);
        int releasedTickets = ticketRepository.deleteTicketsForFailedBookings(SeatStatus.HELD ,PaymentStatus.FAILED);

        seatMapCacheEvictor.evictAfterCommit(affectedShowIds);

        if (failedBookings > 0 || releasedTickets > 0) {
            log.info("Sweeper: Failed {} expired bookings and released {} held tickets.", failedBookings, releasedTickets);
        }
    }
}
