package com.dupeclient.client.gui.modern;

import com.dupeclient.client.gui.modern.theme.MidnightPalette;
import com.dupeclient.client.gui.modern.theme.MidnightShapes;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Shared midnight-style chrome for single-line text inputs (widgets, panels, overlays).
 */
public final class ModernTextInputChrome {
    public static final int PAD_X = 8;
    public static final int PAD_Y = 5;
    public static final int MIN_HEIGHT = 20;
    public static final int TEXT_COLOR = MidnightPalette.TEXT_PRIMARY;
    public static final int PLACEHOLDER_COLOR = MidnightPalette.TEXT_MUTED;
    public static final int CARET_COLOR = 0xFF93C5FD;
    public static final int SELECTION_COLOR = 0x883B82F6;

    private static final int FILL = 0xFF0A0E14;
    private static final int FILL_FOCUS = 0xFF121A26;
    private static final int BORDER = MidnightPalette.BORDER_LIGHT;
    private static final int BORDER_FOCUS = MidnightPalette.BORDER_FOCUS;
    private static final int FOCUS_GLOW = 0x5560A5FA;

    private ModernTextInputChrome() {
    }

    public static void drawField(DrawContext context, int x, int y, int w, int h, boolean focused) {
        int height = Math.max(MIN_HEIGHT, h);
        int radius = MidnightShapes.controlRadius(height);
        if (focused) {
            MidnightShapes.fillRoundedRect(context, x - 1, y - 1, w + 2, height + 2, radius + 1, FOCUS_GLOW);
        }
        int fill = focused ? FILL_FOCUS : FILL;
        int border = focused ? BORDER_FOCUS : BORDER;
        MidnightShapes.fillRoundedFrame(context, x, y, w, height, radius, fill, border);
        if (focused) {
            context.fill(x + 2, y + height - 2, x + w - 2, y + height - 1, 0x8860A5FA);
        }
    }

    public static void drawPlaceholder(TextRenderer tr, DrawContext context, int x, int y, int maxWidth, String placeholder) {
        if (placeholder == null || placeholder.isEmpty()) {
            return;
        }
        String shown = tr.trimToWidth(placeholder, Math.max(4, maxWidth));
        context.drawTextWithShadow(tr, Text.literal(shown), x, y, PLACEHOLDER_COLOR);
    }

    public static int textY(int fieldY, int fieldH) {
        return fieldY + (Math.max(MIN_HEIGHT, fieldH) - 8) / 2;
    }

    public static boolean caretVisible() {
        return System.currentTimeMillis() / 530L % 2L == 0L;
    }
}
