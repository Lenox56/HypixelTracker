package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Kleine Helfer rund um die Antwort von /v2/skyblock/profiles. */
public final class ProfileUtil {

    private ProfileUtil() {
    }

    /** Schluessel unter "members": UUID ohne Bindestriche, klein geschrieben. */
    public static String memberKey(String uuid) {
        return uuid.replace("-", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Die zu beruecksichtigenden Profile: alle (aggregate) oder nur das im Spiel
     * aktive ("selected"), ersatzweise das erste.
     */
    public static List<JsonNode> relevantProfiles(JsonNode profilesResponse, boolean aggregate) {
        List<JsonNode> all = new ArrayList<>();
        profilesResponse.path("profiles").forEach(all::add);
        if (aggregate || all.isEmpty()) {
            return all;
        }
        for (JsonNode profile : all) {
            if (profile.path("selected").asBoolean(false)) {
                return List.of(profile);
            }
        }
        return List.of(all.get(0));
    }

    /** Entfernt Minecraft-Farbcodes wie "§6". */
    public static String stripColorCodes(String text) {
        return text == null ? null : text.replaceAll("§.", "");
    }

    /** "SEA_SERPENT" -> "Sea Serpent" */
    public static String titleCase(String key) {
        StringBuilder sb = new StringBuilder();
        for (String word : key.toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
