package com.hypixeltracker.model;

/**
 * Ein moeglicher AH-Flip: der guenstigste Sofortkauf eines Items im Vergleich
 * zum naechstguenstigeren Angebot desselben Items ("Normalpreis").
 */
public class AuctionItem {

    private String auctionUuid;
    private String itemName;
    private String internalName;
    private long startingBid;
    private long normalPrice;
    private int listingCount;
    private long profit;

    public AuctionItem() {
    }

    public AuctionItem(String auctionUuid, String itemName, String internalName,
                       long startingBid, long normalPrice, int listingCount, long profit) {
        this.auctionUuid = auctionUuid;
        this.itemName = itemName;
        this.internalName = internalName;
        this.startingBid = startingBid;
        this.normalPrice = normalPrice;
        this.listingCount = listingCount;
        this.profit = profit;
    }

    /** Wieviel Prozent unter dem Normalpreis diese Auktion liegt. */
    public double getPercentBelowNormal() {
        if (normalPrice <= 0) return 0;
        return (1.0 - ((double) startingBid / normalPrice)) * 100.0;
    }

    /** Befehl zum Oeffnen der Auktion im Spiel. */
    public String getViewCommand() {
        return "/viewauction " + auctionUuid;
    }

    public String getAuctionUuid() {
        return auctionUuid;
    }

    public void setAuctionUuid(String auctionUuid) {
        this.auctionUuid = auctionUuid;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    /** Preis des guenstigsten Sofortkaufs. */
    public long getStartingBid() {
        return startingBid;
    }

    public void setStartingBid(long startingBid) {
        this.startingBid = startingBid;
    }

    /** Preis des naechstguenstigeren Sofortkaufs desselben Items. */
    public long getNormalPrice() {
        return normalPrice;
    }

    public void setNormalPrice(long normalPrice) {
        this.normalPrice = normalPrice;
    }

    /** Anzahl Sofortkauf-Angebote dieses Items im AH. */
    public int getListingCount() {
        return listingCount;
    }

    public void setListingCount(int listingCount) {
        this.listingCount = listingCount;
    }

    /** Geschaetzter Gewinn nach AH-Gebuehren beim Weiterverkauf zum Normalpreis. */
    public long getProfit() {
        return profit;
    }

    public void setProfit(long profit) {
        this.profit = profit;
    }
}
