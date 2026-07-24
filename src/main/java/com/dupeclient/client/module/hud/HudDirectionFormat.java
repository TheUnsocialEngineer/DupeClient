package com.dupeclient.client.module.hud;

import net.minecraft.util.math.MathHelper;

import java.util.Locale;

final class HudDirectionFormat {
    private static final String[] OCTANTS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private HudDirectionFormat() {
    }

    static String facing(float yaw) {
        int idx = octantIndex(yaw);
        return "Facing: " + OCTANTS[idx];
    }

    static String compass(float yaw) {
        int active = octantIndex(yaw);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < OCTANTS.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            if (i == active) {
                sb.append('[').append(OCTANTS[i]).append(']');
            } else {
                sb.append(OCTANTS[i]);
            }
        }
        return sb.toString();
    }

    static String degrees(float yaw) {
        return String.format(Locale.US, "%.0f°", MathHelper.wrapDegrees(yaw));
    }

    private static int octantIndex(float yaw) {
        return Math.floorMod(Math.round(MathHelper.wrapDegrees(yaw) / 45f), 8);
    }
}
