package com.example.ticket_booking_system;

import com.example.ticket_booking_system.dto.InitiateBookingRequestDto;
import com.example.ticket_booking_system.dto.SeatRequestDto;
import com.example.ticket_booking_system.entity.*;
import com.example.ticket_booking_system.repository.*;
import com.example.ticket_booking_system.service.BookingService;
import com.example.ticket_booking_system.service.CacheEvictor;
import com.example.ticket_booking_system.service.SeatUnlockScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
public class TimeTravelSweeperTest {
    @MockitoBean private CacheEvictor cacheEvictor;
    @Autowired
    private SeatUnlockScheduler scheduler;

    @Autowired private BookingService bookingService;

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TheatreRepository theatreRepository;
    @Autowired private ScreenRepository screenRepository;
    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // This overrides your TimeConfig Bean with a mock we can control
    @MockitoBean
    private Clock clock;

    @Test
    public void testSweeperDeletesExpiredHolds() {
        // 1. Control the clock: "now" is when the booking is created
        Instant now = Instant.now();
        Mockito.when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        Mockito.when(clock.instant()).thenReturn(now);

        // 2. Create a real pending booking with its own data
        Long showId = createTestShow();
        Long userId = createUsers(1).get(0);
        Long bookingId = bookingService
                .initiateBooking(requestFor(showId, "A", 1), userId)
                .getBookingId();

        // sanity check: before time travel it is pending and the seat is held
        assertEquals(PaymentStatus.PENDING,
                bookingRepository.findById(bookingId).orElseThrow().getPaymentStatus());
        assertEquals(1, ticketRepository.findByBookingId(bookingId).size());

        // 3. Time travel: 11 minutes later (the hold lasts 10)
        Mockito.when(clock.instant()).thenReturn(now.plus(11, ChronoUnit.MINUTES));

        // 4. Run the sweeper by hand
        scheduler.unlockExpiredSeats();

        // 5. The booking is failed and the seat is freed
        Booking swept = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(PaymentStatus.FAILED, swept.getPaymentStatus());
        assertTrue(ticketRepository.findByBookingId(bookingId).isEmpty(),
                "The held ticket rows should be deleted, freeing the seat.");
    }
    private InitiateBookingRequestDto requestFor(Long showId, String row, int number) {
        SeatRequestDto seat = new SeatRequestDto(row,number);
//        seat.setSeatRow(row);
//        seat.setSeatNumber(number);
        InitiateBookingRequestDto dto = new InitiateBookingRequestDto();
        dto.setShowId(showId);
        dto.setSeats(List.of(seat));
        return dto;
    }
    private Long createTestShow() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        User manager = createUser("manager-" + unique);

        Theatre theatre = new Theatre();
        theatre.setName("Test Theatre " + unique);
        theatre.setLocation("Test City");
        theatre.setManager(manager);
        theatre = theatreRepository.save(theatre);

        Screen screen = new Screen();
        screen.setName("Screen 1");
        screen.setTheatre(theatre);
        screen.setNumRows(5);
        screen.setSeatsPerRow(10);
        screen = screenRepository.save(screen);

        Event event = new Event();
        event.setTitle("Test Movie " + unique);
        event.setDescription("Test description");
        event.setDurationInMinutes(120);
        event.setLanguage("English");
        event = eventRepository.save(event);

        Show show = new Show();
        show.setEvent(event);
        show.setScreen(screen);
        show.setStartTime(LocalDateTime.now().plusDays(1));
        show.setTicketPrice(200.0);
        return showRepository.save(show).getId();
    }
    private List<Long> createUsers(int count) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(createUser("user-" + unique + "-" + i).getId());
        }
        return ids;
    }
    private User createUser(String key) {
        User user = new User();
        user.setName(key);
        user.setEmail(key + "@test.com");
        user.setPassword("not-used");
        user.setRole(Role.CUSTOMER);   // use any value that exists in your Role enum
        return userRepository.save(user);
    }
}