package com.dupeclient.client.module.packet.command;

import com.dupeclient.client.module.fuzzer.economy.EconomyCommandDetector;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Sends slash commands as {@link CommandExecutionC2SPacket} (bypassing vanilla chat UI) with
 * pacing, burst limits, and adaptive backoff when servers reject spam.
 */
public final class CommandPacketSender {
    public static final CommandPacketSender INSTANCE = new CommandPacketSender();

    private static final List<String> RATE_LIMIT_HINTS = List.of(
            "wait before using",
            "slow down",
            "too fast",
            "too many",
            "spam",
            "cooldown",
            "rate limit",
            "rate-limit",
            "throttle",
            "try again",
            "please wait",
            "command spam",
            "muted",
            "blocked command");

    private static final List<String> SUCCESS_HINTS = List.of(
            "paid",
            "sent",
            "transferred",
            "received",
            "success",
            "balance");

    private long baseIntervalMs = 350L;
    private long effectiveIntervalMs = 350L;
    private long lastSendMs;
    private long backoffUntilMs;
    private int consecutiveRateLimits;
    private int maxPerTick = 1;
    private int maxInWindow = 6;
    private int windowMs = 5000;
    private int sentThisTick;
    private final Deque<Long> recentSends = new ArrayDeque<>();

    private CommandPacketSender() {
    }

    /** Call once per client tick before any send attempts. */
    public void beginTick() {
        sentThisTick = 0;
    }

    public void configure(long minIntervalMs, int perTickCap, int burstCap, int burstWindowMs) {
        baseIntervalMs = Math.max(0L, minIntervalMs);
        effectiveIntervalMs = baseIntervalMs;
        maxPerTick = Math.max(1, Math.min(10, perTickCap));
        maxInWindow = Math.max(1, Math.min(60, burstCap));
        windowMs = Math.max(1000, Math.min(60000, burstWindowMs));
    }

    public void resetBackoff() {
        consecutiveRateLimits = 0;
        backoffUntilMs = 0L;
        effectiveIntervalMs = baseIntervalMs;
        recentSends.clear();
    }

    public boolean isReady() {
        long now = System.currentTimeMillis();
        if (now < backoffUntilMs) {
            return false;
        }
        if (sentThisTick >= maxPerTick) {
            return false;
        }
        if (now - lastSendMs < effectiveIntervalMs) {
            return false;
        }
        pruneWindow(now);
        return recentSends.size() < maxInWindow;
    }

    public long msUntilReady() {
        long now = System.currentTimeMillis();
        long wait = 0L;
        if (now < backoffUntilMs) {
            wait = Math.max(wait, backoffUntilMs - now);
        }
        long intervalWait = effectiveIntervalMs - (now - lastSendMs);
        if (intervalWait > 0) {
            wait = Math.max(wait, intervalWait);
        }
        pruneWindow(now);
        if (recentSends.size() >= maxInWindow && !recentSends.isEmpty()) {
            long oldest = recentSends.peekFirst();
            long windowWait = windowMs - (now - oldest);
            if (windowWait > 0) {
                wait = Math.max(wait, windowWait);
            }
        }
        return wait;
    }

    /** Sends when pacing allows; returns false if throttled this tick. */
    public boolean sendCommand(MinecraftClient client, String command) {
        if (!isReady()) {
            return false;
        }
        dispatch(client, command);
        recordSend();
        return true;
    }

    /** Sends immediately (still uses command packet); skips interval but respects active backoff. */
    public boolean sendCommandImmediate(MinecraftClient client, String command) {
        long now = System.currentTimeMillis();
        if (now < backoffUntilMs) {
            return false;
        }
        dispatch(client, command);
        recordSend();
        return true;
    }

    public void onIncomingChatLine(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (containsAny(lower, RATE_LIMIT_HINTS)) {
            consecutiveRateLimits = Math.min(8, consecutiveRateLimits + 1);
            long extra = Math.min(30_000L, baseIntervalMs + 500L * consecutiveRateLimits);
            backoffUntilMs = System.currentTimeMillis() + extra;
            effectiveIntervalMs = Math.min(10_000L, effectiveIntervalMs + 200L);
            return;
        }
        if (consecutiveRateLimits > 0 && containsAny(lower, SUCCESS_HINTS)) {
            consecutiveRateLimits = Math.max(0, consecutiveRateLimits - 1);
            effectiveIntervalMs = Math.max(baseIntervalMs, effectiveIntervalMs - 100L);
        }
    }

    private void dispatch(MinecraftClient client, String command) {
        if (client == null || command == null) {
            return;
        }
        String normalized = EconomyCommandDetector.normalizeCommand(command);
        if (normalized.isEmpty()) {
            return;
        }
        client.execute(() -> {
            if (client.player != null && client.getNetworkHandler() != null) {
                PacketUtilsManager.INSTANCE.sendBypass(client, new CommandExecutionC2SPacket(normalized));
            }
        });
    }

    private void recordSend() {
        long now = System.currentTimeMillis();
        lastSendMs = now;
        sentThisTick++;
        recentSends.addLast(now);
        pruneWindow(now);
    }

    private void pruneWindow(long now) {
        while (!recentSends.isEmpty() && now - recentSends.peekFirst() > windowMs) {
            recentSends.pollFirst();
        }
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
