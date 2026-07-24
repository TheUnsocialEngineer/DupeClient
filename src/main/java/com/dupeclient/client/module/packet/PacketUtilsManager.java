package com.dupeclient.client.module.packet;

import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import com.dupeclient.client.mixin.ClientConnectionInvoker;
import com.dupeclient.client.module.packet.fabricator.FabricatorSendScheduler;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import com.ui_utils.SharedVariables;
import io.netty.channel.ChannelFutureListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;

public class PacketUtilsManager {
    public static final PacketUtilsManager INSTANCE = new PacketUtilsManager();

    private static final ThreadLocal<Boolean> BYPASS_HOOK = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> BYPASS_INCOMING_HOOK = ThreadLocal.withInitial(() -> false);

    private final Deque<QueuedPacket> queue = new ArrayDeque<>();
    private final Deque<QueuedPacket> packetDelayQueue = new ArrayDeque<>();
    private final Deque<QueuedIncoming> packetDelayIncomingQueue = new ArrayDeque<>();
    private final Object packetDelayIncomingLock = new Object();
    private final FeatureHotkeyManager hotkeys = new FeatureHotkeyManager();
    private PacketUtilsSettings settings = new PacketUtilsSettings();
    private boolean textInputFocused;
    private long lastSpamAtMs;
    private long uiUtilsFlushQueuedAtMs = -1L;
    private java.util.Set<Class<? extends Packet<?>>> packetDelayClassFilterCache;
    private String packetDelayClassFilterCacheKey = "";
    private java.util.Set<Class<? extends Packet<?>>> packetDelayS2cClassFilterCache;
    private String packetDelayS2cClassFilterCacheKey = "";

    private PacketUtilsManager() {
    }

    public void flushAllQueuedNow() {
        long now = System.currentTimeMillis();
        for (QueuedPacket packet : queue) {
            packet.dueAtMs = now;
        }
    }

    public void initialize() {
        settings = PacketUtilsConfigManager.load();
        if (settings.uiElementsToggleKey == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
            settings.uiElementsToggleKey = org.lwjgl.glfw.GLFW.GLFW_KEY_P;
        }
        SharedVariables.enabled = settings.uiUtilsOverlayEnabled;
    }

    public PacketUtilsSettings getSettings() {
        return settings;
    }

    public void setTextInputFocused(boolean focused) {
        this.textInputFocused = focused;
    }

    public boolean isTextInputFocused() {
        return textInputFocused;
    }

    public static boolean isIncomingHookBypassed() {
        return Boolean.TRUE.equals(BYPASS_INCOMING_HOOK.get());
    }

    public static boolean isOutgoingHookBypassed() {
        return Boolean.TRUE.equals(BYPASS_HOOK.get());
    }

    public static void setIncomingHookBypassed(boolean bypassed) {
        BYPASS_INCOMING_HOOK.set(bypassed);
    }

    public boolean onIncomingPacket(Connection connection, Packet<?> packet) {
        if (Boolean.TRUE.equals(BYPASS_INCOMING_HOOK.get())) {
            return false;
        }
        if (!settings.packetDelayEnabled || !shouldPacketDelayIncoming(packet)) {
            return false;
        }
        synchronized (packetDelayIncomingLock) {
            packetDelayIncomingQueue.addLast(new QueuedIncoming(connection, packet));
        }
        if (settings.logPacketNamesOnDelay) {
            notifyInfo("Delaying S2C: " + PacketUtils.getPacketTypeName(packet));
        }
        notifyBlockedPacket(packet, true);
        return true;
    }

    public boolean onOutgoingPacket(Connection connection, Packet<?> packet, ChannelFutureListener callbacks, boolean flush) {
        if (Boolean.TRUE.equals(BYPASS_HOOK.get())) {
            return false;
        }

        PacketKind kind = PacketKind.fromPacket(packet);

        if (settings.packetDelayEnabled && shouldPacketDelay(packet)) {
            packetDelayQueue.addLast(new QueuedPacket(connection, packet, callbacks, flush, Long.MAX_VALUE));
            if (settings.logPacketNamesOnDelay) {
                notifyInfo("Delaying Packet: " + PacketUtils.getPacketTypeName(packet));
            }
            notifyBlockedPacket(packet, false);
            return true;
        }

        if (settings.desyncEnabled && settings.desyncKinds.contains(kind)) {
            queue.addLast(new QueuedPacket(connection, packet, callbacks, flush, Long.MAX_VALUE));
            return true;
        }

        // Core queue mode inspired by du-addon PacketUtils:
        // matched packets are queued and drained with a per-tick cap.
        if (settings.delayEnabled && settings.delayKinds.contains(kind)) {
            queue.addLast(new QueuedPacket(connection, packet, callbacks, flush, 0L));
            return true;
        }

        if (settings.advancedModuleEnabled && settings.advancedKinds.contains(kind)) {
            long due = System.currentTimeMillis() + (long) settings.advancedDelayMs;
            queue.addLast(new QueuedPacket(connection, packet, callbacks, flush, due));
            return true;
        }

        return false;
    }

    public void tick(Minecraft client) {
        handleFeatureHotkeys(client);
        processQueue();
        tickSpam(client);
        tickUiUtilsDelayedFlush(client);
        FabricatorSendScheduler.INSTANCE.tick(client);
    }

    public void flushDesync() {
        long now = System.currentTimeMillis();
        for (QueuedPacket packet : queue) {
            if (packet.dueAtMs == Long.MAX_VALUE) {
                packet.dueAtMs = now;
            }
        }
    }

    public void clearQueue() {
        queue.clear();
    }

    public int queueSize() {
        return queue.size();
    }

    public int packetDelayQueueSize() {
        return packetDelayQueue.size();
    }

    public int packetDelayIncomingQueueSize() {
        synchronized (packetDelayIncomingLock) {
            return packetDelayIncomingQueue.size();
        }
    }

    public void setPacketDelayEnabled(boolean enabled) {
        if (enabled && blockIfPolicyLocked()) {
            return;
        }
        if (settings.packetDelayEnabled == enabled) {
            return;
        }
        settings.packetDelayEnabled = enabled;
        if (!enabled) {
            flushPacketDelayQueue();
        }
        save();
        moduleFeedback("Packet delay " + (enabled ? "enabled" : "disabled"));
    }

    public void togglePacketDelayEnabled() {
        setPacketDelayEnabled(!settings.packetDelayEnabled);
    }

    public void flushPacketDelayQueue() {
        int sent = 0;
        while (!packetDelayQueue.isEmpty()) {
            QueuedPacket queued = packetDelayQueue.pollFirst();
            if (queued != null) {
                sendDirect(queued);
                sent++;
            }
        }
        List<QueuedIncoming> incomingBatch = new ArrayList<>();
        synchronized (packetDelayIncomingLock) {
            while (!packetDelayIncomingQueue.isEmpty()) {
                QueuedIncoming qi = packetDelayIncomingQueue.pollFirst();
                if (qi != null) {
                    incomingBatch.add(qi);
                }
            }
        }
        int incomingReleased = incomingBatch.size();
        if (!incomingBatch.isEmpty()) {
            Minecraft client = Minecraft.getInstance();
            Runnable replay = () -> {
                BYPASS_INCOMING_HOOK.set(true);
                try {
                    for (QueuedIncoming qi : incomingBatch) {
                        if (qi.connection == null || !qi.connection.isConnected()) {
                            continue;
                        }
                        PacketListener listener = qi.connection.getPacketListener();
                        if (listener == null) {
                            continue;
                        }
                        ((ClientConnectionInvoker) qi.connection).dupeclient$invokeHandlePacket(qi.packet, listener);
                    }
                } finally {
                    BYPASS_INCOMING_HOOK.set(false);
                }
            };
            if (client != null) {
                client.execute(replay);
            } else {
                replay.run();
            }
        }
        if (sent > 0 || incomingReleased > 0) {
            notifyInfo("Released " + sent + " outgoing and " + incomingReleased + " incoming delayed packet(s).");
        } else {
            moduleFeedback("Packet delay flush: nothing queued.");
        }
    }

    public void save() {
        PacketUtilsConfigManager.save(settings);
    }

    /** Turns off active packet features when leaving a world or server (if enabled in settings). */
    public void onSessionLeave() {
        if (!settings.disableActiveOnLeave) {
            return;
        }
        forceDisableAllFeatures(true);
    }

    /** Policy lock for non-P2W community servers — always disables exploit packet features. */
    public void forceDisableAllFeatures() {
        forceDisableAllFeatures(false);
    }

    private void forceDisableAllFeatures(boolean notifyLeave) {
        boolean changed = false;
        if (settings.packetDelayEnabled) {
            settings.packetDelayEnabled = false;
            flushPacketDelayQueue();
            changed = true;
        }
        if (settings.delayEnabled) {
            settings.delayEnabled = false;
            clearQueue();
            changed = true;
        }
        if (settings.desyncEnabled) {
            settings.desyncEnabled = false;
            flushDesync();
            changed = true;
        }
        if (settings.spamEnabled) {
            settings.spamEnabled = false;
            changed = true;
        }
        if (settings.advancedModuleEnabled) {
            settings.advancedModuleEnabled = false;
            clearQueue();
            changed = true;
        }
        if (SharedVariables.delayUIPackets) {
            SharedVariables.delayUIPackets = false;
            uiUtilsFlushQueuedAtMs = -1L;
            changed = true;
        }
        if (FabricatorSendScheduler.INSTANCE.isActive()) {
            FabricatorSendScheduler.INSTANCE.stop(null);
            changed = true;
        }
        if (settings.uiUtilsOverlayEnabled) {
            settings.uiUtilsOverlayEnabled = false;
            SharedVariables.enabled = false;
            changed = true;
        }
        if (settings.packetDelayOverlayVisible) {
            settings.packetDelayOverlayVisible = false;
            changed = true;
        }
        if (changed) {
            save();
            if (notifyLeave) {
                moduleFeedback("Active packet features disabled (left world/server).");
            }
        }
    }

    private void handleFeatureHotkeys(Minecraft client) {
        handleOverlayToggleHotkeys(client);
        if (textInputFocused) {
            return;
        }

        if (hotkeys.consumePress(client, settings.packetDelayToggleKey)) {
            togglePacketDelayEnabled();
        }
        if (hotkeys.consumePress(client, settings.uiElementsToggleKey)) {
            settings.uiElementsEnabled = !settings.uiElementsEnabled;
            save();
            moduleFeedback("UI elements overlay hints " + (settings.uiElementsEnabled ? "ON" : "OFF"));
        }
        if (hotkeys.consumePress(client, settings.delayToggleKey)) {
            settings.delayEnabled = !settings.delayEnabled;
            save();
            moduleFeedback("Core packet queue " + (settings.delayEnabled ? "ON" : "OFF"));
        }
        if (hotkeys.consumePress(client, settings.coreQueueToggleKey)) {
            settings.delayEnabled = !settings.delayEnabled;
            save();
            moduleFeedback("Core packet queue " + (settings.delayEnabled ? "ON" : "OFF"));
        }
        if (hotkeys.consumePress(client, settings.coreQueueFlushKey)) {
            flushAllQueuedNow();
        }
        if (hotkeys.consumePress(client, settings.desyncToggleKey)) {
            settings.desyncEnabled = !settings.desyncEnabled;
            if (!settings.desyncEnabled) {
                flushDesync();
            }
            save();
            moduleFeedback("Desync hold " + (settings.desyncEnabled ? "ON" : "OFF"));
        }
        if (hotkeys.consumePress(client, settings.advancedToggleKey)) {
            settings.advancedModuleEnabled = !settings.advancedModuleEnabled;
            save();
            moduleFeedback("Advanced delay module " + (settings.advancedModuleEnabled ? "ON" : "OFF"));
        }
        if (hotkeys.consumePress(client, settings.spamToggleKey)) {
            settings.spamEnabled = !settings.spamEnabled;
            save();
            moduleFeedback("Command spam " + (settings.spamEnabled ? "ON" : "OFF"));
        }
        if (hotkeys.consumePress(client, settings.uiUtilsCloseWithoutPacketKey)) {
            moduleFeedback("Close without packet hotkey → closing current screen");
            if (client != null) {
                client.setScreen(null);
            }
        }
        if (hotkeys.consumePress(client, settings.uiUtilsDelayToggleKey)) {
            toggleUiUtilsDelay();
            save();
        }
        if (hotkeys.consumePress(client, settings.uiUtilsSendPacketsToggleKey)) {
            SharedVariables.sendUIPackets = !SharedVariables.sendUIPackets;
            save();
            moduleFeedback("UI Utils send live packets " + (SharedVariables.sendUIPackets ? "ON" : "OFF"));
        }
        if (hotkeys.consumePress(client, settings.uiUtilsSendQueuedKey)) {
            flushUiUtilsQueueNow(client);
        }
    }

    private void handleOverlayToggleHotkeys(Minecraft client) {
        if (InputFocusGuards.shouldBlockOverlayToggleHotkeys(client)) {
            return;
        }
        if (hotkeys.consumePress(client, settings.packetDelayOverlayToggleKey)) {
            PacketFabricatorOverlay.INSTANCE.showDelayTab();
            moduleFeedbackConfigToggle(
                    "Packet fabricator delay tab " + (settings.fabricatorVisible ? "shown" : "hidden"));
        }
        if (hotkeys.consumePress(client, settings.uiUtilsOverlayToggleKey)) {
            settings.uiUtilsOverlayEnabled = !settings.uiUtilsOverlayEnabled;
            SharedVariables.enabled = settings.uiUtilsOverlayEnabled;
            save();
            moduleFeedback("UI Utils overlay " + (settings.uiUtilsOverlayEnabled ? "ON" : "OFF"));
        }
        if (hotkeys.consumePress(client, settings.fabricatorToggleKey)) {
            PacketFabricatorOverlay.INSTANCE.toggleVisible();
            moduleFeedback("Packet fabricator " + (PacketFabricatorOverlay.INSTANCE.isVisible() ? "shown" : "hidden"));
        }
        if (hotkeys.consumePress(client, settings.slotIdsToggleKey)) {
            settings.slotIdsOverlayEnabled = !settings.slotIdsOverlayEnabled;
            save();
            moduleFeedback("Slot ID overlay " + (settings.slotIdsOverlayEnabled ? "ON" : "OFF"));
        }
    }

    public void setUiUtilsOverlayEnabled(boolean enabled) {
        settings.uiUtilsOverlayEnabled = enabled;
        SharedVariables.enabled = enabled;
        save();
        moduleFeedback("UI Utils overlay " + (enabled ? "ON" : "OFF"));
    }

    public void toggleUiUtilsDelay() {
        if (!SharedVariables.delayUIPackets) {
            SharedVariables.delayUIPackets = true;
            uiUtilsFlushQueuedAtMs = -1L;
            return;
        }
        SharedVariables.delayUIPackets = false;
        scheduleUiUtilsFlush();
    }

    public void scheduleUiUtilsFlush() {
        if (SharedVariables.delayedUIPackets.isEmpty()) {
            uiUtilsFlushQueuedAtMs = -1L;
            return;
        }
        long delayMs = Math.max(0L, (long) settings.uiUtilsDelayReleaseMs);
        uiUtilsFlushQueuedAtMs = System.currentTimeMillis() + delayMs;
    }

    public void flushUiUtilsQueueNow(Minecraft client) {
        if (client == null || client.getConnection() == null || SharedVariables.delayedUIPackets.isEmpty()) {
            uiUtilsFlushQueuedAtMs = -1L;
            return;
        }
        int n = SharedVariables.delayedUIPackets.size();
        for (Packet<?> packet : SharedVariables.delayedUIPackets) {
            client.getConnection().send(packet);
        }
        SharedVariables.delayedUIPackets.clear();
        uiUtilsFlushQueuedAtMs = -1L;
        moduleFeedback("UI Utils: sent " + n + " queued packet(s).");
    }

    private void tickUiUtilsDelayedFlush(Minecraft client) {
        if (uiUtilsFlushQueuedAtMs < 0L) {
            return;
        }
        if (System.currentTimeMillis() < uiUtilsFlushQueuedAtMs) {
            return;
        }
        flushUiUtilsQueueNow(client);
    }

    private void tickSpam(Minecraft client) {
        if (!settings.spamEnabled) {
            return;
        }
        if (client == null || client.player == null || client.getConnection() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSpamAtMs < (long) settings.spamDelayMs) {
            return;
        }
        lastSpamAtMs = now;

        String text = settings.spamMessage == null ? "" : settings.spamMessage.trim();
        if (!text.isEmpty()) {
            client.player.connection.sendChat(text.startsWith("/") ? text.substring(1) : text);
        }
    }

    private void processQueue() {
        if (queue.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        int sent = 0;
        int maxPerTick = Math.max(1, Math.min(settings.maxSendPerTick, 50));

        while (!queue.isEmpty() && sent < maxPerTick) {
            QueuedPacket next = queue.peekFirst();
            if (next == null || next.dueAtMs > now) {
                break;
            }
            queue.pollFirst();
            sendDirect(next);
            sent++;
        }
    }

    private void sendDirect(QueuedPacket queued) {
        if (queued.connection == null || !queued.connection.isConnected()) {
            return;
        }
        BYPASS_HOOK.set(true);
        try {
            queued.connection.send(queued.packet, queued.callbacks, queued.flush);
        } finally {
            BYPASS_HOOK.set(false);
        }
    }

    /** Sends a packet without delay/desync hooks (used by packet fabricator). */
    public void sendBypass(Minecraft client, Packet<?> packet) {
        if (client == null || client.getConnection() == null || packet == null) {
            return;
        }
        Connection connection = client.getConnection().getConnection();
        if (connection == null || !connection.isConnected()) {
            return;
        }
        BYPASS_HOOK.set(true);
        try {
            connection.send(packet);
        } finally {
            BYPASS_HOOK.set(false);
        }
    }

    private static final class QueuedPacket {
        private final Connection connection;
        private final Packet<?> packet;
        private final ChannelFutureListener callbacks;
        private final boolean flush;
        private long dueAtMs;

        private QueuedPacket(Connection connection, Packet<?> packet, ChannelFutureListener callbacks, boolean flush, long dueAtMs) {
            this.connection = connection;
            this.packet = packet;
            this.callbacks = callbacks;
            this.flush = flush;
            this.dueAtMs = dueAtMs;
        }
    }

    private static final class QueuedIncoming {
        private final Connection connection;
        private final Packet<?> packet;

        private QueuedIncoming(Connection connection, Packet<?> packet) {
            this.connection = connection;
            this.packet = packet;
        }
    }

    private boolean shouldPacketDelay(Packet<?> packet) {
        java.util.List<String> names = settings.packetDelayC2sClassNames;
        if (names == null || names.isEmpty()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Packet<?>> clazz = (Class<? extends Packet<?>>) packet.getClass();
        Class<? extends Packet<?>> resolved = PacketUtils.resolveC2sPacketClass(clazz);
        return packetDelayClassFilter().contains(resolved);
    }

    private java.util.Set<Class<? extends Packet<?>>> packetDelayClassFilter() {
        java.util.List<String> names = settings.packetDelayC2sClassNames;
        String key = names == null ? "" : String.join("\0", names);
        if (key.equals(packetDelayClassFilterCacheKey) && packetDelayClassFilterCache != null) {
            return packetDelayClassFilterCache;
        }
        packetDelayClassFilterCacheKey = key;
        packetDelayClassFilterCache = java.util.Set.copyOf(PacketUtils.c2sPacketSetFromNames(names));
        return packetDelayClassFilterCache;
    }

    private boolean shouldPacketDelayIncoming(Packet<?> packet) {
        java.util.List<String> names = settings.packetDelayS2cClassNames;
        if (names == null || names.isEmpty()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Class<? extends Packet<?>> clazz = (Class<? extends Packet<?>>) packet.getClass();
        Class<? extends Packet<?>> resolved = PacketUtils.resolveS2cPacketClass(clazz);
        return packetDelayS2cClassFilter().contains(resolved);
    }

    private java.util.Set<Class<? extends Packet<?>>> packetDelayS2cClassFilter() {
        java.util.List<String> names = settings.packetDelayS2cClassNames;
        String key = names == null ? "" : String.join("\0", names);
        if (key.equals(packetDelayS2cClassFilterCacheKey) && packetDelayS2cClassFilterCache != null) {
            return packetDelayS2cClassFilterCache;
        }
        packetDelayS2cClassFilterCacheKey = key;
        packetDelayS2cClassFilterCache = java.util.Set.copyOf(PacketUtils.s2cPacketSetFromNames(names));
        return packetDelayS2cClassFilterCache;
    }

    private void notifyBlockedPacket(Packet<?> packet, boolean incoming) {
        if (!settings.packetDelayBlockedChatNotify || !settings.moduleChatFeedback) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        String name = PacketUtils.getPacketTypeName(packet);
        String line = incoming ? "Blocked inbound: " + name : "Blocked outbound: " + name;
        Component msg = Component.literal(line).withStyle(ChatFormatting.GRAY);
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(msg, false);
            }
        });
    }

    private void notifyInfo(String message) {
        moduleFeedback(message);
    }

    /** HUD line for toggles, hotkeys, and notify-style status; respects {@link PacketUtilsSettings#moduleChatFeedback}. */
    public void moduleFeedback(String message) {
        if (!settings.moduleChatFeedback) {
            return;
        }
        sendPacketUtilsHud(packetUtilsPrefix().copy().append(Component.literal(message).withStyle(ChatFormatting.GRAY)));
    }

    /** Shown even when chat feedback is off (e.g. toggling the feedback option itself). */
    public void moduleFeedbackConfigToggle(String message) {
        sendPacketUtilsHud(packetUtilsPrefix().copy().append(Component.literal(message).withStyle(ChatFormatting.GRAY)));
    }

    private static MutableComponent packetUtilsPrefix() {
        return Component.literal("[Packet Utils] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private void sendPacketUtilsHud(Component line) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(line, false);
            }
        });
    }

    private boolean blockIfPolicyLocked() {
        if (P2wServerPolicy.INSTANCE.isModulesLocked()) {
            moduleFeedback("Modules locked on non-P2W server.");
            return true;
        }
        return false;
    }
}
