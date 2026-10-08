package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.model.AttributeShard;
import com.hypixeltracker.model.ShardRarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wertet die Hunting-Attribute eines Spielers aus.
 *
 * Quelle: profiles[].members[uuid].attributes.stacks
 * (Shard-ID -> Anzahl gesyphonter Shards). Namen, Seltenheit und die
 * vollstaendige Liste aller Attribute kommen aus {@link ReferenceData}.
 */
public class AttributeService {

    private final ReferenceData reference;

    public AttributeService() {
        this(ReferenceData.get());
    }

    AttributeService(ReferenceData reference) {
        this.reference = reference;
    }

    /** Liest die Stacks des Spielers - aus dem aktiven Profil oder (aggregate) dem Maximum ueber alle Profile. */
    public Map<String, Integer> readStacks(JsonNode profilesResponse, String uuid, boolean aggregate) {
        String memberKey = ProfileUtil.memberKey(uuid);
        Map<String, Integer> stacks = new LinkedHashMap<>();
        for (JsonNode profile : ProfileUtil.relevantProfiles(profilesResponse, aggregate)) {
            profile.path("members").path(memberKey).path("attributes").path("stacks").fields()
                    .forEachRemaining(e -> stacks.merge(e.getKey(), e.getValue().asInt(0), Math::max));
        }
        return stacks;
    }

    /** Rohdaten des attributes-Objekts aus dem aktiven Profil (fuer die Fehlersuche). */
    public JsonNode rawAttributes(JsonNode profilesResponse, String uuid) {
        String memberKey = ProfileUtil.memberKey(uuid);
        for (JsonNode profile : ProfileUtil.relevantProfiles(profilesResponse, false)) {
            return profile.path("members").path(memberKey).path("attributes");
        }
        return null;
    }

    /** Alle Attribute: freigeschaltete mit Stufe, noch fehlende mit Stufe 0. */
    public List<AttributeShard> buildOverview(Map<String, Integer> stacks) {
        Map<String, AttributeShard> result = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : stacks.entrySet()) {
            ReferenceData.AttributeInfo info = reference.attributeByKey(entry.getKey());
            if (info != null) {
                result.merge(info.bazaarName(), toShard(info, entry.getValue()),
                        (a, b) -> a.getStacks() >= b.getStacks() ? a : b);
            } else {
                // Unbekannter Shard (neuer als die Referenzdaten) trotzdem anzeigen
                String key = entry.getKey();
                result.put(key, new AttributeShard(key, prettify(key), "?",
                        ShardRarity.fromShardCode(key), entry.getValue()));
            }
        }
        for (ReferenceData.AttributeInfo info : reference.attributes()) {
            result.putIfAbsent(info.bazaarName(), toShard(info, 0));
        }

        List<AttributeShard> list = new ArrayList<>(result.values());
        list.sort(Comparator.comparing(AttributeShard::isOwned).reversed()
                .thenComparing(Comparator.comparingInt(AttributeShard::getTier).reversed())
                .thenComparing(AttributeShard::getAttributeName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private static AttributeShard toShard(ReferenceData.AttributeInfo info, int stacks) {
        return new AttributeShard(info.bazaarName(), info.attributeName(), info.shardName(), info.rarity(), stacks);
    }

    /** "SHARD_SEA_SERPENT" -> "Sea Serpent" */
    static String prettify(String key) {
        String base = key.toUpperCase(Locale.ROOT).startsWith("SHARD_") ? key.substring("SHARD_".length()) : key;
        return ProfileUtil.titleCase(base);
    }
}
