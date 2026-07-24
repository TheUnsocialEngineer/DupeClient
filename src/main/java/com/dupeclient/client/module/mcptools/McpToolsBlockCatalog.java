package com.dupeclient.client.module.mcptools;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Searchable block id list for MCPTools mine commands (maps display names → mineflayer ids). */
public final class McpToolsBlockCatalog {
    public record Entry(String displayName, String fullId, String mineId, String dropdownLabel) {
    }

    private static volatile List<Entry> entries = List.of();
    private static volatile List<String> dropdownLabels = List.of();

    private McpToolsBlockCatalog() {
    }

    public static List<String> dropdownLabels() {
        ensureLoaded();
        return dropdownLabels;
    }

    /** Resolves dropdown label, friendly name, or id to a mineflayer block name (no namespace). */
    public static String resolveMineId(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        ensureLoaded();
        String trimmed = input.trim();
        for (Entry entry : entries) {
            if (entry.dropdownLabel().equalsIgnoreCase(trimmed)
                    || entry.fullId().equalsIgnoreCase(trimmed)
                    || entry.mineId().equalsIgnoreCase(trimmed)
                    || entry.displayName().equalsIgnoreCase(trimmed)) {
                return entry.mineId();
            }
        }
        int dot = trimmed.indexOf('·');
        if (dot > 0) {
            return normalizeMineId(trimmed.substring(dot + 1));
        }
        int arrow = trimmed.indexOf("->");
        if (arrow > 0) {
            return normalizeMineId(trimmed.substring(arrow + 2));
        }
        return normalizeMineId(trimmed);
    }

    public static String dropdownLabelForMineId(String mineId) {
        if (mineId == null || mineId.isBlank()) {
            return "";
        }
        ensureLoaded();
        String normalized = normalizeMineId(mineId);
        for (Entry entry : entries) {
            if (entry.mineId().equals(normalized)) {
                return entry.dropdownLabel();
            }
        }
        return humanizePath(normalized) + " · " + normalized;
    }

    private static String normalizeMineId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("minecraft:")) {
            value = value.substring("minecraft:".length());
        }
        return value;
    }

    private static void ensureLoaded() {
        if (!entries.isEmpty()) {
            return;
        }
        synchronized (McpToolsBlockCatalog.class) {
            if (!entries.isEmpty()) {
                return;
            }
            List<Entry> built = new ArrayList<>();
            for (Block block : Registries.BLOCK) {
                Identifier id = Registries.BLOCK.getId(block);
                if (id == null || block == Blocks.AIR) {
                    continue;
                }
                String fullId = id.toString();
                String mineId = id.getPath();
                String display = block.getName().getString();
                if (display == null || display.isBlank()) {
                    display = humanizePath(mineId);
                }
                String label = display + " · " + mineId;
                built.add(new Entry(display, fullId, mineId, label));
            }
            built.sort(Comparator.comparing(Entry::displayName, String.CASE_INSENSITIVE_ORDER));
            entries = List.copyOf(built);
            dropdownLabels = entries.stream().map(Entry::dropdownLabel).toList();
        }
    }

    private static String humanizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
