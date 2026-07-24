package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.modern.UiComponents;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Shared hotkey pill rows for hub module panels. */
final class PanelKeybinds {
    static final int ROW_H = 20;
    static final int PILL_W = 98;

    private PanelKeybinds() {
    }

    static String keyName(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode < 0) {
            return "None";
        }
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        return name != null ? name.toUpperCase(Locale.ROOT) : "Key " + keyCode;
    }

    static int captureKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
    }

    static void drawRow(
            Font tr,
            GuiGraphics context,
            int x,
            int y,
            int w,
            String label,
            int keyCode,
            boolean listening) {
        UiComponents.drawPillKeybind(
                tr, context, x, y, w, ROW_H, label,
                listening ? "Press key..." : keyName(keyCode), listening);
    }

    static boolean clickRow(double mx, double my, int sx, int sy, int inner) {
        return mx >= sx + inner - PILL_W && mx <= sx + inner && my >= sy && my <= sy + ROW_H;
    }
}
