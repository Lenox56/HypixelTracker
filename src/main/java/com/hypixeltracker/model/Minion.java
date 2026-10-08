package com.hypixeltracker.model;

/**
 * Ein Minion-Typ (z.B. COBBLESTONE) mit aktuell erreichter Tier-Stufe
 * (aus crafted_generators, coop-weit vereinigt) und der maximal
 * moeglichen Stufe (aus den Referenzdaten, siehe ReferenceData).
 */
public class Minion {

    private String minionType;
    private String displayName;
    private int currentTier;
    private int maxTier;

    public Minion() {
    }

    public Minion(String minionType, String displayName, int currentTier, int maxTier) {
        this.minionType = minionType;
        this.displayName = displayName;
        this.currentTier = currentTier;
        this.maxTier = maxTier;
    }

    /** Anzahl noch nicht gebauter Stufen (fuer die Tabellenspalte). */
    public int getMissingTiers() {
        return Math.max(0, maxTier - currentTier);
    }

    public String getMinionType() {
        return minionType;
    }

    public void setMinionType(String minionType) {
        this.minionType = minionType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(int currentTier) {
        this.currentTier = currentTier;
    }

    public int getMaxTier() {
        return maxTier;
    }

    public void setMaxTier(int maxTier) {
        this.maxTier = maxTier;
    }
}
