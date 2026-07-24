package com.dupeclient.client.module.utility;

import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import com.dupeclient.client.module.packet.FeatureHotkeyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ChatGamesManager {
    public static final ChatGamesManager INSTANCE = new ChatGamesManager();

    private ChatGamesSettings settings = new ChatGamesSettings();
    private final FeatureHotkeyManager hotkeys = new FeatureHotkeyManager();
    private final FeatureHotkeyManager overlayHotkeys = new FeatureHotkeyManager();
    private boolean textInputFocused;
    private long lastAnswerTime;

    private ChatGamesManager() {
    }

    public void initialize() {
        settings = ChatGamesConfigManager.load();
    }

    public ChatGamesSettings getSettings() {
        return settings;
    }

    public void save() {
        ChatGamesConfigManager.save(settings);
    }

    public void setTextInputFocused(boolean focused) {
        textInputFocused = focused;
    }

    public void tick(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        if (!InputFocusGuards.shouldBlockOverlayToggleHotkeys(client)) {
            if (overlayHotkeys.consumePress(client, settings.overlayToggleKey)) {
                ChatGamesOverlay.INSTANCE.toggleOverlayVisible();
                feedbackConfigToggle("Chat Games overlay " + (settings.overlayVisible ? "shown" : "hidden"));
            }
        }
        if (!textInputFocused) {
            if (hotkeys.consumePress(client, settings.toggleKey)) {
                setEnabled(!settings.enabled);
            }
        }
    }

    public void setEnabled(boolean enabled) {
        if (enabled && P2wServerPolicy.INSTANCE.isModulesLocked()) {
            feedback("Modules locked on non-P2W server.");
            return;
        }
        if (settings.enabled == enabled) {
            return;
        }
        settings.enabled = enabled;
        if (!enabled) {
            ChatGamesOverlay.INSTANCE.setOverlayVisible(false);
        }
        feedback("Chat Games " + (enabled ? "enabled" : "disabled"));
        save();
    }

    public void forceDisable() {
        if (settings.enabled) {
            settings.enabled = false;
            save();
        }
    }

    public void onSessionLeave() {
        if (settings.disableOnLeave && settings.enabled) {
            settings.enabled = false;
        }
        ChatGamesOverlay.INSTANCE.setOverlayVisible(false);
    }

    public void onIncomingGameMessage(String raw) {
        if (!settings.enabled || raw == null || raw.isBlank()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.getConnection() == null) {
            return;
        }

        String plain = ChatGamesSolver.stripPlain(raw);
        if (ChatGamesSolver.isOwnFeedback(plain)) {
            return;
        }

        if (settings.wordGames) {
            String word = ChatGamesSolver.extractWriteOutWord(plain);
            if (word != null) {
                if (!tryConsumeCooldown()) {
                    return;
                }
                sendAnswer(client, word);
                if (settings.chatFeedback) {
                    feedback("Sent word: " + word);
                }
                return;
            }
        }

        if (!settings.mathsOnly) {
            return;
        }

        Double symbolX = ChatGamesSolver.trySolveForX(plain);
        if (symbolX != null) {
            if (!tryConsumeCooldown()) {
                return;
            }
            String answer = ChatGamesSolver.formatAnswer(symbolX);
            sendAnswer(client, answer);
            if (settings.chatFeedback) {
                feedback("Solved for x: " + answer);
            }
            return;
        }

        String expr = ChatGamesSolver.extractMathExpression(plain);
        if (expr == null) {
            return;
        }
        Double result = ChatGamesSolver.evaluateMath(expr);
        if (result == null) {
            return;
        }
        if (!tryConsumeCooldown()) {
            return;
        }
        String answer = ChatGamesSolver.formatAnswer(result);
        sendAnswer(client, answer);
        if (settings.chatFeedback) {
            feedback("Answered: " + expr.trim() + " = " + answer);
        }
    }

    private boolean tryConsumeCooldown() {
        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(1, settings.cooldownSeconds) * 1000L;
        if (lastAnswerTime != 0 && (now - lastAnswerTime) < cooldownMs) {
            return false;
        }
        lastAnswerTime = now;
        return true;
    }

    private static void sendAnswer(Minecraft client, String message) {
        client.execute(() -> {
            if (client.player != null && client.getConnection() != null) {
                client.getConnection().sendChat(message);
            }
        });
    }

    public void feedback(String message) {
        if (!settings.chatFeedback || message == null || message.isBlank()) {
            return;
        }
        sendFeedbackLine(message);
    }

    public void feedbackConfigToggle(String message) {
        sendFeedbackLine(message);
    }

    private void sendFeedbackLine(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        Component line = Component.literal("[Chat Games] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal(message).withStyle(ChatFormatting.GRAY));
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(line);
            }
        });
    }
}
