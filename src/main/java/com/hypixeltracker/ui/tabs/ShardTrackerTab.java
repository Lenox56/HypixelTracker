package com.hypixeltracker.ui.tabs;

import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.model.AttributeShard;
import com.hypixeltracker.service.HypixelApiService;
import com.hypixeltracker.service.NbtInventoryParser;
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
 * Zeigt an, welche Attribute-Shards der Spieler bereits besitzt
 * (per API/NBT ausgelesen) und welche laut Referenzliste noch fehlen.
 *
 * TODO: Verknuepfung mit der bestehenden Excel-Referenzliste aus dem
 * urspruenglichen AttributeTracker (A_Guide_to_Every_Attribute.xlsx),
 * um die "Soll"-Seite zu befuellen.
 */
public class ShardTrackerTab {

    private final HypixelApiService apiService;
    private final AppConfig config;
    private final NbtInventoryParser nbtParser = new NbtInventoryParser();

    private final ObservableList<AttributeShard> shards = FXCollections.observableArrayList();

    public ShardTrackerTab(HypixelApiService apiService, AppConfig config) {
        this.apiService = apiService;
        this.config = config;
    }

    public BorderPane build() {
        TableView<AttributeShard> table = new TableView<>(shards);

        TableColumn<AttributeShard, String> nameCol = new TableColumn<>("Attribut");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("attributeName"));

        TableColumn<AttributeShard, Integer> tierCol = new TableColumn<>("Stufe");
        tierCol.setCellValueFactory(new PropertyValueFactory<>("tier"));

        TableColumn<AttributeShard, Boolean> ownedCol = new TableColumn<>("Vorhanden");
        ownedCol.setCellValueFactory(new PropertyValueFactory<>("owned"));

        table.getColumns().addAll(nameCol, tierCol, ownedCol);

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
        // Laeuft bewusst synchron zur Uebersichtlichkeit im Grundgeruest.
        // TODO: auf einen Background-Task (javafx.concurrent.Task) umstellen,
        // damit die UI waehrend des API-Calls nicht einfriert.
        try {
            statusLabel.setText("Synchronisiere...");
            String uuid = apiService.resolveUuid(config.getMinecraftUsername());
            var profiles = apiService.getProfiles(uuid);

            shards.clear();
            // TODO: aus profiles -> members -> inv_contents / ender_chest_contents
            // etc. die Inventar-Rohdaten holen, mit nbtParser.extractItemIds(...)
            // dekodieren und mit der Attribut-Referenzliste abgleichen.

            statusLabel.setText("Synchronisation abgeschlossen (" + shards.size() + " Shards gefunden).");
        } catch (Exception ex) {
            statusLabel.setText("Fehler: " + ex.getMessage());
        }
    }
}
