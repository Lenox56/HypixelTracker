package com.hypixeltracker.server;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientRateLimiterTest {

    private final MutableClock clock = new MutableClock();
    private final ClientRateLimiter limiter = new ClientRateLimiter(2, clock);

    @Test
    void blocksAfterLimitUntilWindowEnds() {
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertFalse(limiter.tryAcquire("1.2.3.4"));

        clock.advance(Duration.ofMinutes(1));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
    }

    @Test
    void clientsAreLimitedSeparately() {
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");

        assertTrue(limiter.tryAcquire("b"));
    }
}
