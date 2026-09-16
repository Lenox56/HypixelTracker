package com.hypixeltracker.ui.tabs;

import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.model.Minion;
import com.hypixeltracker.service.HypixelApiService;
import com.hypixeltracker.service.MinionReferenceData;
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

import java.util.HashSet;
import java.util.Set;

/**
 * Zeigt pro Minion-Typ die aktuell erreichte vs. maximale Tier-Stufe.
 * crafted_generators wird coop-weit (ueber alle Mitglieder eines
 * Profils) vereinigt, siehe Kommentar in sync().
 */
public class MinionTrackerTab {

    private final HypixelApiService apiService;
    private final AppConfig config;
    private final ObservableList<Minion> minions = FXCollections.observableArrayList();

    public MinionTrackerTab(HypixelApiService apiService, AppConfig config) {
        this.apiService = apiService;
        this.config = config;
    }

    public BorderPane build() {
        TableView<Minion> table = new TableView<>(minions);

        TableColumn<Minion, String> nameCol = new TableColumn<>("Minion");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("displayName"));

        TableColumn<Minion, Integer> currentCol = new TableColumn<>("Aktuelle Stufe");
        currentCol.setCellValueFactory(new PropertyValueFactory<>("currentTier"));

        TableColumn<Minion, Integer> maxCol = new TableColumn<>("Max. Stufe");
        maxCol.setCellValueFactory(new PropertyValueFactory<>("maxTier"));

        table.getColumns().addAll(nameCol, currentCol, maxCol);

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

            // crafted_generators ist pro Coop gemeinsam - daher ueber alle
            // "members" eines Profils vereinigen (Set statt einfacher Liste).
            Set<String> craftedAcrossCoop = new HashSet<>();
            // TODO: fuer jedes Profil -> "members" durchlaufen -> jeweils
            // "crafted_generators" (Array von Strings wie "WHEAT_7") einsammeln
            // und in craftedAcrossCoop mergen.

            minions.clear();
            for (var entry : MinionReferenceData.MAX_TIER.entrySet()) {
                String type = entry.getKey();
                int maxTier = entry.getValue();
                int currentTier = highestTierFor(type, craftedAcrossCoop);
                minions.add(new Minion(type, MinionReferenceData.displayName(type), currentTier, maxTier));
            }

            statusLabel.setText("Synchronisation abgeschlossen (" + minions.size() + " Minion-Typen).");
        } catch (Exception ex) {
            statusLabel.setText("Fehler: " + ex.getMessage());
        }
    }

    private int highestTierFor(String minionType, Set<String> craftedEntries) {
        int highest = 0;
        for (String entry : craftedEntries) {
            // Format ist z.B. "WHEAT_7" -> Typ + Tier
            int lastUnderscore = entry.lastIndexOf('_');
            if (lastUnderscore < 0) continue;
            String type = entry.substring(0, lastUnderscore);
            if (!type.equals(minionType)) continue;
            try {
                int tier = Integer.parseInt(entry.substring(lastUnderscore + 1));
                highest = Math.max(highest, tier);
            } catch (NumberFormatException ignored) {
            }
        }
        return highest;
    }
}
