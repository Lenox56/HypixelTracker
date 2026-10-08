package com.hypixeltracker.service;

import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Dekodiert die base64/gzip/NBT-kodierten Item-Daten der SkyBlock-API:
 * Inventar-Felder (inv_contents, talisman_bag, ...) und item_bytes von Auktionen.
 * Beide enthalten ein Root-Compound mit einer Liste "i" von Items; die
 * SkyBlock-ID steht jeweils unter tag.ExtraAttributes.id.
 */
public class NbtInventoryParser {

    /** Alle Items aus einem base64-kodierten Feld (leere Slots werden uebersprungen). */
    public List<CompoundTag> decodeItems(String base64Data) throws IOException {
        List<CompoundTag> items = new ArrayList<>();
        if (base64Data == null || base64Data.isBlank()) {
            return items;
        }
        byte[] raw = Base64.getDecoder().decode(base64Data.trim());
        NamedTag namedTag = new NBTDeserializer(true).fromStream(new ByteArrayInputStream(raw));
        if (!(namedTag.getTag() instanceof CompoundTag root)) {
            return items;
        }
        ListTag<?> list = root.getListTag("i");
        if (list == null) {
            return items;
        }
        for (Tag<?> entry : list) {
            if (entry instanceof CompoundTag item && extraAttributes(item) != null) {
                items.add(item);
            }
        }
        return items;
    }

    /** Interne Item-IDs (z.B. "SPEED_TALISMAN") aus einem base64-kodierten Feld. */
    public List<String> extractItemIds(String base64Data) throws IOException {
        List<String> ids = new ArrayList<>();
        for (CompoundTag item : decodeItems(base64Data)) {
            String id = itemId(item);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** Struktur: tag -> ExtraAttributes */
    public static CompoundTag extraAttributes(CompoundTag item) {
        CompoundTag tag = item.getCompoundTag("tag");
        return tag == null ? null : tag.getCompoundTag("ExtraAttributes");
    }

    public static String itemId(CompoundTag item) {
        CompoundTag extra = extraAttributes(item);
        if (extra == null || !extra.containsKey("id")) {
            return null;
        }
        String id = extra.getString("id");
        return id == null || id.isBlank() ? null : id;
    }
}
