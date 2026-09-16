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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kapselt Zugriffe auf die MediaWiki-API des Hypixel-SkyBlock-Wikis.
 * Wird sowohl von der freien Suchleiste als auch vom kontextuellen
 * Info-Icon pro fehlendem Item genutzt.
 */
public class WikiSearchService {

    private static final String API_BASE = "https://hypixelskyblock.minecraft.wiki/api.php";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Einfacher In-Memory-Cache, damit wiederholte Klicks auf dasselbe Item keine neue Anfrage ausloesen. */
    private final Map<String, String> summaryCache = new ConcurrentHashMap<>();

    public record SearchResult(String title, String snippet) {
    }

    /** Freie Volltextsuche (fuer die Suchleiste). */
    public List<SearchResult> search(String query) throws IOException, InterruptedException {
        String url = API_BASE + "?action=query&list=search&format=json"
                + "&srsearch=" + encode(query)
                + "&srlimit=10";

        JsonNode root = fetch(url);
        List<SearchResult> results = new ArrayList<>();
        for (JsonNode hit : root.path("query").path("search")) {
            String snippet = hit.path("snippet").asText("")
                    .replaceAll("<[^>]*>", ""); // HTML-Hervorhebungen entfernen
            results.add(new SearchResult(hit.path("title").asText(), snippet));
        }
        return results;
    }

    /** Gezielter Lookup mit bekanntem, exaktem Artikeltitel (fuer die Info-Icons). */
    public String getSummary(String exactTitle) throws IOException, InterruptedException {
        if (summaryCache.containsKey(exactTitle)) {
            return summaryCache.get(exactTitle);
        }

        String url = API_BASE + "?action=query&prop=extracts&exintro=true&explaintext=true&format=json"
                + "&titles=" + encode(exactTitle);

        JsonNode root = fetch(url);
        JsonNode pages = root.path("query").path("pages");
        String summary = "Kein Artikel gefunden.";
        if (pages.fields().hasNext()) {
            summary = pages.fields().next().getValue().path("extract").asText(summary);
        }

        summaryCache.put(exactTitle, summary);
        return summary;
    }

    private JsonNode fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "HypixelTracker/1.0")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
