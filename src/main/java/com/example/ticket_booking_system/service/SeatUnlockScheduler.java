package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.entity.Booking;
import com.example.ticket_booking_system.entity.PaymentStatus;
import com.example.ticket_booking_system.entity.SeatStatus;
import com.example.ticket_booking_system.entity.ShowSeat;
import com.example.ticket_booking_system.repository.BookingRepository;
import com.example.ticket_booking_system.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatUnlockScheduler {
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 60000) //Runs Every 60,000 milliseconds (1 minute)
    @Transactional
    public void unlockExpiredSeats() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(10);

        List<ShowSeat> expiredSeats = showSeatRepository.findByStatusAndLockedAtBefore(SeatStatus.HELD, cutoffTime);

        if (expiredSeats.isEmpty()) {
            return;
        }
        int failedBookings = 0;

        for (ShowSeat seat : expiredSeats) {
            // 2. Fail the associated booking
            Booking booking = seat.getBooking();
            if (booking != null && booking.getPaymentStatus() == PaymentStatus.PENDING) {
                booking.setPaymentStatus(PaymentStatus.FAILED);
                bookingRepository.save(booking);
                failedBookings++;
            }

            // 3. Release the seat
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setLockedAt(null);
            seat.setBooking(null);
        }

        int releasedCount = showSeatRepository.releaseExpiredSeats(
                SeatStatus.AVAILABLE,
                SeatStatus.HELD,
                cutoffTime
        );

        showSeatRepository.saveAll(expiredSeats);
        log.info("Released {} expired seats and automatically failed {} pending bookings.",
                expiredSeats.size(), failedBookings);
    }
}
