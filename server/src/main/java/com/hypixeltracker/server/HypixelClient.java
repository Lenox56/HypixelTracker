package com.hypixeltracker.server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Einzige Stelle, an der der Hypixel-API-Key verwendet wird.
 * Wertet die RateLimit-Header aus und pausiert selbststaendig,
 * wenn das Limit des Keys erreicht ist, statt Hypixel weiter anzufragen.
 */
public class HypixelClient {

    private static final String BASE_URL = "https://api.hypixel.net";
    private static final String USER_AGENT = "HypixelTracker-Server/1.0";

    public record ApiResponse(int status, String body) {
    }

    /** Wird geworfen, solange das Rate-Limit des Keys ausgeschoepft ist. */
    public static class RateLimitedException extends IOException {
        private final long retryAfterSeconds;

        public RateLimitedException(long retryAfterSeconds) {
            super("Rate-Limit des Hypixel-Keys erreicht");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long retryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String apiKey;

    /** Bis zu diesem Zeitpunkt werden keine Anfragen an Hypixel geschickt. */
    private volatile Instant blockedUntil = Instant.EPOCH;

    public HypixelClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public ApiResponse getProfiles(String uuid) throws IOException, InterruptedException {
        return get("/v2/skyblock/profiles?uuid=" + uuid);
    }

    private ApiResponse get(String path) throws IOException, InterruptedException {
        long waitSeconds = Duration.between(Instant.now(), blockedUntil).toSeconds();
        if (waitSeconds > 0) {
            throw new RateLimitedException(waitSeconds);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(15))
                .header("API-Key", apiKey)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        long resetSeconds = headerLong(response, "RateLimit-Reset").orElse(60L);
        if (response.statusCode() == 429) {
            blockedUntil = Instant.now().plusSeconds(Math.max(resetSeconds, 1));
            throw new RateLimitedException(resetSeconds);
        }
        // Letzte Anfrage des Fensters verbraucht -> bis zum Reset pausieren
        if (headerLong(response, "RateLimit-Remaining").orElse(1L) <= 0) {
            blockedUntil = Instant.now().plusSeconds(Math.max(resetSeconds, 1));
        }

        return new ApiResponse(response.statusCode(), response.body());
    }

    private static Optional<Long> headerLong(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).flatMap(value -> {
            try {
                return Optional.of(Long.parseLong(value.trim()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }
}
