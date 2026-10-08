package com.hypixeltracker.ui.tabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.model.Minion;
import com.hypixeltracker.service.HypixelApiService;
import com.hypixeltracker.service.MinionService;
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

import java.util.List;
import java.util.Set;

/**
 * Zeigt pro Minion-Typ die aktuell erreichte vs. maximale Stufe,
 * coop-weit zusammengefasst. Unvollstaendige Minions stehen oben.
 */
public class MinionTrackerTab {

    private record SyncResult(int uniqueMinions, List<Minion> minions) {
    }

    private final HypixelApiService apiService;
    private final AppConfig config;
    private final MinionService minionService = new MinionService();
    private final ObservableList<Minion> minions = FXCollections.observableArrayList();

    public MinionTrackerTab(HypixelApiService apiService, AppConfig config) {
        this.apiService = apiService;
        this.config = config;
    }

    public BorderPane build() {
        TableView<Minion> table = new TableView<>(minions);

        TableColumn<Minion, String> nameCol = new TableColumn<>("Minion");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        nameCol.setPrefWidth(200);

        TableColumn<Minion, Integer> currentCol = new TableColumn<>("Aktuelle Stufe");
        currentCol.setCellValueFactory(new PropertyValueFactory<>("currentTier"));
        currentCol.setCellFactory(col -> BackgroundSync.textCell(t -> t == 0 ? "-" : String.valueOf(t)));

        TableColumn<Minion, Integer> maxCol = new TableColumn<>("Max. Stufe");
        maxCol.setCellValueFactory(new PropertyValueFactory<>("maxTier"));

        TableColumn<Minion, Integer> missingCol = new TableColumn<>("Fehlende Stufen");
        missingCol.setCellValueFactory(new PropertyValueFactory<>("missingTiers"));
        missingCol.setCellFactory(col -> BackgroundSync.textCell(n -> n == 0 ? "fertig" : String.valueOf(n)));

        table.getColumns().addAll(List.of(nameCol, currentCol, maxCol, missingCol));

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
            Set<String> crafted = minionService.readCraftedGenerators(profiles, aggregate);
            return new SyncResult(crafted.size(), minionService.buildOverview(crafted));
        }, result -> {
            List<Minion> list = result.minions();
            minions.setAll(list);
            long maxed = list.stream().filter(m -> m.getMissingTiers() == 0 && m.getCurrentTier() > 0).count();
            int missingTotal = list.stream().mapToInt(Minion::getMissingTiers).sum();
            statusLabel.setText("Synchronisation abgeschlossen: " + result.uniqueMinions() + " einzigartige Minions gebaut, "
                    + maxed + " von " + list.size() + " Minion-Typen auf Max-Stufe, "
                    + missingTotal + " Stufen fehlen insgesamt.");
        });
    }
}
