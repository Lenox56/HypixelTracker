package com.hypixeltracker.ui;

import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.service.HypixelApiService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * Einfacher Einstellungsdialog. Bewusst OHNE Feld fuer einen Hypixel-API-Key:
 * laut API-Policy duerfen Nutzer keine Keys in die App eintragen, der Key
 * liegt ausschliesslich auf dem eigenen Server.
 */
public class SettingsDialog {

    private final AppConfig config;
    private final HypixelApiService apiService;

    public SettingsDialog(AppConfig config, HypixelApiService apiService) {
        this.config = config;
        this.apiService = apiService;
    }

    public void showAndUpdate() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Einstellungen");

        TextField serverUrlField = new TextField(config.getServerUrl());
        TextField usernameField = new TextField(config.getMinecraftUsername());
        CheckBox aggregateCheckBox = new CheckBox("Alle Profile zusammenfassen");
        aggregateCheckBox.setSelected(config.isAggregateAllProfiles());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Minecraft-Username:"), usernameField);
        grid.addRow(1, aggregateCheckBox);
        grid.addRow(2, new Label("Server (nur bei Bedarf aendern):"), serverUrlField);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String serverUrl = serverUrlField.getText().trim();
                config.setServerUrl(serverUrl.isEmpty() ? AppConfig.DEFAULT_SERVER_URL : serverUrl);
                config.setMinecraftUsername(usernameField.getText().trim());
                config.setAggregateAllProfiles(aggregateCheckBox.isSelected());
                config.save();
                apiService.setServerUrl(config.getServerUrl());
            }
        });
    }
}
