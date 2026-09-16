package com.hypixeltracker.model;

/**
 * Ein Produkt aus /skyblock/bazaar. buyPrice/sellPrice kommen aus
 * quick_status (buyPrice = instant buy, sellPrice = instant sell).
 * Die Spanne dazwischen ist die Flipping-Marge.
 */
public class BazaarProduct {

    private String productId;
    private double buyPrice;
    private double sellPrice;
    private long buyVolume;
    private long sellVolume;

    public BazaarProduct() {
    }

    public BazaarProduct(String productId, double buyPrice, double sellPrice, long buyVolume, long sellVolume) {
        this.productId = productId;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.buyVolume = buyVolume;
        this.sellVolume = sellVolume;
    }

    /** Marge pro Einheit, wenn man am Sell-Order-Preis kauft und am Buy-Order-Preis verkauft. */
    public double margin() {
        return buyPrice - sellPrice;
    }

    public double marginPercent() {
        if (sellPrice <= 0) return 0;
        return (margin() / sellPrice) * 100.0;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
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

    public long getBuyVolume() {
        return buyVolume;
    }

    public void setBuyVolume(long buyVolume) {
        this.buyVolume = buyVolume;
    }

    public long getSellVolume() {
        return sellVolume;
    }

    public void setSellVolume(long sellVolume) {
        this.sellVolume = sellVolume;
    }
}
