package com.hypixeltracker.ui;

import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.service.HypixelApiService;
import com.hypixeltracker.service.MarketDataService;
import com.hypixeltracker.service.WikiSearchService;
import com.hypixeltracker.ui.tabs.AccessoryTrackerTab;
import com.hypixeltracker.ui.tabs.BazaarFlipTab;
import com.hypixeltracker.ui.tabs.MinionTrackerTab;
import com.hypixeltracker.ui.tabs.ShardTrackerTab;
import com.hypixeltracker.ui.tabs.WikiSearchTab;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * Zentrale Klasse, die alle Modul-Tabs zusammensteckt.
 * Jeder Tab bekommt Zugriff auf dieselben Service-Instanzen,
 * damit z.B. die Server-Adresse nur einmal zentral verwaltet wird.
 */
public class MainView {

    private final AppConfig config;
    private final HypixelApiService apiService;
    private final MarketDataService marketData;
    private final WikiSearchService wikiService;

    public MainView(AppConfig config) {
        this.config = config;
        this.apiService = new HypixelApiService(config.getServerUrl());
        this.marketData = new MarketDataService(apiService);
        this.wikiService = new WikiSearchService();
    }

    public Parent build() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                new Tab("Attribute / Shards", new ShardTrackerTab(apiService, config).build()),
                new Tab("Accessoires", new AccessoryTrackerTab(apiService, marketData, config).build()),
                new Tab("Minions", new MinionTrackerTab(apiService, config).build()),
                new Tab("Bazaar Flipping", new BazaarFlipTab(marketData).build()),
                new Tab("Wiki-Suche", new WikiSearchTab(wikiService).build())
        );
        tabPane.getTabs().forEach(tab -> tab.setClosable(false));

        Button settingsButton = new Button("Einstellungen");
        settingsButton.setOnAction(e -> new SettingsDialog(config, apiService).showAndUpdate());

        HBox topBar = new HBox(settingsButton);
        topBar.setPadding(new Insets(8));

        // Pflicht laut Hypixel-API-Policy: klarstellen, dass kein Bezug zu Hypixel besteht
        Label disclaimer = new Label("Inoffizielles Fan-Projekt. Nicht mit Hypixel verbunden "
                + "und nicht von Hypixel unterstuetzt oder gebilligt.");
        disclaimer.setPadding(new Insets(4, 8, 4, 8));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);
        root.setBottom(disclaimer);
        return root;
    }
}
