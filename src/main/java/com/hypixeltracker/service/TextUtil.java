package com.hypixeltracker.service;

import java.util.Locale;

/** Kleine Text-Helfer fuer Namen aus der SkyBlock-API. */
public final class TextUtil {

    private TextUtil() {
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
