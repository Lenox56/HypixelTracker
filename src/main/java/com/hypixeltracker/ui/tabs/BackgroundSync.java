package com.hypixeltracker.ui.tabs;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fuehrt Synchronisierungen im Hintergrund aus, damit das Fenster waehrend
 * der API-Anfragen nicht einfriert. Der Button ist solange deaktiviert.
 */
final class BackgroundSync {

    /** Arbeit, die im Hintergrund laeuft und Fortschritt melden kann. */
    @FunctionalInterface
    interface Work<T> {
        T run(Consumer<String> progress) throws Exception;
    }

    private BackgroundSync() {
    }

    static <T> void run(Button button, Label status, Work<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.run(message -> Platform.runLater(() -> status.setText(message)));
            }
        };
        task.setOnSucceeded(e -> {
            button.setDisable(false);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            button.setDisable(false);
            Throwable ex = task.getException();
            status.setText("Fehler: " + (ex == null ? "unbekannt" : ex.getMessage()));
        });

        button.setDisable(true);
        status.setText("Synchronisiere...");
        Thread thread = new Thread(task, "background-sync");
        thread.setDaemon(true);
        thread.start();
    }

    /** Prueft, ob ein Minecraft-Name eingetragen ist, und meldet sonst einen Hinweis. */
    static boolean requireUsername(String username, Label status) {
        if (username == null || username.isBlank()) {
            status.setText("Bitte zuerst unter \"Einstellungen\" den Minecraft-Username eintragen.");
            return false;
        }
        return true;
    }

    /** Tabellenzelle mit eigener Textformatierung. */
    static <S, T> TableCell<S, T> textCell(Function<T, String> format) {
        return new TableCell<>() {
            @Override
            protected void updateItem(T value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : format.apply(value));
            }
        };
    }

    /** Coins lesbar formatieren: 1234567 -> "1.234.567". */
    static String coins(Number value) {
        return value == null || value.doubleValue() < 0 ? "-" : String.format("%,d", Math.round(value.doubleValue()));
    }
}
