package com.hypixeltracker.service;

import com.hypixeltracker.model.Minion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.hypixeltracker.service.TestData.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinionServiceTest {

    private final MinionService service = new MinionService();

    @Test
    void mergesCraftedGeneratorsOfAllCoopMembers() throws Exception {
        var profiles = json("""
                {'profiles':[{'selected':true,'members':{
                  'a':{'player_data':{'crafted_generators':['WHEAT_1','WHEAT_2']}},
                  'b':{'player_data':{'crafted_generators':['WHEAT_12','COBBLESTONE_3']}},
                  'c':{'crafted_generators':['CLAY_1']}
                }},{'selected':false,'members':{'a':{'player_data':{'crafted_generators':['SNOW_5']}}}}]}""");

        assertEquals(Set.of("WHEAT_1", "WHEAT_2", "WHEAT_12", "COBBLESTONE_3", "CLAY_1"),
                service.readCraftedGenerators(profiles, false));
        assertTrue(service.readCraftedGenerators(profiles, true).contains("SNOW_5"));
    }

    @Test
    void buildsOverviewForAllMinionTypes() {
        List<Minion> list = service.buildOverview(Set.of("WHEAT_7", "WHEAT_12", "COBBLESTONE_3", "SOMETHING"));

        assertEquals(ReferenceData.get().minionMaxTiers().size(), list.size());
        Minion wheat = list.stream().filter(m -> m.getMinionType().equals("WHEAT")).findFirst().orElseThrow();
        assertEquals(12, wheat.getCurrentTier());
        assertEquals(ReferenceData.get().minionMaxTiers().get("WHEAT"), wheat.getMaxTier());
        assertEquals("Wheat Minion", wheat.getDisplayName());
        // Unvollstaendige zuerst
        assertTrue(list.get(0).getMissingTiers() >= list.get(list.size() - 1).getMissingTiers());
    }

    @Test
    void minionNewerThanReferenceDataIsKept() {
        List<Minion> list = service.buildOverview(Set.of("BRAND_NEW_4"));

        assertTrue(list.stream().anyMatch(m -> m.getMinionType().equals("BRAND_NEW") && m.getCurrentTier() == 4));
    }
}
