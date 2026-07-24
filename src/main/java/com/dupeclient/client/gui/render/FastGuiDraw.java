package com.dupeclient.client.gui.render;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Low-allocation GUI line drawing (2px strokes, fewer {@code fill} calls than 1×1 rasterization).
 */
public final class FastGuiDraw {
    private FastGuiDraw() {
    }

    public static void drawLine(GuiGraphics context, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        int guard = Math.min(4096, dx + dy + 8);
        for (int i = 0; i < guard; i++) {
            context.fill(x, y, x + 2, y + 2, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
}
