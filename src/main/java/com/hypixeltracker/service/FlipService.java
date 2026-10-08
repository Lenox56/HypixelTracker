package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.model.AuctionItem;
import com.hypixeltracker.model.BazaarProduct;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Findet Flip-Moeglichkeiten im Auktionshaus und im Bazaar. */
public class FlipService {

    /** Mindestens so viele Sofortkauf-Angebote, damit der Vergleichspreis aussagekraeftig ist. */
    static final int MIN_LISTINGS = 3;

    /**
     * AH-Flips: Der guenstigste Sofortkauf eines Items liegt deutlich unter dem
     * zweitguenstigsten. Gewinn = Weiterverkauf zum zweitguenstigsten Preis
     * abzueglich AH-Gebuehren minus Kaufpreis.
     */
    public List<AuctionItem> findAuctionFlips(Map<String, List<MarketDataService.Listing>> listingsByKey,
                                              double minPercentBelow, long minProfit) {
        List<AuctionItem> flips = new ArrayList<>();
        for (Map.Entry<String, List<MarketDataService.Listing>> entry : listingsByKey.entrySet()) {
            List<MarketDataService.Listing> listings = entry.getValue();
            if (listings.size() < MIN_LISTINGS) {
                continue;
            }
            MarketDataService.Listing cheapest = listings.get(0);
            long normal = listings.get(1).price();
            long profit = Math.round(normal - ahFees(normal)) - cheapest.price();

            AuctionItem flip = new AuctionItem(cheapest.auctionUuid(), cheapest.itemName(), entry.getKey(),
                    cheapest.price(), normal, listings.size(), profit);
            if (profit >= minProfit && flip.getPercentBelowNormal() >= minPercentBelow) {
                flips.add(flip);
            }
        }
        flips.sort(Comparator.comparingLong(AuctionItem::getProfit).reversed());
        return flips;
    }

    /**
     * AH-Gebuehren fuer den Weiterverkauf: Einstellgebuehr 1 % (unter 10 Mio.),
     * 2 % (unter 100 Mio.) bzw. 2,5 %, plus 1 % beim Einloesen ueber 1 Mio.
     */
    static double ahFees(long price) {
        double listingFee = price < 10_000_000 ? 0.01 : price < 100_000_000 ? 0.02 : 0.025;
        double claimFee = price > 1_000_000 ? 0.01 : 0;
        return price * (listingFee + claimFee);
    }

    /** Bazaar-Flips mit genug Handelsvolumen, sortiert nach Marge in Prozent. */
    public List<BazaarProduct> findBazaarFlips(JsonNode bazaar, Function<String, String> names, long minWeeklyVolume) {
        List<BazaarProduct> flips = new ArrayList<>();
        bazaar.path("products").fields().forEachRemaining(entry -> {
            JsonNode q = entry.getValue().path("quick_status");
            BazaarProduct product = new BazaarProduct(entry.getKey(), names.apply(entry.getKey()),
                    q.path("buyPrice").asDouble(), q.path("sellPrice").asDouble(),
                    q.path("buyMovingWeek").asLong(), q.path("sellMovingWeek").asLong());
            if (product.getSellPrice() > 0 && product.getMargin() > 0 && product.getWeeklyVolume() >= minWeeklyVolume) {
                flips.add(product);
            }
        });
        flips.sort(Comparator.comparingDouble(BazaarProduct::getMarginPercent).reversed());
        return flips;
    }
}
