package com.example.ticket_booking_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatMapCacheEvictor {
    private final CacheManager cacheManager;

    public void evictAfterCommit(Collection<Long> showIds) {
        if (showIds.isEmpty()) return;

        Runnable evict = () -> {
            try {
                Cache cache = cacheManager.getCache("seatmap");
                if (cache != null) showIds.forEach(cache::evict);
            } catch (Exception e) {
                // Booking is already committed. A Redis failure must not break it; the 10s TTL cleans up.
                log.warn("Failed to evict seatmap cache for shows {}", showIds, e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                //registerSynchronization tells Spring: Run this code after the current transaction commits. If the transaction rolls back (for example, a seat conflict), it never runs.
                @Override
                public void afterCommit() { evict.run(); }
            });
        } else {
            evict.run();
        }
    }
}
