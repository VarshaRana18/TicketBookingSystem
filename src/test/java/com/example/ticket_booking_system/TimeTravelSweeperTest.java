package com.example.ticket_booking_system;

import com.example.ticket_booking_system.entity.Booking;
import com.example.ticket_booking_system.entity.PaymentStatus;
import com.example.ticket_booking_system.repository.BookingRepository;
import com.example.ticket_booking_system.repository.TicketRepository;
import com.example.ticket_booking_system.service.SeatUnlockScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class TimeTravelSweeperTest {

    @Autowired
    private SeatUnlockScheduler scheduler;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    // This overrides your TimeConfig Bean with a mock we can control
    @MockitoBean
    private Clock clock;

    @Test
    public void testSweeperDeletesExpiredHolds() {
        // 1. Freeze time at right now
        Instant now = Instant.now();
        Mockito.when(clock.instant()).thenReturn(now);
        Mockito.when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        // Assuming Booking ID 2 was just created (PENDING) with held tickets
        Long bookingId = 1L;

        // 2. Time Travel: Fast forward the system clock by exactly 11 minutes
        Instant elevenMinutesLater = now.plus(11, ChronoUnit.MINUTES);
        Mockito.when(clock.instant()).thenReturn(elevenMinutesLater);

        // 3. Manually trigger the background sweeper
        scheduler.unlockExpiredSeats();

        // 4. Assertions: Prove the database was physically cleaned
        Booking sweptBooking = bookingRepository.findById(bookingId).get();
        assertEquals(PaymentStatus.FAILED, sweptBooking.getPaymentStatus());

        boolean ticketsExist = ticketRepository.findByBookingId(bookingId).isEmpty();
        assertTrue(ticketsExist, "The physical ticket rows should be deleted, freeing up the seats.");
    }
}