package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class PanelOverlayActions {
    public static final int BTN_H = 18;

    private PanelOverlayActions() {
    }

    public static void drawOpenOverlayButton(Font tr, GuiGraphicsExtractor context, int x, int y, int width, String label) {
        UiComponents.drawPillActionButton(tr, context, x, y, width, BTN_H, label);
    }

    public static boolean clickOpenOverlay(double mx, double my, int x, int y, int width) {
        return mx >= x && mx <= x + width && my >= y && my <= y + BTN_H;
    }

    public static void openOverlay(IngameModuleOverlay overlay) {
        IngameOverlayHost.onModuleOverlayOpening(overlay);
        overlay.setOverlayVisible(true);
    }

    public static void drawStatusLine(Font tr, GuiGraphicsExtractor context, int x, int y, int width, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        String trimmed = line.length() > 80 ? line.substring(0, 77) + "…" : line;
        context.text(tr, Component.literal(trimmed), x, y, UiTokens.TEXT_DIM);
    }
}
