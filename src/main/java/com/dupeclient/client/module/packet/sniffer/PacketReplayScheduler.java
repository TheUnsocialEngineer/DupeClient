package com.dupeclient.client.module.packet.sniffer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;

import java.util.ArrayDeque;
import java.util.Deque;

/** Replays queued packets over multiple ticks with optional delay. */
public final class PacketReplayScheduler {
    public static final PacketReplayScheduler INSTANCE = new PacketReplayScheduler();

    private final Deque<QueuedReplay> pending = new ArrayDeque<>();
    private boolean running;
    private long lastSendAtMs;
    private int sent;
    private int total;

    private PacketReplayScheduler() {
    }

    public boolean isRunning() {
        return running;
    }

    public int pendingCount() {
        return pending.size();
    }

    public void tick(MinecraftClient client) {
        if (!running || pending.isEmpty()) {
            if (running && pending.isEmpty()) {
                finish();
            }
            return;
        }
        PacketSnifferSettings settings = PacketSnifferManager.INSTANCE.getSettings();
        int delayMs = Math.max(0, settings.replayDelayMs);
        long now = System.currentTimeMillis();
        if (delayMs > 0 && now - lastSendAtMs < delayMs) {
            return;
        }

        int perTick = Math.max(1, Math.min(100, settings.replayPacketsPerTick));
        int batch = 0;
        while (batch < perTick && !pending.isEmpty()) {
            if (delayMs > 0 && batch > 0 && System.currentTimeMillis() - lastSendAtMs < delayMs) {
                break;
            }
            QueuedReplay next = pending.pollFirst();
            if (next == null) {
                break;
            }
            boolean ok = switch (next.direction) {
                case C2S -> PacketReplayer.sendC2s(client, next.packet);
                case S2C -> PacketReplayer.injectS2c(client, next.packet);
            };
            if (ok) {
                sent++;
            }
            batch++;
            lastSendAtMs = System.currentTimeMillis();
        }
        if (pending.isEmpty()) {
            finish();
        }
    }

    public void queue(PacketSnifferEntry entry, int times) {
        if (entry == null || entry.packet == null || times <= 0) {
            PacketSnifferManager.INSTANCE.feedback("Nothing to queue for replay");
            return;
        }
        if (!entry.canReplay()) {
            PacketSnifferManager.INSTANCE.feedback("Only C2S packets can be replayed");
            return;
        }
        for (int i = 0; i < times; i++) {
            pending.addLast(new QueuedReplay(entry.direction, entry.packet));
        }
        total = pending.size();
        sent = 0;
        running = true;
        PacketSnifferManager.INSTANCE.feedback("Queued " + times + " replay(s) for " + entry.name);
    }

    public void queuePacket(PacketDirection direction, Packet<?> packet, int times) {
        if (packet == null || times <= 0) {
            return;
        }
        if (direction != PacketDirection.C2S) {
            PacketSnifferManager.INSTANCE.feedback("Only C2S packets can be queued");
            return;
        }
        for (int i = 0; i < times; i++) {
            pending.addLast(new QueuedReplay(PacketDirection.C2S, packet));
        }
        total = pending.size();
        sent = 0;
        running = true;
        PacketSnifferManager.INSTANCE.feedback("Queued " + times + " packet(s)");
    }

    public void stop() {
        stop(false);
    }

    /** Stops replay without chat feedback (for periodic module suppressors). */
    public void stopQuietly() {
        stop(true);
    }

    private void stop(boolean quiet) {
        if (!running && pending.isEmpty()) {
            return;
        }
        pending.clear();
        running = false;
        sent = 0;
        total = 0;
        if (!quiet) {
            PacketSnifferManager.INSTANCE.feedback("Replay queue stopped");
        }
    }

    private void finish() {
        PacketSnifferManager.INSTANCE.feedback("Replay finished (" + sent + "/" + total + ")");
        running = false;
        sent = 0;
        total = 0;
    }

    private record QueuedReplay(PacketDirection direction, Packet<?> packet) {
    }
}
