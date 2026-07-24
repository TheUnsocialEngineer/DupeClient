package com.dupeclient.client.module.payall;

import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.module.fuzzer.economy.EconomyCommandDetector;
import com.dupeclient.client.module.packet.FeatureHotkeyManager;
import com.dupeclient.client.module.packet.command.CommandPacketSender;
import com.dupeclient.client.module.security.SecurityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class PayAllManager {
    public static final PayAllManager INSTANCE = new PayAllManager();

    private PayAllSettings settings = new PayAllSettings();
    private final FeatureHotkeyManager overlayHotkeys = new FeatureHotkeyManager();
    private final Object stateLock = new Object();
    private final List<String> logs = new ArrayList<>();
    private final Set<String> manualPlayers = new HashSet<>();
    private final Set<String> excludedPlayers = new HashSet<>();

    private volatile boolean paying;
    private volatile boolean paused;
    private volatile boolean shouldStop;
    private volatile float progress;
    private volatile long delayMs = 1000L;
    private volatile boolean reverseSyntax;
    private volatile String payCommand = "pay";
    private volatile String balanceCommand = "bal";
    private volatile AmountMode amountMode = AmountMode.FIXED_INPUT;
    private volatile String lastError = "";
    private volatile boolean moduleChatFeedback = true;

    private Phase phase = Phase.IDLE;
    private final List<PaymentTarget> paymentQueue = new ArrayList<>();
    private int paymentIndex;
    private long minPayAmount;
    private long maxPayAmount;
    private long balanceWaitDeadlineMs;
    private List<String> pendingTargets = List.of();
    private final Random paymentRandom = new Random();

    private static final Pattern NUMBER_PATTERN = Pattern.compile("([0-9][0-9,._]*(?:\\.[0-9]+)?[kmb]?)", Pattern.CASE_INSENSITIVE);

    private PayAllManager() {
    }

    public void initialize() {
        this.settings = PayAllConfigManager.load();
        this.moduleChatFeedback = this.settings.moduleChatFeedback;
    }

    public PayAllSettings getSettings() {
        return this.settings;
    }

    public void saveSettings() {
        this.settings.moduleChatFeedback = this.moduleChatFeedback;
        PayAllConfigManager.save(this.settings);
    }

    public void tick(Minecraft client) {
        if (client == null) {
            return;
        }
        tickPayment(client);
        if (client.screen == null && this.settings.excludeStaff && client.getWindow() != null) {
            this.syncStaffExclusions(client);
        }
        if (client.getWindow() == null || InputFocusGuards.shouldBlockOverlayToggleHotkeys(client)) {
            return;
        }
        if (this.overlayHotkeys.consumePress(client, this.settings.overlayToggleKey)) {
            PayAllOverlay.INSTANCE.toggleOverlayVisible();
            this.moduleFeedbackConfigToggle("PayAll overlay " + (this.settings.overlayVisible ? "shown" : "hidden"));
        }
    }

    private void tickPayment(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            return;
        }
        if (phase == Phase.IDLE) {
            return;
        }
        if (paused && phase == Phase.PAYING) {
            return;
        }
        if (shouldStop) {
            finishPayment(true);
            return;
        }

        if (phase == Phase.WAITING_BALANCE) {
            if (System.currentTimeMillis() >= balanceWaitDeadlineMs) {
                lastError = "Failed to read balance via /" + balanceCommand;
                finishPayment(true);
            }
            return;
        }

        if (phase != Phase.PAYING || paymentIndex >= paymentQueue.size()) {
            if (phase == Phase.PAYING && paymentIndex >= paymentQueue.size()) {
                finishPayment(false);
            }
            return;
        }

        CommandPacketSender sender = CommandPacketSender.INSTANCE;
        sender.configure(delayMs, 1, Math.max(3, (int) Math.min(12, 60000 / Math.max(1000, delayMs))), 5000);
        if (!sender.isReady()) {
            return;
        }

        PaymentTarget target = paymentQueue.get(paymentIndex);
        String command = buildPayCommand(target.playerName(), target.amount());
        if (sender.sendCommand(client, command)) {
            progress = (float) (paymentIndex + 1) / (float) paymentQueue.size();
            addLog("Paid " + target.playerName() + " " + target.amount());
            paymentIndex++;
        }
    }

    public boolean isExcludeStaff() {
        return this.settings.excludeStaff;
    }

    public void setExcludeStaff(boolean enabled) {
        this.settings.excludeStaff = enabled;
        this.saveSettings();
        this.moduleFeedback("Auto-exclude staff " + (enabled ? "on" : "off"));
    }

    private void syncStaffExclusions(Minecraft client) {
        if (!SecurityManager.INSTANCE.getSettings().staffDetectionEnabled) {
            return;
        }
        for (String name : this.getOnlineTabPlayerNames(client)) {
            if (SecurityManager.INSTANCE.isStaffUsername(client, name)) {
                addExcludedPlayerQuiet(name);
            }
        }
    }

    private void addExcludedPlayerQuiet(String name) {
        if (name == null) {
            return;
        }
        String cleaned = name.trim().toLowerCase(Locale.ROOT);
        if (cleaned.isEmpty()) {
            return;
        }
        synchronized (stateLock) {
            excludedPlayers.add(cleaned);
        }
    }

    public boolean isModuleChatFeedback() {
        return this.moduleChatFeedback;
    }

    public void setModuleChatFeedback(boolean enabled) {
        this.moduleChatFeedback = enabled;
        this.settings.moduleChatFeedback = enabled;
        this.saveSettings();
    }

    public void moduleFeedback(String message) {
        if (!this.moduleChatFeedback) {
            return;
        }
        sendHudLine(payAllPrefix().copy().append(Component.literal(message).withStyle(ChatFormatting.GRAY)));
    }

    public void moduleFeedbackConfigToggle(String message) {
        sendHudLine(payAllPrefix().copy().append(Component.literal(message).withStyle(ChatFormatting.GRAY)));
    }

    private static MutableComponent payAllPrefix() {
        return Component.literal("[PayAll] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private void sendHudLine(Component line) {
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

    private void moduleFeedbackAsync(Minecraft client, String message) {
        if (!this.moduleChatFeedback || client == null) {
            return;
        }
        MutableComponent line = payAllPrefix().copy().append(Component.literal(message).withStyle(ChatFormatting.GRAY));
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(line, false);
            }
        });
    }

    public List<String> getLogs() {
        synchronized (stateLock) {
            return new ArrayList<>(logs);
        }
    }

    public int getManualCount() {
        synchronized (stateLock) {
            return manualPlayers.size();
        }
    }

    public int getExcludedCount() {
        synchronized (stateLock) {
            return excludedPlayers.size();
        }
    }

    public int getOnlineCount(Minecraft client) {
        return resolveTargets(client).size();
    }

    public List<String> getOnlineTabPlayerNames(Minecraft client) {
        List<String> names = new ArrayList<>();
        if (client == null || client.getConnection() == null) {
            return names;
        }
        for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {
            if (entry == null || entry.getProfile() == null) {
                continue;
            }
            String name = entry.getProfile().name();
            if (name == null || name.isBlank()) {
                continue;
            }
            names.add(name);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public boolean isPaying() {
        return paying;
    }

    public boolean isPaused() {
        return paused;
    }

    public float getProgress() {
        return progress;
    }

    public String getLastError() {
        return lastError;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long value) {
        delayMs = Math.max(0L, Math.min(60000L, value));
    }

    public AmountMode getAmountMode() {
        return amountMode;
    }

    public void cycleAmountMode() {
        amountMode = amountMode == AmountMode.FIXED_INPUT ? AmountMode.BALANCE_GAINED : AmountMode.FIXED_INPUT;
        moduleFeedback("Amount mode: " + amountMode.display());
    }

    public boolean isReverseSyntax() {
        return reverseSyntax;
    }

    public void setReverseSyntax(boolean enabled) {
        if (reverseSyntax == enabled) {
            return;
        }
        reverseSyntax = enabled;
        moduleFeedback("Reverse pay syntax " + (enabled ? "on" : "off"));
    }

    public String getPayCommand() {
        return payCommand;
    }

    public void setPayCommand(String command) {
        if (command == null) {
            return;
        }
        String normalized = EconomyCommandDetector.normalizeCommand(command);
        if (!normalized.isEmpty() && !normalized.equalsIgnoreCase(payCommand)) {
            payCommand = normalized;
            moduleFeedback("Pay command set to /" + payCommand);
        }
    }

    public String getBalanceCommand() {
        return balanceCommand;
    }

    public void setBalanceCommand(String command) {
        if (command == null) {
            return;
        }
        String normalized = EconomyCommandDetector.normalizeCommand(command);
        if (!normalized.isEmpty() && !normalized.equalsIgnoreCase(balanceCommand)) {
            balanceCommand = normalized;
            moduleFeedback("Balance command set to /" + balanceCommand);
        }
    }

    public List<String> getPayCommandOptions(Minecraft client) {
        return EconomyCommandDetector.payCommandOptions(client);
    }

    public List<String> getBalanceCommandOptions(Minecraft client) {
        return EconomyCommandDetector.balanceCommandOptions(client);
    }

    public void addManualPlayer(String name) {
        if (name == null) {
            return;
        }
        String cleaned = name.trim();
        if (cleaned.isEmpty()) {
            return;
        }
        boolean added;
        synchronized (stateLock) {
            added = manualPlayers.add(cleaned);
        }
        if (added) {
            moduleFeedback("Added manual target: " + cleaned);
        }
    }

    public void addExcludedPlayer(String name) {
        if (name == null) {
            return;
        }
        String cleaned = name.trim().toLowerCase(Locale.ROOT);
        if (cleaned.isEmpty()) {
            return;
        }
        boolean added;
        synchronized (stateLock) {
            added = excludedPlayers.add(cleaned);
        }
        if (added) {
            moduleFeedback("Excluded player: " + cleaned);
        }
    }

    public void removeExcludedPlayer(String name) {
        if (name == null) {
            return;
        }
        String cleaned = name.trim().toLowerCase(Locale.ROOT);
        if (cleaned.isEmpty()) {
            return;
        }
        boolean removed;
        synchronized (stateLock) {
            removed = excludedPlayers.remove(cleaned);
        }
        if (removed) {
            moduleFeedback("Removed exclusion: " + cleaned);
        }
    }

    public List<String> getIncludedTargetNames(Minecraft client) {
        List<String> names = new ArrayList<>(resolveTargets(client));
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public List<String> getExcludedNamesSorted() {
        synchronized (stateLock) {
            List<String> list = new ArrayList<>(excludedPlayers);
            list.sort(String.CASE_INSENSITIVE_ORDER);
            return list;
        }
    }

    public void clearManualPlayers() {
        int n;
        synchronized (stateLock) {
            n = manualPlayers.size();
            manualPlayers.clear();
        }
        if (n > 0) {
            moduleFeedback("Cleared " + n + " manual player(s).");
        }
    }

    public void clearExcludedPlayers() {
        int n;
        synchronized (stateLock) {
            n = excludedPlayers.size();
            excludedPlayers.clear();
        }
        if (n > 0) {
            moduleFeedback("Cleared " + n + " excluded player(s).");
        }
    }

    public boolean startPaying(Minecraft client, String amountSpec) {
        if (client == null || client.player == null || client.getConnection() == null) {
            lastError = "Not connected.";
            return false;
        }
        if (paying) {
            return false;
        }
        long[] range = parseAmountRange(amountSpec);
        if (range == null) {
            return false;
        }
        List<String> targets = resolveTargets(client);
        if (targets.isEmpty()) {
            lastError = "No target players.";
            return false;
        }

        paying = true;
        paused = false;
        shouldStop = false;
        progress = 0.0f;
        lastError = "";
        paymentIndex = 0;
        paymentQueue.clear();
        CommandPacketSender.INSTANCE.resetBackoff();
        CommandPacketSender.INSTANCE.configure(delayMs, 1, Math.max(3, (int) Math.min(12, 60000 / Math.max(1000, delayMs))), 5000);

        minPayAmount = range[0];
        maxPayAmount = range[1];
        pendingTargets = new ArrayList<>(targets);
        Collections.shuffle(pendingTargets);

        if (amountMode == AmountMode.BALANCE_GAINED) {
            phase = Phase.WAITING_BALANCE;
            balanceWaitDeadlineMs = System.currentTimeMillis() + 3500L;
            addLog("Requesting balance via /" + balanceCommand + "…");
            CommandPacketSender.INSTANCE.sendCommandImmediate(client, balanceCommand);
            moduleFeedback("PayAll reading balance…");
            return true;
        }

        buildPaymentQueue(pendingTargets, minPayAmount, maxPayAmount);
        phase = Phase.PAYING;
        addLog("Starting payall for " + paymentQueue.size() + " players (packet mode).");
        moduleFeedback("PayAll started for " + paymentQueue.size() + " target(s).");
        return true;
    }

    public void togglePause() {
        if (paying && phase == Phase.PAYING) {
            paused = !paused;
            addLog(paused ? "Paused." : "Resumed.");
            moduleFeedback(paused ? "PayAll paused." : "PayAll resumed.");
        }
    }

    public void cancel() {
        if (!paying && phase == Phase.IDLE) {
            shouldStop = true;
            return;
        }
        boolean wasPaying = paying;
        finishPayment(true);
        if (wasPaying) {
            moduleFeedback("PayAll cancelled.");
        }
    }

    public void cancelIfActive() {
        if (paying || phase != Phase.IDLE) {
            cancel();
        }
    }

    public void onSessionLeave() {
        if (paying) {
            cancel();
        }
        PayAllOverlay.INSTANCE.setOverlayVisible(false);
    }

    public void onIncomingChatLine(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (phase == Phase.WAITING_BALANCE) {
            String lower = message.toLowerCase(Locale.ROOT);
            if (lower.contains("balance") || lower.contains("bal") || lower.contains("money") || lower.contains("$")) {
                long value = parseBalanceValue(message);
                if (value > 0L) {
                    int n = pendingTargets.size();
                    long each = Math.max(1L, value / (long) n);
                    addLog("Balance " + value + " split across " + n + " player(s), " + each + " each.");
                    buildPaymentQueue(pendingTargets, each, each);
                    phase = Phase.PAYING;
                    moduleFeedback("PayAll started for " + paymentQueue.size() + " target(s).");
                }
            }
            return;
        }
    }

    private void buildPaymentQueue(List<String> targets, long minAmount, long maxAmount) {
        paymentQueue.clear();
        paymentIndex = 0;
        for (String playerName : targets) {
            long amount = maxAmount <= minAmount
                    ? minAmount
                    : minAmount + paymentRandom.nextLong(maxAmount - minAmount + 1L);
            paymentQueue.add(new PaymentTarget(playerName, amount));
        }
    }

    private String buildPayCommand(String playerName, long amount) {
        return reverseSyntax
                ? payCommand + " " + amount + " " + playerName
                : payCommand + " " + playerName + " " + amount;
    }

    private void finishPayment(boolean cancelled) {
        if (!cancelled && phase == Phase.PAYING) {
            addLog("Payall finished.");
            Minecraft client = Minecraft.getInstance();
            moduleFeedbackAsync(client, "PayAll finished.");
        } else if (cancelled) {
            addLog("Cancelled.");
        }
        paying = false;
        paused = false;
        shouldStop = false;
        progress = 0.0f;
        phase = Phase.IDLE;
        paymentQueue.clear();
        paymentIndex = 0;
        pendingTargets = List.of();
    }

    private List<String> resolveTargets(Minecraft client) {
        Set<String> excluded;
        Set<String> manual;
        synchronized (stateLock) {
            excluded = new HashSet<>(excludedPlayers);
            manual = new HashSet<>(manualPlayers);
        }
        List<String> names = new ArrayList<>();
        if (!manual.isEmpty()) {
            names.addAll(manual);
        } else if (client.getConnection() != null) {
            for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {
                if (entry.getProfile() == null) {
                    continue;
                }
                String name = entry.getProfile().name();
                if (name == null || name.isBlank()) {
                    continue;
                }
                names.add(name);
            }
        }
        if (client.player != null) {
            names.remove(client.player.getGameProfile().name());
        }
        names.removeIf(name -> excluded.contains(name.toLowerCase(Locale.ROOT)));
        if (settings.excludeStaff) {
            names.removeIf(name -> SecurityManager.INSTANCE.isStaffUsername(client, name));
        }
        return new ArrayList<>(new HashSet<>(names));
    }

    private long[] parseAmountRange(String spec) {
        if (spec == null || spec.isBlank()) {
            lastError = "Amount empty.";
            return null;
        }
        String raw = spec.trim().toLowerCase(Locale.ROOT);
        if (!raw.contains("-")) {
            long value = parseShortNumber(raw);
            if (value < 1L) {
                lastError = "Amount must be > 0.";
                return null;
            }
            return new long[]{value, value};
        }
        String[] parts = raw.split("-", 2);
        long min = parseShortNumber(parts[0]);
        long max = parseShortNumber(parts[1]);
        if (min < 1L || max < min) {
            lastError = "Invalid range.";
            return null;
        }
        return new long[]{min, max};
    }

    private long parseShortNumber(String input) {
        String s = input.trim();
        long multiplier = 1L;
        if (s.endsWith("k")) {
            multiplier = 1000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("m")) {
            multiplier = 1_000_000L;
            s = s.substring(0, s.length() - 1);
        } else if (s.endsWith("b")) {
            multiplier = 1_000_000_000L;
            s = s.substring(0, s.length() - 1);
        }
        if (s.isEmpty()) {
            return -1L;
        }
        try {
            if (s.contains(".")) {
                return (long) (Double.parseDouble(s) * multiplier);
            }
            return Long.parseLong(s) * multiplier;
        } catch (NumberFormatException ignored) {
            lastError = "Invalid amount format.";
            return -1L;
        }
    }

    private long parseBalanceValue(String message) {
        Matcher matcher = NUMBER_PATTERN.matcher(message);
        long best = -1L;
        while (matcher.find()) {
            long value = parseShortNumber(matcher.group(1).replace("_", "").replace(",", ""));
            if (value > best) {
                best = value;
            }
        }
        return best;
    }

    private void addLog(String message) {
        synchronized (stateLock) {
            logs.add(message);
            if (logs.size() > 60) {
                logs.remove(0);
            }
        }
    }

    private record PaymentTarget(String playerName, long amount) {
    }

    private enum Phase {
        IDLE,
        WAITING_BALANCE,
        PAYING
    }

    public enum AmountMode {
        FIXED_INPUT("Fixed per player"),
        BALANCE_GAINED("Balance (/bal ÷ online)");

        private final String display;

        AmountMode(String display) {
            this.display = display;
        }

        public String display() {
            return display;
        }
    }
}
