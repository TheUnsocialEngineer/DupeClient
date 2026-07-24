package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.core.notify.ClientNotificationHub;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
            sendFeedback(Text.literal("No pending P2W action. Use /p2w mark or /p2w unmark first.").formatted(Formatting.YELLOW));
            return;
        }
        PendingKind expected = "unmark".equalsIgnoreCase(kindArg) ? PendingKind.UNMARK : PendingKind.MARK;
        if (pendingKind != expected) {
            sendFeedback(Text.literal("Pending action mismatch. Start again with /p2w mark or /p2w unmark.").formatted(Formatting.YELLOW));
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
            sendFeedback(Text.literal("Nothing to abort.").formatted(Formatting.GRAY));
            return;
        }
        clearPending();
        sendFeedback(Text.literal("P2W action cancelled.").formatted(Formatting.YELLOW));
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        PendingKind kind = pendingKind;
        String server = pendingServer;
        clearPending();
        if (kind == null || server.isBlank()) {
            return;
        }
        String uuid = client.player.getUuid().toString();
        String name = client.player.getName().getString();
        int gen = generation.incrementAndGet();
        boolean markAsP2w = kind == PendingKind.MARK;
        P2wPresenceApi.MarkEvidence evidence = new P2wPresenceApi.MarkEvidence(
                P2wVerification.evidenceP2wScore(),
                P2wVerification.evidenceScanCompleted(server),
                P2wVerification.evidencePluginCount(server),
                P2wVerification.evidenceSessionMinutes(server));
        sendFeedback(Text.literal("Submitting verified P2W " + (markAsP2w ? "mark" : "non-P2W mark") + "…").formatted(Formatting.GRAY));
        EXEC.execute(() -> {
            try {
                P2wPresenceApi.SubmitResult result = P2wPresenceApi.submitMark(server, evidence, markAsP2w, uuid, name);
                if (generation.get() != gen) {
                    return;
                }
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc == null) {
                    return;
                }
                mc.execute(() -> handleSubmitResult(server, markAsP2w, result));
            } catch (Exception e) {
                DupeClient.LOGGER.warn("[P2W] mark submit failed", e);
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.execute(() -> sendFeedback(Text.literal("P2W submit error: " + e.getMessage()).formatted(Formatting.RED)));
                }
            }
        });
    }

    private void handleSubmitResult(String server, boolean markAsP2w, P2wPresenceApi.SubmitResult result) {
        if (!result.success()) {
            sendFeedback(Text.literal(result.message()).formatted(Formatting.RED));
            ClientNotificationHub.error(result.message());
            return;
        }
        if ("verified".equals(result.status())) {
            P2wServerPolicy.INSTANCE.refreshRegistryAsync();
            String msg = result.message().isBlank()
                    ? (markAsP2w ? "Server verified as P2W." : "Server verified as non-P2W.")
                    : result.message();
            sendFeedback(Text.literal(msg).formatted(Formatting.GREEN));
            ClientNotificationHub.success(msg);
            if (!markAsP2w) {
                P2wServerPolicy.INSTANCE.applyPolicyForCurrentServer(true);
            }
            return;
        }
        String pending = result.message().isBlank()
                ? "Mark pending verification (" + result.votes() + "/" + result.required() + " submissions)."
                : result.message();
        sendFeedback(Text.literal(pending).formatted(Formatting.YELLOW));
        ClientNotificationHub.warn(pending);
        P2wServerPolicy.INSTANCE.refreshRegistryAsync();
    }

    public static String currentServerAddress() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return "";
        }
        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address != null) {
            return P2wPresenceApi.normalizeServer(client.getCurrentServerEntry().address);
        }
        if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null
                && client.getNetworkHandler().getConnection().getAddress() != null) {
            String raw = client.getNetworkHandler().getConnection().getAddress().toString();
            raw = raw == null ? "" : raw.replaceFirst("^/", "").trim();
            return P2wPresenceApi.normalizeServer(raw);
        }
        return "";
    }

    private static void sendFeedback(Text line) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.player != null) {
                MutableText msg = Text.literal("[P2W] ").formatted(Formatting.GOLD, Formatting.BOLD).append(line);
                client.player.sendMessage(msg, false);
            }
        });
    }
}
