package com.dupeclient.client.module.macro;

import net.minecraft.screen.slot.SlotActionType;

import java.util.Locale;

/** Editor cycle + parsing for {@link SlotActionType} names on {@link MacroStepType#CLICK_SLOT}. */
public final class MacroSlotActions {
    private static final String[] CYCLE = {
            "PICKUP",
            "QUICK_MOVE",
            "SWAP",
            "CLONE",
            "THROW",
            "QUICK_CRAFT",
            "PICKUP_ALL"
    };

    private MacroSlotActions() {
    }

    public static String[] cycleValues() {
        return CYCLE;
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "QUICK_MOVE";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        for (String s : CYCLE) {
            if (s.equals(u)) {
                return s;
            }
        }
        try {
            SlotActionType.valueOf(u);
            return u;
        } catch (IllegalArgumentException e) {
            return "QUICK_MOVE";
        }
    }

    public static String next(String current) {
        String n = normalize(current);
        for (int i = 0; i < CYCLE.length; i++) {
            if (CYCLE[i].equals(n)) {
                return CYCLE[(i + 1) % CYCLE.length];
            }
        }
        return CYCLE[0];
    }

    public static SlotActionType toVanilla(String raw) {
        try {
            return SlotActionType.valueOf(normalize(raw));
        } catch (IllegalArgumentException e) {
            return SlotActionType.QUICK_MOVE;
        }
    }
}
