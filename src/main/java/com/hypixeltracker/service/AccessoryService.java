package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.model.MissingItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ist/Soll-Abgleich der Accessoires.
 *
 * Ist: die "accessories" der Fortschrittsdaten vom Server (Item-IDs aus
 * Accessoire-Tasche, Inventar, Enderkiste und Rucksaecken; nur sichtbar, wenn
 * die Inventar-API in den SkyBlock-Einstellungen aktiviert ist).
 * Soll: alle Items der Kategorie ACCESSORY aus der Item-Datenbank, ohne die
 * in den Referenzdaten ignorierten Sonder-Items.
 */
public class AccessoryService {

    /** Ergebnis des Auslesens: gefundene IDs und ob ueberhaupt Inventardaten sichtbar waren. */
    public record OwnedItems(Set<String> ids, boolean inventoryApiEnabled) {
    }

    private final ReferenceData reference;

    public AccessoryService() {
        this(ReferenceData.get());
    }

    AccessoryService(ReferenceData reference) {
        this.reference = reference;
    }

    /** Vorhandene Accessoires aus den Fortschrittsdaten des Servers ("accessories"). */
    public OwnedItems readOwnedItems(JsonNode progress) {
        JsonNode accessories = progress.path("accessories");
        Set<String> ids = new HashSet<>();
        accessories.path("owned").forEach(n -> ids.add(n.asText()));
        return new OwnedItems(ids, accessories.path("inventoryApiEnabled").asBoolean(false));
    }

    /**
     * @param allItems Item-Datenbank (ID -> Info)
     * @param prices   ID -> aktueller Preis (Coins) oder fehlend/negativ, wenn unbekannt
     */
    public List<MissingItem> buildOverview(Set<String> ownedIds, Map<String, MarketDataService.ItemInfo> allItems,
                                           Map<String, Long> prices) {
        List<MissingItem> result = new ArrayList<>();
        for (MarketDataService.ItemInfo item : allItems.values()) {
            if (!"ACCESSORY".equals(item.category()) || reference.isIgnoredAccessory(item.id())) {
                continue;
            }
            List<String> upgrades = reference.accessoryUpgrades(item.id());
            MissingItem.Status status;
            if (ownedIds.contains(item.id())) {
                status = MissingItem.Status.OWNED;
            } else if (upgrades.stream().anyMatch(ownedIds::contains)) {
                status = MissingItem.Status.UPGRADE_OWNED;
            } else {
                status = MissingItem.Status.MISSING;
            }

            MissingItem row = new MissingItem(item.id(), item.name(), rarity(item.tier()), status,
                    prices.getOrDefault(item.id(), -1L));
            if (!upgrades.isEmpty()) {
                String next = upgrades.get(0);
                MarketDataService.ItemInfo nextInfo = allItems.get(next);
                row.setNote("Upgrade: " + (nextInfo != null ? nextInfo.name() : TextUtil.titleCase(next)));
            }
            result.add(row);
        }

        // Fehlende zuerst, darin die guenstigsten oben; unbekannte Preise ans Ende
        result.sort(Comparator.comparing(MissingItem::getStatus)
                .thenComparing(row -> row.getEstimatedCost() < 0 ? Double.MAX_VALUE : row.getEstimatedCost())
                .thenComparing(MissingItem::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static String rarity(String tier) {
        return tier == null || tier.isBlank() ? "" : TextUtil.titleCase(tier);
    }
}
