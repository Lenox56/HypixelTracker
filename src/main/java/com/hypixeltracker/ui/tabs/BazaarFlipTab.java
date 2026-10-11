package com.hypixeltracker.ui.tabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.model.BazaarProduct;
import com.hypixeltracker.service.FlipService;
import com.hypixeltracker.service.MarketDataService;
import com.hypixeltracker.service.TextUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * Bazaar-Produkte mit der groessten Marge nach Steuer zwischen Kauforder
 * und Verkaufsangebot, bei ausreichendem Handelsvolumen.
 */
public class BazaarFlipTab {

    private final MarketDataService marketData;
    private final FlipService flipService = new FlipService();
    private final ObservableList<BazaarProduct> bazaarFlips = FXCollections.observableArrayList();

    public BazaarFlipTab(MarketDataService marketData) {
        this.marketData = marketData;
    }

    public BorderPane build() {
        TableView<BazaarProduct> table = new TableView<>(bazaarFlips);
        TableColumn<BazaarProduct, String> productCol = new TableColumn<>("Produkt");
        productCol.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        productCol.setPrefWidth(240);
        TableColumn<BazaarProduct, Double> buyOrderCol = coinColumn("Kauforder bei", "sellPrice");
        TableColumn<BazaarProduct, Double> sellOfferCol = coinColumn("Verkaufen bei", "buyPrice");
        TableColumn<BazaarProduct, Double> marginCol = coinColumn("Gewinn/Stueck (nach Steuer)", "margin");
        marginCol.setPrefWidth(180);
        TableColumn<BazaarProduct, Double> marginPercentCol = new TableColumn<>("Marge");
        marginPercentCol.setCellValueFactory(new PropertyValueFactory<>("marginPercent"));
        marginPercentCol.setCellFactory(col -> BackgroundSync.textCell(p -> String.format("%.1f %%", p)));
        TableColumn<BazaarProduct, Long> volumeCol = coinColumn("Volumen/Woche", "weeklyVolume");
        table.getColumns().addAll(List.of(productCol, buyOrderCol, sellOfferCol, marginCol, marginPercentCol, volumeCol));

        Spinner<Integer> minVolume = new Spinner<>(0, 100_000_000, 10_000, 5_000);
        minVolume.setEditable(true);
        minVolume.setPrefWidth(120);

        Label statusLabel = new Label("Noch nicht geladen.");
        Button refreshButton = new Button("Preise aktualisieren");
        refreshButton.setOnAction(e -> refresh(refreshButton, statusLabel, minVolume.getValue()));

        VBox topBox = new VBox(8, refreshButton,
                new HBox(6, new Label("Mindestvolumen/Woche"), minVolume), statusLabel);
        topBox.setPadding(new Insets(10));

        BorderPane pane = new BorderPane();
        pane.setTop(topBox);
        pane.setCenter(table);
        return pane;
    }

    private void refresh(Button button, Label statusLabel, long minVolume) {
        BackgroundSync.run(button, statusLabel, progress -> {
            progress.accept("Lade Bazaar und Item-Namen...");
            JsonNode bazaar = marketData.bazaar();
            Map<String, MarketDataService.ItemInfo> items = marketData.items();
            return flipService.findBazaarFlips(bazaar, id -> name(id, items), minVolume);
        }, result -> {
            bazaarFlips.setAll(result);
            statusLabel.setText("Fertig: " + result.size() + " Bazaar-Produkte mit positiver Marge.");
        });
    }

    /** Name aus der Item-Datenbank; Verzauberungen wie "ENCHANTMENT_SHARPNESS_6" lesbar machen. */
    private static String name(String id, Map<String, MarketDataService.ItemInfo> items) {
        MarketDataService.ItemInfo info = items.get(id);
        if (info != null) {
            return info.name();
        }
        if (id.startsWith("ENCHANTMENT_")) {
            return TextUtil.titleCase(id.substring("ENCHANTMENT_".length())) + " (Buch)";
        }
        return TextUtil.titleCase(id);
    }

    private static <S, T extends Number> TableColumn<S, T> coinColumn(String title, String property) {
        TableColumn<S, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setCellFactory(c -> BackgroundSync.textCell(BackgroundSync::coins));
        col.setPrefWidth(120);
        return col;
    }
}
