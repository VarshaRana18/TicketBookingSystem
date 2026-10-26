package com.example.ticket_booking_system;

import com.example.ticket_booking_system.dto.InitiateBookingRequestDto;
import com.example.ticket_booking_system.dto.InitiateBookingResponseDto;
import com.example.ticket_booking_system.dto.SeatRequestDto;
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
public class DoubleClickIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Test
    public void testDoubleClickPaymentConfirmation() throws InterruptedException {
        Long userId = 3L; // User 1 created by DataSeeder

        // 1. SETUP: Create a real, fresh PENDING booking specifically for this test
        InitiateBookingRequestDto request = new InitiateBookingRequestDto();
        request.setShowId(5L); // Show 1 created by DataSeeder
        SeatRequestDto seat = new SeatRequestDto();
        seat.setSeatRow("A"); // Using Row D to avoid conflicts with previous tests
        seat.setSeatNumber(3);
        request.setSeats(List.of(seat));

        InitiateBookingResponseDto initResponse = bookingService.initiateBooking(request, userId);
        final Long targetBookingId = initResponse.getBookingId();

        // 2. THE RACE: Simulate the impatient user double-clicking "Pay"
        int concurrentClicks = 2;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentClicks);
        CountDownLatch readyCounter = new CountDownLatch(concurrentClicks);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finishedCounter = new CountDownLatch(concurrentClicks);

        AtomicInteger successfulCharges = new AtomicInteger(0);
        AtomicInteger blockedCharges = new AtomicInteger(0);

        for (int i = 0; i < concurrentClicks; i++) {
            executor.submit(() -> {
                try {
                    readyCounter.countDown();
                    startSignal.await(); // Hold threads at the starting line

                    // Both threads attempt to confirm the EXACT same booking ID at the exact same millisecond
                    bookingService.confirmPayment(targetBookingId, userId);
                    successfulCharges.incrementAndGet();

                } catch (Exception e) {
                    blockedCharges.incrementAndGet();
                } finally {
                    finishedCounter.countDown();
                }
            });
        }

        readyCounter.await();
        startSignal.countDown(); // FIRE!
        finishedCounter.await();

        // PROOF: Only 1 click processed, the second was blocked by the database @Version lock
        assertEquals(1, successfulCharges.get());
        assertEquals(1, blockedCharges.get());

        executor.shutdown();
    }
}