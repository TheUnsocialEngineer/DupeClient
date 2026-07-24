package com.dupeclient.client.module.mcptools;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class McpToolsBotHandle {
    public enum State {
        CONNECTING,
        CONNECTED,
        STOPPED
    }

    public final String id;
    public final String username;
    public volatile boolean selected;
    public volatile State state = State.CONNECTING;
    public volatile boolean throttleRetryUsed;
    public volatile boolean joinRetryCancelled;

    private final McpToolsBotSession session = new McpToolsBotSession();
    private final McpToolsBotAuthHandler auth = new McpToolsBotAuthHandler();
    private final AtomicBoolean joinPhaseDone = new AtomicBoolean(false);
    private volatile CountDownLatch joinLatch = new CountDownLatch(1);
    private final AtomicBoolean connectionSlotReleased = new AtomicBoolean(false);

    public McpToolsBotHandle(String username) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.username = username;
        this.selected = true;
    }

    public McpToolsBotSession session() {
        return session;
    }

    public McpToolsBotAuthHandler auth() {
        return auth;
    }

    public boolean isActive() {
        return session.isActive();
    }

    public void resetJoinPhase() {
        joinPhaseDone.set(false);
        joinLatch = new CountDownLatch(1);
        connectionSlotReleased.set(false);
    }

    /** Release the global join slot once per handshake attempt. */
    public boolean releaseConnectionSlotOnce() {
        return connectionSlotReleased.compareAndSet(false, true);
    }

    public void completeJoinPhase() {
        if (joinPhaseDone.compareAndSet(false, true)) {
            joinLatch.countDown();
        }
    }

    public boolean awaitJoinPhase(long timeoutMs) {
        CountDownLatch latch = joinLatch;
        if (latch == null || joinPhaseDone.get()) {
            return true;
        }
        try {
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
