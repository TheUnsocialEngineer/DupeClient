package com.dupeclient.client.module.macro;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

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
    public static KeyMapping binding(Minecraft client, String holdKeyId) {
        if (client == null || client.options == null) {
            return null;
        }
        Options o = client.options;
        return switch (normalize(holdKeyId)) {
            case "FORWARD" -> o.keyUp;
            case "BACK" -> o.keyDown;
            case "LEFT" -> o.keyLeft;
            case "RIGHT" -> o.keyRight;
            case "JUMP" -> o.keyJump;
            case "SNEAK" -> o.keyShift;
            case "SPRINT" -> o.keySprint;
            case "ATTACK" -> o.keyAttack;
            case "USE" -> o.keyUse;
            case "DROP" -> o.keyDrop;
            case "INVENTORY" -> o.keyInventory;
            case "PICK_BLOCK" -> o.keyPickItem;
            case "SWAP_HANDS" -> o.keySwapOffhand;
            default -> o.keyUp;
        };
    }
}
