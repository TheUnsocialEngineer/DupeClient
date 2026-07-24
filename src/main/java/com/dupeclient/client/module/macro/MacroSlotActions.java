package com.dupeclient.client.module.macro;

import java.util.Locale;
import net.minecraft.world.inventory.ClickType;

/** Editor cycle + parsing for {@link ClickType} names on {@link MacroStepType#CLICK_SLOT}. */
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
            ClickType.valueOf(u);
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

    public static ClickType toVanilla(String raw) {
        try {
            return ClickType.valueOf(normalize(raw));
        } catch (IllegalArgumentException e) {
            return ClickType.QUICK_MOVE;
        }
    }
}
