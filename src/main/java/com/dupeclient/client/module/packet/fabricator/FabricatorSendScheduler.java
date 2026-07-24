package com.dupeclient.client.module.packet.fabricator;

import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Sends fabricated click-slot packets over multiple ticks with pause/stop. */
public final class FabricatorSendScheduler {
    public static final FabricatorSendScheduler INSTANCE = new FabricatorSendScheduler();

    public enum State {
        IDLE,
        RUNNING,
        PAUSED
    }

    private final Deque<ClickSlotC2SPacket> pending = new ArrayDeque<>();
    private State state = State.IDLE;
    private int sentCount;
    private int totalCount;
    private long lastSendAtMs;

    private FabricatorSendScheduler() {
    }

    public State getState() {
        return state;
    }

    public int getSentCount() {
        return sentCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPendingCount() {
        return pending.size();
    }

    public boolean isActive() {
        return state == State.RUNNING || state == State.PAUSED;
    }

    public void tick(MinecraftClient client) {
        if (state != State.RUNNING || pending.isEmpty()) {
            if (state == State.RUNNING && pending.isEmpty()) {
                finish("Sent " + sentCount + " packet(s).");
            }
            return;
        }
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            stop("Stopped: left game.");
            return;
        }
        PacketUtilsSettings settings = PacketUtilsManager.INSTANCE.getSettings();
        int delayMs = Math.max(0, settings.fabricatorSendDelayMs);
        long now = System.currentTimeMillis();
        if (delayMs > 0 && now - lastSendAtMs < delayMs) {
            updateProgressStatus();
            return;
        }

        ScreenHandler handler = resolveHandler(client);
        if (handler == null) {
            stop("Stopped: no screen handler.");
            return;
        }

        int perTick = Math.max(1, Math.min(500, settings.fabricatorPacketsPerTick));
        int batch = 0;
        while (batch < perTick && !pending.isEmpty()) {
            if (delayMs > 0 && batch > 0 && System.currentTimeMillis() - lastSendAtMs < delayMs) {
                break;
            }
            ClickSlotC2SPacket packet = pending.pollFirst();
            if (packet == null) {
                break;
            }
            ClickSlotC2SPacket refreshed = ClickSlotPackets.refresh(packet, handler);
            if (refreshed == null) {
                stop("Stopped: failed to refresh packet.");
                return;
            }
            PacketUtilsManager.INSTANCE.sendBypass(client, refreshed);
            sentCount++;
            batch++;
            lastSendAtMs = System.currentTimeMillis();
        }
        updateProgressStatus();
        if (pending.isEmpty()) {
            finish("Sent " + sentCount + " packet(s).");
        }
    }

    public boolean start(MinecraftClient client, List<ClickSlotC2SPacket> packets) {
        if (packets == null || packets.isEmpty()) {
            return false;
        }
        stop(null);
        pending.clear();
        pending.addAll(packets);
        sentCount = 0;
        totalCount = packets.size();
        state = State.RUNNING;
        lastSendAtMs = 0L;
        updateProgressStatus();
        PacketUtilsManager.INSTANCE.moduleFeedback("Fabricator sending " + totalCount + " packet(s)…");
        tick(client);
        return true;
    }

    public void pause() {
        if (state != State.RUNNING) {
            return;
        }
        state = State.PAUSED;
        updateProgressStatus();
        PacketUtilsManager.INSTANCE.moduleFeedback("Fabricator send paused (" + sentCount + "/" + totalCount + ").");
    }

    public void resume(MinecraftClient client) {
        if (state != State.PAUSED || pending.isEmpty()) {
            return;
        }
        state = State.RUNNING;
        updateProgressStatus();
        PacketUtilsManager.INSTANCE.moduleFeedback("Fabricator send resumed.");
        tick(client);
    }

    public void stop() {
        stop(sentCount > 0
                ? "Stopped after " + sentCount + "/" + totalCount + " packet(s)."
                : "Send stopped.");
    }

    public void stop(String status) {
        pending.clear();
        state = State.IDLE;
        sentCount = 0;
        totalCount = 0;
        lastSendAtMs = 0L;
        if (status != null && !status.isBlank()) {
            PacketFabricator.INSTANCE.setLastStatus(status);
            PacketUtilsManager.INSTANCE.moduleFeedback(status);
        }
    }

    public void togglePause(MinecraftClient client) {
        if (state == State.RUNNING) {
            pause();
        } else if (state == State.PAUSED) {
            resume(client);
        }
    }

    private void finish(String status) {
        pending.clear();
        state = State.IDLE;
        PacketFabricator.INSTANCE.setLastStatus(status);
        PacketUtilsManager.INSTANCE.moduleFeedback(status);
        sentCount = 0;
        totalCount = 0;
    }

    private void updateProgressStatus() {
        String label = switch (state) {
            case RUNNING -> "Sending " + sentCount + "/" + totalCount + " (" + pending.size() + " left)";
            case PAUSED -> "Paused " + sentCount + "/" + totalCount + " (" + pending.size() + " left)";
            default -> "";
        };
        if (!label.isBlank()) {
            PacketFabricator.INSTANCE.setLastStatus(label);
        }
    }

    private static ScreenHandler resolveHandler(MinecraftClient client) {
        return FabricatorInventorySlots.activeHandler(client);
    }
}
