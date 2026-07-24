package com.dupeclient.client.module.waypoint;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class DupeClientWaypointStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = DupeClientConfigDir.root().resolve("waypoints.json");

    private DupeClientWaypointStore() {
    }

    static Data load() {
        if (!Files.exists(PATH)) {
            return new Data(new ArrayList<>(), WaypointShareAudience.FRIENDS_ONLY);
        }
        try {
            JsonObject root = GSON.fromJson(Files.readString(PATH), JsonObject.class);
            if (root == null) {
                return new Data(new ArrayList<>(), WaypointShareAudience.FRIENDS_ONLY);
            }
            List<DupeClientWaypoint> list = new ArrayList<>();
            if (root.has("waypoints") && root.get("waypoints").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("waypoints")) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    DupeClientWaypoint wp = DupeClientWaypoint.fromJson(el.getAsJsonObject());
                    if (wp != null) {
                        list.add(wp);
                    }
                }
            }
            WaypointShareAudience def = WaypointShareAudience.FRIENDS_ONLY;
            if (root.has("defaultShareAudience")) {
                def = WaypointShareAudience.fromWire(root.get("defaultShareAudience").getAsString());
            }
            return new Data(list, def);
        } catch (Exception ex) {
            return new Data(new ArrayList<>(), WaypointShareAudience.FRIENDS_ONLY);
        }
    }

    static void save(List<DupeClientWaypoint> waypoints, WaypointShareAudience defaultShareAudience) {
        try {
            Files.createDirectories(PATH.getParent());
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (DupeClientWaypoint wp : waypoints) {
                JsonObject o = new JsonObject();
                o.addProperty("id", wp.id());
                o.addProperty("name", wp.name());
                o.addProperty("x", wp.x());
                o.addProperty("y", wp.y());
                o.addProperty("z", wp.z());
                o.addProperty("dimension", wp.dimension());
                o.addProperty("color", wp.colorArgb());
                o.addProperty("shape", wp.shape().wireValue());
                o.addProperty("shareAudience", wp.shareAudience().wireValue());
                o.addProperty("updatedAtMs", wp.updatedAtMs());
                arr.add(o);
            }
            root.add("waypoints", arr);
            root.addProperty("defaultShareAudience", defaultShareAudience.wireValue());
            Files.writeString(PATH, GSON.toJson(root));
        } catch (IOException ignored) {
        }
    }

    record Data(List<DupeClientWaypoint> waypoints, WaypointShareAudience defaultShareAudience) {
    }
}
