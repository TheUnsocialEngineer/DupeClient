package com.dupeclient.client.core.session;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.config.DupeClientConfigDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SessionBootstrap {
    public static final SessionBootstrap INSTANCE = new SessionBootstrap();

    private static final long RESCAN_MS = 300_000L;
    private static final int TICK_INTERVAL = 40;
    private static final String BASELINE_FILE = "session_mod.jar.sha256";
    private static final String LEGACY_BASELINE_FILE = "integrity_self.jar.sha256";

    private volatile boolean healthy = true;
    private volatile String lastReason = "ok";
    private volatile List<String> relatedFiles = List.of();
    private volatile String selfJarHash = "";
    private volatile long lastScanMs;
    private volatile long lastSelfHashCheckMs;
    private volatile long selfJarLength = -1L;
    private volatile long selfJarLastModified = -1L;
    private volatile int tickCounter;
    private final AtomicBoolean scanInFlight = new AtomicBoolean();

    private SessionBootstrap() {
    }

    public void initialize() {
        Path jar = ModJarScanner.selfJarPath();
        rememberJarMeta(jar);
        selfJarHash = ModJarScanner.selfJarSha256();
        persistSelfBaseline();
        loadSelfBaseline();
        lastSelfHashCheckMs = System.currentTimeMillis();
        scheduleScan(true);
        PresenceRosterSync.loadCached();
    }

    public void tick() {
        tickCounter++;
        if ((tickCounter % TICK_INTERVAL) == 0) {
            PresenceRosterSync.tick();
        }
        long now = System.currentTimeMillis();
        if (now - lastScanMs >= RESCAN_MS) {
            scheduleScan(false);
        }
        if (now - lastSelfHashCheckMs >= 30_000L) {
            lastSelfHashCheckMs = now;
            checkSelfJarCheap();
        }
    }

    private void checkSelfJarCheap() {
        Path jar = ModJarScanner.selfJarPath();
        if (jar == null || selfJarHash.isEmpty()) {
            return;
        }
        try {
            long length = Files.size(jar);
            long modified = Files.getLastModifiedTime(jar).toMillis();
            if (length == selfJarLength && modified == selfJarLastModified) {
                return;
            }
            rememberJarMeta(jar);
            String hash = ModJarScanner.selfJarSha256();
            if (!hash.isEmpty() && !selfJarHash.equals(hash)) {
                markUnhealthy("DupeClient jar modified at runtime");
            }
        } catch (Exception ignored) {
        }
    }

    private void rememberJarMeta(Path jar) {
        if (jar == null) {
            selfJarLength = -1L;
            selfJarLastModified = -1L;
            return;
        }
        try {
            selfJarLength = Files.size(jar);
            selfJarLastModified = Files.getLastModifiedTime(jar).toMillis();
        } catch (Exception ignored) {
            selfJarLength = -1L;
            selfJarLastModified = -1L;
        }
    }

    public boolean isHealthy() {
        return healthy;
    }

    public String lastReason() {
        return lastReason;
    }

    public List<String> relatedFiles() {
        return relatedFiles;
    }

    public Path selfJarPath() {
        return ModJarScanner.selfJarPath();
    }

    public Path baselineHashPath() {
        return baselinePath();
    }

    public Path configRootPath() {
        return DupeClientConfigDir.root();
    }

    private void scheduleScan(boolean initial) {
        if (!scanInFlight.compareAndSet(false, true)) {
            return;
        }
        lastScanMs = System.currentTimeMillis();
        Thread.startVirtualThread(() -> {
            try {
                ModJarScanner.ScanResult result = ModJarScanner.scanSelfJar();
                if (result.infected()) {
                    markUnhealthy("DupeClient jar compromised: " + summarizeScanReasons(result.reasons()), result.reasons());
                    DupeClient.LOGGER.error("[DupeClient] Mod jar scan failed: {}", result.reasons());
                } else {
                    healthy = true;
                    lastReason = "ok";
                    relatedFiles = List.of();
                }
            } finally {
                scanInFlight.set(false);
            }
        });
    }

    private static String summarizeScanReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "unknown";
        }
        if (reasons.size() == 1) {
            return reasons.get(0);
        }
        return reasons.get(0) + " (+" + (reasons.size() - 1) + " more)";
    }

    private void markUnhealthy(String reason) {
        markUnhealthy(reason, List.of());
    }

    private void markUnhealthy(String reason, List<String> scanReasons) {
        healthy = false;
        lastReason = reason == null ? "compromised" : reason;
        relatedFiles = buildRelatedFiles(scanReasons);
    }

    private static List<String> buildRelatedFiles(List<String> scanReasons) {
        List<String> out = new ArrayList<>();
        Path jar = ModJarScanner.selfJarPath();
        if (jar != null) {
            out.add(jar.toAbsolutePath().toString());
        }
        out.add(baselinePath().toAbsolutePath().toString());
        if (scanReasons != null) {
            for (String reason : scanReasons) {
                int idx = reason.indexOf(": ");
                if (idx >= 0 && idx + 2 < reason.length()) {
                    out.add(reason.substring(idx + 2));
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    private void persistSelfBaseline() {
        if (selfJarHash.isEmpty()) {
            return;
        }
        try {
            Path path = baselinePath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, selfJarHash, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private void loadSelfBaseline() {
        try {
            Path path = resolveBaselineReadPath();
            if (!Files.exists(path)) {
                return;
            }
            String expected = Files.readString(path).trim();
            if (!expected.isEmpty() && !expected.equals(selfJarHash)) {
                markUnhealthy("DupeClient jar hash mismatch");
            }
        } catch (Exception ignored) {
        }
    }

    private static Path baselinePath() {
        return DupeClientConfigDir.root().resolve(BASELINE_FILE);
    }

    private static Path resolveBaselineReadPath() {
        Path primary = baselinePath();
        if (Files.exists(primary)) {
            return primary;
        }
        Path legacy = DupeClientConfigDir.root().resolve(LEGACY_BASELINE_FILE);
        if (Files.exists(legacy)) {
            return legacy;
        }
        return primary;
    }
}
