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
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
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

        TableColumn<AttributeShard, String> nameCol = new TableColumn<>("Shard");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("attributeName"));
        nameCol.setPrefWidth(220);

        TableColumn<AttributeShard, String> rarityCol = new TableColumn<>("Seltenheit");
        rarityCol.setCellValueFactory(new PropertyValueFactory<>("rarity"));

        TableColumn<AttributeShard, Integer> tierCol = new TableColumn<>("Stufe");
        tierCol.setCellValueFactory(new PropertyValueFactory<>("tier"));
        tierCol.setCellFactory(col -> textCell(level -> level + " / " + ShardRarity.MAX_LEVEL));

        TableColumn<AttributeShard, Integer> stacksCol = new TableColumn<>("Gesyphont");
        stacksCol.setCellValueFactory(new PropertyValueFactory<>("stacks"));

        TableColumn<AttributeShard, Integer> nextCol = new TableColumn<>("Bis naechste Stufe");
        nextCol.setCellValueFactory(new PropertyValueFactory<>("shardsToNextLevel"));
        nextCol.setCellFactory(col -> textCell(n -> n < 0 ? "?" : n == 0 ? "max" : n + " Shards"));
        nextCol.setPrefWidth(140);

        table.getColumns().addAll(List.of(nameCol, rarityCol, tierCol, stacksCol, nextCol));

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
        if (username == null || username.isBlank()) {
            statusLabel.setText("Bitte zuerst unter \"Einstellungen\" den Minecraft-Username eintragen.");
            return;
        }

        // Im Hintergrund, damit das Fenster waehrend der Anfragen nicht einfriert
        Task<List<AttributeShard>> task = new Task<>() {
            @Override
            protected List<AttributeShard> call() throws Exception {
                String uuid = apiService.resolveUuid(username);
                JsonNode profiles = apiService.getProfiles(uuid);
                writeDebugFile(attributeService.rawAttributes(profiles, uuid));
                JsonNode items = apiService.getItemResources();
                return attributeService.buildOverview(attributeService.readStacks(profiles, uuid, aggregate), items);
            }
        };

        task.setOnSucceeded(e -> {
            shards.setAll(task.getValue());
            long unlocked = shards.stream().filter(AttributeShard::isOwned).count();
            long maxed = shards.stream().filter(s -> s.getTier() >= ShardRarity.MAX_LEVEL).count();
            statusLabel.setText("Synchronisation abgeschlossen: " + unlocked + " Attribute freigeschaltet, "
                    + maxed + " auf Stufe " + ShardRarity.MAX_LEVEL + ", "
                    + (shards.size() - unlocked) + " noch nicht freigeschaltet.");
            syncButton.setDisable(false);
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            statusLabel.setText("Fehler: " + (ex == null ? "unbekannt" : ex.getMessage()));
            syncButton.setDisable(false);
        });

        syncButton.setDisable(true);
        statusLabel.setText("Synchronisiere...");
        Thread thread = new Thread(task, "attribute-sync");
        thread.setDaemon(true);
        thread.start();
    }

    private static void writeDebugFile(JsonNode rawAttributes) {
        try {
            DEBUG_FILE.getParentFile().mkdirs();
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(DEBUG_FILE, rawAttributes);
        } catch (Exception e) {
            System.err.println("Konnte Debug-Datei nicht schreiben: " + e.getMessage());
        }
    }

    private static <T> TableCell<AttributeShard, T> textCell(java.util.function.Function<T, String> format) {
        return new TableCell<>() {
            @Override
            protected void updateItem(T value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : format.apply(value));
            }
        };
    }
}
