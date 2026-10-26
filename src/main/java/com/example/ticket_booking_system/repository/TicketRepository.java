package com.example.ticket_booking_system.repository;

import com.example.ticket_booking_system.entity.SeatStatus;
import com.example.ticket_booking_system.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket,Long> {
    @Query("SELECT COALESCE(SUM(t.price), 0.0) FROM Ticket t WHERE t.status = 'BOOKED' AND t.show.screen.theatre.id = :theatreId")
    Double calculateTotalRevenueByTheatreId(@Param("theatreId") Long theatreId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'BOOKED' AND t.show.screen.theatre.id = :theatreId")
    Long countTicketsSoldByTheatreId(@Param("theatreId") Long theatreId);

    @Modifying
    @Query("DELETE FROM Ticket t WHERE t.booking.id = :bookingId")
    int deleteByBookingId(@Param("bookingId") Long bookingId);

    @Modifying
    @Query("DELETE FROM Ticket t WHERE t.status = 'HELD' AND t.booking.id IN (SELECT b.id FROM Booking b WHERE b.paymentStatus = 'FAILED')")
    int deleteTicketsForFailedBookings();

    List<Ticket> findByStatusAndLockedAtBefore(SeatStatus seatStatus, LocalDateTime cutoffTime);

    List<Ticket> findByShowId(Long showId);

    List<Ticket> findByBookingId(Long bookingId);
}
