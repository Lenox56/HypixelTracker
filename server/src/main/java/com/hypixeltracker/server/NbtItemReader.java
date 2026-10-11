package com.hypixeltracker.server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Minimaler Leser fuer die base64/gzip/NBT-kodierten Inventarfelder der
 * SkyBlock-API. Liefert nur die SkyBlock-IDs der Items
 * (Root-Compound -> Liste "i" -> tag.ExtraAttributes.id).
 */
final class NbtItemReader {

    private static final int MAX_DEPTH = 512;

    private NbtItemReader() {
    }

    static List<String> itemIds(String base64Data) throws IOException {
        List<String> ids = new ArrayList<>();
        if (base64Data == null || base64Data.isBlank()) {
            return ids;
        }
        byte[] raw = Base64.getDecoder().decode(base64Data.trim());
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(raw)))) {
            int rootType = in.readUnsignedByte();
            if (rootType != 10) {
                return ids;
            }
            in.readUTF(); // Name des Root-Tags
            Object root = readPayload(in, rootType, 0);
            if (root instanceof Map<?, ?> rootMap && rootMap.get("i") instanceof List<?> items) {
                for (Object item : items) {
                    String id = extraAttributeId(item);
                    if (id != null && !id.isBlank()) {
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }

    private static String extraAttributeId(Object item) {
        if (item instanceof Map<?, ?> itemMap
                && itemMap.get("tag") instanceof Map<?, ?> tag
                && tag.get("ExtraAttributes") instanceof Map<?, ?> extra
                && extra.get("id") instanceof String id) {
            return id;
        }
        return null;
    }

    /** Liest den Inhalt eines Tags: Zahlen/Strings als Java-Werte, Compounds als Map, Listen als List. */
    private static Object readPayload(DataInputStream in, int type, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("NBT zu tief verschachtelt");
        }
        switch (type) {
            case 1: return in.readByte();
            case 2: return in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 7: in.readFully(new byte[checkedLength(in.readInt())]); return null;
            case 8: return in.readUTF();
            case 9: {
                int elementType = in.readUnsignedByte();
                int length = checkedLength(in.readInt());
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    list.add(readPayload(in, elementType, depth + 1));
                }
                return list;
            }
            case 10: {
                Map<String, Object> map = new HashMap<>();
                while (true) {
                    int childType = in.readUnsignedByte();
                    if (childType == 0) {
                        return map;
                    }
                    String name = in.readUTF();
                    map.put(name, readPayload(in, childType, depth + 1));
                }
            }
            case 11: in.readFully(new byte[checkedLength(in.readInt()) * 4]); return null;
            case 12: in.readFully(new byte[checkedLength(in.readInt()) * 8]); return null;
            case 0: return null;
            default: throw new IOException("Unbekannter NBT-Typ " + type);
        }
    }

    private static int checkedLength(int length) throws IOException {
        if (length < 0 || length > 10_000_000) {
            throw new IOException("Ungueltige NBT-Laenge " + length);
        }
        return length;
    }
}
