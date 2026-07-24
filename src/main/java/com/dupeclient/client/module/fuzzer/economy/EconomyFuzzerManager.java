package com.dupeclient.client.module.fuzzer.economy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.module.packet.FeatureHotkeyManager;

/**
 * Sends pay commands with fuzzed amount strings and classifies server responses.
 */
public final class EconomyFuzzerManager {
    public static final EconomyFuzzerManager INSTANCE = new EconomyFuzzerManager();



    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "(-?[0-9][0-9,._]*(?:\\.[0-9]+)?[kmb]?)", Pattern.CASE_INSENSITIVE);

    private static final String[] SUCCESS_HINTS = {
            "paid", "sent", "transferred", "gave", "received", "success", "completed", "you paid"
    };
    private static final String[] REJECT_HINTS = {
            "invalid", "error", "cannot", "can't", "failed", "not enough", "insufficient",
            "usage", "unknown", "must be", "too small", "too large", "minimum", "maximum",
            "not a number", "not a valid", "illegal", "denied", "blocked", "nan", "infinity"
    };

    private final Object stateLock = new Object();
    private final List<String> logs = new ArrayList<>();
    private final List<String> fuzzValues = EconomyFuzzerValues.all();
    private final List<String> pendingResponses = new ArrayList<>();

    private EconomyFuzzerSettings settings = new EconomyFuzzerSettings();
    private final FeatureHotkeyManager overlayHotkeys = new FeatureHotkeyManager();
    private boolean textInputFocused;

    private volatile boolean running;
    private volatile boolean paused;
    private volatile int fuzzIndex;
    private volatile long sentAtMs;
    private volatile long lastResponseLineAtMs;
    private volatile long nextActionAtMs;
    private volatile boolean awaitingResponse;

    private EconomyFuzzerManager() {
    }

    public void initialize() {
        settings = EconomyFuzzerConfigManager.load();
        if (settings.targetPlayer == null) {
            settings.targetPlayer = "";
        }
        if (settings.payCommand == null || settings.payCommand.isBlank()) {
            settings.payCommand = "pay";
        }
        migrateSyntaxMode(settings);
    }

    private static void migrateSyntaxMode(EconomyFuzzerSettings settings) {
        if (settings.syntaxMode == null || settings.syntaxMode.isBlank()) {
            settings.syntaxMode = settings.reverseSyntax ? "amount_player" : "auto";
        }
    }

    public void save() {
        EconomyFuzzerConfigManager.save(settings);
    }

    public EconomyFuzzerSettings getSettings() {
        return settings;
    }

    public void setTextInputFocused(boolean focused) {
        textInputFocused = focused;
    }

    public boolean isTextInputFocused() {
        return textInputFocused;
    }

    public List<String> getLogs() {
        synchronized (stateLock) {
            return new ArrayList<>(logs);
        }
    }

    public List<String> getRecentLogs(int max) {
        synchronized (stateLock) {
            int from = Math.max(0, logs.size() - max);
            return new ArrayList<>(logs.subList(from, logs.size()));
        }
    }

    public void clearLogs() {
        synchronized (stateLock) {
            logs.clear();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public int getFuzzIndex() {
        return fuzzIndex;
    }

    public int getFuzzTotal() {
        return fuzzValues.size();
    }

    public float getProgress() {
        if (fuzzValues.isEmpty()) {
            return 0f;
        }
        return Math.min(1f, (float) fuzzIndex / (float) fuzzValues.size());
    }

    public List<String> getPayCommandOptions(MinecraftClient client) {
        return EconomyCommandDetector.payCommandOptions(client);
    }

    public void setPayCommand(String command) {
        settings.payCommand = EconomyCommandDetector.normalizeCommand(command);
        if (settings.payCommand.isBlank()) {
            settings.payCommand = "pay";
        }
        save();
        feedback("Pay command: /" + settings.payCommand + " (" + resolvedSyntax().displayLabel() + ")");
    }

    public EconomyCommandDetector.ResolvedSyntax resolvedSyntax() {
        return EconomyCommandDetector.resolveSyntax(settings.payCommand, settings.syntaxMode);
    }

    public boolean commandNeedsTarget() {
        return resolvedSyntax().needsTarget();
    }

    public void cycleSyntaxMode() {
        settings.syntaxMode = EconomyCommandDetector.cycleSyntaxMode(settings.syntaxMode);
        settings.reverseSyntax = "amount_player".equals(settings.syntaxMode);
        save();
        feedback("Syntax: " + EconomyCommandDetector.syntaxModeLabel(settings.syntaxMode, settings.payCommand));
    }

    public void cyclePayCommand(MinecraftClient client) {
        List<String> options = getPayCommandOptions(client);
        if (options.isEmpty()) {
            return;
        }
        int idx = EconomyCommandDetector.indexOfIgnoreCase(options, settings.payCommand);
        settings.payCommand = options.get((idx + 1) % options.size());
        save();
        feedback("Pay command: /" + settings.payCommand);
    }

    public List<String> onlinePlayerNames(MinecraftClient client) {
        List<String> names = new ArrayList<>();
        if (client == null || client.getNetworkHandler() == null) {
            return names;
        }
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (entry != null && entry.getProfile() != null && entry.getProfile().name() != null) {
                String n = entry.getProfile().name();
                if (!n.isBlank()) {
                    names.add(n);
                }
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public void setTargetPlayer(String name) {
        settings.targetPlayer = name == null ? "" : name.trim();
        save();
        if (settings.targetPlayer.isBlank()) {
            feedback("Target cleared.");
        } else {
            feedback("Target: " + settings.targetPlayer);
        }
    }

    public void cycleTargetPlayer(MinecraftClient client, int delta) {
        List<String> names = onlinePlayerNames(client);
        if (client != null && client.player != null) {
            String self = client.player.getName().getString();
            if (!names.contains(self)) {
                names.add(self);
                names.sort(String.CASE_INSENSITIVE_ORDER);
            }
        }
        if (names.isEmpty()) {
            feedback("No players in tab list.");
            return;
        }
        String cur = settings.targetPlayer == null ? "" : settings.targetPlayer.trim();
        int idx = names.indexOf(cur);
        if (idx < 0) {
            idx = delta >= 0 ? 0 : names.size() - 1;
        } else {
            idx = Math.floorMod(idx + delta, names.size());
        }
        settings.targetPlayer = names.get(idx);
        save();
        feedback("Target: " + settings.targetPlayer);
    }

    public String buildCommandPreview(String amount) {
        return "/" + buildChatCommand(amount);
    }

    public void start(MinecraftClient client) {
        if (running) {
            return;
        }
        if (!settings.enabled) {
            feedback("Enable Economy Fuzzer first.");
            return;
        }
        if (commandNeedsTarget() && (settings.targetPlayer == null || settings.targetPlayer.isBlank())) {
            feedback("Select a target player.");
            return;
        }
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            feedback("Join a world/server first.");
            return;
        }
        running = true;
        paused = false;
        fuzzIndex = 0;
        awaitingResponse = false;
        nextActionAtMs = System.currentTimeMillis();
        addLog("Fuzz run started (" + fuzzValues.size() + " values).");
        feedback("Economy fuzz started.");
    }

    public void stop(String reason) {
        if (!running) {
            return;
        }
        running = false;
        paused = false;
        awaitingResponse = false;
        addLog(reason == null ? "Stopped." : reason);
        feedback(reason == null ? "Economy fuzz stopped." : reason);
    }

    public void togglePause() {
        if (!running) {
            return;
        }
        paused = !paused;
        feedback(paused ? "Fuzz paused." : "Fuzz resumed.");
    }

    public void onSessionLeave() {
        if (settings.disableOnLeave) {
            stop("Stopped (left world/server).");
            com.dupeclient.client.module.fuzzer.FuzzerOverlay.INSTANCE.setOverlayVisible(false);
            com.dupeclient.client.module.fuzzer.CommandArgDiscovery.INSTANCE.stopDiscovery("Left server");
        }
    }

    public void tick(MinecraftClient client) {
        if (client != null && client.getWindow() != null
                && !InputFocusGuards.shouldBlockOverlayToggleHotkeys(client)
                && overlayHotkeys.consumePress(client, settings.overlayToggleKey)) {
            com.dupeclient.client.module.fuzzer.FuzzerOverlay.INSTANCE.toggleOverlayVisible();
            feedback("Fuzzer overlay " + (settings.overlayVisible ? "shown" : "hidden"));
        }
        com.dupeclient.client.module.fuzzer.CommandArgDiscovery.INSTANCE.tick(client);
        com.dupeclient.client.module.fuzzer.SqliFuzzerManager.INSTANCE.tick(client);
        com.dupeclient.client.module.fuzzer.MinimessageFuzzerManager.INSTANCE.tick(client);
        if (!running || client == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (paused) {
            return;
        }
        if (awaitingResponse) {
            if (lastResponseLineAtMs > 0
                    && now - lastResponseLineAtMs >= settings.responseWaitMs
                    && now - sentAtMs >= settings.responseWaitMs) {
                finishCurrentTest();
            } else if (now - sentAtMs >= settings.responseTimeoutMs) {
                synchronized (pendingResponses) {
                    if (pendingResponses.isEmpty()) {
                        pendingResponses.add("(no response)");
                    }
                }
                finishCurrentTest();
            }
            return;
        }
        if (now < nextActionAtMs) {
            return;
        }
        if (fuzzIndex >= fuzzValues.size()) {
            stop("Fuzz complete.");
            return;
        }
        sendCurrent(client);
    }

    public void onIncomingChatLine(String message) {
        if (!running || !awaitingResponse || message == null) {
            return;
        }
        String line = message.strip();
        if (line.isEmpty()) {
            return;
        }
        synchronized (pendingResponses) {
            pendingResponses.add(line);
        }
        lastResponseLineAtMs = System.currentTimeMillis();
    }

    private void sendCurrent(MinecraftClient client) {
        String amount = fuzzValues.get(fuzzIndex);
        String command = buildChatCommand(amount);
        pendingResponses.clear();
        awaitingResponse = true;
        sentAtMs = System.currentTimeMillis();
        lastResponseLineAtMs = 0L;
        addLog("[" + (fuzzIndex + 1) + "/" + fuzzValues.size() + "] >> " + command);
        client.execute(() -> {
            if (client.player != null && client.player.networkHandler != null) {
                client.player.networkHandler.sendChatCommand(command);
            }
        });
    }

    private String buildChatCommand(String amount) {
        String cmd = settings.payCommand.trim();
        return switch (resolvedSyntax()) {
            case AMOUNT_ONLY -> cmd + " " + amount;
            case AMOUNT_PLAYER -> cmd + " " + amount + " " + settings.targetPlayer.trim();
            case PLAYER_AMOUNT -> cmd + " " + settings.targetPlayer.trim() + " " + amount;
        };
    }

    private void finishCurrentTest() {
        List<String> responses;
        synchronized (pendingResponses) {
            responses = new ArrayList<>(pendingResponses);
            pendingResponses.clear();
        }
        String amount = fuzzValues.get(fuzzIndex);
        FuzzVerdict verdict = classify(amount, responses);
        String summary = responses.isEmpty() ? "(no response)" : String.join(" | ", responses);
        if (summary.length() > 120) {
            summary = summary.substring(0, 117) + "...";
        }
        addLog("[" + (fuzzIndex + 1) + "] " + verdict.label + " `" + displayAmount(amount) + "` — " + summary);
        if (verdict.abnormal) {
            addLog("  ^^^ ABNORMAL: " + verdict.detail);
            feedback("Abnormal: " + verdict.label + " for `" + displayAmount(amount) + "`");
        }
        fuzzIndex++;
        awaitingResponse = false;
        nextActionAtMs = System.currentTimeMillis() + settings.delayMs;
    }

    private static String displayAmount(String amount) {
        if (amount.isEmpty()) {
            return "(empty)";
        }
        if (amount.indexOf('\n') >= 0 || amount.indexOf('\r') >= 0) {
            return amount.replace("\r", "\\r").replace("\n", "\\n");
        }
        if (amount.indexOf('\u0000') >= 0) {
            return amount.replace("\u0000", "\\0");
        }
        return amount.length() > 48 ? amount.substring(0, 45) + "..." : amount;
    }

    private FuzzVerdict classify(String amount, List<String> responses) {
        String combined = String.join("\n", responses).toLowerCase(Locale.ROOT);
        boolean success = containsAny(combined, SUCCESS_HINTS);
        boolean reject = containsAny(combined, REJECT_HINTS);
        boolean timeout = combined.contains("(no response)");

        if (timeout) {
            return new FuzzVerdict("TIMEOUT", false, "No chat response before timeout");
        }

        int paidHits = countOccurrences(combined, "paid");
        if (paidHits >= 2) {
            return new FuzzVerdict("DOUBLE_PAY", true, "Multiple pay confirmations in one response window");
        }

        if (looksLikeNegativeCredit(combined)) {
            return new FuzzVerdict("NEGATIVE_CREDIT", true, "Response suggests negative balance or receiving funds while paying");
        }

        if (isDangerousAmount(amount) && success && !reject) {
            return new FuzzVerdict("ACCEPTED_BAD", true, "Server accepted a value that should be rejected");
        }

        if (isDangerousAmount(amount) && !reject && !success) {
            return new FuzzVerdict("SUSPECT", true, "No clear reject or success — verify manually");
        }

        if ((amount.equalsIgnoreCase("nan") || amount.equalsIgnoreCase("infinity")
                || amount.equalsIgnoreCase("-infinity") || amount.equalsIgnoreCase("+infinity"))
                && success) {
            return new FuzzVerdict("NAN_INFINITY_OK", true, "Non-finite amount may have been accepted");
        }

        if (amount.contains("\r\n") && responses.size() > 1) {
            return new FuzzVerdict("LOG_INJECTION", true, "Multiple lines after CRLF amount — check server logs");
        }

        if (success && reject) {
            return new FuzzVerdict("MIXED", true, "Both success and error hints in the same response");
        }

        if (reject) {
            return new FuzzVerdict("REJECTED", false, "Server rejected or errored as expected");
        }

        if (success) {
            return new FuzzVerdict("ACCEPTED", false, "Server reported success (verify amount was sane)");
        }

        return new FuzzVerdict("UNKNOWN", false, "Unclassified response");
    }

    private static boolean isDangerousAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return true;
        }
        String a = amount.trim().toLowerCase(Locale.ROOT);
        if (a.startsWith("-") || a.equals("nan") || a.contains("infinity")) {
            return true;
        }
        if (a.contains("\u0000") || a.contains("\r") || a.contains("\n")) {
            return true;
        }
        if (a.startsWith("$") || a.startsWith("€") || a.startsWith("£") || a.contains(",")) {
            return true;
        }
        if (a.contains("e+") || a.contains("e-") || a.contains("e")) {
            return true;
        }
        return a.length() > 32;
    }

    private static boolean looksLikeNegativeCredit(String combined) {
        if (combined.contains("unknown command") || combined.contains("incomplete command")
                || combined.contains("invalid syntax") || combined.contains("correct usage")
                || combined.contains("usage:") || combined.contains("usage is")) {
            return false;
        }
        if (combined.contains("negative") || combined.contains("-$") || combined.contains("received")) {
            return true;
        }
        var m = NUMBER_PATTERN.matcher(combined);
        while (m.find()) {
            String raw = m.group(1).replace(",", "");
            try {
                if (raw.startsWith("-")) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static boolean containsAny(String haystack, String[] needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private static int countOccurrences(String haystack, String word) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(word, idx)) >= 0) {
            count++;
            idx += word.length();
        }
        return count;
    }

    private void addLog(String line) {
        synchronized (stateLock) {
            logs.add(line);
            while (logs.size() > 500) {
                logs.remove(0);
            }
        }
    }

    public void feedback(String message) {
        if (!settings.moduleChatFeedback) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        MutableText line = Text.literal("[EconomyFuzzer] ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal(message).formatted(Formatting.GRAY));
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(line, false);
            }
        });
    }

    private record FuzzVerdict(String label, boolean abnormal, String detail) {
    }
}
