package com.hypixeltracker.server;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilesHandlerTest {

    private static final String UUID = "069a79f444e94726a5befca90e38aaf5";

    private final MutableClock clock = new MutableClock();
    private final HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String start(ProfilesHandler.ProfileFetcher fetcher, int requestsPerMinute) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/skyblock/profiles", new ProfilesHandler(fetcher,
                new ResponseCache<>(Duration.ofMinutes(5), clock),
                new ClientRateLimiter(requestsPerMinute, clock), false));
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/skyblock/profiles";
    }

    private HttpResponse<String> get(String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void passesThroughAndCachesProfiles() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String url = start(uuid -> {
            calls.incrementAndGet();
            return new HypixelClient.ApiResponse(200, "{\"success\":true,\"profiles\":[]}");
        }, 10);

        HttpResponse<String> first = get(url + "?uuid=" + UUID);
        // Schreibweise mit Bindestrichen trifft denselben Cache-Eintrag
        HttpResponse<String> second = get(url + "?uuid=069A79F4-44E9-4726-A5BE-FCA90E38AAF5");

        assertEquals(200, first.statusCode());
        assertEquals("{\"success\":true,\"profiles\":[]}", second.body());
        assertEquals(1, calls.get());
    }

    @Test
    void rejectsInvalidUuidWithoutCallingHypixel() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String url = start(uuid -> {
            calls.incrementAndGet();
            return new HypixelClient.ApiResponse(200, "{}");
        }, 10);

        assertEquals(400, get(url + "?uuid=../../key").statusCode());
        assertEquals(400, get(url).statusCode());
        assertEquals(0, calls.get());
    }

    @Test
    void hidesInvalidServerKeyFromClients() throws Exception {
        String url = start(uuid -> new HypixelClient.ApiResponse(403,
                "{\"success\":false,\"cause\":\"Invalid API key\"}"), 10);

        HttpResponse<String> response = get(url + "?uuid=" + UUID);

        assertEquals(502, response.statusCode());
        assertTrue(response.body().contains("\"success\":false"));
    }

    @Test
    void reportsHypixelRateLimitAsRetryLater() throws Exception {
        String url = start(uuid -> {
            throw new HypixelClient.RateLimitedException(42);
        }, 10);

        HttpResponse<String> response = get(url + "?uuid=" + UUID);

        assertEquals(503, response.statusCode());
        assertEquals("42", response.headers().firstValue("Retry-After").orElse(null));
    }

    @Test
    void limitsRequestsPerClient() throws Exception {
        String url = start(uuid -> new HypixelClient.ApiResponse(200, "{\"success\":true}"), 1);

        assertEquals(200, get(url + "?uuid=" + UUID).statusCode());
        assertEquals(429, get(url + "?uuid=" + UUID).statusCode());
    }

    @Test
    void normalizeUuidAcceptsBothFormats() {
        assertEquals(UUID, ProfilesHandler.normalizeUuid("069a79f4-44e9-4726-a5be-fca90e38aaf5"));
        assertNull(ProfilesHandler.normalizeUuid("notch"));
    }
}
