package com.example.ticket_booking_system.repository;

import com.example.ticket_booking_system.entity.SeatStatus;
import com.example.ticket_booking_system.entity.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ShowSeat s WHERE s.id IN :seatIds")
    List<ShowSeat> findByIdsForUpdate(@Param("seatIds") List<Long> seatIds);

    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = :available, s.lockedAt = null, s.booking = null WHERE s.status = :held AND s.lockedAt < :cutoffTime")
    int releaseExpiredSeats(@Param("available") SeatStatus available,
                            @Param("held") SeatStatus held,
                            @Param("cutoffTime") LocalDateTime cutoffTime);

}
