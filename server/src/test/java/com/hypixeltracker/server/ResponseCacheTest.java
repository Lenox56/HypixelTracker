package com.hypixeltracker.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResponseCacheTest {

    private final MutableClock clock = new MutableClock();
    private final ResponseCache<String> cache = new ResponseCache<>(Duration.ofMinutes(5), clock);

    @Test
    void returnsCachedValueWithinTtl() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        cache.get("a", () -> "v" + loads.incrementAndGet());
        clock.advance(Duration.ofMinutes(4));

        assertEquals("v1", cache.get("a", () -> "v" + loads.incrementAndGet()));
        assertEquals(1, loads.get());
    }

    @Test
    void reloadsAfterTtl() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        cache.get("a", () -> "v" + loads.incrementAndGet());
        clock.advance(Duration.ofMinutes(5));

        assertEquals("v2", cache.get("a", () -> "v" + loads.incrementAndGet()));
    }

    @Test
    void failuresAreNotCached() throws Exception {
        assertThrows(IOException.class, () -> cache.get("a", () -> {
            throw new IOException("kaputt");
        }));

        assertEquals("ok", cache.get("a", () -> "ok"));
    }

    @Test
    void evictExpiredRemovesOldEntries() throws Exception {
        cache.get("a", () -> "v");
        clock.advance(Duration.ofMinutes(6));
        cache.evictExpired();

        assertEquals(0, cache.size());
    }
}
