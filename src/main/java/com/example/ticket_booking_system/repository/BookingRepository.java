package com.example.ticket_booking_system.repository;

import com.example.ticket_booking_system.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking,Long> {
    List<Booking> findByUserIdOrderByBookingTimeDesc(Long userId);
}
