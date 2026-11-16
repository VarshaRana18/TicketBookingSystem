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
public class CacheEvictor {
    private final CacheManager cacheManager;

    public void evictAfterCommit(Collection<Long> showIds) {
        evictAfterCommit("seatmap", showIds);
    }

    public void evictAfterCommit(String cacheName, Collection<Long> keys) {
        if (keys.isEmpty()) return;

        Runnable evict = () -> {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) keys.forEach(cache::evict);
            } catch (Exception e) {
                log.warn("Failed to evict cache '{}' for keys {}", cacheName, keys, e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { evict.run(); }
            });
        } else {
            evict.run();
        }
    }
}
