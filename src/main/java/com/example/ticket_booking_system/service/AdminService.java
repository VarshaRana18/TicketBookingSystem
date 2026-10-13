package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.EventRequestDto;
import com.example.ticket_booking_system.dto.EventResponseDto;
import com.example.ticket_booking_system.entity.Event;
import com.example.ticket_booking_system.repository.EventRepository;
import com.example.ticket_booking_system.repository.TheatreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final EventRepository eventRepository;

    public EventResponseDto createEvent(EventRequestDto dto){
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setDurationInMinutes(dto.getDurationMinutes());
        event.setLanguage(dto.getLanguage());

        Event savedEvent = eventRepository.save(event);

        EventResponseDto response = new EventResponseDto();
        response.setId(savedEvent.getId());
        response.setName(savedEvent.getTitle());
        response.setDescription(savedEvent.getDescription());
        response.setDurationMinutes(savedEvent.getDurationInMinutes());

        return response;
    }
}
