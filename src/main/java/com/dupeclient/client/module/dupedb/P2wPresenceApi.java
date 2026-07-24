package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.cape.DupeClientCapePresence;
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
import java.util.Locale;

public final class P2wPresenceApi {
    private P2wPresenceApi() {
    }

    public record ServerMark(String server, int score, long markedAt, int votes, boolean verified) {
    }

    public record PendingMark(String server, String action, int votes, int required) {
    }

    public record Registry(List<ServerMark> p2w, List<ServerMark> nonP2w, List<PendingMark> pending) {
        public static Registry empty() {
            return new Registry(List.of(), List.of(), List.of());
        }

        public String statusForServer(String server) {
            if (server == null || server.isBlank()) {
                return "";
            }
            String norm = normalizeServer(server);
            for (ServerMark mark : p2w) {
                if (norm.equals(mark.server())) {
                    return "P2W" + (mark.score() >= 0 ? " " + mark.score() + "%" : "");
                }
            }
            for (ServerMark mark : nonP2w) {
                if (norm.equals(mark.server())) {
                    return "Non-P2W";
                }
            }
            for (PendingMark pending : pending) {
                if (norm.equals(pending.server())) {
                    return "Pending " + pending.action() + " (" + pending.votes() + "/" + pending.required() + ")";
                }
            }
            return "";
        }
    }

    public record SubmitResult(boolean success, String status, int votes, int required, String error, String message) {
        public static SubmitResult failed(String error, String message) {
            return new SubmitResult(false, "rejected", 0, 0, error, message);
        }
    }

    public static Registry fetchRegistry() throws IOException {
        String body = getJson(p2wListUrl());
        if (body == null || body.isBlank()) {
            return Registry.empty();
        }
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        return new Registry(
                parseArray(root, "p2w"),
                parseArray(root, "nonP2w"),
                parsePending(root));
    }

    public static SubmitResult submitMark(String server, MarkEvidence evidence, boolean markAsP2w, String minecraftUuid, String username)
            throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("server", server);
        body.addProperty("action", markAsP2w ? "mark" : "unmark");
        body.addProperty("p2wScore", Math.max(0, evidence.p2wScore()));
        body.addProperty("scanCompleted", evidence.scanCompleted());
        body.addProperty("pluginCount", Math.max(0, evidence.pluginCount()));
        body.addProperty("sessionMinutes", Math.max(0, evidence.sessionMinutes()));
        body.addProperty("minecraftUuid", minecraftUuid);
        if (username != null && !username.isBlank()) {
            body.addProperty("minecraftUsername", username.length() > 32 ? username.substring(0, 32) : username);
        }
        body.addProperty("client", DupeClient.MOD_ID);
        body.addProperty("build", DupeClient.BUILD_TAG);
        return postMark(p2wMarkUrl(), body.toString());
    }

    public record MarkEvidence(int p2wScore, boolean scanCompleted, int pluginCount, int sessionMinutes) {
    }

    public static String normalizeServer(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        return s;
    }

    private static List<ServerMark> parseArray(JsonObject root, String key) {
        List<ServerMark> out = new ArrayList<>();
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return out;
        }
        for (JsonElement el : root.getAsJsonArray(key)) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String server = readString(o, "server");
            if (server.isBlank()) {
                continue;
            }
            int score = o.has("score") && !o.get("score").isJsonNull() ? o.get("score").getAsInt() : -1;
            long markedAt = o.has("markedAt") && !o.get("markedAt").isJsonNull() ? o.get("markedAt").getAsLong() : 0L;
            int votes = o.has("votes") && !o.get("votes").isJsonNull() ? o.get("votes").getAsInt() : 0;
            boolean verified = !o.has("verified") || o.get("verified").getAsBoolean();
            out.add(new ServerMark(normalizeServer(server), score, markedAt, votes, verified));
        }
        return out;
    }

    private static List<PendingMark> parsePending(JsonObject root) {
        List<PendingMark> out = new ArrayList<>();
        if (!root.has("pending") || !root.get("pending").isJsonArray()) {
            return out;
        }
        for (JsonElement el : root.getAsJsonArray("pending")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String server = readString(o, "server");
            if (server.isBlank()) {
                continue;
            }
            String action = readString(o, "action");
            int votes = o.has("votes") ? o.get("votes").getAsInt() : 0;
            int required = o.has("required") ? o.get("required").getAsInt() : 2;
            out.add(new PendingMark(normalizeServer(server), action, votes, required));
        }
        return out;
    }

    private static String readString(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        return o.get(key).getAsString().trim();
    }

    private static String p2wBase() {
        return DupeClientCapePresence.resolvedPresenceApiBase();
    }

    private static String p2wListUrl() {
        return p2wBase() + "/p2w";
    }

    private static String p2wMarkUrl() {
        return p2wBase() + "/p2w/mark";
    }

    private static String getJson(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(12_000);
        conn.setReadTimeout(12_000);
        conn.setRequestProperty("Accept", "application/json");
        applyOptionalDupedbAuth(conn);
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

    private static SubmitResult postMark(String urlString, String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(12_000);
        conn.setReadTimeout(12_000);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        applyOptionalDupedbAuth(conn);
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }
        int code = conn.getResponseCode();
        String body = readBody(conn, code);
        conn.disconnect();
        if (body == null || body.isBlank()) {
            if (code >= 200 && code < 300) {
                return new SubmitResult(true, "verified", 0, 0, "", "Mark submitted.");
            }
            return SubmitResult.failed("http_" + code, "Presence API rejected the mark (HTTP " + code + ").");
        }
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        String error = readString(root, "error");
        String message = readString(root, "message");
        if (message.isBlank() && !error.isBlank()) {
            message = error;
        }
        if (code == 422 || !error.isBlank()) {
            return SubmitResult.failed(error.isBlank() ? "rejected" : error, message.isBlank() ? "Mark rejected." : message);
        }
        String status = readString(root, "status");
        int votes = root.has("votes") ? root.get("votes").getAsInt() : 0;
        int required = root.has("required") ? root.get("required").getAsInt() : 0;
        return new SubmitResult(true, status.isBlank() ? "pending" : status, votes, required, "", message);
    }

    private static String readBody(HttpURLConnection conn, int code) {
        try (InputStream in = code >= 400 && conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()) {
            if (in == null) {
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void applyOptionalDupedbAuth(HttpURLConnection conn) {
        String token = DupedbManager.INSTANCE.getOAuthTokenOrEmpty();
        if (token.isBlank()) {
            return;
        }
        conn.setRequestProperty("Authorization", "Bearer " + token);
    }

    private static void drainQuietly(HttpURLConnection conn) {
        try (InputStream in = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()) {
            if (in != null) {
                in.readAllBytes();
            }
        } catch (Exception ignored) {
        }
    }
}
