package com.hypixeltracker.ui.tabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.model.AttributeShard;
import com.hypixeltracker.model.ShardRarity;
import com.hypixeltracker.service.AttributeService;
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

import java.io.File;
import java.util.List;

/**
 * Zeigt die Hunting-Attribute des Spielers (wie im Attribute Menu im Spiel):
 * Stufe, gesyphonte Shards und wie viele Shards bis zur naechsten Stufe fehlen.
 * Noch nicht freigeschaltete Attribute erscheinen mit Stufe 0 am Ende.
 */
public class ShardTrackerTab {

    /** Rohdaten des letzten Syncs, hilfreich falls Namen/Stufen nicht passen. */
    private static final File DEBUG_FILE = new File(System.getProperty("user.home"),
            ".hypixeltracker/debug/attributes.json");

    private final HypixelApiService apiService;
    private final AppConfig config;
    private final AttributeService attributeService = new AttributeService();

    private final ObservableList<AttributeShard> shards = FXCollections.observableArrayList();

    public ShardTrackerTab(HypixelApiService apiService, AppConfig config) {
        this.apiService = apiService;
        this.config = config;
    }

    public BorderPane build() {
        TableView<AttributeShard> table = new TableView<>(shards);

        TableColumn<AttributeShard, String> nameCol = new TableColumn<>("Attribut");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("attributeName"));
        nameCol.setPrefWidth(200);

        TableColumn<AttributeShard, String> shardCol = new TableColumn<>("Shard");
        shardCol.setCellValueFactory(new PropertyValueFactory<>("shardName"));
        shardCol.setPrefWidth(150);

        TableColumn<AttributeShard, String> rarityCol = new TableColumn<>("Seltenheit");
        rarityCol.setCellValueFactory(new PropertyValueFactory<>("rarity"));

        TableColumn<AttributeShard, Integer> tierCol = new TableColumn<>("Stufe");
        tierCol.setCellValueFactory(new PropertyValueFactory<>("tier"));
        tierCol.setCellFactory(col -> BackgroundSync.textCell(level -> level + " / " + ShardRarity.MAX_LEVEL));

        TableColumn<AttributeShard, Integer> stacksCol = new TableColumn<>("Gesyphont");
        stacksCol.setCellValueFactory(new PropertyValueFactory<>("stacks"));

        TableColumn<AttributeShard, Integer> nextCol = new TableColumn<>("Bis naechste Stufe");
        nextCol.setCellValueFactory(new PropertyValueFactory<>("shardsToNextLevel"));
        nextCol.setCellFactory(col -> BackgroundSync.textCell(n -> n < 0 ? "?" : n == 0 ? "max" : n + " Shards"));
        nextCol.setPrefWidth(140);

        table.getColumns().addAll(List.of(nameCol, shardCol, rarityCol, tierCol, stacksCol, nextCol));

        Label statusLabel = new Label("Noch nicht synchronisiert.");
        Button syncButton = new Button("Mit Hypixel-Account synchronisieren");
        syncButton.setOnAction(e -> sync(syncButton, statusLabel));

        VBox topBox = new VBox(8, syncButton, statusLabel);
        topBox.setPadding(new Insets(10));

        BorderPane pane = new BorderPane();
        pane.setTop(topBox);
        pane.setCenter(table);
        return pane;
    }

    private void sync(Button syncButton, Label statusLabel) {
        String username = config.getMinecraftUsername();
        boolean aggregate = config.isAggregateAllProfiles();
        if (!BackgroundSync.requireUsername(username, statusLabel)) {
            return;
        }

        BackgroundSync.run(syncButton, statusLabel, progress -> {
            String uuid = apiService.resolveUuid(username);
            JsonNode profiles = apiService.getProfiles(uuid);
            writeDebugFile(attributeService.rawAttributes(profiles, uuid));
            return attributeService.buildOverview(attributeService.readStacks(profiles, uuid, aggregate));
        }, result -> {
            shards.setAll(result);
            long unlocked = shards.stream().filter(AttributeShard::isOwned).count();
            long maxed = shards.stream().filter(s -> s.getTier() >= ShardRarity.MAX_LEVEL).count();
            statusLabel.setText("Synchronisation abgeschlossen: " + unlocked + " Attribute freigeschaltet, "
                    + maxed + " auf Stufe " + ShardRarity.MAX_LEVEL + ", "
                    + (shards.size() - unlocked) + " noch nicht freigeschaltet.");
        });
    }

    private static void writeDebugFile(JsonNode rawAttributes) {
        try {
            DEBUG_FILE.getParentFile().mkdirs();
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(DEBUG_FILE, rawAttributes);
        } catch (Exception e) {
            System.err.println("Konnte Debug-Datei nicht schreiben: " + e.getMessage());
        }
    }
}
