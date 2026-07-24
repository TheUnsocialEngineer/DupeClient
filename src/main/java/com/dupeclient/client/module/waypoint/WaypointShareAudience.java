package com.dupeclient.client.module.waypoint;

import org.jetbrains.annotations.Nullable;

public enum WaypointShareAudience {
    PUBLIC("public", "Everyone"),
    FRIENDS_ONLY("friends_only", "Friends"),
    PRIVATE("private", "Only me");

    private final String wire;
    private final String label;

    WaypointShareAudience(String wire, String label) {
        this.wire = wire;
        this.label = label;
    }

    public String wireValue() {
        return wire;
    }

    public String label() {
        return label;
    }

    public WaypointShareAudience next() {
        return switch (this) {
            case PUBLIC -> FRIENDS_ONLY;
            case FRIENDS_ONLY -> PRIVATE;
            case PRIVATE -> PUBLIC;
        };
    }

    public static WaypointShareAudience fromWire(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return FRIENDS_ONLY;
        }
        return switch (raw.trim().toLowerCase()) {
            case "public", "everyone" -> PUBLIC;
            case "friends_only", "friends" -> FRIENDS_ONLY;
            case "private", "none", "nobody" -> PRIVATE;
            default -> FRIENDS_ONLY;
        };
    }
}
