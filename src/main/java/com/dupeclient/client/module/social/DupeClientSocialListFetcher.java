package com.dupeclient.client.module.social;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.cape.DupeClientCapePresence;
import com.dupeclient.client.module.cape.DupeClientPresenceConfigManager;
import com.dupeclient.client.module.dupedb.DupedbManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * Fetches {@code GET {apiBase}/list} for the Social screen.
 */
public final class DupeClientSocialListFetcher {

    /** Wall-clock cap so the UI never stays on “Loading…” if the platform HTTP stack misbehaves. */
    private static final int FETCH_DEADLINE_SEC = 22;

    private DupeClientSocialListFetcher() {
    }

    /**
     * @param minecraftClient screen {@code client} — used to hop back to the render thread (avoid relying on
     *                        {@link MinecraftClient#getInstance()} alone, which can be unset in rare launch states).
     * @param onMainThread    (rows, errorHint) — {@code errorHint} is null on success; non-null for timeouts / HTTP / parse issues.
     */
    public static void fetchAsync(@Nullable MinecraftClient minecraftClient, BiConsumer<List<OnlineDupeClientUser>, @Nullable String> onMainThread) {
        CompletableFuture
                .supplyAsync(DupeClientSocialListFetcher::fetchRowsBlocking)
                .orTimeout(FETCH_DEADLINE_SEC, TimeUnit.SECONDS)
                .handle((rows, ex) -> {
                    List<OnlineDupeClientUser> list = rows != null ? rows : Collections.emptyList();
                    String hint = null;
                    if (ex != null) {
                        Throwable t = unwrap(ex);
                        String listUrl = DupeClientCapePresence.resolvedPresenceApiBase() + "/list";
                        if (t instanceof TimeoutException) {
                            hint = "Timed out after " + FETCH_DEADLINE_SEC + "s — " + listUrl;
                        } else if (containsSocketTimeout(t)) {
                            hint = "Connect/read timed out: " + listUrl
                                    + " (VPN/firewall? For local Node set apiBase to http://127.0.0.1:PORT/api/client/presence).";
                        } else {
                            hint = "Could not load list (" + t.getClass().getSimpleName() + "): " + listUrl;
                        }
                        DupeClient.LOGGER.warn("Social presence list fetch failed for {}", listUrl, t);
                    } else if (list.isEmpty() && !Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().enabled)) {
                        hint = "Presence is disabled in config/dupeclient/presence.json.";
                    }
                    finish(minecraftClient, list, hint, onMainThread);
                    return null;
                });
    }

    private static List<OnlineDupeClientUser> fetchRowsBlocking() {
        try {
            if (!Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().enabled)) {
                return Collections.emptyList();
            }
            String url = buildListUrl();
            String json = httpGet(url);
            if (json == null) {
                throw new IOException("presence list: non-2xx or empty body (" + url + ")");
            }
            return parse(json);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    private static String buildListUrl() {
        String base = DupeClientCapePresence.resolvedPresenceApiBase() + "/list";
        MinecraftClient mc = MinecraftClient.getInstance();
        UUID viewerUuid = mc != null && mc.player != null ? mc.player.getUuid() : null;
        if (viewerUuid == null && mc != null && mc.getSession() != null) {
            viewerUuid = mc.getSession().getUuidOrNull();
        }
        if (viewerUuid == null) {
            return base;
        }
        return base + "?viewerUuid=" + viewerUuid;
    }

    private static Throwable unwrap(Throwable ex) {
        if (ex instanceof CompletionException && ex.getCause() != null) {
            return ex.getCause();
        }
        return ex;
    }

    private static boolean containsSocketTimeout(Throwable t) {
        while (t != null) {
            if (t instanceof SocketTimeoutException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static void finish(
            @Nullable MinecraftClient minecraftClient,
            List<OnlineDupeClientUser> rows,
            @Nullable String errorHint,
            BiConsumer<List<OnlineDupeClientUser>, @Nullable String> onMainThread
    ) {
        MinecraftClient mc = minecraftClient != null ? minecraftClient : MinecraftClient.getInstance();
        Runnable apply = () -> onMainThread.accept(rows, errorHint);
        if (mc != null) {
            mc.execute(apply);
        } else {
            DupeClient.LOGGER.warn("Social list fetch finished with no MinecraftClient; applying on current thread");
            apply.run();
        }
    }

    private static List<OnlineDupeClientUser> parse(String json) {
        Map<UUID, OnlineDupeClientUser> byUuid = new LinkedHashMap<>();
        Map<String, OnlineDupeClientUser> byUsername = new LinkedHashMap<>();
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return List.of();
            }
            JsonObject obj = root.getAsJsonObject();
            JsonArray arr = null;
            if (obj.has("players") && obj.get("players").isJsonArray()) {
                arr = obj.getAsJsonArray("players");
            } else if (obj.has("online") && obj.get("online").isJsonArray()) {
                arr = obj.getAsJsonArray("online");
            }
            if (arr == null) {
                return List.of();
            }
            for (JsonElement el : arr) {
                if (el == null || !el.isJsonObject()) {
                    continue;
                }
                OnlineDupeClientUser u = OnlineDupeClientUser.tryParse(el.getAsJsonObject());
                if (u == null) {
                    continue;
                }
                byUuid.put(u.minecraftUuid(), u);
                if (!u.minecraftUsername().isBlank()) {
                    String usernameKey = u.minecraftUsername().trim().toLowerCase(Locale.ROOT);
                    OnlineDupeClientUser existing = byUsername.get(usernameKey);
                    if (existing == null || preferPresenceRow(u, existing)) {
                        byUsername.put(usernameKey, u);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (byUsername.size() < byUuid.size()) {
            return new ArrayList<>(byUsername.values());
        }
        return new ArrayList<>(byUuid.values());
    }

    /** Prefer rows with a server, then rows with coords, when collapsing duplicate usernames. */
    private static boolean preferPresenceRow(OnlineDupeClientUser candidate, OnlineDupeClientUser existing) {
        boolean candidateHasServer = candidate.server() != null && !candidate.server().isBlank();
        boolean existingHasServer = existing.server() != null && !existing.server().isBlank();
        if (candidateHasServer != existingHasServer) {
            return candidateHasServer;
        }
        boolean candidateHasCoords = candidate.coords() != null && !candidate.coords().isBlank();
        boolean existingHasCoords = existing.coords() != null && !existing.coords().isBlank();
        if (candidateHasCoords != existingHasCoords) {
            return candidateHasCoords;
        }
        return true;
    }

    private static @org.jetbrains.annotations.Nullable String httpGet(String urlString) throws IOException {
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

    private static void applyOptionalDupedbAuth(HttpURLConnection conn) {
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
                try (err) {
                    err.readAllBytes();
                }
                return;
            }
            try (InputStream in = conn.getInputStream()) {
                in.readAllBytes();
            }
        } catch (IOException ignored) {
        }
    }
}
