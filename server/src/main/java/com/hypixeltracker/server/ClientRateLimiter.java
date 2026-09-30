package com.hypixeltracker.server;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Begrenzt, wie viele Anfragen ein einzelner Client (IP) pro Minute
 * stellen darf. Schuetzt den Key davor, dass ein einzelner Nutzer
 * das gemeinsame Hypixel-Limit aufbraucht.
 */
public class ClientRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private static final class Window {
        Instant start;
        int count;

        Window(Instant start) {
            this.start = start;
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxRequestsPerWindow;
    private final Clock clock;

    public ClientRateLimiter(int maxRequestsPerWindow, Clock clock) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.clock = clock;
    }

    public boolean tryAcquire(String clientId) {
        Instant now = clock.instant();
        boolean[] allowed = new boolean[1];
        windows.compute(clientId, (id, window) -> {
            if (window == null || !now.isBefore(window.start.plus(WINDOW))) {
                window = new Window(now);
            }
            allowed[0] = window.count < maxRequestsPerWindow;
            if (allowed[0]) {
                window.count++;
            }
            return window;
        });
        return allowed[0];
    }

    public void evictExpired() {
        Instant now = clock.instant();
        windows.values().removeIf(window -> !now.isBefore(window.start.plus(WINDOW)));
    }
}
