package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.DupeClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Matches server plugin names against DupeDB using {@code GET /api/exploits/search}.
 */
public final class DupeDbPluginMatcher {
    private static final int SEARCH_PAGE_LIMIT = 50;
    private static final int MAX_SEARCH_PAGES = 2000;

    private DupeDbPluginMatcher() {
    }

    public static String normalizePluginKey(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public static boolean exploitReferencesPlugin(JsonObject exploit, String serverPlugin) {
        String scanKey = normalizePluginKey(serverPlugin);
        if (scanKey.isEmpty()) {
            return false;
        }

        if (exploit.has("plugin_name") && !exploit.get("plugin_name").isJsonNull()) {
            String pn = exploit.get("plugin_name").getAsString().trim();
            if (!pn.isEmpty()) {
                if (normalizePluginKey(pn).equals(scanKey) || pn.equalsIgnoreCase(serverPlugin.trim())) {
                    return true;
                }
            }
        }

        if (exploit.has("plugins") && exploit.get("plugins").isJsonArray()) {
            JsonArray plugs = exploit.getAsJsonArray("plugins");
            for (JsonElement pe : plugs) {
                if (!pe.isJsonObject()) {
                    continue;
                }
                JsonObject po = pe.getAsJsonObject();
                if (!po.has("name") || po.get("name").isJsonNull()) {
                    continue;
                }
                String name = po.get("name").getAsString().trim();
                if (name.isEmpty()) {
                    continue;
                }
                if (normalizePluginKey(name).equals(scanKey) || name.equalsIgnoreCase(serverPlugin.trim())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static List<JsonObject> fetchAllExploitsSearch(Consumer<String> debug) {
        List<JsonObject> all = new ArrayList<>();
        int page = 1;

        while (page <= MAX_SEARCH_PAGES) {
            String url = ServerSearchDupedbApi.DUPEDB_EXPLOITS_SEARCH_URL + "?page=" + page + "&limit=" + SEARCH_PAGE_LIMIT
                + "&sort=date_submitted&order=desc";
            String json = ServerSearchDupedbApi.fetch(url, debug);
            if (json == null) {
                if (page == 1) {
                    return null;
                }
                break;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray arr = root.has("exploits") ? root.getAsJsonArray("exploits") : ServerSearchDupedbApi.parseExploitsArray(root);
            if (arr == null || arr.isEmpty()) {
                break;
            }

            for (JsonElement elem : arr) {
                if (elem.isJsonObject()) {
                    all.add(elem.getAsJsonObject());
                }
            }

            boolean fetchMore = false;
            if (root.has("pagination") && root.get("pagination").isJsonObject()) {
                JsonObject pag = root.getAsJsonObject("pagination");
                if (pag.has("hasMore")) {
                    fetchMore = pag.get("hasMore").getAsBoolean();
                } else if (pag.has("pages")) {
                    int pages = Math.max(1, pag.get("pages").getAsInt());
                    fetchMore = page < pages;
                }
            }

            if (!fetchMore) {
                break;
            }

            page++;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return all;
    }

    public static DupeDbSearchResult searchPluginMatches(List<String> serverPlugins) {
        List<DupeDbMatch> matches = new ArrayList<>();
        Map<String, String> exploitUrlByPluginKey = new LinkedHashMap<>();
        if (serverPlugins == null || serverPlugins.isEmpty()) {
            return new DupeDbSearchResult(false, matches, exploitUrlByPluginKey);
        }

        try {
            Consumer<String> debug = msg -> DupeClient.LOGGER.debug("[DupeDB] {}", msg);

            String statsJson = ServerSearchDupedbApi.fetch(ServerSearchDupedbApi.DUPEDB_STATS_URL, debug);
            if (statsJson == null) {
                DupeClient.LOGGER.warn("[DupeDB] stats request failed. Use /dupedb login if needed.");
                return new DupeDbSearchResult(true, matches, exploitUrlByPluginKey);
            }

            List<JsonObject> allExploits = fetchAllExploitsSearch(debug);
            if (allExploits == null) {
                DupeClient.LOGGER.warn("[DupeDB] failed to load /api/exploits/search (first page). Check token and dupedb.net.");
                return new DupeDbSearchResult(true, matches, exploitUrlByPluginKey);
            }

            DupeClient.LOGGER.info("[DupeDB] loaded {} exploit row(s) from /api/exploits/search", allExploits.size());

            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (String p : serverPlugins) {
                if (p != null && !p.isBlank()) {
                    unique.add(p.trim());
                }
            }

            for (String plugin : unique) {
                String norm = normalizePluginKey(plugin);
                if (norm.isEmpty()) {
                    continue;
                }
                for (JsonObject exploit : allExploits) {
                    if (!exploitReferencesPlugin(exploit, plugin)) {
                        continue;
                    }
                    String exploitId = exploit.has("id") ? exploit.get("id").getAsString()
                        : exploit.has("_id") ? exploit.get("_id").getAsString() : null;
                    if (exploitId == null) {
                        continue;
                    }
                    exploitUrlByPluginKey.putIfAbsent(norm, "https://dupedb.net/exploit/" + exploitId);
                    break;
                }
            }

            Set<String> seenExploitIds = new HashSet<>();
            for (String plugin : unique) {
                for (JsonObject exploit : allExploits) {
                    if (!exploitReferencesPlugin(exploit, plugin)) {
                        continue;
                    }

                    String exploitId = exploit.has("id") ? exploit.get("id").getAsString()
                        : exploit.has("_id") ? exploit.get("_id").getAsString() : null;
                    if (exploitId == null || !seenExploitIds.add(exploitId)) {
                        continue;
                    }

                    String pluginVersion = exploit.has("plugin_version") && !exploit.get("plugin_version").isJsonNull()
                        ? exploit.get("plugin_version").getAsString() : null;
                    if (pluginVersion != null) {
                        pluginVersion = pluginVersion.trim();
                    }
                    boolean versionConfirmed = (pluginVersion == null || pluginVersion.isEmpty() || "*".equals(pluginVersion));

                    String exploitTitle = exploit.has("name") ? exploit.get("name").getAsString()
                        : exploit.has("title") ? exploit.get("title").getAsString() : "Exploit #" + exploitId;

                    matches.add(new DupeDbMatch(
                        "https://dupedb.net/exploit/" + exploitId,
                        exploitTitle,
                        versionConfirmed
                    ));
                    DupeClient.LOGGER.info("[DupeDB] MATCH plugin '{}' -> exploit id={} title={}", plugin, exploitId, exploitTitle);
                }
            }
        } catch (Exception e) {
            DupeClient.LOGGER.error("[DupeDB] matcher error", e);
        }
        return new DupeDbSearchResult(false, matches, exploitUrlByPluginKey);
    }

    public record DupeDbSearchResult(boolean apiFailed, List<DupeDbMatch> matches, Map<String, String> exploitUrlByPluginKey) {
        public boolean hasAnyMatch() {
            return matches != null && !matches.isEmpty();
        }
    }

    public record DupeDbMatch(String url, String title, boolean versionConfirmed) {
    }
}
