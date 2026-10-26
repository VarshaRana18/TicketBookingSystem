package com.example.ticket_booking_system.repository;

import com.example.ticket_booking_system.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking,Long> {
    List<Booking> findByUserIdOrderByBookingTimeDesc(Long userId);
    @Modifying
    @Query("UPDATE Booking b SET b.paymentStatus = 'FAILED' WHERE b.paymentStatus = 'PENDING' AND b.expiresAt < :now")
    int failExpiredBookings(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Booking b SET b.paymentStatus = 'SUCCESS' WHERE b.id = :id AND b.paymentStatus = 'PENDING' AND b.expiresAt >= :now")
    int confirmIfStillValid(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Booking b SET b.paymentStatus = 'FAILED' WHERE b.id = :id AND b.paymentStatus = 'PENDING'")
    int cancelIfStillPending(@Param("id") Long id);
}
