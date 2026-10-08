package com.hypixeltracker.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShardRarityTest {

    @Test
    void levelFromStacksUsesCumulativeThresholds() {
        assertEquals(0, ShardRarity.EPIC.levelFromStacks(0));
        assertEquals(1, ShardRarity.EPIC.levelFromStacks(1));
        assertEquals(3, ShardRarity.EPIC.levelFromStacks(5));
        assertEquals(10, ShardRarity.EPIC.levelFromStacks(32));
        assertEquals(10, ShardRarity.EPIC.levelFromStacks(500));
        assertEquals(9, ShardRarity.COMMON.levelFromStacks(95));
        assertEquals(10, ShardRarity.LEGENDARY.levelFromStacks(24));
    }

    @Test
    void shardsToNextLevel() {
        assertEquals(1, ShardRarity.EPIC.shardsToNextLevel(5));   // Stufe 3, Stufe 4 ab 6
        assertEquals(0, ShardRarity.EPIC.shardsToNextLevel(32));  // max
        assertEquals(1, ShardRarity.COMMON.shardsToNextLevel(0));
    }

    @Test
    void parsesTierAndShardCodes() {
        assertEquals(ShardRarity.LEGENDARY, ShardRarity.fromTier("legendary"));
        assertNull(ShardRarity.fromTier("SPECIAL"));
        assertEquals(ShardRarity.EPIC, ShardRarity.fromShardCode("E54"));
        assertEquals(ShardRarity.UNCOMMON, ShardRarity.fromShardCode("u3"));
        assertNull(ShardRarity.fromShardCode("SHARD_SCARF"));
    }
}
