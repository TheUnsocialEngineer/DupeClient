package com.dupeclient.client.module.fuzzer;

import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerManager;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class SqliFuzzerManager {
    public static final SqliFuzzerManager INSTANCE = new SqliFuzzerManager();

    private final List<String> logs = new ArrayList<>();
    private volatile boolean running;
    private volatile boolean paused;
    private volatile int index;
    private volatile long nextAtMs;

    private SqliFuzzerManager() {
    }

    public EconomyFuzzerSettings settings() {
        return EconomyFuzzerManager.INSTANCE.getSettings();
    }

    public void save() {
        EconomyFuzzerManager.INSTANCE.save();
    }

    public void setCommand(String command) {
        EconomyFuzzerSettings s = settings();
        s.sqliCommand = command == null ? "" : command.trim();
        save();
        String args = CommandEnumerator.describeArgs(s.sqliCommand);
        feedback("SQLI command: /" + s.sqliCommand + " (inject: " + args + ")");
    }

    public String getCommand() {
        return settings().sqliCommand == null ? "" : settings().sqliCommand.trim();
    }

    public String getArgSummary() {
        return CommandEnumerator.describeArgs(getCommand());
    }

    public boolean isDestructivePayloadsEnabled() {
        return settings().sqliDestructivePayloads;
    }

    public void setDestructivePayloadsEnabled(boolean enabled) {
        EconomyFuzzerSettings s = settings();
        if (s.sqliDestructivePayloads == enabled) {
            return;
        }
        s.sqliDestructivePayloads = enabled;
        save();
        feedback("Destructive SQLI payloads " + (enabled ? "ON" : "OFF")
                + " (" + payloadCount() + " probes).");
    }

    public void toggleDestructivePayloads() {
        setDestructivePayloadsEnabled(!isDestructivePayloadsEnabled());
    }

    public int payloadCount() {
        return payloads().size();
    }

    private List<String> payloads() {
        return SqliFuzzerValues.all(settings().sqliDestructivePayloads);
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public int getIndex() {
        return index;
    }

    public int getTotal() {
        return CommandEnumerator.fuzzSteps(getCommand(), payloads().size());
    }

    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    public List<String> getRecentLogs(int max) {
        int from = Math.max(0, logs.size() - max);
        return new ArrayList<>(logs.subList(from, logs.size()));
    }

    public void clearLogs() {
        logs.clear();
    }

    public void start(Minecraft client) {
        if (running) {
            return;
        }
        if (getCommand().isBlank()) {
            feedback("Select a command first.");
            return;
        }
        if (client == null || client.player == null) {
            feedback("Join a server first.");
            return;
        }
        running = true;
        paused = false;
        index = 0;
        nextAtMs = 0L;
        logs.clear();
        int total = getTotal();
        List<String> slots = CommandEnumerator.argSlots(getCommand());
        String mode = slots.isEmpty() ? "append" : slots.size() + " arg slot(s)";
        feedback("SQLI fuzz started (" + total + " steps, " + mode + ").");
    }

    public void stop(String reason) {
        if (!running) {
            return;
        }
        running = false;
        paused = false;
        addLog(reason == null ? "Stopped." : reason);
        feedback(reason == null ? "SQLI fuzz stopped." : reason);
    }

    public void togglePause() {
        if (!running) {
            return;
        }
        paused = !paused;
        feedback(paused ? "SQLI fuzz paused." : "SQLI fuzz resumed.");
    }

    public void tick(Minecraft client) {
        if (!running || paused || client == null || client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextAtMs) {
            return;
        }
        int total = getTotal();
        if (index >= total) {
            stop("SQLI fuzz complete.");
            return;
        }
        String template = getCommand();
        List<String> payloads = payloads();
        int payloadIdx = CommandEnumerator.payloadIndexForStep(index, payloads.size());
        int slotIdx = CommandEnumerator.slotIndexForStep(template, index, payloads.size());
        String payload = payloads.get(payloadIdx);
        String command = CommandEnumerator.injectPayload(template, payload, slotIdx);
        String slotLabel = slotIdx < 0 ? "append" : CommandEnumerator.argSlots(template).get(slotIdx);
        addLog("[" + (index + 1) + "/" + total + "] <" + slotLabel + "> >> " + command);
        client.execute(() -> {
            if (client.player != null && client.player.connection != null) {
                client.player.connection.sendCommand(command);
            }
        });
        index++;
        nextAtMs = now + Math.max(100L, settings().sqliDelayMs);
    }

    public void onIncomingChatLine(String message) {
        if (!running || message == null) {
            return;
        }
        String line = message.strip();
        if (line.isEmpty()) {
            return;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("sql") || lower.contains("syntax") || lower.contains("database")
                || lower.contains("mysql") || lower.contains("sqlite") || lower.contains("jdbc")
                || lower.contains("query") || lower.contains("exception") || lower.contains("error")
                || lower.contains("sqlstate") || lower.contains("hibernate") || lower.contains("preparedstatement")) {
            addLog("<< SUSPECT: " + line);
        }
    }

    private void addLog(String line) {
        logs.add(line);
        if (logs.size() > 80) {
            logs.remove(0);
        }
    }

    private void feedback(String message) {
        EconomyFuzzerSettings s = settings();
        if (!s.moduleChatFeedback) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        MutableComponent line = Component.literal("[SQLI Fuzzer] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(message).withStyle(ChatFormatting.GRAY));
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(line, false);
            }
        });
    }
}
