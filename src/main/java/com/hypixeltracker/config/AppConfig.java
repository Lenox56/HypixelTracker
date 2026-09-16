package com.hypixeltracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * Haelt lokale Einstellungen (v.a. den vom Nutzer selbst eingetragenen
 * Hypixel-API-Key). Wird NICHT mit ins JAR/EXE gebaut, sondern liegt
 * als separate Datei neben der App - siehe Policy-Hinweis: eigener Key
 * pro Nutzer, kein fest eingebetteter Key.
 */
public class AppConfig {

    private static final File CONFIG_FILE = new File(System.getProperty("user.home"),
            ".hypixeltracker/config.json");

    private String hypixelApiKey = "";
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

    public String getHypixelApiKey() {
        return hypixelApiKey;
    }

    public void setHypixelApiKey(String hypixelApiKey) {
        this.hypixelApiKey = hypixelApiKey;
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
