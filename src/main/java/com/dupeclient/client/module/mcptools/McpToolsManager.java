package com.dupeclient.client.module.mcptools;

import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.core.notify.ClientNotificationHub;
import com.dupeclient.client.core.session.HubModuleRules;
import com.dupeclient.client.module.packet.FeatureHotkeyManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class McpToolsManager {
    public static final McpToolsManager INSTANCE = new McpToolsManager();

    @FunctionalInterface
    public interface ProgressSink {
        void onProgress(String line);
    }

    private McpToolsSettings settings = new McpToolsSettings();
    private final FeatureHotkeyManager overlayHotkeys = new FeatureHotkeyManager();
    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());
    private final Object runLock = new Object();

    private volatile boolean syncing;
    private volatile boolean running;
    private volatile String syncStatus = "";
    private volatile String bundleVersion = "";
    private volatile String remoteJobId = "";
    private volatile boolean moduleChatFeedback = true;
    private final McpToolsBotFleet botFleet = new McpToolsBotFleet();
    private final AtomicBoolean botJoinQueueCancelled = new AtomicBoolean(false);
    private volatile boolean staffLockApplied;

    private McpToolsManager() {
    }

    public void initialize() {
        settings = McpToolsConfigManager.load();
        moduleChatFeedback = settings.moduleChatFeedback;
        bundleVersion = McpToolsBundleSync.isBundleReady() ? "local" : "";
    }

    public McpToolsSettings getSettings() {
        return settings;
    }

    public void saveSettings() {
        settings.moduleChatFeedback = moduleChatFeedback;
        McpToolsConfigManager.save(settings);
    }

    public boolean isSyncing() {
        return syncing;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isBotSessionActive() {
        return botFleet.hasAnyActive();
    }

    public List<McpToolsBotHandle> getBots() {
        return botFleet.bots();
    }

    public int activeBotCount() {
        return botFleet.activeCount();
    }

    public McpToolsBotActionTarget botActionTarget() {
        return McpToolsBotActionTarget.fromName(settings.botActionTarget);
    }

    public void cycleBotActionTarget() {
        McpToolsBotActionTarget next = botActionTarget().next();
        settings.botActionTarget = next.name();
        saveSettings();
        moduleFeedback("Bot target: " + next.label);
    }

    public boolean showBotControls() {
        McpToolsTool tool = McpToolsTool.fromId(settings.selectedToolId);
        return tool.interactiveBot || botFleet.hasAnyActive();
    }

    public String syncStatus() {
        return syncStatus;
    }

    public String bundleVersion() {
        return bundleVersion;
    }

    public List<String> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    public void clearLogs() {
        synchronized (logs) {
            logs.clear();
        }
    }

    /** Fills host/port from the server the client is currently connected to. */
    public boolean applyCurrentServerToHost(MinecraftClient client) {
        McpToolsServerAddress address = McpToolsServerAddress.fromConnectedClient(client);
        if (address == null) {
            moduleFeedback("Not connected to a multiplayer server.");
            return false;
        }
        settings.lastHost = address.host();
        settings.lastPort = address.port();
        saveSettings();
        moduleFeedback("Host set to " + address.host() + ":" + address.port());
        return true;
    }

    public void appendLog(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        String cleaned = stripMcColorCodes(line.trim());
        synchronized (logs) {
            logs.add(cleaned);
            while (logs.size() > 200) {
                logs.removeFirst();
            }
        }
    }

    private static String stripMcColorCodes(String line) {
        return McpToolsBotLogFilter.normalize(line);
    }

    public void tick(MinecraftClient client) {
        if (client == null) {
            return;
        }
        if (!HubModuleRules.exploitFeaturesAllowed()) {
            if (running) {
                stopRun("Access restricted.");
            } else if (botFleet.hasAnyActive()) {
                stopAllBots("Access restricted.");
            } else {
                botJoinQueueCancelled.set(true);
            }
            if (settings.overlayVisible) {
                McpToolsOverlay.INSTANCE.setOverlayVisible(false);
            }
            return;
        }
        staffLockApplied = false;
        if (client.getWindow() == null || InputFocusGuards.shouldBlockOverlayToggleHotkeys(client)) {
            return;
        }
        if (settings.enabled && overlayHotkeys.consumePress(client, settings.overlayToggleKey)) {
            McpToolsOverlay.INSTANCE.toggleOverlayVisible();
            moduleFeedbackConfigToggle("MCPTools overlay " + (settings.overlayVisible ? "shown" : "hidden"));
        }
    }

    public void syncBundleAsync() {
        if (syncing) {
            return;
        }
        syncing = true;
        syncStatus = "Starting sync…";
        appendLog("Syncing MCPTool bundle from presence…");
        Thread.startVirtualThread(() -> {
            try {
                String version = McpToolsBundleSync.syncFromPresence(line -> {
                    syncStatus = line;
                    appendLog(line);
                });
                McpToolsLocalRunner.invalidateDependencyCache();
                bundleVersion = version;
                syncStatus = "Ready (" + version + ")";
                appendLog("Bundle synced (" + version + ").");
                moduleFeedback("Bundle synced (" + version + ").");
                McpToolsChangelog.Notice notice = McpToolsChangelog.pendingNotice(version);
                if (notice != null) {
                    ClientNotificationHub.warn(notice.body());
                }
            } catch (Exception e) {
                syncStatus = "Sync failed";
                appendLog("Sync failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                moduleFeedback("Bundle sync failed.");
            } finally {
                syncing = false;
            }
        });
    }

    public void runSelectedTool() {
        if (!settings.enabled || !HubModuleRules.exploitFeaturesAllowed()) {
            moduleFeedback("MCPTools unavailable.");
            return;
        }
        McpToolsTool tool = McpToolsTool.fromId(settings.selectedToolId);
        if (tool.interactiveBot) {
            addBots(resolvedBotSpawnCount());
            return;
        }
        stopAllBots(null);
        synchronized (runLock) {
            if (running) {
                moduleFeedback("Already running.");
                return;
            }
            running = true;
        }
        appendLog("Running " + tool.label + " → " + settings.lastHost + ":" + settings.lastPort
                + " (" + McpToolsMcVersion.fromId(settings.lastMcVersion).dropdownLabel() + ")");
        moduleFeedback("Running " + tool.label + "…");
        saveSettings();
        if (settings.remoteRunner) {
            runRemote(tool);
        } else {
            runLocal(tool);
        }
    }

    public void addBot() {
        if (!settings.enabled || !HubModuleRules.exploitFeaturesAllowed()) {
            moduleFeedback("MCPTools unavailable.");
            return;
        }
        if (settings.remoteRunner) {
            moduleFeedback("Bot sessions require local runner (disable remote runner).");
            return;
        }
        saveSettings();
        botFleet.addBot(settings, settings.lastUsername, false, this::appendLog, this::onFleetChanged);
    }

    public void addBots(int count) {
        botFleet.pruneStopped();
        int n = Math.max(1, Math.min(count, McpToolsBotFleet.MAX_BOTS - botFleet.liveCount()));
        if (n <= 0) {
            moduleFeedback("Bot limit reached (" + McpToolsBotFleet.MAX_BOTS + ").");
            return;
        }
        if (!settings.enabled || !HubModuleRules.exploitFeaturesAllowed()) {
            moduleFeedback("MCPTools unavailable.");
            return;
        }
        if (settings.remoteRunner) {
            moduleFeedback("Bot sessions require local runner (disable remote runner).");
            return;
        }
        saveSettings();
        boolean randomNames = n > 1;
        if (n == 1) {
            botFleet.cancelPendingRetries();
            botFleet.addBot(settings, settings.lastUsername, randomNames, this::appendLog, this::onFleetChanged);
            return;
        }
        int delaySec = resolvedJoinDelaySec();
        botJoinQueueCancelled.set(false);
        botFleet.cancelPendingRetries();
        Thread.startVirtualThread(() -> runBotJoinQueue(n, randomNames, delaySec));
        moduleFeedback("Joining " + n + " bots (" + delaySec + "s apart, one at a time)…");
    }

    private void runBotJoinQueue(int count, boolean randomNames, int delaySec) {
        int currentDelay = delaySec;
        for (int i = 0; i < count; i++) {
            if (botJoinQueueCancelled.get()) {
                appendLog("Bot join queue stopped.");
                return;
            }
            if (i > 0 || (i == 0 && botFleet.hasAnyActive())) {
                String label = i == 0
                        ? "Waiting " + currentDelay + "s before join (bots already connected)…"
                        : "Join delay " + currentDelay + "s (" + (i + 1) + "/" + count + ")…";
                appendLog(label);
                if (sleepJoinDelay(currentDelay)) {
                    return;
                }
            }
            if (botJoinQueueCancelled.get()) {
                appendLog("Bot join queue stopped.");
                return;
            }
            if (!settings.enabled || !HubModuleRules.exploitFeaturesAllowed()) {
                appendLog("MCPTools unavailable — join queue stopped.");
                return;
            }
            if (botFleet.liveCount() >= McpToolsBotFleet.MAX_BOTS) {
                appendLog("Bot limit reached (" + McpToolsBotFleet.MAX_BOTS + ").");
                return;
            }
            McpToolsBotHandle handle = botFleet.addBot(
                    settings, settings.lastUsername, randomNames, this::appendLog, this::onFleetChanged);
            if (handle == null) {
                return;
            }
            if (!botFleet.awaitJoinPhase(handle)) {
                appendLog("[" + handle.username + "] Join phase timed out.");
            }
            if (botFleet.consumeThrottleBackoff()) {
                currentDelay = Math.min(60, currentDelay + delaySec);
                appendLog("Throttle detected — increasing join delay to " + currentDelay + "s.");
            }
        }
    }

    private boolean sleepJoinDelay(int delaySec) {
        try {
            Thread.sleep(delaySec * 1000L);
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            appendLog("Bot join queue interrupted.");
            return true;
        }
    }

    public int resolvedJoinDelaySec() {
        return Math.max(1, Math.min(60, settings.botJoinDelaySec));
    }

    public int resolvedBotSpawnCount() {
        return Math.max(1, Math.min(settings.botSpawnCount, McpToolsBotFleet.MAX_BOTS));
    }

    public void joinBotsFromSettings() {
        addBots(resolvedBotSpawnCount());
    }

    public void stopBot(String id) {
        botFleet.stopBot(id, this::appendLog);
        onFleetChanged();
    }

    public void stopAllBots(String reason) {
        botJoinQueueCancelled.set(true);
        boolean hadBots = botFleet.hasAnyActive();
        botFleet.stopAll(this::appendLog);
        onFleetChanged();
        if (reason != null && !reason.isBlank() && hadBots) {
            appendLog(reason);
            moduleFeedback(reason);
        }
    }

    public void toggleBotSelected(String id) {
        botFleet.toggleSelected(id);
    }

    public void selectOnlyBot(String id) {
        McpToolsBotHandle previous = botFleet.focusedBot();
        botFleet.selectOnly(id);
        McpToolsBotHandle bot = botFleet.find(id);
        if (bot != null && (previous == null || !previous.id.equals(bot.id))) {
            moduleFeedback("Controlling " + bot.username + ".");
        }
    }

    public void selectAllBots() {
        botFleet.selectAllActive();
        moduleFeedback("All bots selected.");
    }

    public void selectNoBots() {
        botFleet.selectNone();
        moduleFeedback("Bot selection cleared.");
    }

    private void onFleetChanged() {
        synchronized (runLock) {
            running = botFleet.hasAnyActive();
        }
    }

    public McpToolsBotHandle focusedBot() {
        return botFleet.focusedBot();
    }

    public void focusBot(String id) {
        botFleet.focusBot(id);
    }

    public void clearBotFocus() {
        botFleet.clearFocus();
    }

    private List<McpToolsBotHandle> controlTargets() {
        return botFleet.controlTargets(botActionTarget());
    }

    public void sendBotChat(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        List<McpToolsBotHandle> targets = controlTargets();
        if (targets.isEmpty()) {
            moduleFeedback(botActionTarget() == McpToolsBotActionTarget.SELECTED
                    ? "No bots selected."
                    : "No active bots.");
            return;
        }
        String trimmed = message.trim();
        for (McpToolsBotHandle bot : targets) {
            appendLog("[" + bot.username + "] > " + trimmed);
            bot.session().sendChat(trimmed);
        }
        moduleFeedback("Sent to " + targets.size() + " bot(s).");
    }

    public void sendBotDot(String command, String... args) {
        sendBotDotTo(controlTargets(), command, args, true, "move".equals(command));
    }

    public void holdBotMovement(String movement) {
        sendBotDotTo(controlTargets(), "move", new String[]{"hold", movement}, false, true);
    }

    public void releaseBotMovement(String movement) {
        sendBotDotTo(controlTargets(), "move", new String[]{"release", movement}, false, true);
    }

    public void releaseAllBotMovement() {
        sendBotDotTo(controlTargets(), "move", new String[]{"release", "all"}, true, true);
    }

    public void pulseBotMovement(String movement) {
        sendBotDotTo(controlTargets(), "move", new String[]{movement}, false, true);
    }

    public void toggleBotMovement(String movement) {
        sendBotDotTo(controlTargets(), "move", new String[]{movement}, false, true);
    }

    public void sendBotPathStop() {
        sendBotDotTo(controlTargets(), "pathstop", new String[]{}, true, false);
    }

    public void sendBotGoto(double x, double y, double z) {
        sendBotDotTo(controlTargets(), "goto",
                new String[]{formatCoord(x), formatCoord(y), formatCoord(z)}, true, false);
    }

    public void sendBotMineBlock(String blockInput) {
        String blockId = McpToolsBlockCatalog.resolveMineId(blockInput);
        if (blockId.isBlank()) {
            moduleFeedback("Pick a block to mine.");
            return;
        }
        sendBotDotTo(controlTargets(), "mine", new String[]{blockId}, true, false);
    }

    public boolean sendBotsToLocalPlayer(MinecraftClient client) {
        if (client == null || client.player == null) {
            moduleFeedback("Join a world first.");
            return false;
        }
        sendBotGoto(client.player.getX(), client.player.getBlockY(), client.player.getZ());
        moduleFeedback("Pathing bot(s) to your position.");
        return true;
    }

    private static String formatCoord(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void sendBotDotTo(
            List<McpToolsBotHandle> targets,
            String command,
            String[] args,
            boolean feedback,
            boolean movement) {
        if (targets.isEmpty()) {
            if (feedback) {
                moduleFeedback(focusedBot() != null
                        ? "Focused bot unavailable."
                        : botActionTarget() == McpToolsBotActionTarget.SELECTED
                        ? "No bots selected."
                        : "No active bots.");
            }
            return;
        }
        int sent = 0;
        int blocked = 0;
        for (McpToolsBotHandle bot : targets) {
            if (movement && !bot.auth().isAuthenticated()) {
                blocked++;
                continue;
            }
            if (feedback && !movement) {
                appendLog("[" + bot.username + "] > " + formatDotCommand(command, args));
            }
            bot.session().sendDotCommand(command, args);
            sent++;
        }
        if (movement && sent == 0) {
            if (feedback) {
                moduleFeedback("Bots not logged in yet — wait for login/register.");
            }
            return;
        }
        if (feedback && movement && blocked > 0) {
            moduleFeedback("Sent to " + sent + " bot(s); " + blocked + " still logging in.");
            return;
        }
        if (feedback && !movement) {
            moduleFeedback("Command sent to " + sent + " bot(s).");
        }
    }

    private static String formatDotCommand(String command, String... args) {
        StringBuilder sb = new StringBuilder(command.startsWith(".") ? command : "." + command);
        for (String arg : args) {
            if (arg != null && !arg.isBlank()) {
                sb.append(' ').append(arg.trim());
            }
        }
        return sb.toString();
    }

    public void stopRun(String reason) {
        stopAllBots(null);
        synchronized (runLock) {
            if (!running) {
                return;
            }
            running = false;
        }
        remoteJobId = "";
        if (reason != null && !reason.isBlank()) {
            appendLog(reason);
            moduleFeedback(reason);
        }
    }

    private void finishRun(int exitCode) {
        synchronized (runLock) {
            running = false;
        }
        remoteJobId = "";
        boolean toolError = recentLogsIndicateToolError();
        String msg;
        if (toolError) {
            msg = "Finished with errors (connection failed or server unreachable).";
        } else if (exitCode == 0) {
            msg = "Finished OK.";
        } else {
            msg = "Finished (exit " + exitCode + ").";
        }
        appendLog(msg);
        moduleFeedback(msg);
    }

    private boolean recentLogsIndicateToolError() {
        synchronized (logs) {
            int start = Math.max(0, logs.size() - 20);
            for (int i = start; i < logs.size(); i++) {
                String line = logs.get(i).toLowerCase(java.util.Locale.ROOT);
                if (line.equals("error")
                        || line.contains("incompatible minecraft")
                        || line.contains("timeout")
                        || line.contains("econnrefused")
                        || line.contains("connection refused")
                        || line.contains("getaddrinfo")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void runLocal(McpToolsTool tool) {
        if (!McpToolsBundleSync.isBundleReady()) {
            appendLog("Bundle missing — sync first.");
            finishRun(-1);
            return;
        }
        McpToolsLocalRunner.runAsync(tool, settings, this::appendLog, this::finishRun);
    }

    private void runRemote(McpToolsTool tool) {
        Thread.startVirtualThread(() -> {
            try {
                String jobId = McpToolsPresenceApi.startRemoteJob(tool, settings);
                remoteJobId = jobId;
                appendLog("Remote job " + jobId);
                pollRemoteJob(jobId);
            } catch (Exception e) {
                appendLog("Remote run failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                finishRun(-1);
            }
        });
    }

    private void pollRemoteJob(String jobId) {
        AtomicBoolean active = new AtomicBoolean(true);
        Thread.startVirtualThread(() -> {
            int seen = 0;
            while (active.get() && running) {
                try {
                    Thread.sleep(750);
                    McpToolsPresenceApi.JobStatus status = McpToolsPresenceApi.pollJob(jobId);
                    List<String> out = status.output();
                    if (out.size() > seen) {
                        for (int i = seen; i < out.size(); i++) {
                            appendLog(out.get(i));
                        }
                        seen = out.size();
                    }
                    if ("done".equals(status.status()) || "failed".equals(status.status())) {
                        finishRun(status.exitCode());
                        active.set(false);
                        return;
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    appendLog("Poll error: " + e.getMessage());
                    finishRun(-1);
                    active.set(false);
                    return;
                }
            }
        });
    }

    public void cycleTool() {
        McpToolsTool next = McpToolsTool.fromId(settings.selectedToolId).next();
        settings.selectedToolId = next.id;
        saveSettings();
        moduleFeedback("Tool: " + next.label);
    }

    public void setModuleChatFeedback(boolean enabled) {
        moduleChatFeedback = enabled;
        settings.moduleChatFeedback = enabled;
        saveSettings();
    }

    public void moduleFeedback(String message) {
        if (!moduleChatFeedback) {
            return;
        }
        sendHudLine(prefix().copy().append(Text.literal(message).formatted(Formatting.GRAY)));
    }

    public void moduleFeedbackConfigToggle(String message) {
        sendHudLine(prefix().copy().append(Text.literal(message).formatted(Formatting.GRAY)));
    }

    public void onStaffLock() {
        boolean hadActivity = running || botFleet.hasAnyActive();
        if (!staffLockApplied) {
            staffLockApplied = true;
            if (hadActivity) {
                stopAllBots("Staff lock — MCPTools stopped.");
            } else {
                stopAllBots(null);
            }
            stopRun(null);
            settings.enabled = false;
            settings.overlayVisible = false;
            saveSettings();
            return;
        }
        if (hadActivity) {
            if (running) {
                stopRun(null);
            } else {
                stopAllBots(null);
            }
        } else {
            botJoinQueueCancelled.set(true);
        }
    }

    private static MutableText prefix() {
        return Text.literal("[MCPTools] ").formatted(Formatting.GOLD, Formatting.BOLD);
    }

    private void sendHudLine(Text line) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(line, false);
            }
        });
    }
}
