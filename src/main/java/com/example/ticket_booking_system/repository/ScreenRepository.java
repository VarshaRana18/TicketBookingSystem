package com.example.ticket_booking_system.repository;

import com.example.ticket_booking_system.entity.Booking;
import com.example.ticket_booking_system.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScreenRepository extends JpaRepository<Screen,Long> {
}
