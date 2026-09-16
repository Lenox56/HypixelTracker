package com.hypixeltracker.ui.tabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.model.AuctionItem;
import com.hypixeltracker.model.BazaarProduct;
import com.hypixeltracker.service.HypixelApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Zwei Unter-Tabellen: Auktionen deutlich unter Normalpreis, und
 * Bazaar-Produkte mit der groessten Kauf/Verkauf-Marge (Flipping).
 *
 * TODO Normalpreis-Ermittlung: aktuell nur ein simpler Platzhalter
 * (z.B. gleitender Durchschnitt der letzten X Abfragen persistieren,
 * siehe PersistenceService). Fuer echte Treffsicherheit lohnt sich ein
 * Median über mehrere Tage statt nur des aktuellen Snapshots.
 */
public class AuctionBazaarTab {

    private final HypixelApiService apiService;
    private final ObservableList<AuctionItem> underpricedAuctions = FXCollections.observableArrayList();
    private final ObservableList<BazaarProduct> bazaarFlips = FXCollections.observableArrayList();

    public AuctionBazaarTab(HypixelApiService apiService) {
        this.apiService = apiService;
    }

    public BorderPane build() {
        TableView<AuctionItem> auctionTable = new TableView<>(underpricedAuctions);
        TableColumn<AuctionItem, String> itemCol = new TableColumn<>("Item");
        itemCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        TableColumn<AuctionItem, Long> bidCol = new TableColumn<>("Preis");
        bidCol.setCellValueFactory(new PropertyValueFactory<>("startingBid"));
        TableColumn<AuctionItem, Double> percentCol = new TableColumn<>("% unter Normalpreis");
        percentCol.setCellValueFactory(new PropertyValueFactory<>("percentBelowNormal"));
        auctionTable.getColumns().addAll(itemCol, bidCol, percentCol);

        TableView<BazaarProduct> bazaarTable = new TableView<>(bazaarFlips);
        TableColumn<BazaarProduct, String> productCol = new TableColumn<>("Produkt");
        productCol.setCellValueFactory(new PropertyValueFactory<>("productId"));
        TableColumn<BazaarProduct, Double> marginCol = new TableColumn<>("Marge (Coins)");
        marginCol.setCellValueFactory(new PropertyValueFactory<>("margin"));
        TableColumn<BazaarProduct, Double> marginPercentCol = new TableColumn<>("Marge (%)");
        marginPercentCol.setCellValueFactory(new PropertyValueFactory<>("marginPercent"));
        bazaarTable.getColumns().addAll(productCol, marginCol, marginPercentCol);

        Label statusLabel = new Label("Noch nicht geladen.");
        Button refreshButton = new Button("Preise aktualisieren");
        refreshButton.setOnAction(e -> refresh(statusLabel));

        Spinner<Integer> minPercentSpinner = new Spinner<>(1, 90, 20);
        HBoxHelper filterBox = new HBoxHelper("Mindest-Rabatt (%):", minPercentSpinner);

        VBox topBox = new VBox(8, refreshButton, filterBox.node, statusLabel);
        topBox.setPadding(new Insets(10));

        SplitPane splitPane = new SplitPane(auctionTable, bazaarTable);

        BorderPane pane = new BorderPane();
        pane.setTop(topBox);
        pane.setCenter(splitPane);
        return pane;
    }

    private void refresh(Label statusLabel) {
        try {
            statusLabel.setText("Lade Bazaar-Daten...");
            JsonNode bazaar = apiService.getBazaar();
            bazaarFlips.clear();
            bazaar.path("products").fields().forEachRemaining(entry -> {
                JsonNode quickStatus = entry.getValue().path("quick_status");
                double buy = quickStatus.path("buyPrice").asDouble();
                double sell = quickStatus.path("sellPrice").asDouble();
                long buyVolume = quickStatus.path("buyVolume").asLong();
                long sellVolume = quickStatus.path("sellVolume").asLong();
                bazaarFlips.add(new BazaarProduct(entry.getKey(), buy, sell, buyVolume, sellVolume));
            });
            bazaarFlips.sort((a, b) -> Double.compare(b.marginPercent(), a.marginPercent()));

            statusLabel.setText("Lade Auktionen (Seite 0)...");
            underpricedAuctions.clear();
            Map<String, Long> normalPriceCache = buildNormalPriceCache(bazaar);

            JsonNode auctions = apiService.getAuctions(0);
            for (JsonNode auction : auctions.path("auctions")) {
                if (!auction.path("bin").asBoolean(false)) continue; // nur BIN-Auktionen fuer Sofortkauf-Flipping

                String itemName = auction.path("item_name").asText();
                long startingBid = auction.path("starting_bid").asLong();

                // TODO: sauberere Normalpreis-Ermittlung statt Bazaar-Fallback -
                // idealerweise eigener rollierender Median aus AH-Snapshots
                // (siehe PersistenceService), da viele AH-Items nicht im Bazaar sind.
                long normalPrice = normalPriceCache.getOrDefault(itemName, 0L);
                if (normalPrice <= 0) continue;

                AuctionItem item = new AuctionItem(
                        auction.path("uuid").asText(),
                        itemName,
                        auction.path("item_id").asText(""),
                        startingBid,
                        true,
                        normalPrice
                );
                if (item.percentBelowNormal() >= 20) { // TODO: an Spinner-Wert aus der UI koppeln
                    underpricedAuctions.add(item);
                }
            }
            underpricedAuctions.sort((a, b) -> Double.compare(b.percentBelowNormal(), a.percentBelowNormal()));

            statusLabel.setText("Fertig: " + bazaarFlips.size() + " Bazaar-Produkte, "
                    + underpricedAuctions.size() + " guenstige Auktionen.");
        } catch (Exception ex) {
            statusLabel.setText("Fehler: " + ex.getMessage());
        }
    }

    /** Platzhalter-Normalpreis: nutzt den Bazaar-Kaufpreis als grobe Referenz, wo verfuegbar. */
    private Map<String, Long> buildNormalPriceCache(JsonNode bazaar) {
        Map<String, Long> cache = new HashMap<>();
        bazaar.path("products").fields().forEachRemaining(entry -> {
            double buyPrice = entry.getValue().path("quick_status").path("buyPrice").asDouble();
            cache.put(entry.getKey(), Math.round(buyPrice));
        });
        return cache;
    }

    /** Kleiner Helfer, um Label + Control kompakt nebeneinander zu bauen. */
    private static class HBoxHelper {
        final javafx.scene.layout.HBox node;

        HBoxHelper(String labelText, javafx.scene.Node control) {
            node = new javafx.scene.layout.HBox(8, new Label(labelText), control);
        }
    }
}
