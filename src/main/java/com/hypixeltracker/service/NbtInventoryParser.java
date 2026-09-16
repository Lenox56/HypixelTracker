package com.hypixeltracker.service;

import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Wandelt die base64/gzip/NBT-kodierten Inventar-Felder aus der
 * SkyBlock-Profile-API (z.B. inv_contents, ender_chest_contents,
 * talisman_bag) in eine einfache Liste von Item-IDs (ExtraAttributes.id) um.
 *
 * Nutzt Querz/NBT (aktiv gepflegt, im Gegensatz zur zuerst gewaehlten
 * xyz.nickr:nbt, die archiviert war). NBTUtil.read() erwartet eine Datei,
 * daher wird der base64-dekodierte Inhalt kurz in eine temporaere Datei
 * geschrieben - das ist minimal langsamer als reines In-Memory-Parsing,
 * aber deutlich weniger fehleranfaellig als die interne Stream-API von
 * Hand nachzubauen.
 *
 * WICHTIG: Die genaue NBT-Struktur kann sich mit Spiel-Updates leicht
 * aendern - bei Problemen einmal den rohen Tag-Baum ausgeben
 * (System.out.println(root)) und gegenpruefen.
 */
public class NbtInventoryParser {

    /**
     * @param base64Data der Rohwert eines Inventar-Feldes aus der API-Antwort
     * @return Liste der internen Item-IDs (z.B. "ATTRIBUTE_SHARD", "TALISMAN_...")
     */
    public List<String> extractItemIds(String base64Data) throws IOException {
        List<String> ids = new ArrayList<>();
        if (base64Data == null || base64Data.isBlank()) {
            return ids;
        }

        byte[] raw = Base64.getDecoder().decode(base64Data);

        File tempFile = File.createTempFile("hypixeltracker-inv", ".nbt");
        try {
            Files.write(tempFile.toPath(), raw);

            NamedTag namedTag = NBTUtil.read(tempFile);
            if (!(namedTag.getTag() instanceof CompoundTag root)) {
                return ids;
            }

            ListTag<?> items = root.getListTag("i");
            if (items == null) {
                return ids;
            }

            for (Tag<?> entry : items) {
                if (!(entry instanceof CompoundTag itemCompound)) continue;
                String id = extractExtraAttributeId(itemCompound);
                if (id != null) {
                    ids.add(id);
                }
            }
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
        return ids;
    }

    private String extractExtraAttributeId(CompoundTag itemCompound) {
        // Struktur (vereinfacht): tag -> ExtraAttributes -> id
        CompoundTag tagCompound = itemCompound.getCompoundTag("tag");
        if (tagCompound == null) return null;

        CompoundTag extraAttributes = tagCompound.getCompoundTag("ExtraAttributes");
        if (extraAttributes == null) return null;

        return extraAttributes.getString("id");
    }
}
