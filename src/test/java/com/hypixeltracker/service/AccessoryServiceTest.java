package com.hypixeltracker.service;

import com.hypixeltracker.model.MissingItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.hypixeltracker.service.TestData.UUID;
import static com.hypixeltracker.service.TestData.encodeItems;
import static com.hypixeltracker.service.TestData.item;
import static com.hypixeltracker.service.TestData.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessoryServiceTest {

    private final AccessoryService service = new AccessoryService();

    @Test
    void readsAccessoriesFromBagInventoryAndBackpacks() throws Exception {
        var profiles = json("""
                {'profiles':[{'selected':true,'members':{'%s':{'inventory':{
                  'bag_contents':{'talisman_bag':{'type':0,'data':'%s'}},
                  'inv_contents':{'type':0,'data':'%s'},
                  'backpack_contents':{'0':{'type':0,'data':'%s'}}
                }}}}]}""".formatted(UUID,
                encodeItems(item("SPEED_RING"), item("CANDY_TALISMAN")),
                encodeItems(item("ASPECT_OF_THE_END")),
                encodeItems(item("RED_CLAW_ARTIFACT"))));

        AccessoryService.OwnedItems owned = service.readOwnedItems(profiles, UUID, false);

        assertTrue(owned.inventoryApiEnabled());
        assertEquals(Set.of("SPEED_RING", "CANDY_TALISMAN", "ASPECT_OF_THE_END", "RED_CLAW_ARTIFACT"), owned.ids());
    }

    @Test
    void detectsDisabledInventoryApi() throws Exception {
        var profiles = json("{'profiles':[{'selected':true,'members':{'%s':{}}}]}".formatted(UUID));

        assertFalse(service.readOwnedItems(profiles, UUID, false).inventoryApiEnabled());
    }

    @Test
    void marksUpgradesAndSortsMissingByPrice() {
        Map<String, MarketDataService.ItemInfo> items = Map.of(
                "SPEED_TALISMAN", new MarketDataService.ItemInfo("SPEED_TALISMAN", "Speed Talisman", "COMMON", "ACCESSORY"),
                "SPEED_RING", new MarketDataService.ItemInfo("SPEED_RING", "Speed Ring", "UNCOMMON", "ACCESSORY"),
                "SPEED_ARTIFACT", new MarketDataService.ItemInfo("SPEED_ARTIFACT", "Speed Artifact", "RARE", "ACCESSORY"),
                "CANDY_TALISMAN", new MarketDataService.ItemInfo("CANDY_TALISMAN", "Candy Talisman", "COMMON", "ACCESSORY"),
                "PUNCHCARD_ARTIFACT", new MarketDataService.ItemInfo("PUNCHCARD_ARTIFACT", "Punchcard", "RARE", "ACCESSORY"),
                "HYPERION", new MarketDataService.ItemInfo("HYPERION", "Hyperion", "LEGENDARY", "SWORD"));
        Map<String, Long> prices = Map.of("SPEED_ARTIFACT", 50_000L, "CANDY_TALISMAN", 1_000L);

        List<MissingItem> rows = service.buildOverview(Set.of("SPEED_RING"), items, prices);

        // Kein Schwert, kein ignoriertes Sonder-Accessoire
        assertEquals(4, rows.size());
        assertEquals("CANDY_TALISMAN", rows.get(0).getInternalName()); // fehlt, guenstigster zuerst
        assertEquals("SPEED_ARTIFACT", rows.get(1).getInternalName());
        assertEquals(MissingItem.Status.UPGRADE_OWNED, status(rows, "SPEED_TALISMAN"));
        assertEquals(MissingItem.Status.OWNED, status(rows, "SPEED_RING"));
        assertEquals("Upgrade: Speed Artifact", rows.stream()
                .filter(r -> r.getInternalName().equals("SPEED_RING")).findFirst().orElseThrow().getNote());
    }

    private static MissingItem.Status status(List<MissingItem> rows, String id) {
        return rows.stream().filter(r -> r.getInternalName().equals(id)).findFirst().orElseThrow().getStatus();
    }
}
