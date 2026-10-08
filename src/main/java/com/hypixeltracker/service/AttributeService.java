package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.model.AttributeShard;
import com.hypixeltracker.model.ShardRarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wertet die Hunting-Attribute eines Spielers aus.
 *
 * Quelle: profiles[].members[uuid].attributes.stacks
 * (Shard-ID -> Anzahl gesyphonter Shards). Namen und Seltenheit kommen aus
 * /v2/resources/skyblock/items (Items mit ID "SHARD_..."), daraus ergibt
 * sich auch die Liste der noch fehlenden Attribute.
 */
public class AttributeService {

    private static final String SHARD_PREFIX = "SHARD_";

    private record ShardInfo(String name, ShardRarity rarity) {
    }

    /** Liest die Stacks des Spielers - aus dem aktiven Profil oder (aggregate) dem Maximum ueber alle Profile. */
    public Map<String, Integer> readStacks(JsonNode profilesResponse, String uuid, boolean aggregate) {
        String memberKey = uuid.replace("-", "").toLowerCase(Locale.ROOT);
        Map<String, Integer> stacks = new LinkedHashMap<>();

        JsonNode selected = null;
        for (JsonNode profile : profilesResponse.path("profiles")) {
            if (aggregate) {
                mergeMax(stacks, attributeStacks(profile, memberKey));
            } else if (selected == null || profile.path("selected").asBoolean(false)) {
                selected = profile;
            }
        }
        if (!aggregate && selected != null) {
            mergeMax(stacks, attributeStacks(selected, memberKey));
        }
        return stacks;
    }

    /** Rohdaten des attributes-Objekts aus dem aktiven Profil (fuer die Fehlersuche). */
    public JsonNode rawAttributes(JsonNode profilesResponse, String uuid) {
        String memberKey = uuid.replace("-", "").toLowerCase(Locale.ROOT);
        JsonNode fallback = null;
        for (JsonNode profile : profilesResponse.path("profiles")) {
            JsonNode attributes = profile.path("members").path(memberKey).path("attributes");
            if (profile.path("selected").asBoolean(false)) {
                return attributes;
            }
            if (fallback == null) {
                fallback = attributes;
            }
        }
        return fallback;
    }

    /**
     * Baut die Uebersicht: alle Attribute des Spielers plus (falls die IDs
     * zuordenbar sind) alle noch fehlenden Shards aus der Item-Datenbank.
     */
    public List<AttributeShard> buildOverview(Map<String, Integer> stacks, JsonNode itemResources) {
        Map<String, ShardInfo> shardItems = readShardItems(itemResources);
        Map<String, AttributeShard> result = new LinkedHashMap<>();
        boolean anyKeyMatchedItem = false;

        for (Map.Entry<String, Integer> entry : stacks.entrySet()) {
            String itemId = toItemId(entry.getKey(), shardItems);
            ShardInfo info = itemId == null ? null : shardItems.get(itemId);
            anyKeyMatchedItem |= info != null;

            String name = info != null ? info.name() : prettify(entry.getKey());
            ShardRarity rarity = info != null ? info.rarity() : ShardRarity.fromShardCode(entry.getKey());
            String key = itemId != null ? itemId : entry.getKey();
            result.put(key, new AttributeShard(key, name, rarity, entry.getValue()));
        }

        // Fehlende nur ergaenzen, wenn die Stack-Schluessel zu den Item-IDs passen,
        // sonst wuerden freigeschaltete Attribute doppelt als "fehlend" auftauchen.
        if (anyKeyMatchedItem || stacks.isEmpty()) {
            for (Map.Entry<String, ShardInfo> item : shardItems.entrySet()) {
                result.computeIfAbsent(item.getKey(),
                        id -> new AttributeShard(id, item.getValue().name(), item.getValue().rarity(), 0));
            }
        }

        List<AttributeShard> list = new ArrayList<>(result.values());
        list.sort(Comparator.comparing(AttributeShard::isOwned).reversed()
                .thenComparing(Comparator.comparingInt(AttributeShard::getTier).reversed())
                .thenComparing(AttributeShard::getAttributeName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private static Map<String, ShardInfo> readShardItems(JsonNode itemResources) {
        Map<String, ShardInfo> shards = new HashMap<>();
        if (itemResources == null) {
            return shards;
        }
        for (JsonNode item : itemResources.path("items")) {
            String id = item.path("id").asText("");
            if (id.startsWith(SHARD_PREFIX)) {
                String name = stripColorCodes(item.path("name").asText(prettify(id)));
                shards.put(id, new ShardInfo(name, ShardRarity.fromTier(item.path("tier").asText(null))));
            }
        }
        return shards;
    }

    private static String toItemId(String stackKey, Map<String, ShardInfo> shardItems) {
        String upper = stackKey.toUpperCase(Locale.ROOT);
        if (shardItems.containsKey(upper)) {
            return upper;
        }
        if (shardItems.containsKey(SHARD_PREFIX + upper)) {
            return SHARD_PREFIX + upper;
        }
        return null;
    }

    private static JsonNode attributeStacks(JsonNode profile, String memberKey) {
        return profile.path("members").path(memberKey).path("attributes").path("stacks");
    }

    private static void mergeMax(Map<String, Integer> target, JsonNode stacks) {
        stacks.fields().forEachRemaining(e -> target.merge(e.getKey(), e.getValue().asInt(0), Math::max));
    }

    static String stripColorCodes(String text) {
        return text.replaceAll("§.", "");
    }

    /** "SHARD_SEA_SERPENT" -> "Sea Serpent" */
    static String prettify(String key) {
        String base = key.toUpperCase(Locale.ROOT).startsWith(SHARD_PREFIX) ? key.substring(SHARD_PREFIX.length()) : key;
        StringBuilder sb = new StringBuilder();
        for (String word : base.toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
