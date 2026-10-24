package com.example.ticket_booking_system.config;

import com.example.ticket_booking_system.entity.*;
import com.example.ticket_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final EventRepository eventRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception{
        if (theatreRepository.count() > 0) {
            log.info("Database already contains data. Skipping Data Seeder.");
            return;
        }

        log.info("Starting database seeding process (Sparse Allocation Mode)...");

        User testUser = new User();
        testUser.setName("Test Customer");
        testUser.setEmail("customer@test.com");
        testUser.setPassword("password123");
        testUser.setRole(Role.CUSTOMER);
        userRepository.save(testUser);

        User manager = new User();
        manager.setName("Test Manager");
        manager.setEmail("manager@test.com");
        manager.setPassword("password123");
        manager.setRole(Role.VENUE_MANAGER);
        userRepository.save(manager);

        Theatre bansal = new Theatre();
        bansal.setName("Bansal Cineplex");
        bansal.setLocation("Gotri Road");
        bansal.setManager(manager);

        Theatre inox = new Theatre();
        inox.setName("Inox");
        inox.setLocation("Race Course Circle");
        inox.setManager(manager);

        theatreRepository.saveAll(List.of(bansal, inox));

        // NEW: Defining the physical layout purely through math (5 rows, 10 seats = 50 total capacity)
        Screen bansalS1 = new Screen();
        bansalS1.setName("S1");
        bansalS1.setNumRows(5);
        bansalS1.setSeatsPerRow(10);
        bansalS1.setTheatre(bansal);

        Screen bansalS2 = new Screen();
        bansalS2.setName("S2");
        bansalS2.setNumRows(5);
        bansalS2.setSeatsPerRow(10);
        bansalS2.setTheatre(bansal);

        Screen inoxS1 = new Screen();
        inoxS1.setName("S1");
        inoxS1.setNumRows(5);
        inoxS1.setSeatsPerRow(10);
        inoxS1.setTheatre(inox);

        screenRepository.saveAll(List.of(bansalS1, bansalS2, inoxS1));

        // ZERO SEAT GENERATION NEEDED!

        Event movie1 = new Event(null, "Inception", "A thief who steals corporate secrets through dream-sharing technology.", 148, "English");
        Event movie2 = new Event(null, "Interstellar", "A team of explorers travel through a wormhole in space.", 169, "English");
        eventRepository.saveAll(List.of(movie1, movie2));

        LocalDateTime tonight = LocalDateTime.now().withHour(19).withMinute(0).withSecond(0).withNano(0);

        createShow(movie1, bansalS1, tonight, 200.0);
        createShow(movie2, inoxS1, tonight.plusHours(1), 250.0);

        log.info("Database seeding completed successfully! Ready for testing.");
    }

    // Notice how clean and O(1) this method is now
    private Show createShow(Event event, Screen screen, LocalDateTime startTime, Double basePrice) {
        Show show = new Show();
        show.setEvent(event);
        show.setScreen(screen);
        show.setStartTime(startTime);
        show.setTicketPrice(basePrice);

        return showRepository.save(show);
    }
}