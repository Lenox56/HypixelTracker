package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypixeltracker.model.ShardRarity;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lokale Referenzdaten aus src/main/resources/data/ (erzeugt mit
 * tools/update_reference_data.py aus dem NotEnoughUpdates-REPO):
 * Attribut-Shards, Accessoire-Upgrades und maximale Minion-Stufen.
 */
public final class ReferenceData {

    public record AttributeInfo(String bazaarName, String shardId, String shardName,
                                String attributeName, ShardRarity rarity) {
    }

    private static volatile ReferenceData instance;

    private final List<AttributeInfo> attributes;
    private final Map<String, AttributeInfo> attributesByKey;
    private final Map<String, List<String>> accessoryUpgrades;
    private final Set<String> ignoredAccessories;
    private final Map<String, Integer> minionMaxTiers;

    ReferenceData(JsonNode attributesJson, JsonNode accessoriesJson, JsonNode minionsJson) {
        List<AttributeInfo> attrs = new ArrayList<>();
        Map<String, AttributeInfo> byKey = new HashMap<>();
        for (JsonNode a : attributesJson) {
            AttributeInfo info = new AttributeInfo(
                    a.path("bazaarName").asText(),
                    a.path("shardId").asText(),
                    a.path("shardName").asText(),
                    a.path("attributeName").asText(),
                    ShardRarity.fromTier(a.path("rarity").asText()));
            attrs.add(info);
            byKey.put(info.bazaarName().toUpperCase(Locale.ROOT), info);
            byKey.put(info.shardId().toUpperCase(Locale.ROOT), info);
        }
        this.attributes = Collections.unmodifiableList(attrs);
        this.attributesByKey = byKey;

        Map<String, List<String>> upgrades = new HashMap<>();
        accessoriesJson.path("upgrades").fields().forEachRemaining(e -> {
            List<String> list = new ArrayList<>();
            e.getValue().forEach(v -> list.add(v.asText()));
            upgrades.put(e.getKey(), list);
        });
        this.accessoryUpgrades = upgrades;

        Set<String> ignored = new HashSet<>();
        accessoriesJson.path("ignored").forEach(v -> ignored.add(v.asText()));
        this.ignoredAccessories = ignored;

        Map<String, Integer> minions = new LinkedHashMap<>();
        minionsJson.fields().forEachRemaining(e -> minions.put(e.getKey(), e.getValue().asInt()));
        this.minionMaxTiers = Collections.unmodifiableMap(minions);
    }

    public static ReferenceData get() {
        if (instance == null) {
            synchronized (ReferenceData.class) {
                if (instance == null) {
                    ObjectMapper mapper = new ObjectMapper();
                    instance = new ReferenceData(read(mapper, "attributes.json"),
                            read(mapper, "accessories.json"), read(mapper, "minions.json"));
                }
            }
        }
        return instance;
    }

    private static JsonNode read(ObjectMapper mapper, String name) {
        try (InputStream in = ReferenceData.class.getResourceAsStream("/data/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Referenzdatei /data/" + name + " fehlt im JAR");
            }
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<AttributeInfo> attributes() {
        return attributes;
    }

    /** Sucht ein Attribut ueber "SHARD_GROVE" oder den Shard-Code "C1" (Gross/Klein egal). */
    public AttributeInfo attributeByKey(String key) {
        return key == null ? null : attributesByKey.get(key.toUpperCase(Locale.ROOT));
    }

    /** Alle hoeheren Stufen eines Accessoires, z.B. SPEED_TALISMAN -> [SPEED_RING, SPEED_ARTIFACT, ...]. */
    public List<String> accessoryUpgrades(String accessoryId) {
        return accessoryUpgrades.getOrDefault(accessoryId, List.of());
    }

    /** Accessoires, die nicht mitzaehlen (z.B. Rift-, Event- oder Sonder-Items). */
    public boolean isIgnoredAccessory(String accessoryId) {
        return ignoredAccessories.contains(accessoryId);
    }

    /** Minion-Typ (z.B. "WHEAT") -> maximale Stufe. */
    public Map<String, Integer> minionMaxTiers() {
        return minionMaxTiers;
    }
}
