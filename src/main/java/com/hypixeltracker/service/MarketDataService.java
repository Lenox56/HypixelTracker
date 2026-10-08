package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import net.querz.nbt.tag.CompoundTag;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Marktdaten ohne API-Key: Bazaar, Item-Datenbank und ein kompletter Scan
 * aller Auktionshaus-Seiten. Ergebnisse werden kurz zwischengespeichert,
 * damit Accessoire- und Flipping-Tab das AH nicht doppelt laden.
 */
public class MarketDataService {

    /** Hypixel aktualisiert Bazaar und Auktionen etwa jede Minute. */
    private static final Duration MARKET_TTL = Duration.ofSeconds(60);
    private static final int PARALLEL_PAGE_LOADS = 4;

    /** Eine Sofortkauf-Auktion. key = ID fuer den Preisvergleich (z.B. "HYPERION"). */
    public record Listing(String auctionUuid, String key, String itemName, long price) {
    }

    /** Ein Item aus der Item-Datenbank. */
    public record ItemInfo(String id, String name, String tier, String category) {
    }

    private final HypixelApiService apiService;
    private final NbtInventoryParser nbtParser = new NbtInventoryParser();

    private Map<String, ItemInfo> items;
    private JsonNode bazaar;
    private Instant bazaarLoadedAt = Instant.EPOCH;
    private Map<String, List<Listing>> binListings;
    private Instant auctionsLoadedAt = Instant.EPOCH;

    public MarketDataService(HypixelApiService apiService) {
        this.apiService = apiService;
    }

    /** Item-Datenbank (ID -> Name, Seltenheit, Kategorie), einmal pro Programmstart geladen. */
    public synchronized Map<String, ItemInfo> items() throws IOException, InterruptedException {
        if (items == null) {
            Map<String, ItemInfo> map = new HashMap<>();
            for (JsonNode item : apiService.getItemResources().path("items")) {
                String id = item.path("id").asText("");
                if (!id.isEmpty()) {
                    map.put(id, new ItemInfo(id, ProfileUtil.stripColorCodes(item.path("name").asText(id)),
                            item.path("tier").asText(""), item.path("category").asText("")));
                }
            }
            items = Collections.unmodifiableMap(map);
        }
        return items;
    }

    /** Lesbarer Name zu einer ID, ersatzweise aus der ID gebildet. */
    public String displayName(String id) throws IOException, InterruptedException {
        ItemInfo info = items().get(id);
        return info != null ? info.name() : ProfileUtil.titleCase(id);
    }

    public synchronized JsonNode bazaar() throws IOException, InterruptedException {
        if (bazaar == null || Instant.now().isAfter(bazaarLoadedAt.plus(MARKET_TTL))) {
            bazaar = apiService.getBazaar();
            bazaarLoadedAt = Instant.now();
        }
        return bazaar;
    }

    /**
     * Alle Sofortkauf-Auktionen, gruppiert nach Preis-Key und aufsteigend sortiert.
     * Laedt beim ersten Aufruf (bzw. nach einer Minute) alle AH-Seiten.
     */
    public synchronized Map<String, List<Listing>> binListings(Consumer<String> progress)
            throws IOException, InterruptedException {
        if (binListings == null || Instant.now().isAfter(auctionsLoadedAt.plus(MARKET_TTL))) {
            binListings = loadAllBinListings(progress);
            auctionsLoadedAt = Instant.now();
        }
        return binListings;
    }

    private Map<String, List<Listing>> loadAllBinListings(Consumer<String> progress)
            throws IOException, InterruptedException {
        progress.accept("Lade Auktionshaus (Seite 1)...");
        JsonNode first = apiService.getAuctions(0);
        int totalPages = Math.max(1, first.path("totalPages").asInt(1));

        Map<String, List<Listing>> result = new HashMap<>();
        addListings(first, result);

        AtomicInteger done = new AtomicInteger(1);
        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_PAGE_LOADS);
        try {
            List<Future<JsonNode>> pages = new ArrayList<>();
            for (int page = 1; page < totalPages; page++) {
                int p = page;
                pages.add(pool.submit(() -> {
                    JsonNode node = apiService.getAuctions(p);
                    progress.accept("Lade Auktionshaus (Seite " + done.incrementAndGet() + " von " + totalPages + ")...");
                    return node;
                }));
            }
            for (Future<JsonNode> page : pages) {
                try {
                    addListings(page.get(), result);
                } catch (ExecutionException e) {
                    // Seiten koennen waehrend des Ladens wegfallen (AH aktualisiert sich) -> ueberspringen
                    System.err.println("AH-Seite uebersprungen: " + e.getCause());
                }
            }
        } finally {
            pool.shutdownNow();
        }

        result.values().forEach(list -> list.sort((a, b) -> Long.compare(a.price(), b.price())));
        return result;
    }

    private void addListings(JsonNode page, Map<String, List<Listing>> result) {
        for (JsonNode auction : page.path("auctions")) {
            if (!auction.path("bin").asBoolean(false) || auction.path("claimed").asBoolean(false)) {
                continue;
            }
            String key = priceKey(auction.path("item_bytes").asText(null));
            if (key == null) {
                continue;
            }
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(new Listing(
                    auction.path("uuid").asText(),
                    key,
                    ProfileUtil.stripColorCodes(auction.path("item_name").asText(key)),
                    auction.path("starting_bid").asLong()));
        }
    }

    /**
     * Key fuer den Preisvergleich aus den Item-Daten einer Auktion:
     * normale Items ueber ihre SkyBlock-ID, Verzauberungsbuecher mit genau
     * einer Verzauberung als "ENCHANTMENT_NAME_STUFE". Haustiere werden
     * uebersprungen, weil ihr Preis stark vom Level abhaengt.
     */
    String priceKey(String itemBytes) {
        try {
            List<CompoundTag> decoded = nbtParser.decodeItems(itemBytes);
            if (decoded.isEmpty()) {
                return null;
            }
            CompoundTag item = decoded.get(0);
            String id = NbtInventoryParser.itemId(item);
            if (id == null || id.equals("PET")) {
                return null;
            }
            if (id.equals("ENCHANTED_BOOK")) {
                CompoundTag enchantments = NbtInventoryParser.extraAttributes(item).getCompoundTag("enchantments");
                if (enchantments == null || enchantments.size() != 1) {
                    return null;
                }
                String name = enchantments.keySet().iterator().next();
                return "ENCHANTMENT_" + name.toUpperCase(Locale.ROOT) + "_" + enchantments.getInt(name);
            }
            return id;
        } catch (Exception e) {
            return null;
        }
    }
}
