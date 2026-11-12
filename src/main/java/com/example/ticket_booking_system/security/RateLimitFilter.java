package com.example.ticket_booking_system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimiter rateLimiter;

    @Value("${ratelimit.enabled:true}")
    private boolean enabled;
    @Value("${ratelimit.auth-per-minute:10}")
    private int authLimit;
    @Value("${ratelimit.booking-per-minute:30}")
    private int bookingLimit;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (enabled) {
            String path = request.getRequestURI();
            String key = null;
            int limit = 0;

            if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
                key = "auth:" + request.getRemoteAddr();
                limit = authLimit;
            } else if (path.equals("/api/bookings/initiate")) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
                    key = "booking:" + auth.getName();
                    limit = bookingLimit;
                }
            }

            if (key != null && !rateLimiter.tryAcquire(key, limit, 60_000)) {
                response.setStatus(429);
                response.setHeader("Retry-After", "60");
                response.setContentType("application/problem+json");
                response.getWriter().write(
                        "{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"Too many requests. Please try again later.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
