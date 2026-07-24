package com.dupeclient.client.module.acaudit;

import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import com.dupeclient.client.module.packet.FeatureHotkeyManager;
import com.dupeclient.client.module.packet.fabricator.ClickSlotPackets;
import com.mojang.brigadier.suggestion.Suggestion;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

public final class AcAuditManager {
    public static final AcAuditManager INSTANCE = new AcAuditManager();

    private static final int LOG_CAPACITY = 40;

    private final FeatureHotkeyManager toggleHotkeys = new FeatureHotkeyManager();
    private final FeatureHotkeyManager overlayHotkeys = new FeatureHotkeyManager();
    private final Deque<String> logLines = new ArrayDeque<>();

    private AcAuditSettings settings = new AcAuditSettings();

    private long lastTimePacketMs;
    private double tps = 20.0;
    private int inThisSec;
    private int outThisSec;
    private int setbacksThisSec;
    private int inRate;
    private int outRate;
    private int setbackRate;
    private int lastSecond = -1;

    private int setbacksMoving;
    private int setbacksStill;
    private long setbackWindowStartMs;

    private long lastMoveSentMs = -1;
    private long correctionRttMin = Long.MAX_VALUE;
    private long correctionRttMax;
    private long correctionRttSum;
    private int correctionCount;

    private final Map<String, Integer> packetCounts = new HashMap<>();
    private long packetCadenceWindowStartMs;
    private int packetCadenceTotal;
    private List<String> topPacketLines = List.of();

    private String brand;
    private String platform = "unknown";
    private String lastDisconnect;

    private final Set<String> discoveredCommands = new LinkedHashSet<>();
    private final Set<String> pluginNamespaces = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> anticheatPlugins = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private int commandCompletionId;
    private int commandFingerprintTimer;
    private int commandSweepIdx;

    private final List<SlotProbe> slotProbes = new ArrayList<>();
    private int slotProbeIndex;
    private int slotProbeTimer;
    private int slotSyncPacketsSent;
    private String slotSyncProbeLabel;

    private AcAuditManager() {
    }

    public void initialize() {
        settings = AcAuditConfigManager.load();
        // Slot overlay is handled by UI Utils / fabricator — keep AC Audit off that path.
        settings.rawSlotOverlayEnabled = false;
        if ("SLOT".equalsIgnoreCase(settings.overlayTab)) {
            settings.overlayTab = "MONITOR";
        }
        resetSessionMetrics();
        com.dupeclient.client.gui.GitHubMarkTexture.preloadAsync();
    }

    public void save() {
        AcAuditConfigManager.save(settings);
    }

    public AcAuditSettings getSettings() {
        return settings;
    }

    public List<String> getLogLines() {
        return List.copyOf(logLines);
    }

    public AcAuditMetrics getMetrics() {
        int ping = -1;
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null && client.getConnection() != null) {
            var entry = client.getConnection().getPlayerInfo(client.player.getUUID());
            if (entry != null) {
                ping = entry.getLatency();
            }
        }
        long avg = correctionCount > 0 ? correctionRttSum / correctionCount : 0;
        return new AcAuditMetrics(
                tps,
                ping,
                setbackRate,
                inRate,
                outRate,
                setbacksMoving,
                setbacksStill,
                brand,
                platform,
                lastDisconnect,
                correctionRttMin,
                correctionRttMax,
                avg,
                correctionCount,
                topPacketLines,
                List.copyOf(anticheatPlugins),
                List.copyOf(pluginNamespaces),
                discoveredCommands.size(),
                slotSyncProbeLabel,
                slotProbeIndex,
                slotProbes.size(),
                slotSyncPacketsSent);
    }

    public void setEnabled(boolean enabled) {
        if (enabled && P2wServerPolicy.INSTANCE.isModulesLocked()) {
            feedback("Modules locked on non-P2W server.");
            return;
        }
        if (settings.enabled == enabled) {
            return;
        }
        settings.enabled = enabled;
        if (!enabled) {
            settings.slotSyncProbeActive = false;
            settings.commandFingerprintActive = false;
            AcAuditOverlay.INSTANCE.setOverlayVisible(false);
            resetSessionMetrics();
        }
        feedback("AC Audit " + (enabled ? "enabled" : "disabled"));
        save();
    }

    public void tick(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        if (!InputFocusGuards.shouldBlockOverlayToggleHotkeys(client)) {
            if (overlayHotkeys.consumePress(client, settings.overlayToggleKey)) {
                AcAuditOverlay.INSTANCE.toggleOverlayVisible();
            }
        }
        if (!InputFocusGuards.shouldBlockGlobalHotkeys(client)) {
            if (toggleHotkeys.consumePress(client, settings.toggleKey)) {
                setEnabled(!settings.enabled);
            }
        }
        if (!settings.enabled) {
            return;
        }
        tickSecondBoundary();
        tickSetbackReport();
        tickPacketCadence();
        tickCommandFingerprint(client);
        tickSlotSyncProbe(client);
    }

    public void observeIncoming(Packet<?> packet) {
        if (!settings.enabled) {
            return;
        }
        inThisSec++;
        if (packet instanceof ClientboundSetTimePacket) {
            long now = System.currentTimeMillis();
            if (lastTimePacketMs != 0) {
                long interval = now - lastTimePacketMs;
                if (interval > 0) {
                    tps = Math.min(20.0, 20000.0 / interval);
                }
            }
            lastTimePacketMs = now;
        } else if (packet instanceof ClientboundPlayerPositionPacket) {
            setbacksThisSec++;
            if (Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
                setbacksMoving++;
            } else {
                setbacksStill++;
            }
            if (settings.correctionVerbose && lastMoveSentMs >= 0) {
                long rtt = System.currentTimeMillis() - lastMoveSentMs;
                correctionCount++;
                correctionRttSum += rtt;
                if (rtt < correctionRttMin) {
                    correctionRttMin = rtt;
                }
                if (rtt > correctionRttMax) {
                    correctionRttMax = rtt;
                }
                appendLog(String.format(Locale.ROOT, "Correction RTT: %dms (~%.1f ticks)", rtt, rtt / 50.0));
            }
            if (settings.setbackVerbose) {
                appendLog("Setback received");
            }
        } else if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof BrandPayload brandPayload) {
            brand = brandPayload.brand();
            platform = AcAuditPluginClassifier.classifyPlatform(brand);
            appendLog("Brand: \"" + brand + "\"");
            appendLog("Platform: " + platform);
            if (settings.announcePlatform) {
                feedback("Platform: " + platform + " (\"" + brand + "\")");
            }
        } else if (packet instanceof ClientboundDisconnectPacket disconnect) {
            lastDisconnect = disconnect.reason().getString();
            appendLog("Disconnect: " + lastDisconnect);
            if (AcAuditPluginClassifier.looksLikePacketLimiterKick(lastDisconnect)) {
                appendLog("Likely packet-limiter / anti-spam kick");
                feedback("Disconnect looks like packet-limiter kick.");
            }
            onSessionLeave();
        } else if (packet instanceof ClientboundCommandSuggestionsPacket suggestions) {
            onCommandSuggestions(suggestions);
        }

        if (settings.packetCadenceEnabled) {
            String name = packet.getClass().getSimpleName();
            packetCounts.merge(name, 1, Integer::sum);
            packetCadenceTotal++;
        }
    }

    public void observeOutgoing(Packet<?> packet) {
        if (!settings.enabled) {
            return;
        }
        outThisSec++;
        if (packet instanceof ServerboundMovePlayerPacket) {
            lastMoveSentMs = System.currentTimeMillis();
        }
    }

    public void fireManualClick(Minecraft client) {
        if (!settings.enabled || client == null || client.player == null || client.getConnection() == null) {
            feedback("Join a world and enable AC Audit first.");
            return;
        }
        AbstractContainerMenu handler = client.player.containerMenu;
        int syncId = settings.manualClickSyncMode == AcAuditSettings.ManualSyncMode.CUSTOM
                ? settings.manualClickCustomSyncId
                : handler != null ? handler.containerId : 0;
        int revision = switch (settings.manualClickRevMode) {
            case CURRENT -> handler != null ? handler.getStateId() : 0;
            case ZERO -> 0;
            case CUSTOM -> settings.manualClickCustomRev;
        };
        ContainerInput action = resolveManualAction(settings.manualClickAction);
        if (settings.manualClickPrePickupSlot >= 0) {
            client.getConnection().send(ClickSlotPackets.create(
                    syncId, revision, settings.manualClickPrePickupSlot, 0, ContainerInput.PICKUP));
        }
        int count = Math.max(1, settings.manualClickCount);
        for (int i = 0; i < count; i++) {
            client.getConnection().send(new ServerboundContainerClickPacket(
                    syncId,
                    revision,
                    (short) settings.manualClickSlot,
                    (byte) settings.manualClickButton,
                    action,
                    new Int2ObjectArrayMap<>(),
                    HashedStack.EMPTY));
        }
        appendLog(String.format(Locale.ROOT,
                "Manual click x%d sync=%d rev=%d slot=%d btn=%d %s",
                count, syncId, revision, settings.manualClickSlot, settings.manualClickButton, action.name()));
        feedback("Sent manual click packet(s).");
    }

    public void startCommandFingerprint() {
        if (!settings.enabled) {
            feedback("Enable AC Audit first.");
            return;
        }
        discoveredCommands.clear();
        pluginNamespaces.clear();
        anticheatPlugins.clear();
        commandCompletionId = 0;
        commandSweepIdx = 0;
        commandFingerprintTimer = 0;
        settings.commandFingerprintActive = true;
        appendLog("Command fingerprint started");
        save();
    }

    public void stopCommandFingerprint() {
        if (!settings.commandFingerprintActive) {
            return;
        }
        settings.commandFingerprintActive = false;
        appendLog("Commands discovered: " + discoveredCommands.size());
        if (!pluginNamespaces.isEmpty()) {
            appendLog("Namespaces: " + String.join(", ", pluginNamespaces));
        }
        save();
    }

    public void startSlotSyncProbe() {
        if (!settings.enabled) {
            feedback("Enable AC Audit first.");
            return;
        }
        rebuildSlotProbes();
        if (slotProbes.isEmpty()) {
            feedback("Open a container or inventory first.");
            return;
        }
        slotProbeIndex = 0;
        slotProbeTimer = 0;
        slotSyncPacketsSent = 0;
        settings.slotSyncProbeActive = true;
        appendLog("Slot sync probe started (" + slotProbes.size() + " cases)");
        save();
    }

    public void stopSlotSyncProbe() {
        settings.slotSyncProbeActive = false;
        slotSyncProbeLabel = null;
        appendLog("Slot sync probe stopped (" + slotSyncPacketsSent + " sent)");
        save();
    }

    public void onSessionLeave() {
        settings.slotSyncProbeActive = false;
        settings.commandFingerprintActive = false;
        if (settings.disableOnLeave && settings.enabled) {
            settings.enabled = false;
            settings.overlayVisible = false;
            save();
        }
    }

    private void tickSecondBoundary() {
        int sec = (int) (System.currentTimeMillis() / 1000L);
        if (sec == lastSecond) {
            return;
        }
        lastSecond = sec;
        inRate = inThisSec;
        outRate = outThisSec;
        setbackRate = setbacksThisSec;
        inThisSec = 0;
        outThisSec = 0;
        setbacksThisSec = 0;
        if (settings.logProbeToChat) {
            AcAuditMetrics m = getMetrics();
            String line = String.format(Locale.ROOT,
                    "TPS ~%.1f | ping %dms | setbacks/s %d | in/s %d | out/s %d",
                    m.tps, m.ping, m.setbackRate, m.inRate, m.outRate);
            feedback(line);
            appendLog(line);
        }
    }

    private void tickSetbackReport() {
        long now = System.currentTimeMillis();
        int intervalMs = Math.max(1, settings.setbackReportIntervalSec) * 1000;
        if (now - setbackWindowStartMs < intervalMs) {
            return;
        }
        if (setbacksMoving > 0 || setbacksStill > 0) {
            appendLog(String.format(Locale.ROOT,
                    "Setbacks/s moving=%d still=%d", setbacksMoving, setbacksStill));
        }
        setbacksMoving = 0;
        setbacksStill = 0;
        setbackWindowStartMs = now;
    }

    private void tickPacketCadence() {
        if (!settings.packetCadenceEnabled) {
            return;
        }
        long now = System.currentTimeMillis();
        if (packetCadenceWindowStartMs == 0) {
            packetCadenceWindowStartMs = now;
            return;
        }
        long elapsed = now - packetCadenceWindowStartMs;
        int intervalMs = Math.max(1, settings.packetCadenceIntervalSec) * 1000;
        if (elapsed < intervalMs) {
            return;
        }
        double secs = elapsed / 1000.0;
        List<String> lines = packetCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Math.max(1, settings.packetCadenceTopN))
                .map(e -> String.format(Locale.ROOT, "%s %.1f/s", e.getKey(), e.getValue() / secs))
                .toList();
        topPacketLines = lines;
        appendLog(String.format(Locale.ROOT, "S2C cadence %.0f/s (%d pkts)", packetCadenceTotal / secs, packetCadenceTotal));
        for (String line : lines) {
            appendLog("  " + line);
        }
        packetCounts.clear();
        packetCadenceTotal = 0;
        packetCadenceWindowStartMs = now;
    }

    private void tickCommandFingerprint(Minecraft client) {
        if (!settings.commandFingerprintActive || client.player == null || client.getConnection() == null) {
            return;
        }
        if (commandFingerprintTimer > 0) {
            commandFingerprintTimer--;
            return;
        }
        commandFingerprintTimer = Math.max(1, settings.commandFingerprintDelayTicks);
        String partial;
        if (settings.commandFingerprintSweep) {
            partial = "/" + (char) ('a' + (commandSweepIdx % 26));
            commandSweepIdx++;
        } else {
            partial = settings.commandFingerprintPrefix != null ? settings.commandFingerprintPrefix : "/";
        }
        client.getConnection().send(new ServerboundCommandSuggestionPacket(commandCompletionId++, partial));
    }

    private void tickSlotSyncProbe(Minecraft client) {
        if (!settings.slotSyncProbeActive || client.player == null || client.getConnection() == null) {
            return;
        }
        if (slotProbes.isEmpty()) {
            rebuildSlotProbes();
            if (slotProbes.isEmpty()) {
                stopSlotSyncProbe();
                return;
            }
        }
        if (slotProbeTimer > 0) {
            slotProbeTimer--;
            return;
        }
        SlotProbe probe = slotProbes.get(slotProbeIndex % slotProbes.size());
        slotSyncProbeLabel = probe.label;
        appendLog(String.format(Locale.ROOT, "[%d/%d] %s", slotProbeIndex + 1, slotProbes.size(), probe.label));
        client.getConnection().send(new ServerboundContainerClickPacket(
                probe.syncId,
                probe.revision,
                (short) probe.slot,
                (byte) probe.button,
                ContainerInput.PICKUP,
                new Int2ObjectArrayMap<>(),
                HashedStack.EMPTY));
        slotSyncPacketsSent++;
        slotProbeIndex++;
        slotProbeTimer = Math.max(1, settings.slotSyncProbeDelayTicks);
        if (slotProbeIndex >= slotProbes.size()) {
            if (settings.slotSyncProbeLoop) {
                slotProbeIndex = 0;
            } else {
                stopSlotSyncProbe();
            }
        }
    }

    private void onCommandSuggestions(ClientboundCommandSuggestionsPacket packet) {
        if (!settings.commandFingerprintActive) {
            return;
        }
        int before = discoveredCommands.size();
        for (Suggestion suggestion : packet.toSuggestions().getList()) {
            String text = suggestion.getText();
            if (!discoveredCommands.add(text)) {
                continue;
            }
            int colon = text.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            int start = text.startsWith("/") ? 1 : 0;
            String ns = text.substring(start, colon);
            pluginNamespaces.add(ns);
            if (AcAuditPluginClassifier.isAnticheatNamespace(ns)) {
                anticheatPlugins.add(ns);
                appendLog("AC plugin cmd: " + text);
            } else {
                appendLog("Plugin cmd: " + text);
            }
        }
        if (discoveredCommands.size() > before && discoveredCommands.size() % 25 == 0) {
            appendLog("Discovered " + discoveredCommands.size() + " commands...");
        }
    }

    private void rebuildSlotProbes() {
        slotProbes.clear();
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        AbstractContainerMenu handler = client.player.containerMenu;
        if (handler == null) {
            return;
        }
        int syncId = handler.containerId;
        int revision = handler.getStateId();
        int slotCount = handler.slots.size();
        int validSlot = slotCount > 0 ? 0 : 0;
        AcAuditSettings.SlotSyncField field = settings.slotSyncProbeField != null
                ? settings.slotSyncProbeField
                : AcAuditSettings.SlotSyncField.ALL;
        boolean all = field == AcAuditSettings.SlotSyncField.ALL;

        if (all || field == AcAuditSettings.SlotSyncField.SYNC_ID) {
            for (int value : new int[]{syncId + 1, syncId + 100, -1, 0, 127, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
                slotProbes.add(new SlotProbe("syncId=" + value, value, validSlot, 0, revision));
            }
        }
        if (all || field == AcAuditSettings.SlotSyncField.SLOT) {
            for (int value : new int[]{-1, -999, slotCount, slotCount + 64, 32767, -32768}) {
                slotProbes.add(new SlotProbe("slot=" + value, syncId, value, 0, revision));
            }
        }
        if (all || field == AcAuditSettings.SlotSyncField.BUTTON) {
            for (int value : new int[]{-1, 2, 40, 127, -128}) {
                slotProbes.add(new SlotProbe("button=" + value, syncId, validSlot, value, revision));
            }
        }
        if (all || field == AcAuditSettings.SlotSyncField.REVISION) {
            for (int value : new int[]{revision + 1, revision + 1000, -1, 0, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
                slotProbes.add(new SlotProbe("revision=" + value, syncId, validSlot, 0, value));
            }
        }
    }

    private static ContainerInput resolveManualAction(AcAuditSettings.ManualClickAction action) {
        return switch (action) {
            case PICKUP -> ContainerInput.PICKUP;
            case QUICK_MOVE -> ContainerInput.QUICK_MOVE;
            case SWAP -> ContainerInput.SWAP;
            case CLONE -> ContainerInput.CLONE;
            case THROW -> ContainerInput.THROW;
            case QUICK_CRAFT -> ContainerInput.QUICK_CRAFT;
            case PICKUP_ALL -> ContainerInput.PICKUP_ALL;
        };
    }

    private void resetSessionMetrics() {
        lastTimePacketMs = 0;
        tps = 20.0;
        inThisSec = 0;
        outThisSec = 0;
        setbacksThisSec = 0;
        inRate = 0;
        outRate = 0;
        setbackRate = 0;
        lastSecond = -1;
        setbacksMoving = 0;
        setbacksStill = 0;
        setbackWindowStartMs = System.currentTimeMillis();
        lastMoveSentMs = -1;
        correctionRttMin = Long.MAX_VALUE;
        correctionRttMax = 0;
        correctionRttSum = 0;
        correctionCount = 0;
        packetCounts.clear();
        packetCadenceWindowStartMs = 0;
        packetCadenceTotal = 0;
        topPacketLines = List.of();
        slotProbes.clear();
        slotProbeIndex = 0;
        slotProbeTimer = 0;
        slotSyncPacketsSent = 0;
        slotSyncProbeLabel = null;
    }

    private void appendLog(String line) {
        logLines.addLast(line);
        while (logLines.size() > LOG_CAPACITY) {
            logLines.removeFirst();
        }
    }

    private void feedback(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal("[AC Audit] " + message).withStyle(ChatFormatting.AQUA));
        }
    }

    private record SlotProbe(String label, int syncId, int slot, int button, int revision) {
    }
}
