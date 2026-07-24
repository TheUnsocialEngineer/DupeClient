package com.dupeclient.client.module.cape;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.dupedb.DupedbManager;
import com.dupeclient.client.module.social.DupeClientSocialFriendsManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

/**
 * <p>Feather/Lunar-style cape visibility without the logical Minecraft server running this mod: each client
 * calls your HTTPS presence API so everyone can resolve “who runs DupeClient” by UUID.</p>
 *
 * <p><b>Config</b>: {@code config/dupeclient/presence.json} — {@link DupeClientPresenceSettings#apiBase} (no trailing
 * slash), reloaded on every play session join.</p>
 */
public final class DupeClientCapePresence {

    private static final long HEARTBEAT_INTERVAL_MS = 45_000L;
    /** Healthy cadence (~3s). Failures back off much further. */
    private static final int QUERY_EVERY_TICKS = 60;
    private static final int MAX_UUIDS_PER_QUERY = 48;
    private static final long FAILURE_BASE_MS = 5_000L;
    private static final long FAILURE_MAX_MS = 120_000L;
    private static final int TIMEOUT_HEALTHY_MS = 8_000;
    private static final int TIMEOUT_DEGRADED_MS = 3_000;

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DupeClient-CapePresence");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private static final AtomicInteger generation = new AtomicInteger();
    private static final AtomicBoolean heartbeatInFlight = new AtomicBoolean();
    private static final AtomicBoolean queryInFlight = new AtomicBoolean();

    private static int tickCounter;
    private static long lastHeartbeatMs;
    private static volatile UUID lastPresenceUuid;
    private static volatile String lastPresenceUsername;
    private static volatile String lastSentServerHint;
    private static boolean warnedUnsupported;
    private static volatile boolean queryImmediatelyAfterJoin;
    private static volatile boolean warnedPresenceFailure;
    private static volatile int consecutiveFailures;
    private static volatile long nextQueryAllowedMs;
    private static volatile long nextHeartbeatAllowedMs;
    private static volatile int httpTimeoutMs = TIMEOUT_HEALTHY_MS;

    private DupeClientCapePresence() {
    }

    /**
     * Call from the same play-join hook as DupeDB so each session gets a fresh generation and an immediate heartbeat.
     */
    public static void onPlaySessionJoined() {
        DupeClientPresenceConfigManager.reload();
        generation.incrementAndGet();
        lastHeartbeatMs = 0;
        lastSentServerHint = null;
        warnedUnsupported = false;
        // Keep failure backoff across reconnects if the API was recently unreachable.
        queryImmediatelyAfterJoin = consecutiveFailures == 0;
        tickCounter = QUERY_EVERY_TICKS;
        Minecraft client = Minecraft.getInstance();
        UUID uuid = resolvePresenceUuid(client);
        if (uuid != null) {
            lastPresenceUuid = uuid;
            DupeClientCapeTracker.markDupeClient(uuid);
        }
        lastPresenceUsername = resolvePresenceUsername(client);
    }

    public static void onDisconnected() {
        generation.incrementAndGet();
        UUID uuid = lastPresenceUuid;
        String username = lastPresenceUsername;
        if (uuid == null) {
            Minecraft client = Minecraft.getInstance();
            uuid = resolvePresenceUuid(client);
            username = resolvePresenceUsername(client);
        }
        if (uuid != null) {
            scheduleLogout(uuid, username);
        }
        lastPresenceUuid = null;
        lastPresenceUsername = null;
        lastSentServerHint = null;
        DupeClientCapeTracker.clear();
        DupeClientCapeApplicator.clearCache();
    }

    /** Resolved {@code apiBase} (for social list HTTP, etc.). */
    public static String resolvedPresenceApiBase() {
        return presenceBase();
    }

    public static void tick(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        if (!Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().enabled)) {
            return;
        }
        UUID self = resolvePresenceUuid(client);
        if (self == null) {
            return;
        }
        lastPresenceUuid = self;
        lastPresenceUsername = resolvePresenceUsername(client);
        String serverHint = currentServerHint(client);
        if (!Objects.equals(serverHint, lastSentServerHint)) {
            lastSentServerHint = serverHint;
            lastHeartbeatMs = 0;
        }
        long now = System.currentTimeMillis();
        if (now >= nextHeartbeatAllowedMs && now - lastHeartbeatMs >= HEARTBEAT_INTERVAL_MS) {
            lastHeartbeatMs = now;
            scheduleHeartbeat(self, lastPresenceUsername, serverHint);
        }
        tickCounter++;
        boolean due = queryImmediatelyAfterJoin || tickCounter >= QUERY_EVERY_TICKS;
        if (!due || now < nextQueryAllowedMs) {
            return;
        }
        queryImmediatelyAfterJoin = false;
        tickCounter = 0;
        Set<UUID> targets = collectQueryTargets(client);
        if (!targets.isEmpty()) {
            scheduleQuery(new ArrayList<>(targets), client.player.getUUID());
        }
    }

    private static Set<UUID> collectQueryTargets(Minecraft client) {
        Set<UUID> out = new LinkedHashSet<>();
        if (client.level != null) {
            for (Player p : client.level.players()) {
                if (p != null && p.getUUID() != null) {
                    out.add(p.getUUID());
                    if (out.size() >= MAX_UUIDS_PER_QUERY) {
                        return out;
                    }
                }
            }
        }
        if (client.getConnection() != null) {
            for (PlayerInfo e : client.getConnection().getOnlinePlayers()) {
                if (e.getProfile() != null && e.getProfile().id() != null) {
                    out.add(e.getProfile().id());
                    if (out.size() >= MAX_UUIDS_PER_QUERY) {
                        return out;
                    }
                }
            }
        }
        return out;
    }

    private static void scheduleHeartbeat(UUID self, String minecraftUsername, String serverHint) {
        if (self == null || !heartbeatInFlight.compareAndSet(false, true)) {
            return;
        }
        int gen = generation.get();
        final String capturedServerHint = serverHint;
        EXEC.execute(() -> {
            try {
                if (generation.get() != gen) {
                    return;
                }
                DupeClientPresenceSettings cfg = DupeClientPresenceConfigManager.get();
                if (!Boolean.TRUE.equals(cfg.enabled)) {
                    return;
                }
                if (!Boolean.TRUE.equals(cfg.broadcastPresence)) {
                    return;
                }
                JsonObject body = new JsonObject();
                body.addProperty("minecraftUuid", self.toString());
                if (minecraftUsername != null && !minecraftUsername.isBlank()) {
                    String n = minecraftUsername.trim();
                    if (n.length() > 32) {
                        n = n.substring(0, 32);
                    }
                    body.addProperty("minecraftUsername", n);
                }
                body.addProperty("client", DupeClient.MOD_ID);
                body.addProperty("build", DupeClient.BUILD_TAG);
                String aud = cfg.presenceListAudience;
                if ("friends_only".equalsIgnoreCase(aud)) {
                    body.addProperty("listAudience", "friends_only");
                    JsonArray friends = new JsonArray();
                    for (UUID friendId : DupeClientSocialFriendsManager.friendUuidSet()) {
                        friends.add(friendId.toString());
                    }
                    body.add("friendUuids", friends);
                } else {
                    body.addProperty("listAudience", "public");
                }
                if (Boolean.TRUE.equals(cfg.shareCurrentServer)) {
                    String hint = capturedServerHint;
                    if (hint == null) {
                        Minecraft mc = Minecraft.getInstance();
                        hint = currentServerHint(mc);
                    }
                    if (hint != null && !hint.isBlank()) {
                        if (hint.length() > 256) {
                            hint = hint.substring(0, 256);
                        }
                        body.addProperty("server", hint);
                    }
                }
                if (Boolean.TRUE.equals(cfg.shareCurrentCoords)) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && mc.player != null) {
                        String coords = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
                        body.addProperty("coords", coords);
                    }
                }
                if (generation.get() != gen) {
                    return;
                }
                int code = postJson(heartbeatUrl(), body.toString());
                if (code == 404 || code == 405) {
                    noteUnsupportedOnce();
                    noteFailure("heartbeat HTTP " + code);
                } else if (code < 200 || code >= 300) {
                    noteFailure("heartbeat HTTP " + code);
                } else {
                    noteSuccess();
                }
            } catch (Exception e) {
                noteFailure("heartbeat: " + e.getClass().getSimpleName());
            } finally {
                heartbeatInFlight.set(false);
            }
        });
    }

    private static void scheduleLogout(UUID self, String minecraftUsername) {
        if (self == null) {
            return;
        }
        int gen = generation.get();
        EXEC.execute(() -> {
            try {
                if (generation.get() != gen) {
                    return;
                }
                DupeClientPresenceSettings cfg = DupeClientPresenceConfigManager.get();
                if (!Boolean.TRUE.equals(cfg.enabled)) {
                    return;
                }
                JsonObject body = new JsonObject();
                body.addProperty("minecraftUuid", self.toString());
                if (minecraftUsername != null && !minecraftUsername.isBlank()) {
                    String n = minecraftUsername.trim();
                    if (n.length() > 32) {
                        n = n.substring(0, 32);
                    }
                    body.addProperty("minecraftUsername", n);
                }
                postJson(logoutUrl(), body.toString());
            } catch (Exception ignored) {
            }
        });
    }

    private static void scheduleQuery(List<UUID> uuids, UUID localUuid) {
        if (uuids.isEmpty() || !queryInFlight.compareAndSet(false, true)) {
            return;
        }
        int gen = generation.get();
        EXEC.execute(() -> {
            try {
                if (!Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().enabled)) {
                    return;
                }
                JsonObject body = new JsonObject();
                JsonArray arr = new JsonArray();
                for (UUID u : uuids) {
                    arr.add(u.toString());
                }
                body.add("minecraftUuids", arr);
                String response = postJsonReturningBody(queryUrl(), body.toString());
                if (response == null) {
                    noteFailure("query returned no body (check TLS and URL " + queryUrl() + ")");
                    return;
                }
                if (generation.get() != gen) {
                    return;
                }
                List<UUID> online = parseOnlineUuids(response);
                noteSuccess();
                Minecraft client = Minecraft.getInstance();
                if (client == null) {
                    return;
                }
                client.execute(() -> {
                    if (generation.get() != gen) {
                        return;
                    }
                    DupeClientCapeTracker.replaceOnline(online, localUuid);
                });
            } catch (Exception e) {
                noteFailure("query: " + e.getClass().getSimpleName());
            } finally {
                queryInFlight.set(false);
            }
        });
    }

    private static void noteSuccess() {
        consecutiveFailures = 0;
        nextQueryAllowedMs = 0L;
        nextHeartbeatAllowedMs = 0L;
        httpTimeoutMs = TIMEOUT_HEALTHY_MS;
        warnedPresenceFailure = false;
    }

    private static void noteFailure(String detail) {
        int failures = Math.min(16, consecutiveFailures + 1);
        consecutiveFailures = failures;
        long backoff = Math.min(FAILURE_MAX_MS, FAILURE_BASE_MS << Math.min(failures - 1, 4));
        long now = System.currentTimeMillis();
        nextQueryAllowedMs = now + backoff;
        nextHeartbeatAllowedMs = now + backoff;
        httpTimeoutMs = TIMEOUT_DEGRADED_MS;
        notePresenceFailureOnce(detail + "; backing off " + (backoff / 1000L) + "s");
    }

    private static void notePresenceFailureOnce(String detail) {
        if (warnedPresenceFailure) {
            return;
        }
        warnedPresenceFailure = true;
        DupeClient.LOGGER.warn(
                "DupeClient cape presence failed ({}). Cross-player capes need {} reachable with valid HTTPS. See config/dupeclient/presence.json",
                detail,
                presenceBase());
    }

    private static List<UUID> parseOnlineUuids(String json) {
        List<UUID> list = new ArrayList<>();
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return list;
            }
            JsonObject obj = root.getAsJsonObject();
            JsonArray arr = null;
            if (obj.has("online") && obj.get("online").isJsonArray()) {
                arr = obj.getAsJsonArray("online");
            } else if (obj.has("minecraftUuids") && obj.get("minecraftUuids").isJsonArray()) {
                arr = obj.getAsJsonArray("minecraftUuids");
            } else if (obj.has("uuids") && obj.get("uuids").isJsonArray()) {
                arr = obj.getAsJsonArray("uuids");
            }
            if (arr == null) {
                return list;
            }
            for (JsonElement el : arr) {
                if (el != null && el.isJsonPrimitive()) {
                    try {
                        list.add(UUID.fromString(el.getAsString()));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private static void noteUnsupportedOnce() {
        if (warnedUnsupported) {
            return;
        }
        warnedUnsupported = true;
        DupeClient.LOGGER.debug(
                "DupeClient cape presence API not available (404). Expected POST {} and POST {} (set apiBase in config/dupeclient/presence.json)",
                heartbeatUrl(),
                queryUrl());
    }

    private static String heartbeatUrl() {
        return presenceBase() + "/heartbeat";
    }

    private static String logoutUrl() {
        return presenceBase() + "/logout";
    }

    private static String queryUrl() {
        return presenceBase() + "/query";
    }

    private static @Nullable UUID resolvePresenceUuid(Minecraft mc) {
        if (mc == null) {
            return null;
        }
        if (mc.player != null) {
            return mc.player.getUUID();
        }
        if (mc.getUser() != null) {
            return mc.getUser().getProfileId();
        }
        return null;
    }

    private static String resolvePresenceUsername(Minecraft mc) {
        if (mc == null) {
            return "";
        }
        if (mc.player != null) {
            return mc.player.getName().getString();
        }
        if (mc.getUser() != null && mc.getUser().getName() != null) {
            return mc.getUser().getName();
        }
        return "";
    }

    private static String presenceBase() {
        DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
        String base = s.apiBase;
        if (base == null || base.isBlank()) {
            return DupeClientPresenceSettings.DEFAULT_API_BASE;
        }
        return base;
    }

    private static String currentServerHint(Minecraft mc) {
        if (mc == null) {
            return null;
        }
        if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null) {
            String a = mc.getCurrentServer().ip.trim();
            if (!a.isEmpty()) {
                return a;
            }
        }
        if (mc.level != null && mc.player != null) {
            return "Singleplayer";
        }
        return null;
    }

    private static int currentTimeoutMs() {
        return Math.max(TIMEOUT_DEGRADED_MS, httpTimeoutMs);
    }

    private static int postJson(String urlString, String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        int timeout = currentTimeoutMs();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        applyOptionalDupedbAuth(conn);
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }
        int code = conn.getResponseCode();
        drainResponseBodyQuietly(conn);
        conn.disconnect();
        return code;
    }

    /**
     * @return response body on 2xx, or null otherwise
     */
    private static String postJsonReturningBody(String urlString, String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        int timeout = currentTimeoutMs();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        applyOptionalDupedbAuth(conn);
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
        }
        int code = conn.getResponseCode();
        if (code == 404 || code == 405) {
            noteUnsupportedOnce();
            drainResponseBodyQuietly(conn);
            conn.disconnect();
            return null;
        }
        if (code < 200 || code >= 300) {
            drainResponseBodyQuietly(conn);
            conn.disconnect();
            return null;
        }
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * HttpURLConnection keeps the connection unusable for the next request unless the response body is consumed
     * (including 204 heartbeats with an empty body stream).
     */
    private static void drainResponseBodyQuietly(HttpURLConnection conn) {
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

    private static void applyOptionalDupedbAuth(HttpURLConnection conn) {
        String token = DupedbManager.INSTANCE.getOAuthTokenOrEmpty();
        if (!token.isBlank()) {
            conn.setRequestProperty("X-App-Token", token);
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
    }
}
