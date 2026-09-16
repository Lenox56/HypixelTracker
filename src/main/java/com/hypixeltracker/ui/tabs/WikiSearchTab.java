package com.hypixeltracker.ui.tabs;

import com.hypixeltracker.service.WikiSearchService;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Freie Suchleiste ueber das Hypixel SkyBlock Wiki.
 * Nutzt denselben WikiSearchService, der auch von den kontextuellen
 * Info-Icons in den anderen Tabs verwendet wird (siehe z.B.
 * AccessoryTrackerTab - dort TODO ergaenzen, sobald die Tabelle
 * echte Eintraege hat).
 */
public class WikiSearchTab {

    private final WikiSearchService wikiService;
    private final ListView<WikiSearchService.SearchResult> resultsList = new ListView<>();
    private final TextArea detailArea = new TextArea();

    public WikiSearchTab(WikiSearchService wikiService) {
        this.wikiService = wikiService;
    }

    public BorderPane build() {
        TextField searchField = new TextField();
        searchField.setPromptText("z.B. \"Bonzo's Mask\" oder \"Bazaar Flipping\"");

        Button searchButton = new Button("Suchen");
        searchButton.setOnAction(e -> runSearch(searchField.getText()));
        // simples Debounce: erst 400ms nach dem letzten Tastendruck suchen
        PauseTransition debounce = new PauseTransition(Duration.millis(400));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.stop();
            debounce.setOnFinished(e -> runSearch(newVal));
            debounce.play();
        });

        resultsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(WikiSearchService.SearchResult item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title());
            }
        });
        resultsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showSummary(newVal.title());
            }
        });

        detailArea.setEditable(false);
        detailArea.setWrapText(true);

        HBox searchBar = new HBox(8, searchField, searchButton);
        searchBar.setPadding(new Insets(10));

        VBox resultsBox = new VBox(new Label("Treffer:"), resultsList);
        VBox detailBox = new VBox(new Label("Zusammenfassung:"), detailArea);

        SplitPane splitPane = new SplitPane(resultsBox, detailBox);

        BorderPane pane = new BorderPane();
        pane.setTop(searchBar);
        pane.setCenter(splitPane);
        return pane;
    }

    private void runSearch(String query) {
        if (query == null || query.isBlank()) {
            resultsList.getItems().clear();
            return;
        }
        try {
            var results = wikiService.search(query);
            resultsList.getItems().setAll(results);
        } catch (Exception ex) {
            detailArea.setText("Fehler bei der Suche: " + ex.getMessage());
        }
    }

    private void showSummary(String title) {
        try {
            detailArea.setText(wikiService.getSummary(title));
        } catch (Exception ex) {
            detailArea.setText("Fehler beim Laden: " + ex.getMessage());
        }
    }
}
