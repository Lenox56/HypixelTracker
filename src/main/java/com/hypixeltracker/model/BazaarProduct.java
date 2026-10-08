package com.hypixeltracker.model;

/**
 * Ein Produkt aus /skyblock/bazaar (Werte aus quick_status).
 *
 * buyPrice  = Sofortkauf-Preis (guenstigstes Verkaufsangebot)
 * sellPrice = Sofortverkauf-Preis (hoechste Kauforder)
 *
 * Flip: Kauforder knapp ueber sellPrice setzen, Verkaufsangebot knapp
 * unter buyPrice - die Spanne abzueglich Bazaar-Steuer ist der Gewinn.
 */
public class BazaarProduct {

    /** Bazaar-Steuer auf Verkaeufe ohne Community-Upgrades. */
    public static final double TAX = 0.0125;

    private String productId;
    private String displayName;
    private double buyPrice;
    private double sellPrice;
    private long buyMovingWeek;
    private long sellMovingWeek;

    public BazaarProduct() {
    }

    public BazaarProduct(String productId, String displayName, double buyPrice, double sellPrice,
                         long buyMovingWeek, long sellMovingWeek) {
        this.productId = productId;
        this.displayName = displayName;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.buyMovingWeek = buyMovingWeek;
        this.sellMovingWeek = sellMovingWeek;
    }

    /** Gewinn pro Stueck nach Steuer. */
    public double getMargin() {
        return buyPrice * (1 - TAX) - sellPrice;
    }

    public double getMarginPercent() {
        if (sellPrice <= 0) return 0;
        return getMargin() / sellPrice * 100.0;
    }

    /** Das kleinere der beiden Wochenvolumen - begrenzt, wie viel man realistisch flippen kann. */
    public long getWeeklyVolume() {
        return Math.min(buyMovingWeek, sellMovingWeek);
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public long getBuyMovingWeek() {
        return buyMovingWeek;
    }

    public void setBuyMovingWeek(long buyMovingWeek) {
        this.buyMovingWeek = buyMovingWeek;
    }

    public long getSellMovingWeek() {
        return sellMovingWeek;
    }

    public void setSellMovingWeek(long sellMovingWeek) {
        this.sellMovingWeek = sellMovingWeek;
    }
}
