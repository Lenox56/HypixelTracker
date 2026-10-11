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
    void readsCraftedGeneratorsFromServerProgress() throws Exception {
        var progress = json("{'success':true,'minions':['WHEAT_1','WHEAT_12','COBBLESTONE_3']}");

        assertEquals(Set.of("WHEAT_1", "WHEAT_12", "COBBLESTONE_3"), service.readCraftedGenerators(progress));
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
