package com.dupeclient.client.module.macro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Maps macro {@code holdKeyId} strings to vanilla movement / interact key bindings. */
public final class MacroHoldKeys {
    public static final String[] IDS = {
            "FORWARD",
            "BACK",
            "LEFT",
            "RIGHT",
            "JUMP",
            "SNEAK",
            "SPRINT",
            "ATTACK",
            "USE",
            "DROP",
            "INVENTORY",
            "PICK_BLOCK",
            "SWAP_HANDS"
    };

    private MacroHoldKeys() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "FORWARD";
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        for (String id : IDS) {
            if (id.equals(t)) {
                return id;
            }
        }
        return "FORWARD";
    }

    /** Like {@link #normalize} but returns {@code ""} when blank or unknown (for optional extra keys on walk steps). */
    public static String normalizeAuxKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        for (String id : IDS) {
            if (id.equals(t)) {
                return id;
            }
        }
        return "";
    }

    @Nullable
    public static KeyBinding binding(MinecraftClient client, String holdKeyId) {
        if (client == null || client.options == null) {
            return null;
        }
        GameOptions o = client.options;
        return switch (normalize(holdKeyId)) {
            case "FORWARD" -> o.forwardKey;
            case "BACK" -> o.backKey;
            case "LEFT" -> o.leftKey;
            case "RIGHT" -> o.rightKey;
            case "JUMP" -> o.jumpKey;
            case "SNEAK" -> o.sneakKey;
            case "SPRINT" -> o.sprintKey;
            case "ATTACK" -> o.attackKey;
            case "USE" -> o.useKey;
            case "DROP" -> o.dropKey;
            case "INVENTORY" -> o.inventoryKey;
            case "PICK_BLOCK" -> o.pickItemKey;
            case "SWAP_HANDS" -> o.swapHandsKey;
            default -> o.forwardKey;
        };
    }
}
