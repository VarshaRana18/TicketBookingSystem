package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.AuthenticationRequestDto;
import com.example.ticket_booking_system.dto.AuthenticationResponseDto;
import com.example.ticket_booking_system.entity.User;
import com.example.ticket_booking_system.repository.UserRepository;
import com.example.ticket_booking_system.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponseDto authenticate(AuthenticationRequestDto request) {
        // 1. Spring Security verifies the raw password against the BCrypt hash in the DB
        // If the password is wrong, this will immediately throw a BadCredentialsException
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        String jwtToken = jwtService.generateToken(user);

        return new AuthenticationResponseDto(jwtToken);
    }
}
