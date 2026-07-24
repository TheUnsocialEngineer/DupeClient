package com.dupeclient.client.gui.modern.theme;

import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.render.UiNativeRenderer;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Rounded UI primitives backed by anti-aliased GPU textures.
 */
public final class MidnightShapes {
    private MidnightShapes() {
    }

    public static void fillDisk(GuiGraphics c, int cx, int cy, int r, int argb) {
        UiNativeRenderer.fillDisk(c, cx, cy, r, argb);
    }

    public static void fillRoundedRect(GuiGraphics c, int x, int y, int w, int h, int rad, int argb) {
        UiNativeRenderer.fillRoundedRect(c, x, y, w, h, rad, argb);
    }

    public static void fillRoundedFrame(GuiGraphics c, int x, int y, int w, int h, int r, int innerArgb, int borderArgb) {
        int rr = Math.min(r, Math.min(w / 2, h / 2));
        fillRoundedRect(c, x, y, w, h, rr, borderArgb);
        int inset = 1;
        if (w > inset * 2 && h > inset * 2) {
            fillRoundedRect(c, x + inset, y + inset, w - inset * 2, h - inset * 2, Math.max(0, rr - inset), innerArgb);
        }
    }

    /** Full capsule (toggles, small buttons). */
    public static int pillRadius(int h) {
        return Math.max(4, h / 2);
    }

    /** Cards, nav items, inputs — rounded-lg, not full pill. */
    public static int controlRadius(int h) {
        return Math.min(UiTokens.R_LG, Math.max(UiTokens.R_MD, h / 3));
    }

    /** Section / modal cards. */
    public static int surfaceRadius(int w, int h) {
        return UiTokens.R_XL;
    }
}
