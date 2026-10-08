package com.hypixeltracker.ui.tabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.hypixeltracker.model.AuctionItem;
import com.hypixeltracker.model.BazaarProduct;
import com.hypixeltracker.service.FlipService;
import com.hypixeltracker.service.MarketDataService;
import com.hypixeltracker.service.ProfileUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * Links: Sofortkauf-Auktionen, die deutlich unter dem naechstguenstigeren
 * Angebot desselben Items liegen. Rechts: Bazaar-Produkte mit der groessten
 * Marge nach Steuer bei ausreichendem Handelsvolumen.
 */
public class AuctionBazaarTab {

    private record RefreshResult(List<AuctionItem> auctions, List<BazaarProduct> bazaar, int scannedItems) {
    }

    private final MarketDataService marketData;
    private final FlipService flipService = new FlipService();
    private final ObservableList<AuctionItem> auctionFlips = FXCollections.observableArrayList();
    private final ObservableList<BazaarProduct> bazaarFlips = FXCollections.observableArrayList();

    public AuctionBazaarTab(MarketDataService marketData) {
        this.marketData = marketData;
    }

    public BorderPane build() {
        Label statusLabel = new Label("Noch nicht geladen.");

        TableView<AuctionItem> auctionTable = new TableView<>(auctionFlips);
        TableColumn<AuctionItem, String> itemCol = new TableColumn<>("Item");
        itemCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        itemCol.setPrefWidth(200);
        TableColumn<AuctionItem, Long> priceCol = coinColumn("Preis", "startingBid");
        TableColumn<AuctionItem, Long> normalCol = coinColumn("Naechster BIN", "normalPrice");
        TableColumn<AuctionItem, Long> profitCol = coinColumn("Gewinn (nach Gebuehr)", "profit");
        TableColumn<AuctionItem, Double> percentCol = new TableColumn<>("Rabatt");
        percentCol.setCellValueFactory(new PropertyValueFactory<>("percentBelowNormal"));
        percentCol.setCellFactory(col -> BackgroundSync.textCell(p -> String.format("%.0f %%", p)));
        TableColumn<AuctionItem, Integer> countCol = new TableColumn<>("Angebote");
        countCol.setCellValueFactory(new PropertyValueFactory<>("listingCount"));
        auctionTable.getColumns().addAll(List.of(itemCol, priceCol, normalCol, profitCol, percentCol, countCol));
        auctionTable.setRowFactory(table -> {
            TableRow<AuctionItem> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(row.getItem().getViewCommand());
                    Clipboard.getSystemClipboard().setContent(content);
                    statusLabel.setText("Kopiert: " + row.getItem().getViewCommand() + " - im Spiel in den Chat einfuegen.");
                }
            });
            return row;
        });

        TableView<BazaarProduct> bazaarTable = new TableView<>(bazaarFlips);
        TableColumn<BazaarProduct, String> productCol = new TableColumn<>("Produkt");
        productCol.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        productCol.setPrefWidth(200);
        TableColumn<BazaarProduct, Double> buyOrderCol = coinColumn("Kauforder bei", "sellPrice");
        TableColumn<BazaarProduct, Double> sellOfferCol = coinColumn("Verkaufen bei", "buyPrice");
        TableColumn<BazaarProduct, Double> marginCol = coinColumn("Gewinn/Stueck", "margin");
        TableColumn<BazaarProduct, Double> marginPercentCol = new TableColumn<>("Marge");
        marginPercentCol.setCellValueFactory(new PropertyValueFactory<>("marginPercent"));
        marginPercentCol.setCellFactory(col -> BackgroundSync.textCell(p -> String.format("%.1f %%", p)));
        TableColumn<BazaarProduct, Long> volumeCol = coinColumn("Volumen/Woche", "weeklyVolume");
        bazaarTable.getColumns().addAll(List.of(productCol, buyOrderCol, sellOfferCol, marginCol, marginPercentCol, volumeCol));

        Spinner<Integer> minPercent = spinner(1, 90, 10, 1);
        Spinner<Integer> minProfit = spinner(0, 100_000_000, 100_000, 50_000);
        Spinner<Integer> minVolume = spinner(0, 100_000_000, 10_000, 5_000);

        Button refreshButton = new Button("Preise aktualisieren");
        refreshButton.setOnAction(e -> refresh(refreshButton, statusLabel,
                minPercent.getValue(), minProfit.getValue(), minVolume.getValue()));

        HBox filters = new HBox(16,
                new HBox(6, new Label("AH: Mindest-Rabatt (%)"), minPercent),
                new HBox(6, new Label("Mindestgewinn (Coins)"), minProfit),
                new HBox(6, new Label("Bazaar: Mindestvolumen/Woche"), minVolume));

        VBox topBox = new VBox(8, refreshButton, filters, statusLabel,
                new Label("Tipp: Doppelklick auf eine Auktion kopiert den /viewauction-Befehl."));
        topBox.setPadding(new Insets(10));

        SplitPane splitPane = new SplitPane(
                titled("Auktionshaus: guenstige Sofortkaeufe", auctionTable),
                titled("Bazaar: Kauforder/Verkaufsangebot-Flips", bazaarTable));

        BorderPane pane = new BorderPane();
        pane.setTop(topBox);
        pane.setCenter(splitPane);
        return pane;
    }

    private void refresh(Button button, Label statusLabel, int minPercent, long minProfit, long minVolume) {
        BackgroundSync.run(button, statusLabel, progress -> {
            progress.accept("Lade Bazaar und Item-Namen...");
            JsonNode bazaar = marketData.bazaar();
            Map<String, MarketDataService.ItemInfo> items = marketData.items();
            List<BazaarProduct> bazaarResult = flipService.findBazaarFlips(bazaar, id -> name(id, items), minVolume);

            Map<String, List<MarketDataService.Listing>> listings = marketData.binListings(progress);
            List<AuctionItem> auctionResult = flipService.findAuctionFlips(listings, minPercent, minProfit);
            return new RefreshResult(auctionResult, bazaarResult, listings.size());
        }, result -> {
            auctionFlips.setAll(result.auctions());
            bazaarFlips.setAll(result.bazaar());
            statusLabel.setText("Fertig: " + result.auctions().size() + " AH-Flips (aus " + result.scannedItems()
                    + " verschiedenen Items), " + result.bazaar().size() + " Bazaar-Flips.");
        });
    }

    /** Name aus der Item-Datenbank; Verzauberungen wie "ENCHANTMENT_SHARPNESS_6" lesbar machen. */
    private static String name(String id, Map<String, MarketDataService.ItemInfo> items) {
        MarketDataService.ItemInfo info = items.get(id);
        if (info != null) {
            return info.name();
        }
        if (id.startsWith("ENCHANTMENT_")) {
            return ProfileUtil.titleCase(id.substring("ENCHANTMENT_".length())) + " (Buch)";
        }
        return ProfileUtil.titleCase(id);
    }

    private static <S, T extends Number> TableColumn<S, T> coinColumn(String title, String property) {
        TableColumn<S, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setCellFactory(c -> BackgroundSync.textCell(BackgroundSync::coins));
        col.setPrefWidth(120);
        return col;
    }

    private static Spinner<Integer> spinner(int min, int max, int initial, int step) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial, step);
        spinner.setEditable(true);
        spinner.setPrefWidth(120);
        return spinner;
    }

    private static VBox titled(String title, TableView<?> table) {
        VBox box = new VBox(4, new Label(title), table);
        box.setPadding(new Insets(0, 4, 0, 4));
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }
}
