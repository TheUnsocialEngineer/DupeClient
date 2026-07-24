package com.dupeclient.client.module.packet.fabricator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Parses and formats fabricator user-visible slot lists (e.g. {@code 0,5,100}). */
public final class FabricatorSlotList {
    private FabricatorSlotList() {
    }

    public static boolean hasMultiple(String raw) {
        return raw != null && raw.contains(",");
    }

    public static List<Integer> parseVisible(String raw) {
        List<Integer> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        Set<Integer> seen = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                seen.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
            }
        }
        out.addAll(seen);
        return out;
    }

    public static String format(List<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(slots.get(i));
        }
        return sb.toString();
    }

    public static String toggleVisible(String raw, int visibleSlot) {
        List<Integer> slots = new ArrayList<>(parseVisible(raw));
        if (slots.contains(visibleSlot)) {
            slots.remove(Integer.valueOf(visibleSlot));
        } else {
            slots.add(visibleSlot);
            slots.sort(Integer::compareTo);
        }
        return format(slots);
    }

    public static List<Integer> resolveHandlerSlots(MinecraftClient client, ScreenHandler handler, String raw) {
        List<Integer> handlerSlots = new ArrayList<>();
        for (int visible : parseVisible(raw)) {
            int handlerSlot = FabricatorInventorySlots.resolveHandlerSlot(client, visible);
            if (FabricatorInventorySlots.isValidHandlerSlot(handler, handlerSlot)) {
                handlerSlots.add(handlerSlot);
            }
        }
        return handlerSlots;
    }
}
