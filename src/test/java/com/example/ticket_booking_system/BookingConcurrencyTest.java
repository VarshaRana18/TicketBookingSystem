package com.example.ticket_booking_system;

import com.example.ticket_booking_system.dto.InitiateBookingRequestDto;
import com.example.ticket_booking_system.dto.SeatRequestDto;
import com.example.ticket_booking_system.entity.*;
import com.example.ticket_booking_system.exception.ShowSeatNotAvailableException;
import com.example.ticket_booking_system.repository.*;
import com.example.ticket_booking_system.service.BookingService;
import com.example.ticket_booking_system.service.SeatMapCacheEvictor;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyTest {
    @MockitoBean
    SeatMapCacheEvictor seatMapCacheEvictor;
    @Autowired
    BookingService bookingService;
    @Autowired
    TicketRepository ticketRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    TheatreRepository theatreRepository;
    @Autowired ScreenRepository screenRepository;
    @Autowired
    ShowRepository showRepository;

    @Test
    void onlyOneUserCanBookTheSameSeat() throws Exception {
        int threads = 20;
        Long showId = createTestShow();
        List<Long> userIds = createUsers(threads);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger seatTaken = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        for (Long userId : userIds) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    bookingService.initiateBooking(requestFor(showId, "A", 1), userId);
                    success.incrementAndGet();
                } catch (ShowSeatNotAvailableException e) {
                    seatTaken.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.add(t);
                }
            });
        }

        ready.await();       // wait until all 20 threads are lined up
        start.countDown();   // fire them all at once
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(1, success.get());
        assertEquals(19, seatTaken.get());
        assertTrue(unexpected.isEmpty(), "Unexpected errors: " + unexpected);
        assertEquals(1, ticketRepository.findByShowId(showId).size());
    }

    @RepeatedTest(5)
    void oppositeSeatOrderMustNotCauseUnexpectedErrors() throws Exception {
        Long showId = createTestShow();
        List<Long> users = createUsers(2);
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());
        int rounds = 25;

        for (int r = 0; r < rounds; r++) {
            String row = String.valueOf((char) ('A' + r / 5));
            int a = (r % 5) * 2 + 1;     // seats (1,2), (3,4), (5,6), (7,8), (9,10) in each row
            int b = a + 1;

            InitiateBookingRequestDto forwards = requestFor(showId, List.of(seat(row, a), seat(row, b)));
            InitiateBookingRequestDto backwards = requestFor(showId, List.of(seat(row, b), seat(row, a)));

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch start = new CountDownLatch(1);

            pool.submit(() -> attempt(start, forwards, users.get(0), unexpected));
            pool.submit(() -> attempt(start, backwards, users.get(1), unexpected));

            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        }

        assertTrue(unexpected.isEmpty(), "Unexpected errors: " + unexpected);
    }

    private void attempt(CountDownLatch start, InitiateBookingRequestDto dto,
                         Long userId, List<Throwable> unexpected) {
        try {
            start.await();
            bookingService.initiateBooking(dto, userId);
        } catch (ShowSeatNotAvailableException expected) {
            // fine: someone else got the seats first
        } catch (Throwable t) {
            unexpected.add(t);
        }
    }

    private SeatRequestDto seat(String row, int number) {
        SeatRequestDto s = new SeatRequestDto();
        s.setSeatRow(row);
        s.setSeatNumber(number);
        return s;
    }

    private InitiateBookingRequestDto requestFor(Long showId, List<SeatRequestDto> seats) {
        InitiateBookingRequestDto dto = new InitiateBookingRequestDto();
        dto.setShowId(showId);
        dto.setSeats(seats);
        return dto;
    }

    private InitiateBookingRequestDto requestFor(Long showId, String row, int number) {
        SeatRequestDto seat = new SeatRequestDto();
        seat.setSeatRow(row);
        seat.setSeatNumber(number);
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
