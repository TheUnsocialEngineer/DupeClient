package com.dupeclient.client.module.social;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * One row from {@code GET {apiBase}/list} {@code players[]} .
 */
public record OnlineDupeClientUser(
        UUID minecraftUuid,
        String minecraftUsername,
        String clientId,
        String build,
        @Nullable String server,
        @Nullable String coords,
        PresenceListAudience listAudience) {

    public OnlineDupeClientUser {
        minecraftUsername = minecraftUsername == null ? "" : minecraftUsername.trim();
        clientId = Objects.requireNonNullElse(clientId, "");
        build = Objects.requireNonNullElse(build, "");
        listAudience = listAudience == null ? PresenceListAudience.PUBLIC : listAudience;
    }

    @Nullable
    public static OnlineDupeClientUser tryParse(JsonObject o) {
        if (o == null || !o.has("minecraftUuid") || o.get("minecraftUuid").isJsonNull()) {
            return null;
        }
        UUID id;
        try {
            id = UUID.fromString(o.get("minecraftUuid").getAsString());
        } catch (Exception e) {
            return null;
        }
        String username = "";
        if (o.has("minecraftUsername") && !o.get("minecraftUsername").isJsonNull()) {
            username = o.get("minecraftUsername").getAsString().trim();
        } else if (o.has("username") && !o.get("username").isJsonNull()) {
            username = o.get("username").getAsString().trim();
        } else if (o.has("name") && !o.get("name").isJsonNull()) {
            username = o.get("name").getAsString().trim();
        }
        String client = o.has("client") && !o.get("client").isJsonNull() ? o.get("client").getAsString().trim() : "";
        String build = o.has("build") && !o.get("build").isJsonNull() ? o.get("build").getAsString().trim() : "";
        String server = null;
        if (o.has("server") && !o.get("server").isJsonNull()) {
            String s = o.get("server").getAsString().trim();
            server = s.isEmpty() ? null : s;
        }
        String coords = null;
        if (o.has("coords") && !o.get("coords").isJsonNull()) {
            String c = o.get("coords").getAsString().trim();
            coords = c.isEmpty() ? null : c;
        }
        PresenceListAudience audience = PresenceListAudience.PUBLIC;
        if (o.has("listAudience") && !o.get("listAudience").isJsonNull()) {
            audience = PresenceListAudience.fromWire(o.get("listAudience").getAsString());
        }
        return new OnlineDupeClientUser(id, username, client, build, server, coords, audience);
    }
}
