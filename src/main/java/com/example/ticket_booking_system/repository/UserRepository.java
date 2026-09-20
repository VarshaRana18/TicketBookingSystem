package com.example.ticket_booking_system.repository;

import com.example.ticket_booking_system.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
//    @PersistenceContext
//    private EntityManager entityManager;
    boolean existsByEmail(String email);

}
