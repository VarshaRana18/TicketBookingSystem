package com.example.ticket_booking_system.repository;

import com.example.ticket_booking_system.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event,Long> {
    Optional<Event> findById(Long aLong);
}
