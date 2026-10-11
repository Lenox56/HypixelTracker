package com.hypixeltracker.service;

import com.hypixeltracker.model.BazaarProduct;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.hypixeltracker.service.TestData.encodeItems;
import static com.hypixeltracker.service.TestData.item;
import static com.hypixeltracker.service.TestData.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlipServiceTest {

    private final FlipService service = new FlipService();

    @Test
    void bazaarFlipsNeedVolumeAndPositiveMarginAfterTax() throws Exception {
        var bazaar = json("""
                {'products':{
                  'ENCHANTED_DIAMOND':{'quick_status':{'buyPrice':200,'sellPrice':150,'buyMovingWeek':500000,'sellMovingWeek':400000}},
                  'DEAD_ITEM':{'quick_status':{'buyPrice':1000,'sellPrice':1,'buyMovingWeek':3,'sellMovingWeek':2}},
                  'TIGHT':{'quick_status':{'buyPrice':100,'sellPrice':99,'buyMovingWeek':500000,'sellMovingWeek':500000}}
                }}""");

        List<BazaarProduct> flips = service.findBazaarFlips(bazaar, id -> "Name " + id, 10_000);

        assertEquals(1, flips.size());
        BazaarProduct diamond = flips.get(0);
        assertEquals("Name ENCHANTED_DIAMOND", diamond.getDisplayName());
        assertEquals(200 * 0.9875 - 150, diamond.getMargin(), 0.001);
        assertEquals(400_000, diamond.getWeeklyVolume());
    }

    /** Preis-Key fuer die AH-Preise fehlender Accessoires. */
    @Test
    void priceKeyFromAuctionItemBytes() throws Exception {
        MarketDataService market = new MarketDataService(null);

        assertEquals("HYPERION", market.priceKey(encodeItems(item("HYPERION"))));
        assertEquals("ENCHANTMENT_SHARPNESS_6",
                market.priceKey(encodeItems(item("ENCHANTED_BOOK", Map.of("sharpness", 6)))));
        assertNull(market.priceKey(encodeItems(item("ENCHANTED_BOOK", Map.of("sharpness", 6, "smite", 6)))));
        assertNull(market.priceKey(encodeItems(item("PET"))));
        assertNull(market.priceKey("kein base64"));
        assertTrue(market.priceKey(encodeItems(item("ASPECT_OF_THE_END"))).startsWith("ASPECT"));
    }
}
