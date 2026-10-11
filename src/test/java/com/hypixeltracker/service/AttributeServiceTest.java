package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.model.AttributeShard;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.hypixeltracker.service.TestData.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeServiceTest {

    private final AttributeService service = new AttributeService();

    @Test
    void readsStacksFromServerProgress() throws Exception {
        JsonNode progress = json("{'success':true,'attributes':{'SHARD_GROVE':5,'C2':3}}");

        assertEquals(Map.of("SHARD_GROVE", 5, "C2", 3), service.readStacks(progress));
        assertEquals(Map.of(), service.readStacks(json("{'success':true}")));
    }

    @Test
    void buildsOverviewWithNamesLevelsAndMissingAttributes() {
        List<AttributeShard> list = service.buildOverview(Map.of("SHARD_GROVE", 5));

        assertEquals(ReferenceData.get().attributes().size(), list.size());
        AttributeShard grove = list.get(0);
        assertEquals("Nature Elemental", grove.getAttributeName());
        assertEquals("Grove", grove.getShardName());
        assertEquals("Common", grove.getRarity());
        assertEquals(2, grove.getTier());            // Common: Stufe 2 ab 4, Stufe 3 ab 9
        assertEquals(4, grove.getShardsToNextLevel());
        assertTrue(grove.isOwned());
        assertFalse(list.get(1).isOwned());
    }

    @Test
    void shardCodeKeysAreMatchedToo() {
        // Gleiches Attribut einmal als Shard-Code, einmal als Bazaar-Name: nur eine Zeile
        List<AttributeShard> list = service.buildOverview(Map.of("C1", 9, "SHARD_GROVE", 3));

        assertEquals(ReferenceData.get().attributes().size(), list.size());
        assertEquals("Nature Elemental", list.get(0).getAttributeName());
        assertEquals(3, list.get(0).getTier());
    }

    @Test
    void unknownShardIsStillShown() {
        List<AttributeShard> list = service.buildOverview(Map.of("SHARD_BRAND_NEW", 1));

        assertEquals("Brand New", list.get(0).getAttributeName());
        assertTrue(list.get(0).isOwned());
    }
}
