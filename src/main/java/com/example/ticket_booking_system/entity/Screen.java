package com.example.ticket_booking_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "screens", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "theatre_id"})
})
public class Screen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theatre_id" , nullable = false)
    private Theatre theatre;

    @Column(nullable = false)
    private Integer numRows;

    @Column(nullable = false)
    private Integer seatsPerRow;
}
