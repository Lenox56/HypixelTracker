package com.hypixeltracker.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

/** Erzeugt base64(gzip(NBT)) im Format der SkyBlock-Inventarfelder. */
final class NbtTestData {

    private NbtTestData() {
    }

    static String encodeItemIds(String... ids) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            out.writeByte(10);          // Root-Compound
            out.writeUTF("");
            out.writeByte(9);           // Liste "i" von Compounds
            out.writeUTF("i");
            out.writeByte(10);
            out.writeInt(ids.length + 1);
            for (String id : ids) {
                writeItem(out, id);
            }
            out.writeByte(0);           // leerer Slot
            out.writeByte(0);           // Ende Root
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private static void writeItem(DataOutputStream out, String id) throws IOException {
        out.writeByte(2);               // id (Minecraft-Item-ID, short)
        out.writeUTF("id");
        out.writeShort(397);
        out.writeByte(1);               // Count
        out.writeUTF("Count");
        out.writeByte(1);
        out.writeByte(10);              // tag
        out.writeUTF("tag");
        out.writeByte(9);               // Lore als String-Liste
        out.writeUTF("Lore");
        out.writeByte(8);
        out.writeInt(1);
        out.writeUTF("§7Some lore");
        out.writeByte(10);              // ExtraAttributes
        out.writeUTF("ExtraAttributes");
        out.writeByte(8);
        out.writeUTF("id");
        out.writeUTF(id);
        out.writeByte(11);              // Int-Array, muss uebersprungen werden
        out.writeUTF("gems");
        out.writeInt(2);
        out.writeInt(1);
        out.writeInt(2);
        out.writeByte(0);               // Ende ExtraAttributes
        out.writeByte(0);               // Ende tag
        out.writeByte(0);               // Ende Item
    }
}
