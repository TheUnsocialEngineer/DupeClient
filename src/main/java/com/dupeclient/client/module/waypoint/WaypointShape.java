package com.dupeclient.client.module.waypoint;

import org.jetbrains.annotations.Nullable;

public enum WaypointShape {
    BEACON("beacon", "Beacon"),
    CUBE("cube", "Cube"),
    DIAMOND("diamond", "Diamond"),
    STAR("star", "Star"),
    RING("ring", "Ring");

    private final String wire;
    private final String label;

    WaypointShape(String wire, String label) {
        this.wire = wire;
        this.label = label;
    }

    public String wireValue() {
        return wire;
    }

    public String label() {
        return label;
    }

    public WaypointShape next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static WaypointShape fromWire(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return BEACON;
        }
        String v = raw.trim().toLowerCase();
        for (WaypointShape shape : values()) {
            if (shape.wire.equals(v)) {
                return shape;
            }
        }
        return BEACON;
    }
}
