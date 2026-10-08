package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.model.Minion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wertet die gebauten Minions aus. Quelle ist "crafted_generators"
 * (Eintraege wie "WHEAT_7"), seit API v2 unter member.player_data.
 * Gebaute Minions gelten fuer die ganze Coop, daher werden alle Mitglieder
 * eines Profils zusammengefasst.
 */
public class MinionService {

    private final ReferenceData reference;

    public MinionService() {
        this(ReferenceData.get());
    }

    MinionService(ReferenceData reference) {
        this.reference = reference;
    }

    /** Alle gebauten Minion-Stufen ("WHEAT_7", ...) ueber die Coop bzw. alle Profile. */
    public Set<String> readCraftedGenerators(JsonNode profilesResponse, boolean aggregate) {
        Set<String> crafted = new HashSet<>();
        for (JsonNode profile : ProfileUtil.relevantProfiles(profilesResponse, aggregate)) {
            for (JsonNode member : profile.path("members")) {
                member.path("player_data").path("crafted_generators").forEach(n -> crafted.add(n.asText()));
                member.path("crafted_generators").forEach(n -> crafted.add(n.asText())); // aeltere API-Version
            }
        }
        return crafted;
    }

    public List<Minion> buildOverview(Set<String> craftedGenerators) {
        Map<String, Integer> highest = new LinkedHashMap<>();
        for (String entry : craftedGenerators) {
            int underscore = entry.lastIndexOf('_');
            if (underscore <= 0) continue;
            try {
                int tier = Integer.parseInt(entry.substring(underscore + 1));
                highest.merge(entry.substring(0, underscore), tier, Math::max);
            } catch (NumberFormatException ignored) {
                // kein Minion-Eintrag
            }
        }

        List<Minion> minions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : reference.minionMaxTiers().entrySet()) {
            String type = entry.getKey();
            minions.add(new Minion(type, displayName(type), highest.getOrDefault(type, 0), entry.getValue()));
        }
        // Minions, die neuer als die Referenzdaten sind
        for (Map.Entry<String, Integer> entry : highest.entrySet()) {
            if (!reference.minionMaxTiers().containsKey(entry.getKey())) {
                minions.add(new Minion(entry.getKey(), displayName(entry.getKey()), entry.getValue(), entry.getValue()));
            }
        }

        minions.sort(Comparator.comparingInt(Minion::getMissingTiers).reversed()
                .thenComparing(Minion::getDisplayName));
        return minions;
    }

    static String displayName(String type) {
        return ProfileUtil.titleCase(type) + " Minion";
    }
}
