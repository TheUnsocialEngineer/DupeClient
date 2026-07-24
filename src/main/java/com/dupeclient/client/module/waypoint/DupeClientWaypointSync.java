package com.dupeclient.client.module.waypoint;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.core.session.SocialHubRules;
import com.dupeclient.client.module.cape.DupeClientCapePresence;
import com.dupeclient.client.module.cape.DupeClientPresenceConfigManager;
import com.dupeclient.client.module.dupedb.DupedbManager;
import com.dupeclient.client.module.social.DupeClientSocialFriendsManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class DupeClientWaypointSync {
    private DupeClientWaypointSync() {
    }

    static int syncBlocking(UUID selfUuid, String username, List<DupeClientWaypoint> waypoints) {
        if (selfUuid == null || !SocialHubRules.presenceBroadcastAllowed()) {
            return -1;
        }
        if (!Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().enabled)) {
            return -1;
        }
        if (!Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().shareWaypoints)) {
            return -1;
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("minecraftUuid", selfUuid.toString());
            if (username != null && !username.isBlank()) {
                body.addProperty("minecraftUsername", username.trim());
            }
            JsonArray friendUuids = new JsonArray();
            for (UUID friend : DupeClientSocialFriendsManager.friendUuidSet()) {
                friendUuids.add(friend.toString());
            }
            body.add("friendUuids", friendUuids);
            JsonArray arr = new JsonArray();
            for (DupeClientWaypoint wp : waypoints) {
                if (wp.shareAudience() == WaypointShareAudience.PRIVATE) {
                    continue;
                }
                arr.add(wp.toSyncJson());
            }
            body.add("waypoints", arr);
            return postJson(DupeClientCapePresence.resolvedPresenceApiBase() + "/waypoints/sync", body.toString());
        } catch (Exception ex) {
            DupeClient.LOGGER.debug("Waypoint sync failed", ex);
            return -1;
        }
    }

    static List<SharedDupeClientWaypoint> fetchBlocking(UUID viewerUuid) throws IOException {
        List<SharedDupeClientWaypoint> out = new ArrayList<>();
        if (viewerUuid == null || !SocialHubRules.socialListFetchAllowed()) {
            return out;
        }
        if (!Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().enabled)) {
            return out;
        }
        String url = DupeClientCapePresence.resolvedPresenceApiBase() + "/waypoints?viewerUuid=" + viewerUuid;
        String json = httpGet(url);
        if (json == null || json.isBlank()) {
            throw new IOException("waypoint fetch empty/non-2xx");
        }
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return out;
            }
            JsonArray arr = root.getAsJsonObject().getAsJsonArray("waypoints");
            if (arr == null) {
                return out;
            }
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) {
                    continue;
                }
                SharedDupeClientWaypoint row = SharedDupeClientWaypoint.fromJson(el.getAsJsonObject(), viewerUuid);
                if (row != null) {
                    out.add(row);
                }
            }
        } catch (Exception ex) {
            DupeClient.LOGGER.debug("Waypoint fetch parse failed", ex);
        }
        return out;
    }

    private static int postJson(String urlString, String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        applyAuth(conn);
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }
        int code = conn.getResponseCode();
        drainQuietly(conn);
        conn.disconnect();
        return code;
    }

    private static String httpGet(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("Accept", "application/json");
        applyAuth(conn);
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            drainQuietly(conn);
            conn.disconnect();
            return null;
        }
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private static void applyAuth(HttpURLConnection conn) {
        String token = DupedbManager.INSTANCE.getOAuthTokenOrEmpty();
        if (!token.isBlank()) {
            conn.setRequestProperty("X-App-Token", token);
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
    }

    private static void drainQuietly(HttpURLConnection conn) {
        try {
            InputStream err = conn.getErrorStream();
            if (err != null) {
                err.readAllBytes();
                return;
            }
            try (InputStream in = conn.getInputStream()) {
                in.readAllBytes();
            }
        } catch (IOException ignored) {
        }
    }
}
