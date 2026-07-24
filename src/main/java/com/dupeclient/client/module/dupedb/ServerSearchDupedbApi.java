package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.module.dupedb.DupedbManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * DupeDB HTTP for the server scanner details modal (same endpoints as {@link DupedbManager}).
 */
public final class ServerSearchDupedbApi {
    public static final String DUPEDB_API_BASE = "https://dupedb.net/api";
    public static final String DUPEDB_STATS_URL = DUPEDB_API_BASE + "/public/stats";
    public static final String DUPEDB_EXPLOITS_SEARCH_URL = DUPEDB_API_BASE + "/exploits/search";

    private ServerSearchDupedbApi() {
    }

    public static String fetch(String urlString, Consumer<String> debug) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(urlString).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 DupeClient");
            conn.setRequestProperty("Accept", "application/json");
            String token = DupedbManager.INSTANCE.getOAuthTokenOrEmpty();
            if (token != null && !token.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + token.trim());
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            java.io.InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            StringBuilder body = new StringBuilder();
            if (stream != null) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        body.append(line);
                    }
                }
            }

            if (code != 200) {
                String preview = body.length() > 20 ? body.substring(0, 20) + "..." : body.toString();
                debug.accept("HTTP " + code + ", body: " + preview);
                return null;
            }
            return body.toString();
        } catch (Exception e) {
            debug.accept("Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return null;
        }
    }

    public static JsonArray parseExploitsArray(JsonElement parsed) {
        if (parsed == null) {
            return null;
        }
        if (parsed.isJsonArray()) {
            return parsed.getAsJsonArray();
        }
        if (!parsed.isJsonObject()) {
            return null;
        }
        JsonObject obj = parsed.getAsJsonObject();
        if (obj.has("exploits")) {
            return obj.getAsJsonArray("exploits");
        }
        if (obj.has("data")) {
            JsonElement data = obj.get("data");
            if (data.isJsonArray()) {
                return data.getAsJsonArray();
            }
            if (data.isJsonObject() && data.getAsJsonObject().has("exploits")) {
                return data.getAsJsonObject().getAsJsonArray("exploits");
            }
        }
        if (obj.has("items")) {
            return obj.getAsJsonArray("items");
        }
        if (obj.has("results")) {
            return obj.getAsJsonArray("results");
        }
        return null;
    }
}
