package com.dupeclient.client.module.waypoint;

import com.google.gson.JsonObject;

import java.util.UUID;

public record DupeClientWaypoint(
    String id,
    String name,
    int x,
    int y,
    int z,
    String dimension,
    int colorArgb,
    WaypointShape shape,
    WaypointShareAudience shareAudience,
    long updatedAtMs
) {
    public static DupeClientWaypoint create(String name, int x, int y, int z, String dimension, int colorArgb, WaypointShape shape, WaypointShareAudience shareAudience) {
        long now = System.currentTimeMillis();
        String cleanName = name == null || name.isBlank() ? "Waypoint" : name.trim();
        if (cleanName.length() > 48) {
            cleanName = cleanName.substring(0, 48);
        }
        return new DupeClientWaypoint(
            UUID.randomUUID().toString(),
            cleanName,
            x,
            y,
            z,
            dimension == null ? "" : dimension,
            colorArgb,
            shape == null ? WaypointShape.BEACON : shape,
            shareAudience == null ? WaypointShareAudience.FRIENDS_ONLY : shareAudience,
            now
        );
    }

    public DupeClientWaypoint withEdits(String name, int x, int y, int z, String dimension, int colorArgb, WaypointShape shape, WaypointShareAudience shareAudience) {
        String cleanName = name == null || name.isBlank() ? this.name : name.trim();
        if (cleanName.length() > 48) {
            cleanName = cleanName.substring(0, 48);
        }
        return new DupeClientWaypoint(
            id,
            cleanName,
            x,
            y,
            z,
            dimension == null ? "" : dimension,
            colorArgb,
            shape == null ? WaypointShape.BEACON : shape,
            shareAudience == null ? WaypointShareAudience.FRIENDS_ONLY : shareAudience,
            System.currentTimeMillis()
        );
    }

    public DupeClientWaypoint withShareAudience(WaypointShareAudience audience) {
        return new DupeClientWaypoint(id, name, x, y, z, dimension, colorArgb, shape, audience, System.currentTimeMillis());
    }

    public JsonObject toSyncJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("name", name);
        o.addProperty("x", x);
        o.addProperty("y", y);
        o.addProperty("z", z);
        if (dimension != null && !dimension.isBlank()) {
            o.addProperty("dimension", dimension);
        }
        o.addProperty("color", colorArgb);
        o.addProperty("shape", shape.wireValue());
        o.addProperty("shareAudience", shareAudience.wireValue());
        return o;
    }

    public static DupeClientWaypoint fromJson(JsonObject o) {
        if (o == null || !o.has("id")) {
            return null;
        }
        return new DupeClientWaypoint(
            o.get("id").getAsString(),
            o.has("name") ? o.get("name").getAsString() : "Waypoint",
            o.has("x") ? o.get("x").getAsInt() : 0,
            o.has("y") ? o.get("y").getAsInt() : 0,
            o.has("z") ? o.get("z").getAsInt() : 0,
            o.has("dimension") ? o.get("dimension").getAsString() : "",
            o.has("color") ? o.get("color").getAsInt() : 0xFF4ADE80,
            WaypointShape.fromWire(o.has("shape") ? o.get("shape").getAsString() : null),
            WaypointShareAudience.fromWire(o.has("shareAudience") ? o.get("shareAudience").getAsString() : null),
            o.has("updatedAtMs") ? o.get("updatedAtMs").getAsLong() : 0L
        );
    }
}
