package com.hypixeltracker.model;

/**
 * Eine einzelne Auktion aus /skyblock/auctions, angereichert um den
 * "Normalpreis" (z.B. Lowest-BIN-Median der letzten Abfragen), damit
 * die App unter-Preis-Angebote markieren kann.
 */
public class AuctionItem {

    private String auctionUuid;
    private String itemName;
    private String internalName;
    private long startingBid;
    private boolean bin;
    private long normalPrice;

    public AuctionItem() {
    }

    public AuctionItem(String auctionUuid, String itemName, String internalName,
                        long startingBid, boolean bin, long normalPrice) {
        this.auctionUuid = auctionUuid;
        this.itemName = itemName;
        this.internalName = internalName;
        this.startingBid = startingBid;
        this.bin = bin;
        this.normalPrice = normalPrice;
    }

    /** Wieviel Prozent unter dem ermittelten Normalpreis diese Auktion liegt. */
    public double percentBelowNormal() {
        if (normalPrice <= 0) return 0;
        return (1.0 - ((double) startingBid / normalPrice)) * 100.0;
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

    public long getStartingBid() {
        return startingBid;
    }

    public void setStartingBid(long startingBid) {
        this.startingBid = startingBid;
    }

    public boolean isBin() {
        return bin;
    }

    public void setBin(boolean bin) {
        this.bin = bin;
    }

    public long getNormalPrice() {
        return normalPrice;
    }

    public void setNormalPrice(long normalPrice) {
        this.normalPrice = normalPrice;
    }
}
