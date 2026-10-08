package com.hypixeltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

/** Hilfen, um API-Antworten und NBT-Item-Daten fuer Tests nachzubauen. */
final class TestData {

    static final String UUID = "069a79f444e94726a5befca90e38aaf5";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestData() {
    }

    /** JSON mit ' statt " schreiben, damit Tests lesbar bleiben. */
    static JsonNode json(String text) throws Exception {
        return MAPPER.readTree(text.replace('\'', '"'));
    }

    /** base64(gzip(NBT)) wie in inv_contents/talisman_bag/item_bytes. */
    static String encodeItems(CompoundTag... items) throws Exception {
        ListTag<CompoundTag> list = new ListTag<>(CompoundTag.class);
        for (CompoundTag item : items) {
            list.add(item);
        }
        CompoundTag root = new CompoundTag();
        root.put("i", list);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new NBTSerializer(true).toStream(new NamedTag("", root), out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    static CompoundTag item(String id) {
        return item(id, Map.of());
    }

    static CompoundTag item(String id, Map<String, Integer> enchantments) {
        CompoundTag extra = new CompoundTag();
        extra.putString("id", id);
        if (!enchantments.isEmpty()) {
            CompoundTag ench = new CompoundTag();
            enchantments.forEach(ench::putInt);
            extra.put("enchantments", ench);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("ExtraAttributes", extra);
        CompoundTag item = new CompoundTag();
        item.putShort("id", (short) 397);
        item.putByte("Count", (byte) 1);
        item.put("tag", tag);
        return item;
    }
}
