package com.hypixeltracker.ui;

import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.service.HypixelApiService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * Einfacher Einstellungsdialog: Nutzer traegt HIER seinen eigenen
 * Hypixel-API-Key ein (siehe Policy-Hinweis: Key darf nicht fest
 * im Code/JAR eingebettet sein).
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

        TextField apiKeyField = new TextField(config.getHypixelApiKey());
        TextField usernameField = new TextField(config.getMinecraftUsername());
        CheckBox aggregateCheckBox = new CheckBox("Alle Profile zusammenfassen");
        aggregateCheckBox.setSelected(config.isAggregateAllProfiles());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Hypixel API-Key:"), apiKeyField);
        grid.addRow(1, new Label("Minecraft-Username:"), usernameField);
        grid.addRow(2, aggregateCheckBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                config.setHypixelApiKey(apiKeyField.getText().trim());
                config.setMinecraftUsername(usernameField.getText().trim());
                config.setAggregateAllProfiles(aggregateCheckBox.isSelected());
                config.save();
                apiService.setApiKey(config.getHypixelApiKey());
            }
        });
    }
}
