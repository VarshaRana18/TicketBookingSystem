package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.entity.Booking;
import com.example.ticket_booking_system.entity.PaymentStatus;
import com.example.ticket_booking_system.entity.SeatStatus;
import com.example.ticket_booking_system.entity.Ticket;
import com.example.ticket_booking_system.repository.BookingRepository;
import com.example.ticket_booking_system.repository.TicketRepository;
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
    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 60000) //Runs Every 60,000 milliseconds (1 minute)
    @Transactional
    public void unlockExpiredSeats() {
        List<Ticket> expiredTickets = ticketRepository.findExpiredHeldTickets(LocalDateTime.now());

        if (expiredTickets.isEmpty()) {
            return;
        }
        int failedBookings = 0;

        for (Ticket ticket : expiredTickets) {
            Booking booking = ticket.getBooking();
            if (booking != null && booking.getPaymentStatus() == PaymentStatus.PENDING) {
                booking.setPaymentStatus(PaymentStatus.FAILED);
                bookingRepository.save(booking);
                failedBookings++;
            }
        }

        ticketRepository.deleteAll(expiredTickets);

        log.info("Deleted {} expired tickets and automatically failed {} pending bookings.",
                expiredTickets.size(), failedBookings);

    }
}
