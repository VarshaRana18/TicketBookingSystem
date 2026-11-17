package com.example.ticket_booking_system.service;

import com.example.ticket_booking_system.dto.InitiateBookingRequestDto;
import com.example.ticket_booking_system.dto.SeatRequestDto;
import com.example.ticket_booking_system.entity.Booking;
import com.example.ticket_booking_system.entity.Show;
import com.example.ticket_booking_system.entity.User;
import com.example.ticket_booking_system.repository.BookingRepository;
import com.example.ticket_booking_system.repository.ShowRepository;
import com.example.ticket_booking_system.repository.TicketRepository;
import com.example.ticket_booking_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    TicketRepository ticketRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ShowRepository showRepository;
    @Mock
    BookingRepository bookingRepository;
    @Mock CacheEvictor cacheEvictor;

    BookingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
        service = new BookingService(ticketRepository, userRepository, showRepository,
                bookingRepository,  cacheEvictor,clock);
        ReflectionTestUtils.setField(service, "HOLD_DURATION_MINUTES", 10);
        ReflectionTestUtils.setField(service, "MAX_SEATS_PER_BOOKING", 6);
    }

    @Test
    void initiateBooking_rejectsEmptySeatList() {
        InitiateBookingRequestDto dto = new InitiateBookingRequestDto();
        dto.setShowId(1L);
        dto.setSeats(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.initiateBooking(dto, 1L));
    }

    @Test
    void confirmPayment_failsWhenHoldExpiredOrAlreadyProcessed() {
        User owner = new User();
        owner.setId(1L);
        Booking booking = new Booking();
        booking.setUser(owner);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(bookingRepository.confirmIfStillValid(eq(5L), any())).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> service.confirmPayment(5L, 1L));
        verify(ticketRepository, never()).updateStatusByBookingId(any(), any());
    }

    @Test
    void initiateBooking_rejectsWhenExceedingMaxSeats(){
        InitiateBookingRequestDto dto = new InitiateBookingRequestDto();
        dto.setShowId(1L);
        ReflectionTestUtils.setField(service, "MAX_SEATS_PER_BOOKING", 1);
        dto.setSeats(List.of(
                new SeatRequestDto("A",1),
                new SeatRequestDto("A",2),
                new SeatRequestDto("A",3)));

        assertThrows(IllegalArgumentException.class, () -> service.initiateBooking(dto, 1L));
    }

    @Test
    void initiateBooking_failsWhenDuplicateSeatsRequested(){
        InitiateBookingRequestDto dto = new InitiateBookingRequestDto();
        List<SeatRequestDto> seats = List.of(
                new SeatRequestDto("A", 1),
                new SeatRequestDto("A", 1),
                new SeatRequestDto("A", 2)
        );
        dto.setShowId(1L);
        dto.setSeats(seats);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.initiateBooking(dto, 1L)
        );

        assertEquals("Duplicate seats found in the request payload.", exception.getMessage());
    }

    @Test
    void initiateBooking_failsWhenShowAlreadyStarted(){
        InitiateBookingRequestDto dto = new InitiateBookingRequestDto();
        dto.setShowId(1L);
        dto.setSeats(List.of(new SeatRequestDto("A", 1)));

        User fakeUser = new User();
        fakeUser.setId(1L);

        Show fakeShow = new Show();
        fakeShow.setId(1L);
        // Your test Clock is fixed at 10:00:00Z. We set the show to 09:00:00Z (1 hour ago)
        // (Note: Change setStartTime to whatever your Show entity actually uses, like setTime or setShowTime)
        fakeShow.setStartTime(LocalDateTime.parse("2026-11-20T09:00:00"));

        // Train the mocks to return our fake data
        when(userRepository.findById(1L)).thenReturn(Optional.of(fakeUser));
        when(showRepository.findById(1L)).thenReturn(Optional.of(fakeShow));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.initiateBooking(dto, 1L)
        );

        // Verify the exact error message to prove it failed for the right reason
        assertEquals("Tickets can no longer be booked for a show that has already started.", exception.getMessage());assertThrows(IllegalStateException.class,() -> service.initiateBooking(dto, 1L));
    }
}
