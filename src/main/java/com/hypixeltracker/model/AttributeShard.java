package com.hypixeltracker.model;

/**
 * Repraesentiert einen Attribute-Shard (Item mit ExtraAttribute
 * "attribute_shard" in der NBT-Struktur). Stufe ergibt sich aus
 * gleichnamigen kombinierten Shards.
 */
public class AttributeShard {

    private String internalName;
    private String attributeName;
    private int tier;
    private boolean owned;

    public AttributeShard() {
    }

    public AttributeShard(String internalName, String attributeName, int tier, boolean owned) {
        this.internalName = internalName;
        this.attributeName = attributeName;
        this.tier = tier;
        this.owned = owned;
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

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public boolean isOwned() {
        return owned;
    }

    public void setOwned(boolean owned) {
        this.owned = owned;
    }
}
