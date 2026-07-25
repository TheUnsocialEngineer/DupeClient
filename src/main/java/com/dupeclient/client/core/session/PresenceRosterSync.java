package com.dupeclient.client.core.session;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PresenceRosterSync {
    private static final long REFRESH_MS = 120_000L;
    private static final long MAX_CLOCK_SKEW_MS = 60_000L;
    private static final long FAILURE_BASE_RETRY_MS = 30_000L;
    private static final long FAILURE_MAX_RETRY_MS = 5 * 60_000L;
    private static final long FAILURE_LOG_COOLDOWN_MS = 60_000L;
    private static final String CACHE_FILE = "presence_roster_cache.json";
    private static final String LEGACY_CACHE_FILE = "presence_staff_cache.json";

    private static volatile long lastFetchAttemptMs;
    private static volatile long lastValidIssuedAt;
    private static volatile long lastValidTtlMs;
    private static volatile String lastNonce = "";
    private static volatile String lastSignature = "";
    private static volatile Set<UUID> verifiedStaff = Set.of();
    private static volatile boolean lastSignatureValid;
    private static volatile boolean lastFetchOk;
    private static volatile boolean stickyStaffLock;
    private static volatile String lastError = "";
    private static volatile UUID boundViewerUuid;
    private static final AtomicBoolean fetchInFlight = new AtomicBoolean();
    private static volatile int consecutiveFailures;
    private static volatile long nextRetryAfterFailureMs;
    private static volatile long lastFailureLogMs;
    private static volatile boolean tampered;
    private static volatile boolean sessionNetworkVerified;

    private PresenceRosterSync() {
    }

    public static void initialize() {
        reset();
    }

    public static void reset() {
        lastFetchAttemptMs = 0L;
        lastValidIssuedAt = 0L;
        lastValidTtlMs = 0L;
        lastNonce = "";
        lastSignature = "";
        verifiedStaff = Set.of();
        lastSignatureValid = false;
        lastFetchOk = false;
        stickyStaffLock = false;
        lastError = "";
        boundViewerUuid = null;
        tampered = false;
        consecutiveFailures = 0;
        nextRetryAfterFailureMs = 0L;
        lastFailureLogMs = 0L;
        sessionNetworkVerified = false;
    }

    public static boolean sessionRosterVerified() {
        refreshSessionBinding();
        return sessionNetworkVerified;
    }

    public static boolean isRosterPending() {
        return !sessionRosterVerified() && !tampered;
    }

    public static void tick() {
        refreshSessionBinding();
        long now = System.currentTimeMillis();
        if (!sessionNetworkVerified) {
            if (now >= nextRetryAfterFailureMs && !fetchInFlight.get()) {
                scheduleFetch();
            }
            return;
        }
        if (now < nextRetryAfterFailureMs) {
            return;
        }
        if (now - lastFetchAttemptMs < REFRESH_MS && isCacheFresh(now)) {
            return;
        }
        scheduleFetch();
    }

    public static boolean viewerRestricted() {
        refreshSessionBinding();
        if (tampered) {
            return true;
        }
        return stickyStaffLock;
    }

    public static boolean isResponseTampered() {
        return tampered;
    }

    public static Set<UUID> verifiedStaffUuids() {
        if (!lastSignatureValid || !isCacheFresh(System.currentTimeMillis())) {
            return Collections.emptySet();
        }
        return verifiedStaff;
    }

    public static String statusLine() {
        if (tampered) {
            return "roster tampered — access restricted";
        }
        if (!sessionNetworkVerified) {
            if (fetchInFlight.get()) {
                return "verifying roster…";
            }
            if (!lastError.isEmpty()) {
                return "roster pending — " + lastError;
            }
            return "roster pending — waiting for presence API";
        }
        if (!lastFetchOk) {
            return lastError.isEmpty() ? "roster unavailable" : lastError;
        }
        if (!lastSignatureValid) {
            return "roster signature invalid";
        }
        if (stickyStaffLock) {
            return "staff account — exploit/social locked";
        }
        return "roster verified";
    }

    private static boolean isCacheFresh(long now) {
        if (!lastSignatureValid || lastValidIssuedAt <= 0L) {
            return false;
        }
        long expires = lastValidIssuedAt + Math.max(30_000L, lastValidTtlMs);
        return now <= expires + MAX_CLOCK_SKEW_MS;
    }

    private static void refreshSessionBinding() {
        UUID viewer = currentViewerUuid();
        if (viewer == null) {
            return;
        }
        if (boundViewerUuid == null) {
            boundViewerUuid = viewer;
            sessionNetworkVerified = false;
            scheduleFetch();
            return;
        }
        if (!viewer.equals(boundViewerUuid)) {
            boundViewerUuid = viewer;
            stickyStaffLock = false;
            lastSignatureValid = false;
            sessionNetworkVerified = false;
            tampered = false;
            consecutiveFailures = 0;
            nextRetryAfterFailureMs = 0L;
            scheduleFetch();
        }
    }

    private static void scheduleFetch() {
        if (!fetchInFlight.compareAndSet(false, true)) {
            return;
        }
        lastFetchAttemptMs = System.currentTimeMillis();
        Thread.startVirtualThread(() -> {
            try {
                fetchBlocking();
            } finally {
                fetchInFlight.set(false);
            }
        });
    }

    private static void fetchBlocking() {
        UUID viewer = currentViewerUuid();
        if (viewer == null) {
            lastError = "no session uuid";
            lastFetchOk = false;
            nextRetryAfterFailureMs = System.currentTimeMillis() + 3_000L;
            return;
        }
        try {
            String base = PresenceApiPaths.staffBaseUrl();
            String url = base + "?viewerUuid=" + viewer;
            String json = httpGet(url);
            if (json == null) {
                lastFetchOk = false;
                lastError = "staff http error";
                onFetchFailure(new IOException("staff http error"));
                return;
            }
            applyResponse(json, viewer);
            persistCache();
        } catch (Exception ex) {
            lastFetchOk = false;
            lastError = ex.getClass().getSimpleName();
            onFetchFailure(ex);
        }
    }

    private static void onFetchFailure(Exception ex) {
        consecutiveFailures = Math.min(8, consecutiveFailures + 1);
        long baseRetry = sessionNetworkVerified ? FAILURE_BASE_RETRY_MS : 5_000L;
        long backoff = Math.min(FAILURE_MAX_RETRY_MS, baseRetry * (1L << (consecutiveFailures - 1)));
        nextRetryAfterFailureMs = System.currentTimeMillis() + backoff;
        long now = System.currentTimeMillis();
        if (now - lastFailureLogMs >= FAILURE_LOG_COOLDOWN_MS) {
            lastFailureLogMs = now;
            DupeClient.LOGGER.warn("Presence roster fetch failed ({}). Retrying in {}s.", ex.getClass().getSimpleName(), backoff / 1000L);
        }
    }

    private static void applyResponse(String json, UUID viewer) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        long issuedAt = root.has("issuedAt") ? root.get("issuedAt").getAsLong() : 0L;
        long ttlMs = root.has("ttlMs") ? root.get("ttlMs").getAsLong() : 0L;
        String nonce = root.has("nonce") ? root.get("nonce").getAsString() : "";
        String signature = root.has("signature") ? root.get("signature").getAsString() : "";

        List<String> staffIds = parseStaffIds(root);
        String canonical = PayloadCanonicalizer.staffPayload(issuedAt, ttlMs, nonce, staffIds);
        byte[] hmacKey = SessionSecrets.presenceStaffHmacKey();
        boolean sigOk = PayloadHmac.verifySha256Hmac(canonical, signature, hmacKey);
        long now = System.currentTimeMillis();
        boolean timeOk = issuedAt > 0L && Math.abs(now - issuedAt) <= MAX_CLOCK_SKEW_MS + Math.max(30_000L, ttlMs);

        lastSignatureValid = sigOk && timeOk;
        tampered = false;
        lastFetchOk = true;
        consecutiveFailures = 0;
        nextRetryAfterFailureMs = 0L;
        lastError = lastSignatureValid
                ? ""
                : (sigOk ? "roster expired" : "roster signature invalid");
        if (!lastSignatureValid && !sigOk) {
            if (hmacKey == null || hmacKey.length == 0) {
                DupeClient.LOGGER.error("Presence roster verification failed: staff HMAC key is not configured");
            } else {
                DupeClient.LOGGER.warn("Presence roster signature invalid (viewer={})", viewer);
            }
        } else if (lastSignatureValid) {
            DupeClient.LOGGER.info("Presence roster verified (viewer={}, staff={})", viewer, staffIds.size());
        }
        lastValidIssuedAt = issuedAt;
        lastValidTtlMs = ttlMs;
        lastNonce = nonce;
        lastSignature = signature == null ? "" : signature.trim();

        Set<UUID> parsed = parseStaffUuids(staffIds);
        verifiedStaff = Set.copyOf(parsed);
        applyStickyLock(viewer, parsed.contains(viewer), lastSignatureValid);
        if (lastSignatureValid) {
            sessionNetworkVerified = true;
        }
    }

    private static void applyStickyLock(UUID viewer, boolean viewerIsStaff, boolean signatureValid) {
        if (!signatureValid || viewer == null) {
            return;
        }
        if (viewerIsStaff) {
            stickyStaffLock = true;
        } else {
            stickyStaffLock = false;
        }
    }

    private static List<String> parseStaffIds(JsonObject root) {
        Set<String> staffIds = new LinkedHashSet<>();
        if (root.has("staff") && root.get("staff").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("staff")) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject row = el.getAsJsonObject();
                if (!row.has("minecraftUuid")) {
                    continue;
                }
                String raw = row.get("minecraftUuid").getAsString();
                if (raw != null && !raw.isBlank()) {
                    staffIds.add(raw.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return new ArrayList<>(staffIds);
    }

    private static Set<UUID> parseStaffUuids(List<String> staffIds) {
        Set<UUID> parsed = new LinkedHashSet<>();
        for (String id : staffIds) {
            try {
                parsed.add(UUID.fromString(id));
            } catch (Exception ignored) {
            }
        }
        return parsed;
    }

    private static UUID currentViewerUuid() {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null) {
            return null;
        }
        if (mc.player != null) {
            return mc.player.getUuid();
        }
        if (mc.getSession() != null) {
            return mc.getSession().getUuidOrNull();
        }
        return null;
    }

    private static String httpGet(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(12_000);
        conn.setReadTimeout(12_000);
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            drain(conn);
            conn.disconnect();
            return null;
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

    private static void persistCache() {
        try {
            Path path = cachePath();
            Files.createDirectories(path.getParent());
            JsonObject cache = new JsonObject();
            cache.addProperty("issuedAt", lastValidIssuedAt);
            cache.addProperty("ttlMs", lastValidTtlMs);
            cache.addProperty("nonce", lastNonce);
            cache.addProperty("signature", lastSignature);
            cache.addProperty("signatureValid", lastSignatureValid);
            cache.addProperty("stickyStaffLock", stickyStaffLock);
            if (boundViewerUuid != null) {
                cache.addProperty("boundViewerUuid", boundViewerUuid.toString());
            }
            JsonArray staff = new JsonArray();
            for (UUID id : verifiedStaff) {
                staff.add(id.toString());
            }
            cache.add("staff", staff);
            Files.writeString(path, cache.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static void loadCached() {
        try {
            Path path = resolveCacheReadPath();
            if (!Files.exists(path)) {
                return;
            }
            JsonObject cache = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            lastValidIssuedAt = cache.has("issuedAt") ? cache.get("issuedAt").getAsLong() : 0L;
            lastValidTtlMs = cache.has("ttlMs") ? cache.get("ttlMs").getAsLong() : 0L;
            lastNonce = cache.has("nonce") ? cache.get("nonce").getAsString() : "";
            lastSignature = cache.has("signature") ? cache.get("signature").getAsString() : "";

            List<String> staffIds = new ArrayList<>();
            if (cache.has("staff") && cache.get("staff").isJsonArray()) {
                for (JsonElement el : cache.getAsJsonArray("staff")) {
                    staffIds.add(el.getAsString().trim().toLowerCase(Locale.ROOT));
                }
            }

            if (lastSignature.isBlank()) {
                lastSignatureValid = false;
                stickyStaffLock = false;
                verifiedStaff = Set.of();
                lastFetchOk = false;
                return;
            }

            String canonical = PayloadCanonicalizer.staffPayload(lastValidIssuedAt, lastValidTtlMs, lastNonce, staffIds);
            lastSignatureValid = PayloadHmac.verifySha256Hmac(canonical, lastSignature, SessionSecrets.presenceStaffHmacKey());
            long now = System.currentTimeMillis();
            boolean timeOk = lastValidIssuedAt > 0L
                    && Math.abs(now - lastValidIssuedAt) <= MAX_CLOCK_SKEW_MS + Math.max(30_000L, lastValidTtlMs);
            lastSignatureValid = lastSignatureValid && timeOk;

            if (!lastSignatureValid) {
                tampered = cache.has("signature");
                stickyStaffLock = tampered;
                verifiedStaff = Set.of();
                lastFetchOk = false;
                return;
            }

            tampered = false;
            verifiedStaff = Set.copyOf(parseStaffUuids(staffIds));
            UUID viewer = currentViewerUuid();
            if (cache.has("boundViewerUuid")) {
                try {
                    boundViewerUuid = UUID.fromString(cache.get("boundViewerUuid").getAsString());
                } catch (Exception ignored) {
                    boundViewerUuid = viewer;
                }
            } else {
                boundViewerUuid = viewer;
            }

            boolean viewerStaff = viewer != null && verifiedStaff.contains(viewer);
            if (viewer != null && boundViewerUuid != null && !viewer.equals(boundViewerUuid)) {
                stickyStaffLock = false;
                sessionNetworkVerified = false;
            } else {
                applyStickyLock(viewer, viewerStaff, true);
                if (isCacheFresh(now)) {
                    sessionNetworkVerified = true;
                    lastFetchOk = true;
                    lastError = "";
                }
            }
            if (!sessionNetworkVerified) {
                lastFetchOk = isCacheFresh(now);
            }
        } catch (Exception ignored) {
            tampered = true;
            stickyStaffLock = true;
            sessionNetworkVerified = false;
        }
    }

    private static Path cachePath() {
        return DupeClientConfigDir.root().resolve(CACHE_FILE);
    }

    private static Path resolveCacheReadPath() {
        Path primary = cachePath();
        if (Files.exists(primary)) {
            return primary;
        }
        Path legacy = DupeClientConfigDir.root().resolve(LEGACY_CACHE_FILE);
        if (Files.exists(legacy)) {
            return legacy;
        }
        return primary;
    }
}
