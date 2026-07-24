package com.dupeclient.client.core.notify;

import com.dupeclient.client.gui.modern.UiTokens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ClientNotificationHub {
    private static final Deque<Toast> TOASTS = new ArrayDeque<>();
    private static final int MAX_TOASTS = 5;
    private static final long TOAST_MS = 4_500L;

    private ClientNotificationHub() {
    }

    public record Toast(String message, int color, long expiresAtMs) {
    }

    public static void info(String message) {
        push(message, UiTokens.SLATE_200);
    }

    public static void success(String message) {
        push(message, UiTokens.EMERALD_500);
    }

    public static void warn(String message) {
        push(message, 0xFFFFC857);
    }

    public static void error(String message) {
        push(message, 0xFFFF6B6B);
    }

    private static void push(String message, int color) {
        if (message == null || message.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        TOASTS.addFirst(new Toast(message, color, now + TOAST_MS));
        while (TOASTS.size() > MAX_TOASTS) {
            TOASTS.removeLast();
        }
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        TOASTS.removeIf(t -> t.expiresAtMs() <= now);
    }

    public static void render(DrawContext context, TextRenderer tr, int screenW) {
        if (TOASTS.isEmpty()) {
            return;
        }
        int y = 8;
        int i = 0;
        for (Toast toast : TOASTS) {
            if (i >= MAX_TOASTS) {
                break;
            }
            int w = Math.min(screenW - 16, tr.getWidth(toast.message()) + 16);
            int x = screenW - w - 8;
            context.fill(x - 2, y - 2, x + w + 2, y + 12, UiTokens.argb(0xCC, 0x0F172A));
            context.drawTextWithShadow(tr, Text.literal(toast.message()), x + 6, y, toast.color());
            y += 14;
            i++;
        }
    }

    public static void notifyIfInGame(String message, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        push(message, color);
    }
}
