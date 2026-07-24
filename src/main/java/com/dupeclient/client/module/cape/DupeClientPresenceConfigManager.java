package com.dupeclient.client.module.cape;

import com.dupeclient.client.module.cape.DupeClientPresenceSettings;
import com.dupeclient.DupeBuildConstants;
import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class DupeClientPresenceConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve("presence.json");
    private static volatile DupeClientPresenceSettings settings = new DupeClientPresenceSettings();

    private DupeClientPresenceConfigManager() {
    }

    public static void initialize() {
        reload();
    }

    public static void reload() {
        if (!Files.exists(CONFIG_PATH)) {
            DupeClientPresenceSettings def = normalize(new DupeClientPresenceSettings());
            save(def);
            settings = def;
            return;
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            DupeClientPresenceSettings loaded = GSON.fromJson(raw, DupeClientPresenceSettings.class);
            DupeClientPresenceSettings normalized = normalize(loaded != null ? loaded : new DupeClientPresenceSettings());
            settings = normalized;
            if (needsMigration(raw, normalized)) {
                save(normalized);
            }
        } catch (IOException ignored) {
            settings = normalize(new DupeClientPresenceSettings());
        }
    }

    public static DupeClientPresenceSettings get() {
        return settings;
    }

    public static void save(DupeClientPresenceSettings s) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            DupeClientPresenceSettings normalized = normalize(s);
            normalized.configVersion = DupeClientPresenceSettings.CONFIG_VERSION;
            Files.writeString(CONFIG_PATH, GSON.toJson(normalized));
            settings = normalized;
        } catch (IOException ignored) {
        }
    }

    private static boolean needsMigration(String raw, DupeClientPresenceSettings normalized) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        if (normalized.configVersion < DupeClientPresenceSettings.CONFIG_VERSION) {
            return true;
        }
        try {
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            return !obj.has("shareCurrentCoords")
                    || !obj.has("showCoordsInSocial")
                    || !obj.has("shareWaypoints")
                    || !obj.has("showSharedWaypointsInWorld")
                    || !obj.has("waypointsFriendsOnlyView")
                    || !obj.has("openSocialKey")
                    || !obj.has("openWaypointsKey")
                    || !obj.has("configVersion");
        } catch (Exception ex) {
            return true;
        }
    }

    private static DupeClientPresenceSettings normalize(DupeClientPresenceSettings s) {
        if (s.configVersion <= 0) {
            s.configVersion = 1;
        }
        if (s.enabled == null) {
            s.enabled = Boolean.TRUE;
        }
        if (s.broadcastPresence == null) {
            s.broadcastPresence = Boolean.TRUE;
        }
        if (s.shareCurrentServer == null) {
            s.shareCurrentServer = Boolean.FALSE;
        }
        if (s.shareCurrentCoords == null) {
            s.shareCurrentCoords = Boolean.FALSE;
        }
        if (s.showServersInSocial == null) {
            s.showServersInSocial = Boolean.TRUE;
        }
        if (s.showCoordsInSocial == null) {
            s.showCoordsInSocial = Boolean.TRUE;
        }
        if (s.hideSelfInSocial == null) {
            s.hideSelfInSocial = Boolean.TRUE;
        }
        if (s.presenceListAudience == null || s.presenceListAudience.isBlank()) {
            s.presenceListAudience = "everyone";
        } else {
            String a = s.presenceListAudience.trim().toLowerCase(Locale.ROOT);
            if (!"everyone".equals(a) && !"friends_only".equals(a)) {
                a = "everyone";
            }
            s.presenceListAudience = a;
        }
        if (s.socialListFriendsOnlyView == null) {
            s.socialListFriendsOnlyView = Boolean.FALSE;
        }
        if (s.shareWaypoints == null) {
            s.shareWaypoints = Boolean.TRUE;
        }
        if (s.showSharedWaypointsInWorld == null) {
            s.showSharedWaypointsInWorld = Boolean.TRUE;
        }
        if (s.waypointsFriendsOnlyView == null) {
            s.waypointsFriendsOnlyView = Boolean.FALSE;
        }
        s.apiBase = s.apiBase == null || s.apiBase.isBlank()
                ? DupeBuildConstants.PRESENCE_API_BASE
                : trimTrailingSlashes(s.apiBase.trim());
        s.configVersion = DupeClientPresenceSettings.CONFIG_VERSION;
        return s;
    }

    private static String trimTrailingSlashes(String s) {
        int end;
        for (end = s.length(); end > 0 && s.charAt(end - 1) == '/'; --end) {
        }
        return s.substring(0, end);
    }
}
