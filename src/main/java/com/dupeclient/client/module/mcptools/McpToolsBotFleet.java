package com.dupeclient.client.module.mcptools;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class McpToolsBotFleet {
    public static final int MAX_BOTS = 50;
    private static final long JOIN_PHASE_TIMEOUT_MS = 120_000L;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String USERNAME_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    /** Only one bot handshake at a time — prevents IP connection throttling. */
    private static final Semaphore CONNECTION_SLOT = new Semaphore(1);

    private final CopyOnWriteArrayList<McpToolsBotHandle> bots = new CopyOnWriteArrayList<>();
    private final AtomicBoolean throttleBackoffPending = new AtomicBoolean(false);
    private volatile String focusedBotId;

    public List<McpToolsBotHandle> bots() {
        return List.copyOf(bots);
    }

    public int activeCount() {
        int n = 0;
        for (McpToolsBotHandle bot : bots) {
            if (bot.isActive()) {
                n++;
            }
        }
        return n;
    }

    public int connectingCount() {
        int n = 0;
        for (McpToolsBotHandle bot : bots) {
            if (bot.state == McpToolsBotHandle.State.CONNECTING && bot.isActive()) {
                n++;
            }
        }
        return n;
    }

    public boolean hasAnyActive() {
        return activeCount() > 0;
    }

    public boolean consumeThrottleBackoff() {
        return throttleBackoffPending.compareAndSet(true, false);
    }

    public void cancelPendingRetries() {
        for (McpToolsBotHandle bot : bots) {
            bot.joinRetryCancelled = true;
        }
    }

    public McpToolsBotHandle find(String id) {
        if (id == null) {
            return null;
        }
        for (McpToolsBotHandle bot : bots) {
            if (bot.id.equals(id)) {
                return bot;
            }
        }
        return null;
    }

    public McpToolsBotHandle addBot(
            McpToolsSettings settings,
            String preferredUsername,
            boolean randomUsername,
            Consumer<String> logLine,
            Runnable onFleetChanged) {
        pruneStopped();
        if (liveCount() >= MAX_BOTS) {
            logLine.accept("Bot limit reached (" + MAX_BOTS + ").");
            return null;
        }
        String username = randomUsername ? randomUsername() : uniqueUsername(preferredUsername);
        McpToolsBotHandle handle = new McpToolsBotHandle(username);
        handle.auth().configureForUsername(username);
        bots.add(handle);
        onFleetChanged.run();
        startSession(handle, settings, logLine, onFleetChanged);
        return handle;
    }

    private void startSession(
            McpToolsBotHandle handle,
            McpToolsSettings settings,
            Consumer<String> logLine,
            Runnable onFleetChanged) {
        handle.resetJoinPhase();
        handle.state = McpToolsBotHandle.State.CONNECTING;
        Thread.startVirtualThread(() -> {
            try {
                if (!CONNECTION_SLOT.tryAcquire(90, TimeUnit.SECONDS)) {
                    logLine.accept("[" + handle.username + "] Join slot timeout — server busy.");
                    handle.state = McpToolsBotHandle.State.STOPPED;
                    handle.completeJoinPhase();
                    bots.remove(handle);
                    onFleetChanged.run();
                    return;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                handle.state = McpToolsBotHandle.State.STOPPED;
                handle.completeJoinPhase();
                onFleetChanged.run();
                return;
            }
            if (handle.joinRetryCancelled) {
                releaseConnectionSlot(handle);
                handle.completeJoinPhase();
                return;
            }
            McpToolsServerAddress target = McpToolsServerAddress.resolve(settings.lastHost, settings.lastPort);
            handle.session().start(
                    target.host(),
                    target.port(),
                    handle.username,
                    McpToolsMcVersion.resolveForSettings(settings),
                    settings.botLoginTimeoutSec,
                    line -> onBotSessionLine(handle, settings, line, logLine, onFleetChanged),
                    () -> onBotSessionExit(handle, logLine, onFleetChanged));
        });
    }

    private static void releaseConnectionSlot(McpToolsBotHandle handle) {
        if (handle != null && handle.releaseConnectionSlotOnce()) {
            CONNECTION_SLOT.release();
        }
    }

    private void onBotSessionExit(
            McpToolsBotHandle handle,
            Consumer<String> logLine,
            Runnable onFleetChanged) {
        handle.state = McpToolsBotHandle.State.STOPPED;
        handle.completeJoinPhase();
        releaseConnectionSlot(handle);
        logLine.accept("[" + handle.username + "] disconnected.");
        onFleetChanged.run();
    }

    private void maybeScheduleThrottleRetry(
            McpToolsBotHandle handle,
            McpToolsSettings settings,
            String kickMessage,
            Consumer<String> logLine,
            Runnable onFleetChanged) {
        if (handle.joinRetryCancelled || handle.throttleRetryUsed || !looksLikeConnectionThrottle(kickMessage)) {
            return;
        }
        handle.throttleRetryUsed = true;
        throttleBackoffPending.set(true);
        int delaySec = Math.max(2, Math.min(60, settings.botJoinDelaySec * 2));
        logLine.accept("[" + handle.username + "] Throttled — retrying in " + delaySec + "s…");
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(delaySec * 1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            if (handle.joinRetryCancelled || handle.session().isActive()) {
                return;
            }
            handle.auth().reset();
            handle.throttleRetryUsed = false;
            startSession(handle, settings, logLine, onFleetChanged);
        });
    }

    private static boolean looksLikeConnectionThrottle(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("connection throttled")
                || lower.contains("connect throttled")
                || lower.contains("login timeout")
                || lower.contains("too many connections")
                || lower.contains("too fast")
                || (lower.contains("throttl") && lower.contains("connection"));
    }

    /** Drop finished bots only when over roster cap so failed joins stay visible. */
    public void pruneStopped() {
        if (bots.size() <= MAX_BOTS) {
            return;
        }
        for (McpToolsBotHandle bot : List.copyOf(bots)) {
            if (bots.size() <= MAX_BOTS) {
                break;
            }
            if (bot.state == McpToolsBotHandle.State.STOPPED && !bot.isActive()) {
                bots.remove(bot);
            }
        }
    }

    public int liveCount() {
        pruneStopped();
        return bots.size();
    }

    public boolean awaitJoinPhase(McpToolsBotHandle handle) {
        if (handle == null) {
            return false;
        }
        return handle.awaitJoinPhase(JOIN_PHASE_TIMEOUT_MS);
    }

    private void onBotSessionLine(
            McpToolsBotHandle handle,
            McpToolsSettings settings,
            String line,
            Consumer<String> logLine,
            Runnable onFleetChanged) {
        if (line == null || line.isBlank()) {
            return;
        }
        String username = handle.username;
        handle.auth().onChatLine(
                line,
                handle.session(),
                username,
                msg -> logLine.accept("[" + username + "] " + msg));
        if (McpToolsBotLogFilter.isNoise(line)) {
            return;
        }
        if (line.toLowerCase(Locale.ROOT).contains("bot has connected")) {
            handle.state = McpToolsBotHandle.State.CONNECTED;
            handle.completeJoinPhase();
            releaseConnectionSlot(handle);
        }
        if (line.toLowerCase(Locale.ROOT).contains("spawned in world")) {
            handle.auth().onSpawnedInWorld(handle.session(), handle.username, msg -> logLine.accept("[" + username + "] " + msg));
        }
        if (McpToolsBotLogFilter.isInfrastructureError(line)) {
            logLine.accept("[" + username + "] " + McpToolsBotLogFilter.normalize(line));
            finishConnectingAttempt(handle);
            return;
        }
        McpToolsBotLogFilter.formatSessionLine(line)
                .ifPresent(msg -> {
                    logLine.accept("[" + username + "] " + msg);
                    if (handle.state == McpToolsBotHandle.State.CONNECTING && isJoinFailure(msg)) {
                        finishConnectingAttempt(handle);
                    }
                    if (msg.startsWith("Kicked:")) {
                        maybeScheduleThrottleRetry(handle, settings, msg.substring(7).trim(), logLine, onFleetChanged);
                    }
                });
    }

    private static boolean isJoinFailure(String msg) {
        return msg.startsWith("Kicked:")
                || msg.equals("Login timed out.")
                || msg.equals("Session ended.")
                || msg.equals("Disconnected.");
    }

    private static void finishConnectingAttempt(McpToolsBotHandle handle) {
        handle.completeJoinPhase();
        releaseConnectionSlot(handle);
    }

    public void stopBot(String id, Consumer<String> logLine) {
        McpToolsBotHandle handle = find(id);
        if (handle == null) {
            return;
        }
        handle.joinRetryCancelled = true;
        handle.session().stop();
        handle.state = McpToolsBotHandle.State.STOPPED;
        handle.completeJoinPhase();
        if (id.equals(focusedBotId)) {
            focusedBotId = null;
        }
        bots.remove(handle);
        logLine.accept("Stopped bot [" + handle.username + "].");
    }

    public void stopAll(Consumer<String> logLine) {
        cancelPendingRetries();
        for (McpToolsBotHandle bot : List.copyOf(bots)) {
            bot.joinRetryCancelled = true;
            bot.session().stop();
            bot.state = McpToolsBotHandle.State.STOPPED;
            bot.completeJoinPhase();
            logLine.accept("Stopped bot [" + bot.username + "].");
        }
        bots.clear();
        focusedBotId = null;
        CONNECTION_SLOT.drainPermits();
        CONNECTION_SLOT.release();
    }

    public void toggleSelected(String id) {
        McpToolsBotHandle handle = find(id);
        if (handle != null && handle.isActive()) {
            handle.selected = !handle.selected;
        }
    }

    public void selectOnly(String id) {
        for (McpToolsBotHandle bot : bots) {
            bot.selected = bot.id.equals(id) && bot.isActive();
        }
        focusBot(id);
    }

    public void selectAllActive() {
        for (McpToolsBotHandle bot : bots) {
            if (bot.isActive()) {
                bot.selected = true;
            }
        }
    }

    public void selectNone() {
        for (McpToolsBotHandle bot : bots) {
            bot.selected = false;
        }
    }

    public McpToolsBotHandle focusedBot() {
        return find(focusedBotId);
    }

    public void focusBot(String id) {
        McpToolsBotHandle handle = find(id);
        if (handle != null && handle.isActive()) {
            focusedBotId = id;
        }
    }

    public void clearFocus() {
        focusedBotId = null;
    }

    public List<McpToolsBotHandle> controlTargets(McpToolsBotActionTarget target) {
        McpToolsBotHandle focused = focusedBot();
        if (focused != null && focused.isActive()) {
            return List.of(focused);
        }
        return resolveTargets(target);
    }

    public List<McpToolsBotHandle> resolveTargets(McpToolsBotActionTarget target) {
        List<McpToolsBotHandle> out = new ArrayList<>();
        if (target == McpToolsBotActionTarget.ALL) {
            for (McpToolsBotHandle bot : bots) {
                if (bot.isActive()) {
                    out.add(bot);
                }
            }
            return out;
        }
        for (McpToolsBotHandle bot : bots) {
            if (bot.isActive() && bot.selected) {
                out.add(bot);
            }
        }
        return out;
    }

    private String randomUsername() {
        for (int attempt = 0; attempt < 128; attempt++) {
            int suffixLen = 7 + RANDOM.nextInt(4);
            StringBuilder sb = new StringBuilder("bot");
            for (int i = 0; i < suffixLen; i++) {
                sb.append(USERNAME_ALPHABET.charAt(RANDOM.nextInt(USERNAME_ALPHABET.length())));
            }
            String candidate = sb.substring(0, Math.min(16, sb.length()));
            if (candidate.length() >= 3 && !isUsernameTaken(candidate)) {
                return candidate;
            }
        }
        return ("bot" + idSuffix() + Integer.toHexString(RANDOM.nextInt(0xFFFF))).substring(0, 16);
    }

    private String uniqueUsername(String preferred) {
        String base = preferred == null || preferred.isBlank() ? "MCPToolBot" : preferred.trim();
        if (base.length() > 14) {
            base = base.substring(0, 14);
        }
        if (!isUsernameTaken(base)) {
            return base;
        }
        for (int i = 2; i <= 99; i++) {
            String suffix = String.valueOf(i);
            int maxBase = Math.max(1, 16 - suffix.length());
            String candidate = base.substring(0, Math.min(base.length(), maxBase)) + suffix;
            if (!isUsernameTaken(candidate)) {
                return candidate;
            }
        }
        return base.substring(0, Math.min(base.length(), 8)) + idSuffix();
    }

    private boolean isUsernameTaken(String username) {
        for (McpToolsBotHandle bot : bots) {
            if (bot.state != McpToolsBotHandle.State.STOPPED
                    && bot.username.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    private static String idSuffix() {
        return Integer.toHexString((int) (System.nanoTime() & 0xFFFF)).toLowerCase(Locale.ROOT);
    }
}
