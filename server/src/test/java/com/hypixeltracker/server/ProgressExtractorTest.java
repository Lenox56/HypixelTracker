package com.hypixeltracker.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressExtractorTest {

    private static final String UUID = "069a79f444e94726a5befca90e38aaf5";

    private final ProgressExtractor extractor = new ProgressExtractor();
    private final ObjectMapper mapper = new ObjectMapper();

    private String profiles() throws IOException {
        return """
                {"success":true,"profiles":[
                  {"selected":false,"members":{"%1$s":{
                     "attributes":{"stacks":{"SHARD_GROVE":40,"SHARD_MIST":2}},
                     "player_data":{"crafted_generators":["SNOW_5"]},
                     "inventory":{"inv_contents":{"data":"%2$s"}}}}},
                  {"selected":true,"members":{"%1$s":{
                     "attributes":{"stacks":{"SHARD_GROVE":5}},
                     "player_data":{"crafted_generators":["WHEAT_1"]},
                     "inventory":{"bag_contents":{"talisman_bag":{"data":"%3$s"}},
                                  "backpack_contents":{"0":{"data":"%4$s"}}}},
                   "coopmate":{"crafted_generators":["CLAY_2"],
                               "attributes":{"stacks":{"SHARD_QUEEN_BEE":99}}}}}
                ]}""".formatted(UUID,
                NbtTestData.encodeItemIds("RED_CLAW_ARTIFACT"),
                NbtTestData.encodeItemIds("SPEED_RING", "CANDY_TALISMAN"),
                NbtTestData.encodeItemIds("POTATO_TALISMAN"));
    }

    @Test
    void selectedProfileOnly() throws Exception {
        JsonNode result = mapper.readTree(extractor.extract(profiles(), UUID, false));

        assertEquals(5, result.path("attributes").path("SHARD_GROVE").asInt());
        assertFalse(result.path("attributes").has("SHARD_QUEEN_BEE")); // gehoert dem Coop-Partner
        assertEquals(List.of("CLAY_2", "WHEAT_1"), texts(result.path("minions")));
        assertTrue(result.path("accessories").path("inventoryApiEnabled").asBoolean());
        assertEquals(List.of("CANDY_TALISMAN", "POTATO_TALISMAN", "SPEED_RING"),
                texts(result.path("accessories").path("owned")));
    }

    @Test
    void aggregateMergesAllProfiles() throws Exception {
        JsonNode result = mapper.readTree(extractor.extract(profiles(), UUID, true));

        assertEquals(40, result.path("attributes").path("SHARD_GROVE").asInt());
        assertEquals(2, result.path("attributes").path("SHARD_MIST").asInt());
        assertTrue(texts(result.path("minions")).contains("SNOW_5"));
        assertTrue(texts(result.path("accessories").path("owned")).contains("RED_CLAW_ARTIFACT"));
    }

    @Test
    void missingInventoryMeansApiDisabled() throws Exception {
        String json = "{\"profiles\":[{\"selected\":true,\"members\":{\"" + UUID + "\":{}}}]}";
        JsonNode result = mapper.readTree(extractor.extract(json, UUID, false));

        assertFalse(result.path("accessories").path("inventoryApiEnabled").asBoolean());
        assertEquals(0, result.path("accessories").path("owned").size());
    }

    @Test
    void nbtReaderRejectsGarbage() {
        assertThrows(Exception.class, () -> NbtItemReader.itemIds("bm8gZ3ppcA=="));
    }

    private static List<String> texts(JsonNode array) {
        return mapper().convertValue(array, mapper().getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper();
    }
}
