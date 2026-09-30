package com.hypixeltracker.server;

import java.util.Map;

/**
 * Alle Einstellungen des Servers kommen aus Umgebungsvariablen
 * (in Docker ueber die .env-Datei). Der API-Key steht damit
 * nie im Code oder im Image.
 */
public record ServerConfig(
        String hypixelApiKey,
        int port,
        int cacheTtlSeconds,
        int clientRequestsPerMinute,
        boolean trustProxyHeaders
) {

    public static ServerConfig fromEnv(Map<String, String> env) {
        String apiKey = env.getOrDefault("HYPIXEL_API_KEY", "").trim();
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("Umgebungsvariable HYPIXEL_API_KEY fehlt (siehe .env.example).");
        }
        return new ServerConfig(
                apiKey,
                intEnv(env, "PORT", 8080),
                intEnv(env, "CACHE_TTL_SECONDS", 300),
                intEnv(env, "CLIENT_REQUESTS_PER_MINUTE", 30),
                Boolean.parseBoolean(env.getOrDefault("TRUST_PROXY_HEADERS", "false"))
        );
    }

    private static int intEnv(Map<String, String> env, String name, int defaultValue) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Umgebungsvariable " + name + " ist keine Zahl: " + value);
        }
    }
}
