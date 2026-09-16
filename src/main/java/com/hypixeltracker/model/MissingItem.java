package com.hypixeltracker.model;

/**
 * Gemeinsames Model fuer alles, was in den Tracker-Tabs als Zeile
 * dargestellt wird: Shard, Accessoire oder Minion-Upgrade.
 * Deckt Ist-Zustand, ob es craftbar ist und den aktuellen Preis ab.
 */
public class MissingItem {

    public enum Status {
        OWNED,          // bereits vorhanden
        CRAFTABLE,      // fehlt, aber alle Materialien vorhanden
        MISSING_MATS,   // fehlt, Materialien fehlen ebenfalls
        DROP_ONLY       // nicht craftbar (Boss-/Event-Drop, AH-only, ...)
    }

    private String internalName;
    private String displayName;
    private String category;
    private Status status;
    private double estimatedCost;
    private String note;

    public MissingItem() {
    }

    public MissingItem(String internalName, String displayName, String category, Status status, double estimatedCost) {
        this.internalName = internalName;
        this.displayName = displayName;
        this.category = category;
        this.status = status;
        this.estimatedCost = estimatedCost;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
