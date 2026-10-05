package com.example.ticket_booking_system.config;
import com.example.ticket_booking_system.entity.*;
import com.example.ticket_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception{
        if (theatreRepository.count() > 0) {
            log.info("Database already contains data. Skipping Data Seeder.");
            return;
        }

        log.info("Starting database seeding process...");
        User testUser = new User();
        testUser.setName("Test Customer");
        testUser.setEmail("customer@test.com");
        testUser.setPassword("password123");
        testUser.setRole(Role.CUSTOMER);
        userRepository.save(testUser);

        Theatre bansal = new Theatre(null, "Bansal Cineplex", "Gotri Road", null);
        Theatre inox = new Theatre(null, "Inox", "Race Course Circle", null);
        theatreRepository.saveAll(List.of(bansal, inox));

        Screen bansalS1 = new Screen(null, "S1", bansal, null);
        Screen bansalS2 = new Screen(null, "S2", bansal, null);
        Screen inoxS1 = new Screen(null, "S1", inox, null);
        List<Screen> savedScreens = screenRepository.saveAll(List.of(bansalS1, bansalS2, inoxS1));

        for (Screen screen : savedScreens) {
            generateSeatsForScreen(screen);
        }

        Event movie1 = new Event(null, "Inception", "A thief who steals corporate secrets through dream-sharing technology.", 148, "English");
        Event movie2 = new Event(null, "Interstellar", "A team of explorers travel through a wormhole in space.", 169, "English");
        eventRepository.saveAll(List.of(movie1, movie2));

        LocalDateTime tonight = LocalDateTime.now().withHour(19).withMinute(0).withSecond(0).withNano(0);

        Show show1 = createShow(movie1, bansalS1, tonight, 200.0);
        Show show2 = createShow(movie2, inoxS1, tonight.plusHours(1), 250.0);

        log.info("Database seeding completed successfully! Ready for testing.");
    }

    private void generateSeatsForScreen(Screen screen) {
        List<Seat> seats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E"};

        for (String row : rows) {
            // Assign categories based on your initial rule:
            // A & B = SILVER, C & D = GOLD, E (Single back row) = PREMIUM
            String category;
            if (row.equals("A") || row.equals("B")) {
                category = "SILVER";
            } else if (row.equals("C") || row.equals("D")) {
                category = "GOLD";
            } else {
                category = "PREMIUM";
            }

            for (int seatNum = 1; seatNum <= 10; seatNum++) {
                Seat seat = new Seat();
                seat.setSeatRow(row);
                seat.setSeatNumber(seatNum);
                seat.setCategory(category);
                seat.setScreen(screen);
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);
    }

    private Show createShow(Event event, Screen screen, LocalDateTime startTime, Double basePrice) {
        Show show = new Show();
        show.setEvent(event);
        show.setScreen(screen);
        show.setStartTime(startTime);
        show.setEndTime(startTime.plusMinutes(event.getDurationInMinutes()));
        show.setTicketPrice(basePrice);

        Show savedShow = showRepository.save(show);

        // Fetch all physical seats belonging to this screen
        List<Seat> physicalSeats = seatRepository.findByScreenId(screen.getId());
        List<ShowSeat> showSeats = new ArrayList<>();

        // Create a ShowSeat inventory record for every physical chair
        for (Seat seat : physicalSeats) {
            ShowSeat showSeat = new ShowSeat();
            showSeat.setShow(savedShow);
            showSeat.setSeat(seat);
            showSeat.setStatus(SeatStatus.AVAILABLE);

            // Calculate dynamic pricing based on seat category
            if (seat.getCategory().equals("PREMIUM")) {
                showSeat.setPrice(basePrice + 150.0);
            } else if (seat.getCategory().equals("GOLD")) {
                showSeat.setPrice(basePrice + 50.0);
            } else {
                showSeat.setPrice(basePrice); // SILVER gets base price
            }

            showSeats.add(showSeat);
        }

        showSeatRepository.saveAll(showSeats);
        return savedShow;
    }
}
