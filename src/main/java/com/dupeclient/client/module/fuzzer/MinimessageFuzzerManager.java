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

public final class MinimessageFuzzerManager {
    public static final MinimessageFuzzerManager INSTANCE = new MinimessageFuzzerManager();

    private final List<String> logs = new ArrayList<>();
    private final List<String> templates = MinimessageFuzzerValues.all();
    private volatile boolean running;
    private volatile boolean paused;
    private volatile int index;
    private volatile long nextAtMs;

    private MinimessageFuzzerManager() {
    }

    public EconomyFuzzerSettings settings() {
        return EconomyFuzzerManager.INSTANCE.getSettings();
    }

    public void save() {
        EconomyFuzzerManager.INSTANCE.save();
    }

    public void setTarget(String name) {
        EconomyFuzzerSettings s = settings();
        s.minimessageTarget = name == null ? "" : name.trim();
        save();
    }

    public String getTarget() {
        String t = settings().minimessageTarget;
        return t == null ? "" : t.trim();
    }

    public boolean isMsgMode() {
        String mode = settings().minimessageSendMode;
        return mode == null || !"chat".equalsIgnoreCase(mode.trim());
    }

    public String getMsgCommand() {
        String cmd = settings().minimessageMsgCommand;
        if (cmd == null || cmd.isBlank()) {
            return "msg";
        }
        String trimmed = cmd.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    public String sendModeLabel() {
        return isMsgMode() ? "/" + getMsgCommand() : "Chat";
    }

    public void toggleSendMode() {
        EconomyFuzzerSettings s = settings();
        s.minimessageSendMode = isMsgMode() ? "chat" : "msg";
        save();
        feedback("MiniMessage send: " + sendModeLabel());
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
        return templates.size();
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
        if (client == null || client.player == null) {
            feedback("Join a server first.");
            return;
        }
        if (isMsgMode() && getTarget().isBlank()) {
            feedback("Select a target player for /" + getMsgCommand() + " mode.");
            return;
        }
        running = true;
        paused = false;
        index = 0;
        nextAtMs = 0L;
        logs.clear();
        feedback("MiniMessage fuzz started (" + templates.size() + " probes, " + sendModeLabel() + ").");
    }

    public void stop(String reason) {
        if (!running) {
            return;
        }
        running = false;
        paused = false;
        addLog(reason == null ? "Stopped." : reason);
        feedback(reason == null ? "MiniMessage fuzz stopped." : reason);
    }

    public void togglePause() {
        if (!running) {
            return;
        }
        paused = !paused;
        feedback(paused ? "MiniMessage fuzz paused." : "MiniMessage fuzz resumed.");
    }

    public void tick(Minecraft client) {
        if (!running || paused || client == null || client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextAtMs) {
            return;
        }
        if (index >= templates.size()) {
            stop("MiniMessage fuzz complete.");
            return;
        }
        String template = templates.get(index);
        String message = MinimessageFuzzerValues.formatForTarget(template, getTarget());
        String logLine = isMsgMode()
                ? "/" + getMsgCommand() + " " + getTarget() + " " + message
                : message;
        addLog("[" + (index + 1) + "/" + templates.size() + "] >> " + truncate(logLine, 120));
        final boolean msgMode = isMsgMode();
        final String cmd = getMsgCommand();
        final String target = getTarget();
        final String payload = message;
        client.execute(() -> {
            if (client.player != null && client.player.connection != null) {
                if (msgMode) {
                    client.player.connection.sendCommand(cmd + " " + target + " " + payload);
                } else {
                    client.player.connection.sendChat(payload);
                }
            }
        });
        index++;
        nextAtMs = now + Math.max(100L, settings().minimessageDelayMs);
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
        if (lower.contains("<click") || lower.contains("run_command") || lower.contains("hover:")
                || lower.contains("minimessage") || lower.contains("illegal tag")
                || lower.contains("invalid") || lower.contains("parse")) {
            addLog("<< TAG_LEAK: " + line);
        }
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 3) + "...";
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
        MutableComponent line = Component.literal("[MM Fuzzer] ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal(message).withStyle(ChatFormatting.GRAY));
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(line, false);
            }
        });
    }
}
