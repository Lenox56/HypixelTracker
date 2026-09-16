package com.hypixeltracker.ui.tabs;

import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.model.MissingItem;
import com.hypixeltracker.service.HypixelApiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Ist/Soll-Abgleich fuer Accessoires (talisman_bag), inkl. Status
 * ob ein fehlendes Accessoire aktuell craftbar ist und Materialkosten
 * ueber Bazaar/AH-Preise.
 *
 * TODO: Rezeptdaten aus /resources/skyblock/items einbinden und
 * gegen das Spieler-Inventar (Materialien) abgleichen.
 */
public class AccessoryTrackerTab {

    private final HypixelApiService apiService;
    private final AppConfig config;
    private final ObservableList<MissingItem> items = FXCollections.observableArrayList();

    public AccessoryTrackerTab(HypixelApiService apiService, AppConfig config) {
        this.apiService = apiService;
        this.config = config;
    }

    public BorderPane build() {
        TableView<MissingItem> table = new TableView<>(items);

        TableColumn<MissingItem, String> nameCol = new TableColumn<>("Accessoire");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("displayName"));

        TableColumn<MissingItem, MissingItem.Status> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<MissingItem, Double> costCol = new TableColumn<>("Kosten (Coins)");
        costCol.setCellValueFactory(new PropertyValueFactory<>("estimatedCost"));

        table.getColumns().addAll(nameCol, statusCol, costCol);

        Label statusLabel = new Label("Noch nicht synchronisiert.");
        Button syncButton = new Button("Mit Hypixel-Account synchronisieren");
        syncButton.setOnAction(e -> sync(statusLabel));

        VBox topBox = new VBox(8, syncButton, statusLabel);
        topBox.setPadding(new Insets(10));

        BorderPane pane = new BorderPane();
        pane.setTop(topBox);
        pane.setCenter(table);
        return pane;
    }

    private void sync(Label statusLabel) {
        try {
            statusLabel.setText("Synchronisiere...");
            String uuid = apiService.resolveUuid(config.getMinecraftUsername());
            var profiles = apiService.getProfiles(uuid);
            var itemResources = apiService.getItemResources();

            items.clear();
            // TODO:
            // 1) talisman_bag je Profil (ggf. ueber alle Profile vereinigen, siehe
            //    config.isAggregateAllProfiles()) mit NbtInventoryParser auslesen.
            // 2) Vollstaendige Accessoire-Liste aus itemResources ziehen.
            // 3) Diff bilden -> MissingItem-Eintraege mit Status OWNED/CRAFTABLE/
            //    MISSING_MATS/DROP_ONLY befuellen.
            // 4) Fuer fehlende Materialien Bazaar-/AH-Preise via PriceService
            //    (noch zu ergaenzen) zu estimatedCost aufsummieren.

            statusLabel.setText("Synchronisation abgeschlossen (" + items.size() + " Eintraege).");
        } catch (Exception ex) {
            statusLabel.setText("Fehler: " + ex.getMessage());
        }
    }
}
