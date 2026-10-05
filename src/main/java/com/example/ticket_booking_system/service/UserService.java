package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.CreateUserRequestDto;
import com.example.ticket_booking_system.dto.CreateUserResponseDto;
import com.example.ticket_booking_system.entity.User;
import com.example.ticket_booking_system.exception.DuplicateEmailsException;
import com.example.ticket_booking_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    UserService(UserRepository userRepository,PasswordEncoder passwordEncoder){
        this.userRepository =  userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CreateUserResponseDto addUser(CreateUserRequestDto dto){
        if(userRepository.existsByEmail(dto.getEmail())){
            throw new DuplicateEmailsException("A user with this email already exists");
        }
        User user = mapToUser(dto);
        return mapToDto(userRepository.save(user));
    }

    private User mapToUser(CreateUserRequestDto dto){
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        return user;
    }

    private CreateUserResponseDto mapToDto(User user){
        CreateUserResponseDto dto = new CreateUserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }
}
