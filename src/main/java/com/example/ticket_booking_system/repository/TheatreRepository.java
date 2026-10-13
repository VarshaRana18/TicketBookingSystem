package com.example.ticket_booking_system.repository;

import com.example.ticket_booking_system.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre,Long> {
    List<Theatre> findByManagerId(Long managerId);
}
