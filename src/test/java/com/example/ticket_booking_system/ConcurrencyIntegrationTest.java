package com.example.ticket_booking_system;

import com.example.ticket_booking_system.dto.InitiateBookingRequestDto;
import com.example.ticket_booking_system.dto.SeatRequestDto;
import com.example.ticket_booking_system.exception.ShowSeatNotAvailableException;
import com.example.ticket_booking_system.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ConcurrencyIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Test
    public void testConcurrentBookingForSameSeat() throws InterruptedException {
        int numberOfConcurrentUsers = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentUsers);
        CountDownLatch readyThreadCounter = new CountDownLatch(numberOfConcurrentUsers);
        CountDownLatch callingThreadBlocker = new CountDownLatch(1);
        CountDownLatch completedThreadCounter = new CountDownLatch(numberOfConcurrentUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        // Assume Show ID 1 and Users 1, 2, 3 exist in your test database
        for (int i = 3; i < numberOfConcurrentUsers+3; i++) {
            final Long userId = (long) i;
            executor.submit(() -> {
                try {
                    InitiateBookingRequestDto request = new InitiateBookingRequestDto();
                    request.setShowId(5L);

                    SeatRequestDto seat = new SeatRequestDto();
                    seat.setSeatRow("A");
                    seat.setSeatNumber(5);
                    request.setSeats(List.of(seat));

                    readyThreadCounter.countDown(); // Thread is ready
                    callingThreadBlocker.await();   // Wait for the green light

                    bookingService.initiateBooking(request, userId);
                    successCount.incrementAndGet();

                } catch (ShowSeatNotAvailableException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completedThreadCounter.countDown();
                }
            });
        }

        readyThreadCounter.await(); // Wait until all 3 threads are ready
        callingThreadBlocker.countDown(); // FIRE! Release all threads at the exact same millisecond
        completedThreadCounter.await(); // Wait for all threads to finish executing

        // PROOF: Exactly 1 user gets the seat, the other 2 are rejected by the database
        assertEquals(1, successCount.get());
        assertEquals(2, conflictCount.get());

        executor.shutdown();
    }
}