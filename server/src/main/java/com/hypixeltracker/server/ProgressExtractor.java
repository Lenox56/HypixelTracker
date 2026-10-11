package com.hypixeltracker.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Wertet die Profil-Antwort von Hypixel auf dem Server aus und liefert nur
 * die Fortschrittsdaten, die die App anzeigt. Die rohe API-Antwort verlaesst
 * den Server nicht - er ist damit kein Proxy der Hypixel-API.
 *
 * Ergebnis:
 * <pre>
 * {"success":true,
 *  "attributes":  {"SHARD_GROVE": 5, ...},      gesyphonte Shards je Attribut
 *  "minions":     ["WHEAT_7", ...],              gebaute Minion-Stufen der Coop
 *  "accessories": {"inventoryApiEnabled": true, "owned": ["SPEED_RING", ...]}}
 * </pre>
 */
final class ProgressExtractor {

    private final ObjectMapper mapper = new ObjectMapper();

    String extract(String profilesJson, String uuid, boolean aggregate) throws Exception {
        JsonNode profiles = relevantProfiles(mapper.readTree(profilesJson), aggregate);
        String memberKey = uuid.replace("-", "").toLowerCase();

        Map<String, Integer> attributes = new TreeMap<>();
        TreeSet<String> minions = new TreeSet<>();
        TreeSet<String> accessories = new TreeSet<>();
        boolean inventoryVisible = false;

        for (JsonNode profile : profiles) {
            for (JsonNode member : profile.path("members")) {
                // Gebaute Minions gelten fuer die ganze Coop
                member.path("player_data").path("crafted_generators").forEach(n -> minions.add(n.asText()));
                member.path("crafted_generators").forEach(n -> minions.add(n.asText()));
            }

            JsonNode me = profile.path("members").path(memberKey);
            me.path("attributes").path("stacks").fields()
                    .forEachRemaining(e -> attributes.merge(e.getKey(), e.getValue().asInt(0), Math::max));

            for (JsonNode field : inventoryFields(me)) {
                String data = field.path("data").asText(null);
                if (data != null && !data.isBlank()) {
                    inventoryVisible = true;
                    accessories.addAll(NbtItemReader.itemIds(data));
                }
            }
        }

        ObjectNode result = mapper.createObjectNode();
        result.put("success", true);
        ObjectNode attributeNode = result.putObject("attributes");
        attributes.forEach(attributeNode::put);
        minions.forEach(result.putArray("minions")::add);
        ObjectNode accessoryNode = result.putObject("accessories");
        accessoryNode.put("inventoryApiEnabled", inventoryVisible);
        accessories.forEach(accessoryNode.putArray("owned")::add);
        return mapper.writeValueAsString(result);
    }

    /** Accessoire-Tasche, Inventar, Enderkiste, Rucksaecke (API v2 und aeltere Struktur). */
    private static List<JsonNode> inventoryFields(JsonNode member) {
        JsonNode inventory = member.path("inventory");
        List<JsonNode> fields = new ArrayList<>();
        fields.add(inventory.path("bag_contents").path("talisman_bag"));
        fields.add(inventory.path("inv_contents"));
        fields.add(inventory.path("ender_chest_contents"));
        inventory.path("backpack_contents").forEach(fields::add);
        fields.add(member.path("talisman_bag"));
        fields.add(member.path("inv_contents"));
        fields.add(member.path("ender_chest_contents"));
        return fields;
    }

    /** Alle Profile (aggregate) oder nur das im Spiel aktive, ersatzweise das erste. */
    private JsonNode relevantProfiles(JsonNode response, boolean aggregate) {
        JsonNode all = response.path("profiles");
        if (aggregate || !all.isArray() || all.isEmpty()) {
            return all;
        }
        for (JsonNode profile : all) {
            if (profile.path("selected").asBoolean(false)) {
                return mapper.createArrayNode().add(profile);
            }
        }
        return mapper.createArrayNode().add(all.get(0));
    }
}
