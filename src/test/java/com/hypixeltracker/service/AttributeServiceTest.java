package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypixeltracker.model.AttributeShard;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeServiceTest {

    private static final String UUID = "069a79f444e94726a5befca90e38aaf5";

    private final ObjectMapper mapper = new ObjectMapper();
    private final AttributeService service = new AttributeService();

    private JsonNode json(String text) throws Exception {
        return mapper.readTree(text.replace('\'', '"'));
    }

    private JsonNode profiles() throws Exception {
        return json("""
                {'success':true,'profiles':[
                  {'selected':false,'members':{'%1$s':{'attributes':{'stacks':{'SHARD_SCARF':40,'SHARD_ZEALOT':2}}}}},
                  {'selected':true, 'members':{'%1$s':{'attributes':{'stacks':{'SHARD_SCARF':5}}},
                                               'otheruuid':{'attributes':{'stacks':{'SHARD_BONZO':99}}}}}
                ]}""".formatted(UUID));
    }

    private JsonNode items() throws Exception {
        return json("""
                {'success':true,'items':[
                  {'id':'SHARD_SCARF','name':'§5Scarf Shard','tier':'EPIC'},
                  {'id':'SHARD_ZEALOT','name':'Zealot Shard','tier':'RARE'},
                  {'id':'SHARD_BONZO','name':'Bonzo Shard','tier':'EPIC'},
                  {'id':'ASPECT_OF_THE_END','name':'Aspect of the End','tier':'RARE'}
                ]}""");
    }

    @Test
    void readsOnlySelectedProfileAndOwnMember() throws Exception {
        assertEquals(Map.of("SHARD_SCARF", 5), service.readStacks(profiles(), UUID, false));
    }

    @Test
    void aggregateTakesMaximumAcrossProfiles() throws Exception {
        assertEquals(Map.of("SHARD_SCARF", 40, "SHARD_ZEALOT", 2), service.readStacks(profiles(), UUID, true));
    }

    @Test
    void buildsOverviewWithLevelsAndMissingShards() throws Exception {
        List<AttributeShard> list = service.buildOverview(Map.of("SHARD_SCARF", 5), items());

        assertEquals(3, list.size()); // Scarf + 2 fehlende Shards, kein normales Item
        AttributeShard scarf = list.get(0);
        assertEquals("Scarf Shard", scarf.getAttributeName());
        assertEquals("Epic", scarf.getRarity());
        assertEquals(3, scarf.getTier());
        assertEquals(1, scarf.getShardsToNextLevel());
        assertTrue(scarf.isOwned());
        assertFalse(list.get(1).isOwned());
        assertEquals(0, list.get(1).getTier());
    }

    @Test
    void unknownKeyFormatStillShowsOwnedAttributesWithoutDuplicates() throws Exception {
        List<AttributeShard> list = service.buildOverview(Map.of("E54", 32, "C1", 3), items());

        assertEquals(2, list.size());
        assertEquals("E54", list.get(0).getAttributeName());
        assertEquals(10, list.get(0).getTier());
        assertEquals(1, list.get(1).getTier());
    }

    @Test
    void prettifiesIds() {
        assertEquals("Sea Serpent", AttributeService.prettify("SHARD_SEA_SERPENT"));
    }
}
