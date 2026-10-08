package com.hypixeltracker.model;

/**
 * Ein Attribut aus dem Hunting-System (Attribute Menu / Hunting Box).
 * Die Stufe ergibt sich aus der Anzahl gesyphonter Shards und der
 * Seltenheit des Shards (siehe {@link ShardRarity}).
 */
public class AttributeShard {

    private String internalName;
    private String attributeName;
    private String rarity;
    private int stacks;
    private int tier;
    private int shardsToNextLevel;
    private boolean owned;

    public AttributeShard() {
    }

    public AttributeShard(String internalName, String attributeName, ShardRarity rarity, int stacks) {
        this.internalName = internalName;
        this.attributeName = attributeName;
        this.rarity = rarity == null ? "?" : rarity.displayName();
        this.stacks = stacks;
        this.tier = rarity == null ? 0 : rarity.levelFromStacks(stacks);
        this.shardsToNextLevel = rarity == null ? -1 : rarity.shardsToNextLevel(stacks);
        this.owned = stacks > 0;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    /** Anzahl bisher gesyphonter Shards. */
    public int getStacks() {
        return stacks;
    }

    public void setStacks(int stacks) {
        this.stacks = stacks;
    }

    /** Attribut-Stufe 0-10 (0 = noch nicht freigeschaltet). */
    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    /** Fehlende Shards bis zur naechsten Stufe, 0 bei Stufe 10, -1 wenn unbekannt. */
    public int getShardsToNextLevel() {
        return shardsToNextLevel;
    }

    public void setShardsToNextLevel(int shardsToNextLevel) {
        this.shardsToNextLevel = shardsToNextLevel;
    }

    public boolean isOwned() {
        return owned;
    }

    public void setOwned(boolean owned) {
        this.owned = owned;
    }
}
