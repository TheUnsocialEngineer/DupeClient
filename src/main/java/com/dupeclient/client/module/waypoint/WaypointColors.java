package com.dupeclient.client.module.waypoint;

public final class WaypointColors {
    public static final int[] PRESETS = {
        0xFF4ADE80,
        0xFF60A5FA,
        0xFFF87171,
        0xFFFACC15,
        0xFFFB923C,
        0xFFC084FC,
        0xFF22D3EE,
        0xFFF472B6
    };

    private WaypointColors() {
    }

    public static int cycle(int current) {
        for (int i = 0; i < PRESETS.length; i++) {
            if (PRESETS[i] == current) {
                return PRESETS[(i + 1) % PRESETS.length];
            }
        }
        return PRESETS[0];
    }

    public static String label(int argb) {
        return switch (argb) {
            case 0xFF4ADE80 -> "Green";
            case 0xFF60A5FA -> "Blue";
            case 0xFFF87171 -> "Red";
            case 0xFFFACC15 -> "Yellow";
            case 0xFFFB923C -> "Orange";
            case 0xFFC084FC -> "Purple";
            case 0xFF22D3EE -> "Cyan";
            case 0xFFF472B6 -> "Pink";
            default -> "Custom";
        };
    }
}
