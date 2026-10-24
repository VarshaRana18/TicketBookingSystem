package com.example.ticket_booking_system.dto;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ShowResponseDto {
    private Long showId;
    private String movieTitle;
    private String theatreName;
    private String screenName;
    private LocalDateTime startTime;
    private Double price;
}



