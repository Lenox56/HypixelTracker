package com.hypixeltracker.ui;

import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.service.HypixelApiService;
import com.hypixeltracker.service.WikiSearchService;
import com.hypixeltracker.ui.tabs.AccessoryTrackerTab;
import com.hypixeltracker.ui.tabs.AuctionBazaarTab;
import com.hypixeltracker.ui.tabs.MinionTrackerTab;
import com.hypixeltracker.ui.tabs.ShardTrackerTab;
import com.hypixeltracker.ui.tabs.WikiSearchTab;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * Zentrale Klasse, die alle Modul-Tabs zusammensteckt.
 * Jeder Tab bekommt Zugriff auf dieselben Service-Instanzen,
 * damit z.B. der API-Key nur einmal zentral verwaltet wird.
 */
public class MainView {

    private final AppConfig config;
    private final HypixelApiService apiService;
    private final WikiSearchService wikiService;

    public MainView(AppConfig config) {
        this.config = config;
        this.apiService = new HypixelApiService(config.getHypixelApiKey());
        this.wikiService = new WikiSearchService();
    }

    public Parent build() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                new Tab("Attribute / Shards", new ShardTrackerTab(apiService, config).build()),
                new Tab("Accessoires", new AccessoryTrackerTab(apiService, config).build()),
                new Tab("Minions", new MinionTrackerTab(apiService, config).build()),
                new Tab("AH / Bazaar Flipping", new AuctionBazaarTab(apiService).build()),
                new Tab("Wiki-Suche", new WikiSearchTab(wikiService).build())
        );
        tabPane.getTabs().forEach(tab -> tab.setClosable(false));

        Button settingsButton = new Button("Einstellungen (API-Key)");
        settingsButton.setOnAction(e -> new SettingsDialog(config, apiService).showAndUpdate());

        HBox topBar = new HBox(settingsButton);
        topBar.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);
        return root;
    }
}
