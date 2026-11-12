package com.example.ticket_booking_system.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RateLimiter {
    private record Window(long startMillis, int count) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** Returns true if the request is allowed, false if the limit is exceeded. */
    public boolean tryAcquire(String key, int limit, long windowMillis) {
        long now = System.currentTimeMillis();
        AtomicBoolean allowed = new AtomicBoolean(false);

        windows.compute(key, (k, w) -> {          // atomic per key
            if (w == null || now - w.startMillis() >= windowMillis) {
                allowed.set(true);
                return new Window(now, 1);        // start a fresh window
            }
            if (w.count() < limit) {
                allowed.set(true);
                return new Window(w.startMillis(), w.count() + 1);
            }
            return w;                             // over the limit: count unchanged
        });
        return allowed.get();
    }

    @Scheduled(fixedDelay = 60_000)               // drop old entries so memory doesn't grow forever
    void cleanup() {
        long cutoff = System.currentTimeMillis() - 10 * 60_000;
        windows.entrySet().removeIf(e -> e.getValue().startMillis() < cutoff);
    }
}
