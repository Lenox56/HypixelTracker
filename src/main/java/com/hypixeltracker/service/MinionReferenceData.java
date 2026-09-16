package com.hypixeltracker.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Da es fuer maximale Minion-Tiers und Upgrade-Materialkosten keinen
 * offiziellen API-Endpunkt gibt, wird diese Referenztabelle lokal
 * gepflegt. Bei SkyBlock-Updates (neue Minions/Tiers) muss diese
 * Liste manuell nachgezogen werden.
 *
 * TODO: vollstaendige Liste aller Minion-Typen ergaenzen
 * (aktuell nur Beispieleintraege als Platzhalter).
 */
public class MinionReferenceData {

    /** minionType -> maximal erreichbare Tier-Stufe. */
    public static final Map<String, Integer> MAX_TIER = new LinkedHashMap<>();

    static {
        MAX_TIER.put("COBBLESTONE", 11);
        MAX_TIER.put("WHEAT", 12);
        MAX_TIER.put("ZOMBIE", 11);
        MAX_TIER.put("SKELETON", 11);
        // TODO: restliche Minion-Typen ergaenzen
    }

    /** Schoene Anzeigenamen statt interner IDs. */
    public static String displayName(String minionType) {
        String[] parts = minionType.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
