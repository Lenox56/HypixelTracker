package com.hypixeltracker.service;

import com.hypixeltracker.model.AuctionItem;
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

    private static MarketDataService.Listing listing(String key, long price) {
        return new MarketDataService.Listing("uuid-" + price, key, key, price);
    }

    @Test
    void findsAuctionFlipsAgainstSecondCheapest() {
        Map<String, List<MarketDataService.Listing>> listings = Map.of(
                "HYPERION", List.of(listing("HYPERION", 800_000_000), listing("HYPERION", 1_000_000_000),
                        listing("HYPERION", 1_010_000_000)),
                "DIRT", List.of(listing("DIRT", 90), listing("DIRT", 100), listing("DIRT", 100)),
                "RARE_THING", List.of(listing("RARE_THING", 1), listing("RARE_THING", 1_000_000)));

        List<AuctionItem> flips = service.findAuctionFlips(listings, 10, 100_000);

        assertEquals(1, flips.size()); // DIRT: zu wenig Gewinn, RARE_THING: zu wenige Angebote
        AuctionItem hyperion = flips.get(0);
        assertEquals(1_000_000_000, hyperion.getNormalPrice());
        assertEquals(1_000_000_000 - 35_000_000 - 800_000_000, hyperion.getProfit()); // 2,5 % + 1 % Gebuehr
        assertEquals(20.0, hyperion.getPercentBelowNormal(), 0.001);
        assertEquals("/viewauction uuid-800000000", hyperion.getViewCommand());
    }

    @Test
    void ahFeesDependOnPrice() {
        assertEquals(1_000, FlipService.ahFees(100_000), 0.001);
        assertEquals(600_000, FlipService.ahFees(20_000_000), 0.001); // 2 % + 1 %
    }

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
