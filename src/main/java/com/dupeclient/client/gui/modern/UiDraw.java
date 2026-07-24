package com.dupeclient.client.gui.modern;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Low-level drawing: gradients, “glass” overlays, cards, scrollbars — Tailwind-adjacent visuals.
 */
public final class UiDraw {
    private UiDraw() {
    }

    /** Neutral dark backdrop with soft vignette (modern app shell). */
    public static void fillMidnightBackground(GuiGraphics context, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int top = com.dupeclient.client.gui.modern.theme.MidnightPalette.BG_TOP;
        int bot = com.dupeclient.client.gui.modern.theme.MidnightPalette.BG_BOTTOM;
        context.fillGradient(0, 0, w, h, top, bot);
        context.fillGradient(0, 0, w, h / 3, UiTokens.argb(0x18, UiTokens.INDIGO_600), 0x00000000);
        context.fillGradient(0, h * 2 / 3, w, h, 0x00000000, UiTokens.argb(0x28, 0x000000));
    }

    /** Full-screen wash over the blurred vanilla background: slate + soft mint vignette. */
    public static void fillRootGradient(GuiGraphics context, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        context.fillGradient(0, 0, w, h, UiTokens.argb(0xDD, UiTokens.SLATE_950), UiTokens.argb(0xCC, UiTokens.SLATE_900));
        context.fillGradient(0, 0, w, h / 2 + 40, UiTokens.argb(0x28, UiTokens.MINT_600), 0x00000000);
        context.fillGradient(0, h * 2 / 3, w, h, 0x00000000, UiTokens.argb(0x55, UiTokens.SLATE_950));
    }

    /** Raven-like dark backdrop with subtle animated center rails. */
    public static void fillRavenBackdrop(GuiGraphics context, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        float t = (now % 5000L) / 5000.0f;
        int pulse = (int) (42 + 20 * (0.5f + 0.5f * Mth.sin(t * 6.28318f)));
        int accent = (pulse << 24) | 0x3058A5;
        context.fillGradient(0, 0, w, h, UiTokens.argb(0xF2, 0x05080F), UiTokens.argb(0xEA, 0x0A1020));
        context.fillGradient(0, 0, w, h / 2, UiTokens.argb(0x44, 0x1A2440), 0x00000000);
        context.fillGradient(0, h / 2, w, h, 0x00000000, UiTokens.argb(0x44, 0x000000));
        context.fillGradient(0, 0, w, h, accent, 0x00000000);
        int mid = w / 2;
        int top = Math.max(22, h / 4 - 30);
        int bot = Math.min(h - 18, top + 72);
        context.fill(mid - 18, top, mid - 17, bot, UiTokens.argb(0x88, UiTokens.BLUE_500));
        context.fill(mid + 17, top, mid + 18, bot, UiTokens.argb(0x88, UiTokens.BLUE_500));
    }

    /** Small Raven-inspired center wordmark treatment for major screens. */
    public static void drawRavenWordmark(GuiGraphics context, Font tr, int w, int h, String label) {
        if (w <= 0 || h <= 0 || tr == null || label == null || label.isEmpty()) {
            return;
        }
        int qy = h / 4;
        int cx = w / 2;
        String left = label.substring(0, Math.max(1, Math.min(4, label.length())));
        for (int i = 0; i < left.length(); i++) {
            int col = 0xFF8FB2FF;
            context.drawCenteredString(tr, Component.literal(String.valueOf(left.charAt(i))), cx - 22, qy - 22 + i * 10, col);
        }
        context.drawCenteredString(tr, Component.literal("++"), cx + 24, qy + 20, 0xFF82A8FF);
    }

    /** App bar: glass strip under window controls. */
    public static void drawTopFullWidthBand(GuiGraphics context, int w, int barH) {
        if (w <= 0 || barH <= 0) {
            return;
        }
        context.fill(0, 0, w, barH, UiTokens.argb(0xB8, UiTokens.SLATE_900));
        context.fill(0, barH - 1, w, barH, UiTokens.argb(0x44, UiTokens.SLATE_600));
    }

    /** Main content matte behind the scissor. */
    public static void fillContentWorkspace(GuiGraphics context, int x, int y, int w, int h) {
        // Leave empty so the fullscreen Midnight gradient remains visible inside the scroll region.
    }

    /** Non-scrolling title row behind module name. */
    public static void fillModuleHeaderStrip(GuiGraphics context, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        context.fill(x, y, x + w, y + h, com.dupeclient.client.gui.modern.theme.MidnightPalette.alphaRgb(0x40, 0x09090B));
        context.fill(x, y + h - 1, x + w, y + h, com.dupeclient.client.gui.modern.theme.MidnightPalette.BORDER_LIGHT);
    }

    /** Elevated card: fill + hairline border (no drop shadow — avoids black gaps between stacked sections). */
    public static void cardElevated(GuiGraphics c, int x, int y, int w, int h, int radius) {
        if (w <= 0 || h <= 0) {
            return;
        }
        com.dupeclient.client.gui.modern.theme.MidnightShapes.fillRoundedFrame(
                c,
                x,
                y,
                w,
                h,
                radius,
                com.dupeclient.client.gui.modern.theme.MidnightPalette.PANEL_FILL,
                com.dupeclient.client.gui.modern.theme.MidnightPalette.BORDER_LIGHT);
    }

    public static void drawScrollbar(GuiGraphics context, int x, int top, int bottom, double scroll, double maxScroll) {
        if (maxScroll <= 0.5) {
            return;
        }
        int trackH = bottom - top;
        com.dupeclient.client.gui.modern.theme.MidnightShapes.fillRoundedRect(
                context, x, top, 6, trackH, 3, UiTokens.argb(0x55, UiTokens.SLATE_800));
        int thumbH = Math.max(20, (int) (trackH * (trackH / (trackH + maxScroll))));
        int span = Math.max(0, trackH - thumbH);
        double t = scroll / maxScroll;
        t = Mth.clamp(t, 0.0, 1.0);
        int ty = top + (int) (0.5 + span * t);
        com.dupeclient.client.gui.modern.theme.MidnightShapes.fillRoundedRect(
                context, x + 1, ty, 4, thumbH, 2, UiTokens.argb(0xBB, UiTokens.SLATE_400));
    }

    /** Card surface + subtle top highlight + ring. */
    public static void card(GuiGraphics c, int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        c.fill(x, y, x + w, y + h, UiTokens.argb(0xE8, UiTokens.SLATE_800));
        c.fill(x + 1, y + 1, x + w - 1, y + 2, UiTokens.argb(0x40, UiTokens.SLATE_200));
        ring(c, x, y, w, h, UiTokens.argb(0x44, UiTokens.MINT_600));
    }

    public static void ring(GuiGraphics c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col);
        c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y, x + 1, y + h, col);
        c.fill(x + w - 1, y, x + w, y + h, col);
    }

    /** Soft shadow under elevated surfaces (offset down). */
    public static void dropShadow(GuiGraphics c, int x, int y, int w, int h, int spread) {
        int s = Math.max(1, spread);
        c.fill(x - s, y + h, x + w + s, y + h + s, UiTokens.argb(0x55, 0x000000));
        c.fill(x - s + 1, y + h + s, x + w + s - 1, y + h + s + 1, UiTokens.argb(0x22, 0x000000));
    }
}
