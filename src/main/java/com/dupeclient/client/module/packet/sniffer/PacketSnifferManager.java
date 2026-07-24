package com.dupeclient.client.module.packet.sniffer;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.config.DupeClientConfigDir;
import com.dupeclient.client.module.packet.FeatureHotkeyManager;
import com.dupeclient.client.module.packet.PacketUtils;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Set;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public final class PacketSnifferManager {
    public static final PacketSnifferManager INSTANCE = new PacketSnifferManager();

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final FeatureHotkeyManager hotkeys = new FeatureHotkeyManager();
    private final Object lock = new Object();
    private final List<PacketSnifferEntry> entries = new ArrayList<>();

    private PacketSnifferSettings settings = new PacketSnifferSettings();
    private Set<Class<? extends Packet<?>>> logExcludeC2sCache = Set.of();
    private Set<Class<? extends Packet<?>>> logExcludeS2cCache = Set.of();
    private Set<Class<? extends Packet<?>>> blockC2sCache = Set.of();
    private Set<Class<? extends Packet<?>>> blockS2cCache = Set.of();
    private String logExcludeC2sCacheKey = "";
    private String logExcludeS2cCacheKey = "";
    private String blockC2sCacheKey = "";
    private String blockS2cCacheKey = "";
    private long nextId = 1;
    private int c2sLogged;
    private int s2cLogged;
    private final Map<String, Integer> c2sTypeCounts = new HashMap<>();
    private final Map<String, Integer> s2cTypeCounts = new HashMap<>();
    private Path sessionLogPath;
    private BufferedWriter sessionWriter;
    private List<PacketSnifferEntry> previousSessionEntries = List.of();

    private PacketSnifferManager() {
    }

    public void initialize() {
        settings = PacketSnifferConfigManager.load();
        settings.ensureLists();
        clampMaxEntries();
        invalidateListCaches();
        if (settings.enabled && settings.logToFile) {
            openSessionLogIfNeeded();
        }
    }

    public void save() {
        settings.ensureLists();
        PacketSnifferConfigManager.save(settings);
        invalidateListCaches();
    }

    private void invalidateListCaches() {
        logExcludeC2sCacheKey = "";
        logExcludeS2cCacheKey = "";
        blockC2sCacheKey = "";
        blockS2cCacheKey = "";
    }

    public PacketSnifferSettings getSettings() {
        return settings;
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        if (!InputFocusGuards.shouldBlockOverlayToggleHotkeys(client)
                && settings.overlayToggleKey != -1
                && hotkeys.consumePress(client, settings.overlayToggleKey)) {
            toggleOverlay();
        }
    }

    public void onSessionLeave() {
        synchronized (lock) {
            previousSessionEntries = new ArrayList<>(entries);
        }
        closeSessionLog();
        if (settings.clearOnLeave) {
            clearEntries();
        }
    }

    public List<PacketSnifferEntry> previousSessionSnapshot() {
        synchronized (lock) {
            return List.copyOf(previousSessionEntries);
        }
    }

    public boolean shouldBlockIncoming(Packet<?> packet) {
        return shouldBlock(PacketDirection.S2C, packet);
    }

    public boolean shouldBlockOutgoing(Packet<?> packet) {
        return shouldBlock(PacketDirection.C2S, packet);
    }

    private boolean shouldBlock(PacketDirection direction, Packet<?> packet) {
        if (!settings.blockEnabled || packet == null) {
            return false;
        }
        if (direction == PacketDirection.S2C && PacketUtilsManager.isIncomingHookBypassed()) {
            return false;
        }
        if (direction == PacketDirection.C2S && PacketUtilsManager.isOutgoingHookBypassed()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Packet<?>> clazz = (Class<? extends Packet<?>>) packet.getClass();
        Class<? extends Packet<?>> resolved = direction == PacketDirection.C2S
                ? PacketUtils.resolveC2sPacketClass(clazz)
                : PacketUtils.resolveS2cPacketClass(clazz);
        Set<Class<? extends Packet<?>>> filter = direction == PacketDirection.C2S ? blockC2sFilter() : blockS2cFilter();
        if (!filter.contains(resolved)) {
            return false;
        }
        notifyBlocked(packet, direction);
        return true;
    }

    public void observeIncoming(Packet<?> packet) {
        observe(PacketDirection.S2C, packet);
    }

    public void observeOutgoing(Packet<?> packet) {
        observe(PacketDirection.C2S, packet);
    }

    private void observe(PacketDirection direction, Packet<?> packet) {
        if (!settings.enabled || settings.paused || packet == null) {
            return;
        }
        if (direction == PacketDirection.S2C && PacketUtilsManager.isIncomingHookBypassed()) {
            return;
        }
        if (direction == PacketDirection.C2S && PacketUtilsManager.isOutgoingHookBypassed()) {
            return;
        }

        String name = PacketUtils.getPacketTypeName(packet);
        if (shouldIgnore(name) || shouldExcludeFromLog(direction, packet)) {
            return;
        }

        PacketDetailLevel level = PacketDetailLevel.fromString(settings.detailLevel);
        String detail = PacketDetailFormatter.format(packet, level);
        String editable = PacketDetailFormatter.fullData(packet);
        long now = System.currentTimeMillis();
        PacketSnifferEntry entry;
        synchronized (lock) {
            entry = new PacketSnifferEntry(nextId++, now, direction, name, detail, packet, editable);
            entries.add(entry);
            trimEntriesLocked();
            if (direction == PacketDirection.C2S) {
                c2sLogged++;
                c2sTypeCounts.merge(name, 1, Integer::sum);
            } else {
                s2cLogged++;
                s2cTypeCounts.merge(name, 1, Integer::sum);
            }
        }

        if (settings.logToConsole) {
            DupeClient.LOGGER.info("[PacketSniffer] {}", entry.displayLine());
        }
        appendSessionLog(entry);
    }

    private boolean shouldIgnore(String name) {
        if (settings.ignoreKeepAlive && isKeepAliveName(name)) {
            return true;
        }
        if (settings.ignorePlayerMove && isPlayerMoveName(name)) {
            return true;
        }
        return false;
    }

    private boolean shouldExcludeFromLog(PacketDirection direction, Packet<?> packet) {
        @SuppressWarnings("unchecked")
        Class<? extends Packet<?>> clazz = (Class<? extends Packet<?>>) packet.getClass();
        Class<? extends Packet<?>> resolved = direction == PacketDirection.C2S
                ? PacketUtils.resolveC2sPacketClass(clazz)
                : PacketUtils.resolveS2cPacketClass(clazz);
        Set<Class<? extends Packet<?>>> filter = direction == PacketDirection.C2S
                ? logExcludeC2sFilter()
                : logExcludeS2cFilter();
        return filter.contains(resolved);
    }

    private Set<Class<? extends Packet<?>>> logExcludeC2sFilter() {
        settings.ensureLists();
        String key = String.join("\0", settings.logExcludeC2sNames);
        if (key.equals(logExcludeC2sCacheKey)) {
            return logExcludeC2sCache;
        }
        logExcludeC2sCacheKey = key;
        logExcludeC2sCache = Set.copyOf(PacketUtils.c2sPacketSetFromNames(settings.logExcludeC2sNames));
        return logExcludeC2sCache;
    }

    private Set<Class<? extends Packet<?>>> logExcludeS2cFilter() {
        settings.ensureLists();
        String key = String.join("\0", settings.logExcludeS2cNames);
        if (key.equals(logExcludeS2cCacheKey)) {
            return logExcludeS2cCache;
        }
        logExcludeS2cCacheKey = key;
        logExcludeS2cCache = Set.copyOf(PacketUtils.s2cPacketSetFromNames(settings.logExcludeS2cNames));
        return logExcludeS2cCache;
    }

    private Set<Class<? extends Packet<?>>> blockC2sFilter() {
        settings.ensureLists();
        String key = String.join("\0", settings.blockC2sNames);
        if (key.equals(blockC2sCacheKey)) {
            return blockC2sCache;
        }
        blockC2sCacheKey = key;
        blockC2sCache = Set.copyOf(PacketUtils.c2sPacketSetFromNames(settings.blockC2sNames));
        return blockC2sCache;
    }

    private Set<Class<? extends Packet<?>>> blockS2cFilter() {
        settings.ensureLists();
        String key = String.join("\0", settings.blockS2cNames);
        if (key.equals(blockS2cCacheKey)) {
            return blockS2cCache;
        }
        blockS2cCacheKey = key;
        blockS2cCache = Set.copyOf(PacketUtils.s2cPacketSetFromNames(settings.blockS2cNames));
        return blockS2cCache;
    }

    private void notifyBlocked(Packet<?> packet, PacketDirection direction) {
        if (!settings.blockChatNotify || !settings.moduleChatFeedback) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        String name = PacketUtils.getPacketTypeName(packet);
        String line = direction == PacketDirection.S2C ? "Blocked S2C: " + name : "Blocked C2S: " + name;
        MutableText msg = Text.literal("[PacketSniffer] ").formatted(Formatting.AQUA, Formatting.BOLD)
                .append(Text.literal(line).formatted(Formatting.GRAY));
        client.player.sendMessage(msg, false);
    }

    private static boolean isKeepAliveName(String name) {
        return name.contains("KeepAlive")
                || name.contains("Pong")
                || name.equals("ClientTickEndC2SPacket");
    }

    private static boolean isPlayerMoveName(String name) {
        return name.startsWith("PlayerMoveC2SPacket");
    }

    public @org.jetbrains.annotations.Nullable PacketSnifferEntry getEntry(long id) {
        synchronized (lock) {
            for (PacketSnifferEntry entry : entries) {
                if (entry.id == id) {
                    return entry;
                }
            }
        }
        return null;
    }

    public List<PacketSnifferEntry> snapshot(PacketDirection filter) {
        synchronized (lock) {
            if (filter == null) {
                return List.copyOf(entries);
            }
            List<PacketSnifferEntry> out = new ArrayList<>();
            for (PacketSnifferEntry entry : entries) {
                if (entry.direction == filter) {
                    out.add(entry);
                }
            }
            return out;
        }
    }

    public List<PacketSnifferEntry> filteredSnapshot(PacketDirection directionFilter, String search) {
        List<PacketSnifferEntry> base = snapshot(directionFilter);
        if (search == null || search.isBlank()) {
            return base;
        }
        List<PacketSnifferEntry> out = new ArrayList<>();
        for (PacketSnifferEntry entry : base) {
            if (PacketSearchFilter.matches(entry, search)) {
                out.add(entry);
            }
        }
        return out;
    }

    public int entryCount() {
        synchronized (lock) {
            return entries.size();
        }
    }

    public int c2sCount() {
        return c2sLogged;
    }

    public int s2cCount() {
        return s2cLogged;
    }

    public int typeCount(PacketDirection direction, String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return 0;
        }
        synchronized (lock) {
            Map<String, Integer> counts = direction == PacketDirection.C2S ? c2sTypeCounts : s2cTypeCounts;
            return counts.getOrDefault(typeName, 0);
        }
    }

    public List<Map.Entry<String, Integer>> topTypeCounts(PacketDirection direction, int limit) {
        int max = Math.max(1, limit);
        synchronized (lock) {
            Map<String, Integer> counts = direction == PacketDirection.C2S ? c2sTypeCounts : s2cTypeCounts;
            return counts.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                            .thenComparing(Map.Entry::getKey))
                    .limit(max)
                    .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
                    .toList();
        }
    }

    public String statusLine() {
        if (!settings.enabled) {
            return "Disabled · enable capture to log packets in-game";
        }
        String state = settings.paused ? "Paused" : "Capturing";
        return state + " · " + entryCount() + " shown · C2S " + c2sLogged + " / S2C " + s2cLogged;
    }

    public void setEnabled(boolean enabled) {
        if (settings.enabled == enabled) {
            return;
        }
        settings.enabled = enabled;
        if (enabled) {
            openSessionLogIfNeeded();
        } else {
            closeSessionLog();
        }
        save();
        feedback("Packet sniffer " + (enabled ? "enabled" : "disabled"));
    }

    public void toggleEnabled() {
        setEnabled(!settings.enabled);
    }

    public void setPaused(boolean paused) {
        if (settings.paused == paused) {
            return;
        }
        settings.paused = paused;
        save();
        feedback(paused ? "Capture paused" : "Capture resumed");
    }

    public void togglePaused() {
        setPaused(!settings.paused);
    }

    public void cycleDetailLevel() {
        PacketDetailLevel next = PacketDetailLevel.fromString(settings.detailLevel).next();
        settings.detailLevel = next.configValue();
        save();
        feedback("Detail level: " + next.label);
    }

    public void toggleOverlay() {
        PacketSnifferOverlay.INSTANCE.setOverlayVisible(!settings.overlayVisible);
        feedback("Sniffer overlay " + (settings.overlayVisible ? "shown" : "hidden"));
    }

    public void clearEntries() {
        synchronized (lock) {
            entries.clear();
            c2sLogged = 0;
            s2cLogged = 0;
            c2sTypeCounts.clear();
            s2cTypeCounts.clear();
            nextId = 1;
        }
        feedback("Sniffer log cleared");
    }

    public void exportLog() {
        List<PacketSnifferEntry> copy = snapshot(null);
        if (copy.isEmpty()) {
            feedback("Nothing to export");
            return;
        }
        try {
            Path dir = logDir();
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(FILE_STAMP);
            Path out = dir.resolve("export_" + stamp + ".txt");
            List<String> lines = new ArrayList<>(copy.size());
            for (PacketSnifferEntry entry : copy) {
                lines.add(entry.displayLine());
            }
            Files.write(out, lines);
            feedback("Exported " + copy.size() + " lines to " + out.getFileName());
        } catch (IOException e) {
            feedback("Export failed: " + e.getMessage());
            DupeClient.LOGGER.warn("[PacketSniffer] Export failed", e);
        }
    }

    public void openLogFolder() {
        try {
            Path dir = logDir();
            Files.createDirectories(dir);
            java.awt.Desktop.getDesktop().open(dir.toFile());
        } catch (IOException | RuntimeException e) {
            feedback("Could not open folder: " + logDir().toAbsolutePath());
        }
    }

    public void feedback(String message) {
        if (!settings.moduleChatFeedback) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        MutableText line = Text.literal("[PacketSniffer] ").formatted(Formatting.AQUA, Formatting.BOLD)
                .append(Text.literal(message).formatted(Formatting.GRAY));
        client.player.sendMessage(line, false);
    }

    private void trimEntriesLocked() {
        int max = Math.max(100, settings.maxEntries);
        while (entries.size() > max) {
            entries.remove(0);
        }
    }

    private void clampMaxEntries() {
        if (settings.maxEntries < 100) {
            settings.maxEntries = 100;
        } else if (settings.maxEntries > 10000) {
            settings.maxEntries = 10000;
        }
    }

    private Path logDir() {
        return DupeClientConfigDir.root().resolve(DupeClientConfigDir.DIR_PACKET_SNIFFER);
    }

    private void openSessionLogIfNeeded() {
        if (!settings.logToFile) {
            return;
        }
        closeSessionLog();
        try {
            Path dir = logDir();
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(FILE_STAMP);
            sessionLogPath = dir.resolve("session_" + stamp + ".txt");
            sessionWriter = Files.newBufferedWriter(sessionLogPath);
            sessionWriter.write("# DupeClient packet sniffer session " + stamp);
            sessionWriter.newLine();
        } catch (IOException e) {
            sessionWriter = null;
            sessionLogPath = null;
            DupeClient.LOGGER.warn("[PacketSniffer] Could not open session log", e);
        }
    }

    private void closeSessionLog() {
        if (sessionWriter != null) {
            try {
                sessionWriter.flush();
                sessionWriter.close();
            } catch (IOException ignored) {
            }
        }
        sessionWriter = null;
        sessionLogPath = null;
    }

    private void appendSessionLog(PacketSnifferEntry entry) {
        if (!settings.logToFile) {
            return;
        }
        if (sessionWriter == null) {
            openSessionLogIfNeeded();
        }
        if (sessionWriter == null) {
            return;
        }
        try {
            sessionWriter.write(entry.displayLine());
            sessionWriter.newLine();
        } catch (IOException e) {
            closeSessionLog();
        }
    }

    public void onLogToFileToggled(boolean enabled) {
        settings.logToFile = enabled;
        save();
        if (enabled && settings.enabled) {
            openSessionLogIfNeeded();
        } else if (!enabled) {
            closeSessionLog();
        }
    }

    public boolean isExcludedFromLog(PacketDirection direction, String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return false;
        }
        settings.ensureLists();
        List<String> list = direction == PacketDirection.C2S
                ? settings.logExcludeC2sNames
                : settings.logExcludeS2cNames;
        return list.contains(typeName);
    }

    public boolean isBlocked(PacketDirection direction, String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return false;
        }
        settings.ensureLists();
        List<String> list = direction == PacketDirection.C2S
                ? settings.blockC2sNames
                : settings.blockS2cNames;
        return list.contains(typeName);
    }

    public void excludeFromLog(PacketSnifferEntry entry) {
        if (entry == null) {
            return;
        }
        settings.ensureLists();
        List<String> list = entry.direction == PacketDirection.C2S
                ? settings.logExcludeC2sNames
                : settings.logExcludeS2cNames;
        if (!list.contains(entry.name)) {
            list.add(entry.name);
            save();
            purgeEntriesOfType(entry.direction, entry.name);
            feedback("Excluded from log: " + entry.name);
        } else {
            feedback("Already excluded from log: " + entry.name);
        }
    }

    public void includeInLog(PacketSnifferEntry entry) {
        if (entry == null) {
            return;
        }
        settings.ensureLists();
        List<String> list = entry.direction == PacketDirection.C2S
                ? settings.logExcludeC2sNames
                : settings.logExcludeS2cNames;
        if (list.remove(entry.name)) {
            save();
            feedback("Included in log: " + entry.name);
        }
    }

    public void toggleExcludeFromLog(PacketSnifferEntry entry) {
        if (entry == null) {
            return;
        }
        if (isExcludedFromLog(entry.direction, entry.name)) {
            includeInLog(entry);
        } else {
            excludeFromLog(entry);
        }
    }

    public void blockPacketType(PacketSnifferEntry entry) {
        if (entry == null) {
            return;
        }
        settings.ensureLists();
        List<String> list = entry.direction == PacketDirection.C2S
                ? settings.blockC2sNames
                : settings.blockS2cNames;
        if (!list.contains(entry.name)) {
            list.add(entry.name);
        }
        if (!settings.blockEnabled) {
            settings.blockEnabled = true;
        }
        save();
        String verb = entry.direction == PacketDirection.C2S ? "send" : "receive";
        feedback("Blocking " + entry.name + " (" + verb + ")");
    }

    public void unblockPacketType(PacketSnifferEntry entry) {
        if (entry == null) {
            return;
        }
        settings.ensureLists();
        List<String> list = entry.direction == PacketDirection.C2S
                ? settings.blockC2sNames
                : settings.blockS2cNames;
        if (list.remove(entry.name)) {
            save();
            String verb = entry.direction == PacketDirection.C2S ? "send" : "receive";
            feedback("Unblocked " + entry.name + " (" + verb + ")");
        }
    }

    public void toggleBlockPacketType(PacketSnifferEntry entry) {
        if (entry == null) {
            return;
        }
        if (isBlocked(entry.direction, entry.name)) {
            unblockPacketType(entry);
        } else {
            blockPacketType(entry);
        }
    }

    private void purgeEntriesOfType(PacketDirection direction, String name) {
        synchronized (lock) {
            entries.removeIf(e -> e.direction == direction && e.name.equals(name));
        }
    }
}
