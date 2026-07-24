package com.dupeclient.client.module.macro;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;

public final class MacroScheduler {
    private static final MacroScheduler INSTANCE = new MacroScheduler();
    private final Deque<String> queue = new ArrayDeque<>();
    private String runningId;
    private boolean waitingForFinish;

    private MacroScheduler() {
    }

    public static MacroScheduler getInstance() {
        return INSTANCE;
    }

    public void enqueue(String macroId) {
        if (macroId == null || macroId.isBlank()) {
            return;
        }
        synchronized (queue) {
            if (!macroId.equals(runningId)) {
                queue.addLast(macroId);
            }
        }
    }

    public void enqueueAll(Iterable<String> ids) {
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            enqueue(id);
        }
    }

    public void clear() {
        synchronized (queue) {
            queue.clear();
            runningId = null;
            waitingForFinish = false;
        }
    }

    public int pendingCount() {
        synchronized (queue) {
            return queue.size() + (runningId != null ? 1 : 0);
        }
    }

    public String statusLine() {
        synchronized (queue) {
            if (runningId == null && queue.isEmpty()) {
                return "";
            }
            return "Macro queue: " + (runningId != null ? runningId + " + " : "") + queue.size() + " pending";
        }
    }

    public void tick(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        MacroRuntime runtime = MacroRuntime.INSTANCE;
        synchronized (queue) {
            if (runningId != null) {
                if (!runtime.isRunning()) {
                    runningId = null;
                    waitingForFinish = false;
                } else {
                    return;
                }
            }
            if (queue.isEmpty()) {
                return;
            }
            String next = queue.pollFirst();
            if (next == null || next.isBlank()) {
                return;
            }
            runningId = next;
            waitingForFinish = true;
            runtime.start(client, next);
        }
    }
}
