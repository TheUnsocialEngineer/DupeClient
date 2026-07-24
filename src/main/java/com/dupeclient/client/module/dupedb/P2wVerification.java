package com.dupeclient.client.module.dupedb;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class P2wVerification {
    private static final long MIN_SESSION_MS = 5L * 60_000L;
    private static final long SCAN_MAX_AGE_MS = 24L * 60 * 60_000L;
    private static final int MIN_PLUGINS_MARK = 1;
    private static final int MIN_SCORE_MARK = 20;
    private static final int MAX_SCORE_UNMARK = 15;

    private P2wVerification() {
    }

    public record Result(boolean ok, String code, Text message) {
        static Result pass() {
            return new Result(true, "ok", Text.empty());
        }

        static Result fail(String code, String message) {
            return new Result(false, code, Text.literal(message).formatted(Formatting.RED));
        }
    }

    public static Result checkMark(boolean markAsP2w) {
        String server = P2wMarkManager.currentServerAddress();
        if (server.isBlank()) {
            return Result.fail("no_server", "Join a multiplayer server first.");
        }
        long sessionMs = P2wMarkManager.sessionDurationMs(server);
        if (sessionMs < MIN_SESSION_MS) {
            long need = (MIN_SESSION_MS - sessionMs) / 60_000L + 1L;
            return Result.fail("session_short", "Play on this server at least " + need + " more minute(s) before marking.");
        }
        if (!DupedbManager.INSTANCE.hasRecentScanForServer(server)) {
            return Result.fail("scan_required", "Run a DupeDB plugin scan on this server first (/dupedb scan or overlay).");
        }
        int plugins = DupedbManager.INSTANCE.lastScanPluginCountForServer(server);
        if (markAsP2w && plugins < MIN_PLUGINS_MARK) {
            return Result.fail("no_plugins", "Scan must discover at least one plugin before marking as P2W.");
        }
        DupedbP2wScorer.Result score = DupedbManager.INSTANCE.getLastP2wResult();
        int percent = score != null ? score.percent() : -1;
        if (markAsP2w) {
            if (percent < MIN_SCORE_MARK) {
                return Result.fail("score_low", "P2W score must be at least " + MIN_SCORE_MARK + "% (current: "
                        + (percent >= 0 ? percent + "%" : "none — enable Generate P2W score in overlay") + ").");
            }
        } else if (percent > MAX_SCORE_UNMARK) {
            return Result.fail("score_high", "Non-P2W marks require P2W score ≤ " + MAX_SCORE_UNMARK + "% (current: " + percent + "%).");
        }
        return Result.pass();
    }

    public static int evidencePluginCount(String server) {
        return DupedbManager.INSTANCE.lastScanPluginCountForServer(server);
    }

    public static int evidenceP2wScore() {
        DupedbP2wScorer.Result score = DupedbManager.INSTANCE.getLastP2wResult();
        return score != null ? score.percent() : -1;
    }

    public static boolean evidenceScanCompleted(String server) {
        return DupedbManager.INSTANCE.hasRecentScanForServer(server);
    }

    public static int evidenceSessionMinutes(String server) {
        return (int) (P2wMarkManager.sessionDurationMs(server) / 60_000L);
    }

    static long scanMaxAgeMs() {
        return SCAN_MAX_AGE_MS;
    }
}
