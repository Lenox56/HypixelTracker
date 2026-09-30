package com.hypixeltracker;

import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Einstiegspunkt der App. Baut nur das Fenster auf und delegiert
 * den eigentlichen UI-Aufbau an MainView.
 */
public class Main extends Application {

    /** Mehrere Groessen, damit Titelleiste und Taskleiste jeweils ein scharfes Icon bekommen. */
    private static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 128, 256};

    @Override
    public void start(Stage primaryStage) {
        AppConfig config = AppConfig.loadOrCreate();

        MainView mainView = new MainView(config);
        Scene scene = new Scene(mainView.build(), 1100, 750);

        primaryStage.setTitle("Hypixel Tracker");
        addIcons(primaryStage);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void addIcons(Stage stage) {
        for (int size : ICON_SIZES) {
            try (InputStream in = getClass().getResourceAsStream("/icons/app-" + size + ".png")) {
                if (in != null) {
                    stage.getIcons().add(new Image(in));
                }
            } catch (Exception e) {
                System.err.println("Icon " + size + "px konnte nicht geladen werden: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
