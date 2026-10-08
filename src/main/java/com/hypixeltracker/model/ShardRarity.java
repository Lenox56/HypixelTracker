package com.hypixeltracker.model;

import java.util.Locale;

/**
 * Seltenheit eines Attribut-Shards inkl. der kumulierten Anzahl Shards,
 * die fuer Stufe 1-10 gesyphont sein muessen (laut SkyBlock-Wiki).
 */
public enum ShardRarity {
    COMMON("Common", 1, 4, 9, 15, 22, 30, 40, 54, 72, 96),
    UNCOMMON("Uncommon", 1, 3, 6, 10, 15, 21, 28, 36, 48, 64),
    RARE("Rare", 1, 3, 6, 9, 13, 17, 22, 28, 36, 48),
    EPIC("Epic", 1, 2, 4, 6, 9, 12, 16, 20, 25, 32),
    LEGENDARY("Legendary", 1, 2, 3, 5, 7, 9, 12, 15, 19, 24);

    public static final int MAX_LEVEL = 10;

    private final String displayName;
    private final int[] cumulativeStacks;

    ShardRarity(String displayName, int... cumulativeStacks) {
        this.displayName = displayName;
        this.cumulativeStacks = cumulativeStacks;
    }

    public String displayName() {
        return displayName;
    }

    public int levelFromStacks(int stacks) {
        int level = 0;
        while (level < MAX_LEVEL && stacks >= cumulativeStacks[level]) {
            level++;
        }
        return level;
    }

    public int shardsToNextLevel(int stacks) {
        int level = levelFromStacks(stacks);
        return level >= MAX_LEVEL ? 0 : cumulativeStacks[level] - stacks;
    }

    /** Liest die Seltenheit aus dem "tier"-Feld der Item-Ressourcen (z.B. "EPIC"). */
    public static ShardRarity fromTier(String tier) {
        if (tier == null) {
            return null;
        }
        try {
            return valueOf(tier.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Liest die Seltenheit aus einem Shard-Code wie "E54" oder "L4" (erster Buchstabe). */
    public static ShardRarity fromShardCode(String code) {
        if (code == null || !code.matches("(?i)^[CUREL]\\d+$")) {
            return null;
        }
        return switch (Character.toUpperCase(code.charAt(0))) {
            case 'C' -> COMMON;
            case 'U' -> UNCOMMON;
            case 'R' -> RARE;
            case 'E' -> EPIC;
            default -> LEGENDARY;
        };
    }
}
