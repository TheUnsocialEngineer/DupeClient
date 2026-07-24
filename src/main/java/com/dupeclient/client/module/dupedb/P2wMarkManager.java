package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.core.notify.ClientNotificationHub;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class P2wMarkManager {
    public static final P2wMarkManager INSTANCE = new P2wMarkManager();

    private static final long PENDING_TIMEOUT_MS = 60_000L;
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DupeClient-P2wMark");
        t.setDaemon(true);
        return t;
    });

    private enum PendingKind {
        MARK,
        UNMARK
    }

    private final AtomicInteger generation = new AtomicInteger();
    private PendingKind pendingKind;
    private String pendingServer = "";
    private long pendingStartedAt;
    private static volatile String trackedServer = "";
    private static volatile long trackedJoinedAt;

    private P2wMarkManager() {
    }

    public static void trackServerSession(String server) {
        if (server == null || server.isBlank()) {
            trackedServer = "";
            trackedJoinedAt = 0L;
            return;
        }
        if (!server.equalsIgnoreCase(trackedServer)) {
            trackedServer = server;
            trackedJoinedAt = System.currentTimeMillis();
        }
    }

    public static long sessionDurationMs(String server) {
        if (server.isBlank() || trackedJoinedAt <= 0L || !server.equalsIgnoreCase(trackedServer)) {
            return 0L;
        }
        return Math.max(0L, System.currentTimeMillis() - trackedJoinedAt);
    }

    public void clearPending() {
        pendingKind = null;
        pendingServer = "";
        pendingStartedAt = 0L;
    }

    public void requestMark() {
        String server = currentServerAddress();
        P2wVerification.Result check = P2wVerification.checkMark(true);
        if (!check.ok()) {
            sendFeedback(check.message());
            return;
        }
        pendingKind = PendingKind.MARK;
        pendingServer = server;
        pendingStartedAt = System.currentTimeMillis();
        DupedbP2wScorer.Result score = DupedbManager.INSTANCE.getLastP2wResult();
        int percent = score != null ? score.percent() : -1;
        sendFeedback(DupeMiniMessage.markConfirmPrompt(server, percent, consensusHint()));
    }

    public void requestUnmark() {
        String server = currentServerAddress();
        P2wVerification.Result check = P2wVerification.checkMark(false);
        if (!check.ok()) {
            sendFeedback(check.message());
            return;
        }
        pendingKind = PendingKind.UNMARK;
        pendingServer = server;
        pendingStartedAt = System.currentTimeMillis();
        sendFeedback(DupeMiniMessage.unmarkConfirmPrompt(server, consensusHint()));
    }

    public void confirm(String kindArg) {
        expirePendingIfStale();
        if (pendingKind == null || pendingServer.isBlank()) {
            sendFeedback(Component.literal("No pending P2W action. Use /p2w mark or /p2w unmark first.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        PendingKind expected = "unmark".equalsIgnoreCase(kindArg) ? PendingKind.UNMARK : PendingKind.MARK;
        if (pendingKind != expected) {
            sendFeedback(Component.literal("Pending action mismatch. Start again with /p2w mark or /p2w unmark.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        P2wVerification.Result check = P2wVerification.checkMark(expected == PendingKind.MARK);
        if (!check.ok()) {
            sendFeedback(check.message());
            clearPending();
            return;
        }
        submitPending();
    }

    public void abort() {
        if (pendingKind == null) {
            sendFeedback(Component.literal("Nothing to abort.").withStyle(ChatFormatting.GRAY));
            return;
        }
        clearPending();
        sendFeedback(Component.literal("P2W action cancelled.").withStyle(ChatFormatting.YELLOW));
    }

    private static String consensusHint() {
        return "Requires verified DupeDB scan + playtime. Community consensus applies before listing.";
    }

    private void expirePendingIfStale() {
        if (pendingKind != null && pendingStartedAt > 0L
                && System.currentTimeMillis() - pendingStartedAt > PENDING_TIMEOUT_MS) {
            clearPending();
        }
    }

    private void submitPending() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        PendingKind kind = pendingKind;
        String server = pendingServer;
        clearPending();
        if (kind == null || server.isBlank()) {
            return;
        }
        String uuid = client.player.getUUID().toString();
        String name = client.player.getName().getString();
        int gen = generation.incrementAndGet();
        boolean markAsP2w = kind == PendingKind.MARK;
        P2wPresenceApi.MarkEvidence evidence = new P2wPresenceApi.MarkEvidence(
                P2wVerification.evidenceP2wScore(),
                P2wVerification.evidenceScanCompleted(server),
                P2wVerification.evidencePluginCount(server),
                P2wVerification.evidenceSessionMinutes(server));
        sendFeedback(Component.literal("Submitting verified P2W " + (markAsP2w ? "mark" : "non-P2W mark") + "…").withStyle(ChatFormatting.GRAY));
        EXEC.execute(() -> {
            try {
                P2wPresenceApi.SubmitResult result = P2wPresenceApi.submitMark(server, evidence, markAsP2w, uuid, name);
                if (generation.get() != gen) {
                    return;
                }
                Minecraft mc = Minecraft.getInstance();
                if (mc == null) {
                    return;
                }
                mc.execute(() -> handleSubmitResult(server, markAsP2w, result));
            } catch (Exception e) {
                DupeClient.LOGGER.warn("[P2W] mark submit failed", e);
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.execute(() -> sendFeedback(Component.literal("P2W submit error: " + e.getMessage()).withStyle(ChatFormatting.RED)));
                }
            }
        });
    }

    private void handleSubmitResult(String server, boolean markAsP2w, P2wPresenceApi.SubmitResult result) {
        if (!result.success()) {
            sendFeedback(Component.literal(result.message()).withStyle(ChatFormatting.RED));
            ClientNotificationHub.error(result.message());
            return;
        }
        if ("verified".equals(result.status())) {
            P2wServerPolicy.INSTANCE.refreshRegistryAsync();
            String msg = result.message().isBlank()
                    ? (markAsP2w ? "Server verified as P2W." : "Server verified as non-P2W.")
                    : result.message();
            sendFeedback(Component.literal(msg).withStyle(ChatFormatting.GREEN));
            ClientNotificationHub.success(msg);
            if (!markAsP2w) {
                P2wServerPolicy.INSTANCE.applyPolicyForCurrentServer(true);
            }
            return;
        }
        String pending = result.message().isBlank()
                ? "Mark pending verification (" + result.votes() + "/" + result.required() + " submissions)."
                : result.message();
        sendFeedback(Component.literal(pending).withStyle(ChatFormatting.YELLOW));
        ClientNotificationHub.warn(pending);
        P2wServerPolicy.INSTANCE.refreshRegistryAsync();
    }

    public static String currentServerAddress() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return "";
        }
        if (client.getCurrentServer() != null && client.getCurrentServer().ip != null) {
            return P2wPresenceApi.normalizeServer(client.getCurrentServer().ip);
        }
        if (client.getConnection() != null && client.getConnection().getConnection() != null
                && client.getConnection().getConnection().getRemoteAddress() != null) {
            String raw = client.getConnection().getConnection().getRemoteAddress().toString();
            raw = raw == null ? "" : raw.replaceFirst("^/", "").trim();
            return P2wPresenceApi.normalizeServer(raw);
        }
        return "";
    }

    private static void sendFeedback(Component line) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.player != null) {
                MutableComponent msg = Component.literal("[P2W] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD).append(line);
                client.player.sendSystemMessage(msg);
            }
        });
    }
}
