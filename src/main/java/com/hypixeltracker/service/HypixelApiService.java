package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Kapselt alle Aufrufe gegen die Hypixel-API.
 *
 * Die App enthaelt KEINEN API-Key und nimmt auch keinen vom Nutzer an
 * (API-Policy: Nutzer duerfen ihre Keys nicht in Anwendungen Dritter
 * eintragen). Deshalb:
 * - Endpunkte ohne Key (Bazaar, Auktionen, Items) -> direkt an api.hypixel.net
 * - Profile (braucht einen Key) -> ueber den eigenen Server (Ordner server/),
 *   der den Key haelt und die Antworten zwischenspeichert.
 */
public class HypixelApiService {

    private static final String HYPIXEL_URL = "https://api.hypixel.net";
    private static final String USER_AGENT = "HypixelTracker/1.0";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private String serverUrl;

    public HypixelApiService(String serverUrl) {
        setServerUrl(serverUrl);
    }

    /** Erlaubt es, die Server-Adresse zur Laufzeit zu aendern (z.B. nach dem Einstellungsdialog). */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl == null ? "" : serverUrl.replaceAll("/+$", "");
    }

    /** Alle SkyBlock-Profile (Inseln) eines Spielers - ueber den eigenen Server. */
    public JsonNode getProfiles(String playerUuid) throws IOException, InterruptedException {
        if (serverUrl.isBlank()) {
            throw new IllegalStateException("Keine Server-Adresse hinterlegt. Bitte in den Einstellungen eintragen.");
        }
        return getJson(serverUrl + "/v1/skyblock/profiles?uuid=" + playerUuid);
    }

    /** Alle aktiven Auktionen, seitenweise (page 0 = neueste Seite zuerst laut API). */
    public JsonNode getAuctions(int page) throws IOException, InterruptedException {
        return getJson(HYPIXEL_URL + "/v2/skyblock/auctions?page=" + page);
    }

    /** Aktuelle Bazaar-Preise aller Produkte. */
    public JsonNode getBazaar() throws IOException, InterruptedException {
        return getJson(HYPIXEL_URL + "/v2/skyblock/bazaar");
    }

    /** Item-Datenbank inkl. Crafting-Rezepten (fuer Accessoires/Minions-Abgleich). */
    public JsonNode getItemResources() throws IOException, InterruptedException {
        return getJson(HYPIXEL_URL + "/v2/resources/skyblock/items");
    }

    /** UUID eines Spielers anhand des Namens (Mojang-API, kein Hypixel-Key noetig). */
    public String resolveUuid(String username) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/"
                        + URLEncoder.encode(username.trim(), StandardCharsets.UTF_8)))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404 || response.statusCode() == 204) {
            throw new IOException("Minecraft-Account \"" + username + "\" wurde nicht gefunden.");
        }
        JsonNode node = mapper.readTree(response.body());
        if (!node.hasNonNull("id")) {
            throw new IOException("UUID fuer \"" + username + "\" konnte nicht ermittelt werden (Status " + response.statusCode() + ").");
        }
        return node.get("id").asText();
    }

    private JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429 || response.statusCode() == 503) {
            String retryAfter = response.headers().firstValue("Retry-After").orElse("einigen");
            throw new IOException("Zu viele Anfragen - bitte in " + retryAfter + " Sekunden erneut versuchen.");
        }

        if (response.statusCode() != 200) {
            String cause;
            try {
                cause = mapper.readTree(response.body()).path("cause").asText(response.body());
            } catch (IOException e) {
                cause = response.body();
            }
            throw new IOException("Anfrage fehlgeschlagen (Status " + response.statusCode() + "): " + cause);
        }

        JsonNode node = mapper.readTree(response.body());
        if (!node.path("success").asBoolean(false)) {
            throw new IOException("Hypixel-API meldet Fehler: " + node.path("cause").asText("unbekannt"));
        }
        return node;
    }
}
