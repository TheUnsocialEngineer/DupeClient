package com.ui_utils.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class UITheme {
    public static final int BACKGROUND = -434823890;
    public static final int BACKGROUND_HOVER = -266326714;
    public static final int BORDER = -12627080;
    public static final int BORDER_HOVER = -10782552;
    public static final int BORDER_ACCENT = -12877066;
    public static final int BORDER_OUTER = -16117734;
    public static final int TEXT = -328966;
    public static final int TEXT_DIM = -7429950;
    public static final int INPUT_BG = -435021271;
    public static final int INPUT_BORDER = -12627080;
    public static final int INPUT_BORDER_FOCUS = -12877066;

    public static void drawPanel(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, -16117734);
        context.fill(x, y, x + width, y + height, -434823890);
        UITheme.drawBorder(context, x, y, width, height, -12627080);
    }

    public static void drawPanelWithAccent(GuiGraphicsExtractor context, int x, int y, int width, int height) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, -16117734);
        context.fillGradient(x, y, x + width, y + height, -266326714, -434823890);
        context.fill(x, y, x + 2, y + height, -12877066);
        UITheme.drawBorder(context, x, y, width, height, -12627080);
    }

    public static void drawBorder(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static void drawDoubleBorder(GuiGraphicsExtractor context, int x, int y, int width, int height, int innerColor, int outerColor) {
        context.fill(x - 1, y - 1, x + width + 1, y, outerColor);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, outerColor);
        context.fill(x - 1, y - 1, x, y + height + 1, outerColor);
        context.fill(x + width, y - 1, x + width + 1, y + height + 1, outerColor);
        UITheme.drawBorder(context, x, y, width, height, innerColor);
    }

    public static void drawInputBackground(GuiGraphicsExtractor context, int x, int y, int width, int height, boolean focused) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, -16117734);
        context.fill(x, y, x + width, y + height, -435021271);
        UITheme.drawBorder(context, x, y, width, height, focused ? -12877066 : -12627080);
    }
}

