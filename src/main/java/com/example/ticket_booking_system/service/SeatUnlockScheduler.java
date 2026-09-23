package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.entity.SeatStatus;
import com.example.ticket_booking_system.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatUnlockScheduler {
    private final ShowSeatRepository showSeatRepository;

    @Scheduled(fixedRate = 60000) //Runs Every 60,000 milliseconds (1 minute)
    @Transactional
    public void unlockExpiredSeats() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(10);

        int releasedCount = showSeatRepository.releaseExpiredSeats(
                SeatStatus.AVAILABLE,
                SeatStatus.HELD,
                cutoffTime
        );

        if (releasedCount > 0) {
            log.info("Released {} expired seat locks back to AVAILABLE.", releasedCount);
        }
    }
}
