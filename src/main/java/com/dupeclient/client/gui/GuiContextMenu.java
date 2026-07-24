package com.dupeclient.client.gui;

import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.modern.theme.MidnightPalette;
import com.dupeclient.client.gui.modern.theme.MidnightShapes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class GuiContextMenu {
    private static final int ROW_H = 18;
    private static final int PAD_Y = 6;
    private static final int PAD_X = UiTokens.SP_3;
    private boolean open;
    private int x;
    private int y;
    private int w;
    private int h;
    private final List<Entry> entries = new ArrayList<>();

    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
        entries.clear();
    }

    public void open(int anchorX, int anchorY, int screenW, int screenH, int minY, List<Entry> items, Font textRenderer) {
        if (items == null || items.isEmpty()) {
            close();
            return;
        }
        entries.clear();
        entries.addAll(items);
        w = 96;
        for (Entry e : entries) {
            w = Math.max(w, textRenderer.width(e.label()) + PAD_X * 2 + 8);
        }
        h = PAD_Y * 2 + entries.size() * ROW_H;
        x = Math.max(UiTokens.SP_2, Math.min(anchorX, screenW - w - UiTokens.SP_2));
        y = Math.max(minY, Math.min(anchorY, screenH - h - UiTokens.SP_2));
        open = true;
    }

    public void render(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        if (!open || entries.isEmpty()) {
            return;
        }
        int rr = MidnightShapes.controlRadius(h);
        MidnightShapes.fillRoundedFrame(
                context, x, y, w, h, rr,
                MidnightPalette.PANEL_FILL_RAISED,
                MidnightPalette.BORDER_LIGHT);

        int rowY = y + PAD_Y;
        for (Entry e : entries) {
            boolean hot = mouseX >= x + 2 && mouseX < x + w - 2 && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (hot) {
                MidnightShapes.fillRoundedRect(
                        context, x + 3, rowY, w - 6, ROW_H,
                        MidnightShapes.controlRadius(ROW_H),
                        MidnightPalette.SIDEBAR_ACTIVE_R);
            }
            context.drawString(
                    textRenderer,
                    Component.literal(e.label()),
                    x + PAD_X,
                    rowY + (ROW_H - 8) / 2,
                    hot ? MidnightPalette.TEXT_PRIMARY : MidnightPalette.TEXT_SECONDARY);
            rowY += ROW_H;
        }
    }

    public boolean contains(double mx, double my) {
        return open && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public boolean handleClick(double mx, double my, int button) {
        if (button != 0 || !contains(mx, my) || entries.isEmpty()) {
            return false;
        }
        int row = (int) ((my - y - PAD_Y) / ROW_H);
        if (row >= 0 && row < entries.size()) {
            entries.get(row).action().run();
        }
        close();
        return true;
    }

    public record Entry(String label, Runnable action) {
    }
}
