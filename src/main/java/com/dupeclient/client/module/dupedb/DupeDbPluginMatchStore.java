package com.dupeclient.client.module.dupedb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.dupeclient.client.config.DupeClientConfigDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists which servers (ip:port) had at least one DupeDB plugin-exploit match in the scanner modal.
 */
public final class DupeDbPluginMatchStore {
    private static final Path FILE =
            DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_SERVER_SCANNER_DUPEDB_MATCHES);
    private static final Object LOCK = new Object();
    private static Map<String, Boolean> cache;
    private static Map<String, Map<String, String>> linkCache;
    private static boolean loaded;

    private DupeDbPluginMatchStore() {
    }

    public static boolean isMatch(String serverKey) {
        synchronized (LOCK) {
            loadIfNeeded();
            return Boolean.TRUE.equals(cache.get(serverKey));
        }
    }

    public static void setMatch(String serverKey, boolean match) {
        synchronized (LOCK) {
            loadIfNeeded();
            if (match) {
                cache.put(serverKey, true);
            } else {
                cache.remove(serverKey);
                if (linkCache != null) {
                    linkCache.remove(serverKey);
                }
            }
            saveLocked();
        }
    }

    public static void setPluginLinks(String serverKey, Map<String, String> pluginKeyToUrl) {
        synchronized (LOCK) {
            loadIfNeeded();
            if (linkCache == null) {
                linkCache = new HashMap<>();
            }
            if (pluginKeyToUrl == null || pluginKeyToUrl.isEmpty()) {
                linkCache.remove(serverKey);
            } else {
                linkCache.put(serverKey, new LinkedHashMap<>(pluginKeyToUrl));
            }
            saveLocked();
        }
    }

    public static Map<String, String> getPluginLinks(String serverKey) {
        synchronized (LOCK) {
            loadIfNeeded();
            if (linkCache == null) {
                return Map.of();
            }
            Map<String, String> m = linkCache.get(serverKey);
            if (m == null || m.isEmpty()) {
                return Map.of();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(m));
        }
    }

    public static void remove(String serverKey) {
        synchronized (LOCK) {
            loadIfNeeded();
            boolean changed = cache.remove(serverKey) != null;
            if (linkCache != null && linkCache.remove(serverKey) != null) {
                changed = true;
            }
            if (changed) {
                saveLocked();
            }
        }
    }

    private static void loadIfNeeded() {
        if (loaded) {
            return;
        }
        loaded = true;
        cache = new HashMap<>();
        linkCache = new HashMap<>();
        try {
            if (!Files.exists(FILE)) {
                return;
            }
            String text = Files.readString(FILE, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            JsonObject matches = root.has("matches") ? root.getAsJsonObject("matches") : root;
            for (String key : matches.keySet()) {
                try {
                    if (matches.get(key).getAsBoolean()) {
                        cache.put(key, true);
                    }
                } catch (Exception ignored) {
                }
            }
            if (root.has("links") && root.get("links").isJsonObject()) {
                JsonObject linksRoot = root.getAsJsonObject("links");
                for (String serverKey : linksRoot.keySet()) {
                    if (!linksRoot.get(serverKey).isJsonObject()) {
                        continue;
                    }
                    JsonObject inner = linksRoot.getAsJsonObject(serverKey);
                    Map<String, String> m = new LinkedHashMap<>();
                    for (String pk : inner.keySet()) {
                        try {
                            m.put(pk, inner.get(pk).getAsString());
                        } catch (Exception ignored) {
                        }
                    }
                    if (!m.isEmpty()) {
                        linkCache.put(serverKey, m);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveLocked() {
        try {
            JsonObject root = new JsonObject();
            JsonObject m = new JsonObject();
            for (Map.Entry<String, Boolean> e : cache.entrySet()) {
                if (Boolean.TRUE.equals(e.getValue())) {
                    m.addProperty(e.getKey(), true);
                }
            }
            root.add("matches", m);
            if (linkCache != null && !linkCache.isEmpty()) {
                JsonObject linksRoot = new JsonObject();
                for (Map.Entry<String, Map<String, String>> e : linkCache.entrySet()) {
                    if (e.getValue() == null || e.getValue().isEmpty()) {
                        continue;
                    }
                    JsonObject inner = new JsonObject();
                    for (Map.Entry<String, String> pe : e.getValue().entrySet()) {
                        inner.addProperty(pe.getKey(), pe.getValue());
                    }
                    linksRoot.add(e.getKey(), inner);
                }
                if (linksRoot.size() > 0) {
                    root.add("links", linksRoot);
                }
            }
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
