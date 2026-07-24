package com.dupeclient.client.module.packet.fabricator;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Locale;

/** Lightweight item matching for fabricator slot resolution. */
public final class FabricatorItemMatcher {
    private final String query;

    private FabricatorItemMatcher(String query) {
        this.query = query == null ? "" : query.trim();
    }

    public static FabricatorItemMatcher parse(String raw) {
        return new FabricatorItemMatcher(raw);
    }

    public boolean hasQuery() {
        return !query.isBlank();
    }

    public int score(ItemStack stack, int visibleSlot) {
        if (stack == null || stack.isEmpty() || query.isBlank()) {
            return -1;
        }
        String lower = query.toLowerCase(Locale.ROOT);
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id != null) {
            String full = id.toString().toLowerCase(Locale.ROOT);
            String path = id.getPath().toLowerCase(Locale.ROOT);
            String spaced = path.replace('_', ' ');
            if (lower.equals(full) || lower.equals(path) || lower.equals(spaced)) {
                return 320;
            }
            if (full.contains(lower) || path.contains(lower) || spaced.contains(lower)) {
                return 200;
            }
        }
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        if (name.contains(lower)) {
            return 180;
        }
        var custom = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (custom != null && custom.getString().toLowerCase(Locale.ROOT).contains(lower)) {
            return 170;
        }
        return -1;
    }
}
