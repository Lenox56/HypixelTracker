package com.hypixeltracker.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * Haelt lokale Einstellungen des Nutzers (Username, Server-Adresse).
 * Einen Hypixel-API-Key gibt es in der App bewusst NICHT: laut API-Policy
 * duerfen Nutzer keine Keys in Anwendungen Dritter eintragen. Anfragen,
 * die einen Key brauchen, laufen ueber den eigenen Server (Ordner server/).
 */
// ignoreUnknown: alte config.json-Dateien enthalten noch "hypixelApiKey"
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

    /** TODO: nach dem Einrichten des Hetzner-Servers auf die eigene Domain setzen. */
    public static final String DEFAULT_SERVER_URL = "https://tracker.example.com";

    private static final File CONFIG_FILE = new File(System.getProperty("user.home"),
            ".hypixeltracker/config.json");

    private String serverUrl = DEFAULT_SERVER_URL;
    private String minecraftUsername = "";
    /** Wenn true, werden Ist-Daten ueber alle Profile des Spielers vereinigt. */
    private boolean aggregateAllProfiles = true;

    public static AppConfig loadOrCreate() {
        ObjectMapper mapper = new ObjectMapper();
        if (CONFIG_FILE.exists()) {
            try {
                return mapper.readValue(CONFIG_FILE, AppConfig.class);
            } catch (IOException e) {
                System.err.println("Konnte Konfiguration nicht laden, verwende Standardwerte: " + e.getMessage());
            }
        }
        return new AppConfig();
    }

    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(CONFIG_FILE, this);
        } catch (IOException e) {
            System.err.println("Konnte Konfiguration nicht speichern: " + e.getMessage());
        }
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getMinecraftUsername() {
        return minecraftUsername;
    }

    public void setMinecraftUsername(String minecraftUsername) {
        this.minecraftUsername = minecraftUsername;
    }

    public boolean isAggregateAllProfiles() {
        return aggregateAllProfiles;
    }

    public void setAggregateAllProfiles(boolean aggregateAllProfiles) {
        this.aggregateAllProfiles = aggregateAllProfiles;
    }
}
