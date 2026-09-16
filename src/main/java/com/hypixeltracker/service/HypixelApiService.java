package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Kapselt alle Aufrufe gegen die offizielle Hypixel-API.
 * Der API-Key wird NICHT hier fest eingetragen, sondern kommt aus
 * AppConfig (vom Nutzer selbst eingegeben) - siehe API-Policy.
 *
 * TODO: einfachen In-Memory/Disk-Cache ergaenzen (v.a. fuer
 * /resources/skyblock/items und /skyblock/bazaar, die sich nur
 * alle paar Minuten aendern).
 */
public class HypixelApiService {

    private static final String BASE_URL = "https://api.hypixel.net";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private String apiKey;

    public HypixelApiService(String apiKey) {
        this.apiKey = apiKey;
    }

    /** Erlaubt es, den Key zur Laufzeit zu aendern (z.B. nach dem Einstellungsdialog). */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /** Alle SkyBlock-Profile (Inseln) eines Spielers. */
    public JsonNode getProfiles(String playerUuid) throws IOException, InterruptedException {
        return getJson("/v2/skyblock/profiles?uuid=" + playerUuid, true);
    }

    /** Alle aktiven Auktionen, seitenweise (page 0 = neueste Seite zuerst laut API). */
    public JsonNode getAuctions(int page) throws IOException, InterruptedException {
        return getJson("/v2/skyblock/auctions?page=" + page, false);
    }

    /** Aktuelle Bazaar-Preise aller Produkte. */
    public JsonNode getBazaar() throws IOException, InterruptedException {
        return getJson("/v2/skyblock/bazaar", false);
    }

    /** Item-Datenbank inkl. Crafting-Rezepten (fuer Accessoires/Minions-Abgleich). */
    public JsonNode getItemResources() throws IOException, InterruptedException {
        return getJson("/v2/resources/skyblock/items", false);
    }

    /** UUID eines Spielers anhand des Namens (Mojang-API, kein Hypixel-Key noetig). */
    public String resolveUuid(String username) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = mapper.readTree(response.body());
        return node.get("id").asText();
    }

    private JsonNode getJson(String path, boolean requiresKey) throws IOException, InterruptedException {
        if (requiresKey && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalStateException("Kein Hypixel-API-Key hinterlegt. Bitte in den Einstellungen eintragen.");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET();

        if (requiresKey) {
            builder.header("API-Key", apiKey);
        }

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Hypixel-API antwortete mit Status " + response.statusCode() + ": " + response.body());
        }

        JsonNode node = mapper.readTree(response.body());
        if (!node.path("success").asBoolean(false)) {
            throw new IOException("Hypixel-API meldet Fehler: " + node.path("cause").asText("unbekannt"));
        }
        return node;
    }
}
