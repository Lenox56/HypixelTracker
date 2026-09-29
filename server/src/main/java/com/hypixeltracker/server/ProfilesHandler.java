package com.hypixeltracker.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * GET /v1/skyblock/profiles?uuid=...
 * Liefert die Hypixel-Antwort unveraendert durch, damit die App dasselbe
 * JSON-Format wie bei einem direkten Hypixel-Aufruf bekommt.
 */
public class ProfilesHandler implements HttpHandler {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$");

    @FunctionalInterface
    public interface ProfileFetcher {
        HypixelClient.ApiResponse fetch(String uuid) throws IOException, InterruptedException;
    }

    /** Nicht-200-Antworten von Hypixel, die bewusst nicht gecacht werden. */
    static class UpstreamErrorException extends Exception {
        final HypixelClient.ApiResponse response;

        UpstreamErrorException(HypixelClient.ApiResponse response) {
            super("Hypixel antwortete mit Status " + response.status());
            this.response = response;
        }
    }

    private final ProfileFetcher fetcher;
    private final ResponseCache<HypixelClient.ApiResponse> cache;
    private final ClientRateLimiter rateLimiter;
    private final boolean trustProxyHeaders;

    public ProfilesHandler(ProfileFetcher fetcher,
                           ResponseCache<HypixelClient.ApiResponse> cache,
                           ClientRateLimiter rateLimiter,
                           boolean trustProxyHeaders) {
        this.fetcher = fetcher;
        this.cache = cache;
        this.rateLimiter = rateLimiter;
        this.trustProxyHeaders = trustProxyHeaders;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Nur GET erlaubt");
                return;
            }
            if (!rateLimiter.tryAcquire(clientId(exchange))) {
                exchange.getResponseHeaders().set("Retry-After", "60");
                sendError(exchange, 429, "Zu viele Anfragen, bitte kurz warten");
                return;
            }

            String uuid = normalizeUuid(queryParam(exchange, "uuid"));
            if (uuid == null) {
                sendError(exchange, 400, "Parameter uuid fehlt oder ist ungueltig");
                return;
            }

            try {
                HypixelClient.ApiResponse response = cache.get(uuid, () -> {
                    HypixelClient.ApiResponse r = fetcher.fetch(uuid);
                    if (r.status() != 200) {
                        throw new UpstreamErrorException(r);
                    }
                    return r;
                });
                send(exchange, 200, response.body());
            } catch (HypixelClient.RateLimitedException e) {
                exchange.getResponseHeaders().set("Retry-After", String.valueOf(Math.max(e.retryAfterSeconds(), 1)));
                sendError(exchange, 503, "Server ist gerade ausgelastet, bitte in "
                        + Math.max(e.retryAfterSeconds(), 1) + " Sekunden erneut versuchen");
            } catch (UpstreamErrorException e) {
                int status = e.response.status();
                if (status == 400 || status == 404 || status == 422) {
                    // Fehler in der Anfrage selbst -> an den Client weitergeben
                    send(exchange, status, e.response.body());
                } else {
                    // z.B. 403 = Server-Key ungueltig; Details nur ins Log, nicht an Clients
                    System.err.println("Hypixel-Fehler " + status + ": " + e.response.body());
                    sendError(exchange, 502, "Hypixel-API derzeit nicht erreichbar");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendError(exchange, 503, "Server wird beendet");
            } catch (Exception e) {
                System.err.println("Unerwarteter Fehler: " + e);
                sendError(exchange, 502, "Hypixel-API derzeit nicht erreichbar");
            }
        }
    }

    /** Liefert die UUID ohne Bindestriche in Kleinbuchstaben oder null, wenn ungueltig. */
    static String normalizeUuid(String raw) {
        if (raw == null) {
            return null;
        }
        String lower = raw.trim().toLowerCase();
        if (!UUID_PATTERN.matcher(lower).matches()) {
            return null;
        }
        return lower.replace("-", "");
    }

    private String clientId(HttpExchange exchange) {
        if (trustProxyHeaders) {
            List<String> forwarded = exchange.getRequestHeaders().get("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                // Der letzte Eintrag stammt vom eigenen Reverse-Proxy und ist nicht faelschbar
                String[] parts = forwarded.get(forwarded.size() - 1).split(",");
                return parts[parts.length - 1].trim();
            }
        }
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private static String queryParam(HttpExchange exchange, String name) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    static void sendError(HttpExchange exchange, int status, String cause) throws IOException {
        String json = "{\"success\":false,\"cause\":\"" + cause.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        send(exchange, status, json);
    }

    static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
