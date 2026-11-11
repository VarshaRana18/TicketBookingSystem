package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.EventRequestDto;
import com.example.ticket_booking_system.dto.EventResponseDto;
import com.example.ticket_booking_system.entity.Event;
import com.example.ticket_booking_system.entity.Role;
import com.example.ticket_booking_system.entity.User;
import com.example.ticket_booking_system.exception.ResourceNotFoundException;
import com.example.ticket_booking_system.repository.EventRepository;
import com.example.ticket_booking_system.repository.TheatreRepository;
import com.example.ticket_booking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
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

    @Transactional
    public String changeUserRole(Long targetUserId) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

        if (targetUser.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("An admin's role cannot be modified through the API.");
        }

        if (targetUser.getRole() == Role.VENUE_MANAGER) {
            return "User '" + targetUser.getEmail() + "' already has upgraded role.";
        }

        Role previousRole = targetUser.getRole();
        targetUser.setRole(Role.VENUE_MANAGER);

        return "Successfully updated role for user '" + targetUser.getEmail() + "' from " + previousRole + " to " + Role.VENUE_MANAGER + ".";
    }
}
