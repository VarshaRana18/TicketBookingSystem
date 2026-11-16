package com.example.ticket_booking_system.dto;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class ShowResponseDto implements Serializable {
    private Long showId;
    private String movieTitle;
    private String theatreName;
    private String screenName;
    private LocalDateTime startTime;
    private Double price;
}



