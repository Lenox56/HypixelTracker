package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.model.BazaarProduct;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Findet Flip-Moeglichkeiten im Bazaar (Kauforder/Verkaufsangebot). */
public class FlipService {

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
