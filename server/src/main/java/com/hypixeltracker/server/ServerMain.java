package com.hypixeltracker.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Startet den HypixelTracker-Server.
 *
 * Endpunkte:
 *   GET /health                        -> "ok" (fuer Docker-Healthcheck)
 *   GET /v1/skyblock/profiles?uuid=... -> SkyBlock-Profile eines Spielers
 *
 * Keylose Hypixel-Endpunkte (Bazaar, Auktionen, Items) ruft die App
 * weiterhin direkt auf, dafuer wird dieser Server nicht gebraucht.
 */
public class ServerMain {

    public static void main(String[] args) throws IOException {
        ServerConfig config = ServerConfig.fromEnv(System.getenv());
        Clock clock = Clock.systemUTC();

        HypixelClient hypixel = new HypixelClient(config.hypixelApiKey());
        ResponseCache<HypixelClient.ApiResponse> cache =
                new ResponseCache<>(Duration.ofSeconds(config.cacheTtlSeconds()), clock);
        ClientRateLimiter rateLimiter = new ClientRateLimiter(config.clientRequestsPerMinute(), clock);

        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/health", exchange -> {
            try (exchange) {
                ProfilesHandler.send(exchange, 200, "{\"success\":true}");
            }
        });
        server.createContext("/v1/skyblock/profiles",
                new ProfilesHandler(hypixel::getProfiles, cache, rateLimiter, config.trustProxyHeaders()));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        ScheduledExecutorService cleanup = Executors.newSingleThreadScheduledExecutor();
        cleanup.scheduleAtFixedRate(() -> {
            cache.evictExpired();
            rateLimiter.evictExpired();
        }, 1, 1, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(2);
            cleanup.shutdownNow();
        }));

        server.start();
        System.out.println("HypixelTracker-Server laeuft auf Port " + config.port()
                + " (Cache " + config.cacheTtlSeconds() + "s, "
                + config.clientRequestsPerMinute() + " Anfragen/Minute pro Client)");
    }
}
