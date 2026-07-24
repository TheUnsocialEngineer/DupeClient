package com.dupeclient.client.module.waypoint;

import com.google.gson.JsonObject;

import java.util.UUID;

public record SharedDupeClientWaypoint(
    DupeClientWaypoint waypoint,
    UUID ownerUuid,
    String ownerName,
    boolean ownedBySelf
) {
    public static SharedDupeClientWaypoint fromJson(JsonObject o, UUID selfUuid) {
        if (o == null || !o.has("id")) {
            return null;
        }
        DupeClientWaypoint wp = new DupeClientWaypoint(
            o.get("id").getAsString(),
            o.has("name") ? o.get("name").getAsString() : "Waypoint",
            o.has("x") ? o.get("x").getAsInt() : 0,
            o.has("y") ? o.get("y").getAsInt() : 0,
            o.has("z") ? o.get("z").getAsInt() : 0,
            o.has("dimension") ? o.get("dimension").getAsString() : "",
            o.has("color") ? o.get("color").getAsInt() : 0xFF60A5FA,
            WaypointShape.fromWire(o.has("shape") ? o.get("shape").getAsString() : null),
            WaypointShareAudience.fromWire(o.has("shareAudience") ? o.get("shareAudience").getAsString() : null),
            0L
        );
        UUID owner = null;
        if (o.has("ownerUuid") && !o.get("ownerUuid").isJsonNull()) {
            try {
                owner = UUID.fromString(o.get("ownerUuid").getAsString());
            } catch (Exception ignored) {
            }
        }
        String ownerName = o.has("ownerName") ? o.get("ownerName").getAsString() : "";
        boolean self = selfUuid != null && owner != null && selfUuid.equals(owner);
        return new SharedDupeClientWaypoint(wp, owner, ownerName, self);
    }
}
