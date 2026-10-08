package com.hypixeltracker.ui.tabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.model.MissingItem;
import com.hypixeltracker.service.AccessoryService;
import com.hypixeltracker.service.HypixelApiService;
import com.hypixeltracker.service.MarketDataService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Ist/Soll-Abgleich fuer Accessoires: welche fehlen noch, welche sind durch
 * eine hoehere Stufe abgedeckt, und was kosten die fehlenden aktuell
 * (Bazaar-Sofortkauf bzw. guenstigster AH-Sofortkauf).
 */
public class AccessoryTrackerTab {

    private record SyncResult(List<MissingItem> rows, int ownedCount, boolean inventoryApiEnabled) {
    }

    private final HypixelApiService apiService;
    private final MarketDataService marketData;
    private final AppConfig config;
    private final AccessoryService accessoryService = new AccessoryService();
    private final ObservableList<MissingItem> items = FXCollections.observableArrayList();
    private final FilteredList<MissingItem> visibleItems = new FilteredList<>(items);

    public AccessoryTrackerTab(HypixelApiService apiService, MarketDataService marketData, AppConfig config) {
        this.apiService = apiService;
        this.marketData = marketData;
        this.config = config;
    }

    public BorderPane build() {
        TableView<MissingItem> table = new TableView<>(visibleItems);

        TableColumn<MissingItem, String> nameCol = new TableColumn<>("Accessoire");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        nameCol.setPrefWidth(220);

        TableColumn<MissingItem, String> rarityCol = new TableColumn<>("Seltenheit");
        rarityCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<MissingItem, MissingItem.Status> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(170);

        TableColumn<MissingItem, Double> costCol = new TableColumn<>("Preis (Coins)");
        costCol.setCellValueFactory(new PropertyValueFactory<>("estimatedCost"));
        costCol.setCellFactory(col -> BackgroundSync.textCell(BackgroundSync::coins));
        costCol.setPrefWidth(120);

        TableColumn<MissingItem, String> noteCol = new TableColumn<>("Hinweis");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteCol.setPrefWidth(250);

        table.getColumns().addAll(List.of(nameCol, rarityCol, statusCol, costCol, noteCol));

        Label statusLabel = new Label("Noch nicht synchronisiert.");
        Button syncButton = new Button("Mit Hypixel-Account synchronisieren");
        CheckBox loadPrices = new CheckBox("Preise aus Bazaar/AH laden (AH-Scan dauert etwas)");
        loadPrices.setSelected(true);
        CheckBox onlyMissing = new CheckBox("Nur fehlende anzeigen");
        onlyMissing.setSelected(true);
        onlyMissing.selectedProperty().addListener((obs, old, only) -> applyFilter(only));
        applyFilter(true);

        syncButton.setOnAction(e -> sync(syncButton, statusLabel, loadPrices.isSelected()));

        VBox topBox = new VBox(8, syncButton, new HBox(16, loadPrices, onlyMissing), statusLabel);
        topBox.setPadding(new Insets(10));

        BorderPane pane = new BorderPane();
        pane.setTop(topBox);
        pane.setCenter(table);
        return pane;
    }

    private void applyFilter(boolean onlyMissing) {
        visibleItems.setPredicate(row -> !onlyMissing || row.getStatus() == MissingItem.Status.MISSING);
    }

    private void sync(Button syncButton, Label statusLabel, boolean loadPrices) {
        String username = config.getMinecraftUsername();
        boolean aggregate = config.isAggregateAllProfiles();
        if (!BackgroundSync.requireUsername(username, statusLabel)) {
            return;
        }

        BackgroundSync.run(syncButton, statusLabel, progress -> {
            String uuid = apiService.resolveUuid(username);
            JsonNode profiles = apiService.getProfiles(uuid);
            AccessoryService.OwnedItems owned = accessoryService.readOwnedItems(profiles, uuid, aggregate);

            progress.accept("Lade Item-Datenbank...");
            Map<String, MarketDataService.ItemInfo> allItems = marketData.items();
            Map<String, Long> prices = loadPrices ? loadPrices(allItems, progress) : Map.of();

            List<MissingItem> rows = accessoryService.buildOverview(owned.ids(), allItems, prices);
            int ownedCount = (int) rows.stream().filter(r -> r.getStatus() == MissingItem.Status.OWNED).count();
            return new SyncResult(rows, ownedCount, owned.inventoryApiEnabled());
        }, result -> {
            items.setAll(result.rows());
            if (!result.inventoryApiEnabled()) {
                statusLabel.setText("Keine Inventardaten sichtbar. Bitte im Spiel unter SkyBlock-Menue -> Settings -> "
                        + "API Settings die Inventar-API aktivieren und in ein paar Minuten erneut synchronisieren.");
                return;
            }
            long missing = result.rows().stream().filter(r -> r.getStatus() == MissingItem.Status.MISSING).count();
            double missingCost = result.rows().stream()
                    .filter(r -> r.getStatus() == MissingItem.Status.MISSING && r.getEstimatedCost() > 0)
                    .mapToDouble(MissingItem::getEstimatedCost).sum();
            statusLabel.setText("Synchronisation abgeschlossen: " + result.ownedCount() + " Accessoires vorhanden, "
                    + missing + " fehlen" + (missingCost > 0 ? " (zusammen ca. " + BackgroundSync.coins(missingCost)
                    + " Coins, soweit Preise bekannt)" : "") + ".");
        });
    }

    /** Preis je Accessoire: Bazaar-Sofortkauf, sonst guenstigster AH-Sofortkauf. */
    private Map<String, Long> loadPrices(Map<String, MarketDataService.ItemInfo> allItems,
                                         Consumer<String> progress) throws Exception {
        progress.accept("Lade Bazaar-Preise...");
        JsonNode products = marketData.bazaar().path("products");
        Map<String, List<MarketDataService.Listing>> listings = marketData.binListings(progress);

        Map<String, Long> prices = new HashMap<>();
        for (MarketDataService.ItemInfo item : allItems.values()) {
            if (!"ACCESSORY".equals(item.category())) continue;
            double bazaarPrice = products.path(item.id()).path("quick_status").path("buyPrice").asDouble(0);
            if (bazaarPrice > 0) {
                prices.put(item.id(), Math.round(bazaarPrice));
            } else if (listings.containsKey(item.id())) {
                prices.put(item.id(), listings.get(item.id()).get(0).price());
            }
        }
        return prices;
    }
}
