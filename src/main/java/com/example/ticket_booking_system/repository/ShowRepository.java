package com.example.ticket_booking_system.repository;

import com.example.ticket_booking_system.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show,Long> {
    List<Show> findByEventId(Long eventId);
}
