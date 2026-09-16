package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * Generische Speichern/Laden-Logik fuer beliebige Listen-Objekte
 * als lokale JSON-Datei - gleiches Prinzip wie im AttributeTracker
 * (automatisches Speichern bei jeder Aenderung, kein manueller Button).
 */
public class PersistenceService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final File storageDir;

    public PersistenceService() {
        this.storageDir = new File(System.getProperty("user.home"), ".hypixeltracker/data");
        storageDir.mkdirs();
    }

    public <T> void save(String fileName, T data) {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(storageDir, fileName + ".json"), data);
        } catch (IOException e) {
            System.err.println("Konnte " + fileName + " nicht speichern: " + e.getMessage());
        }
    }

    public <T> T load(String fileName, Class<T> type, T defaultValue) {
        File file = new File(storageDir, fileName + ".json");
        if (!file.exists()) {
            return defaultValue;
        }
        try {
            return mapper.readValue(file, type);
        } catch (IOException e) {
            System.err.println("Konnte " + fileName + " nicht laden, verwende Standardwert: " + e.getMessage());
            return defaultValue;
        }
    }
}
