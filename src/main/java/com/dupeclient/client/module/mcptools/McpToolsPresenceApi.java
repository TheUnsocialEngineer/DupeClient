package com.dupeclient.client.module.mcptools;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class McpToolsPresenceApi {
    private McpToolsPresenceApi() {
    }

    public static String mcptoolsBase() {
        return DupeClientCapePresence.resolvedPresenceApiBase() + "/mcptools";
    }

    public record ManifestFile(String path, String sha256, long size) {
    }

    public record Manifest(List<ManifestFile> files, String bundleVersion) {
    }

    public record JobStatus(String jobId, String status, int exitCode, List<String> output) {
    }

    public static Manifest fetchManifest() throws IOException {
        String json = httpGet(mcptoolsBase() + "/manifest");
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String version = root.has("bundleVersion") ? root.get("bundleVersion").getAsString() : "?";
        List<ManifestFile> files = new ArrayList<>();
        if (root.has("files") && root.get("files").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("files")) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject row = el.getAsJsonObject();
                if (!row.has("path") || !row.has("sha256")) {
                    continue;
                }
                long size = row.has("size") ? row.get("size").getAsLong() : 0L;
                files.add(new ManifestFile(row.get("path").getAsString(), row.get("sha256").getAsString(), size));
            }
        }
        return new Manifest(files, version);
    }

    public static byte[] fetchFile(String relPath) throws IOException {
        String url = mcptoolsBase() + "/file?path=" + URLEncoder.encode(relPath, StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(120_000);
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            drain(conn);
            conn.disconnect();
            throw new IOException("HTTP " + code + " for " + relPath);
        }
        try (InputStream in = conn.getInputStream()) {
            return in.readAllBytes();
        } finally {
            conn.disconnect();
        }
    }

    public static String startRemoteJob(McpToolsTool tool, McpToolsSettings settings) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("tool", tool.id);
        body.addProperty("host", settings.lastHost);
        body.addProperty("port", settings.lastPort);
        body.addProperty("username", settings.lastUsername);
        body.addProperty("version", McpToolsMcVersion.resolveForSettings(settings));
        if (tool.needsUpload && settings.uploadText != null && !settings.uploadText.isBlank()) {
            String field = tool == McpToolsTool.BRUTE_AUTH ? "wordlistText" : "commandsText";
            body.addProperty(field, settings.uploadText);
        }
        String json = httpPostJson(mcptoolsBase() + "/run", body.toString());
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("jobId")) {
            throw new IOException("missing jobId");
        }
        return root.get("jobId").getAsString();
    }

    public static JobStatus pollJob(String jobId) throws IOException {
        String json = httpGet(mcptoolsBase() + "/job/" + jobId);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String status = root.has("status") ? root.get("status").getAsString() : "unknown";
        int exit = root.has("exitCode") && !root.get("exitCode").isJsonNull() ? root.get("exitCode").getAsInt() : -1;
        List<String> lines = new ArrayList<>();
        if (root.has("output") && root.get("output").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("output")) {
                lines.add(el.getAsString());
            }
        }
        return new JobStatus(jobId, status, exit, lines);
    }

    private static String httpGet(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(12_000);
        conn.setReadTimeout(30_000);
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            drain(conn);
            conn.disconnect();
            throw new IOException("HTTP " + code);
        }
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private static String httpPostJson(String urlString, String body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(12_000);
        conn.setReadTimeout(30_000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(bytes);
        }
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            drain(conn);
            conn.disconnect();
            throw new IOException("HTTP " + code);
        }
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private static void drain(HttpURLConnection conn) {
        try (InputStream err = conn.getErrorStream()) {
            if (err != null) {
                err.readAllBytes();
            }
        } catch (IOException ignored) {
        }
    }
}
