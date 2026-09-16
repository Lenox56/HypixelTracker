package com.hypixeltracker;

import com.hypixeltracker.config.AppConfig;
import com.hypixeltracker.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Einstiegspunkt der App. Baut nur das Fenster auf und delegiert
 * den eigentlichen UI-Aufbau an MainView.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        AppConfig config = AppConfig.loadOrCreate();

        MainView mainView = new MainView(config);
        Scene scene = new Scene(mainView.build(), 1100, 750);

        primaryStage.setTitle("Hypixel Tracker");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
