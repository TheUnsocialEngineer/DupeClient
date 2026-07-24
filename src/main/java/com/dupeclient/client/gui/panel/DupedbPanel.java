package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.module.dupedb.DupedbConfigManager;
import com.dupeclient.client.module.dupedb.DupedbManager;
import com.dupeclient.client.module.dupedb.DupedbOverlay;
import com.dupeclient.client.module.dupedb.DupedbSettings;
import com.dupeclient.client.module.dupedb.P2wMarkManager;
import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class DupedbPanel extends Panel {
    private static final int TOGGLE_H = UiTokens.ROW_STEP;
    private static final int HINT_H = 10;

    private final DupedbManager manager = DupedbManager.INSTANCE;
    private boolean capturingOverlayHotkey;

    public DupedbPanel(int x, int y) {
        super("dupedb", Component.literal("DupeDB"), x, y, 280, 180);
    }

    private static int cardHeight() {
        return UiTokens.CARD_CONTENT_TOP
                + TOGGLE_H + UiTokens.UI_GAP
                + TOGGLE_H + UiTokens.UI_GAP
                + PanelKeybinds.ROW_H + UiTokens.UI_GAP
                + TOGGLE_H + UiTokens.UI_GAP
                + PanelOverlayActions.BTN_H + UiTokens.UI_GAP
                + HINT_H + UiTokens.UI_GAP
                + HINT_H + 8;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (collapsed) {
            return;
        }
        Font tr = net.minecraft.client.Minecraft.getInstance().font;
        DupedbSettings s = manager.getSettings();
        int tx = x + UiTokens.BODY_INSET;
        int ty = y + bodyTopOffset() + UiTokens.UI_GAP;
        int sw = width - UiTokens.BODY_INSET * 2;
        int inner = sw - UiTokens.SP_4;
        int rx = tx + UiTokens.SP_2;
        int cardTop = ty;
        int cardH = cardHeight();
        UiComponents.drawSurfaceCard(context, tx, cardTop, sw, cardH);

        int body = cardTop + UiTokens.SP_3;
        UiComponents.drawOptionToggle(
                tr, context, rx, body, inner, "Show overlay", s.overlayVisible,
                smoothToggle("dupedb.vis", s.overlayVisible, delta));

        int y1 = body + TOGGLE_H + UiTokens.UI_GAP;
        PanelKeybinds.drawRow(tr, context, rx, y1, inner, "Overlay hotkey", s.overlayToggleKey, capturingOverlayHotkey);

        int y2 = y1 + PanelKeybinds.ROW_H + UiTokens.UI_GAP;
        UiComponents.drawOptionToggle(
                tr, context, rx, y2, inner, "Chat feedback", s.moduleChatFeedback,
                smoothToggle("dupedb.chatFeedback", s.moduleChatFeedback, delta));

        int y3 = y2 + TOGGLE_H + UiTokens.UI_GAP;
        PanelOverlayActions.drawOpenOverlayButton(tr, context, rx, y3, inner, "Open DupeDB overlay");

        int y4 = y3 + PanelOverlayActions.BTN_H + UiTokens.UI_GAP;
        String status = manager.isScanning()
                ? "Scanning… " + manager.getDiscoveredPluginCount() + " plugin(s)"
                : statusLine();
        PanelOverlayActions.drawStatusLine(tr, context, rx, y4, inner, status);

        int y5 = y4 + HINT_H + UiTokens.UI_GAP;
        context.drawString(tr, Component.literal("Run a scan before submitting P2W marks."), rx, y5, UiTokens.TEXT_DIM);
        height = bodyTopOffset() + UiTokens.UI_GAP + cardH + UiTokens.SP_2;
        if (capturingOverlayHotkey) {
            context.drawString(
                    tr,
                    Component.literal("Press key for overlay hotkey (ESC to unbind)"),
                    x + 8,
                    y + height - 12,
                    0xFFFFC857);
        }
    }

    private static String statusLine() {
        String server = P2wMarkManager.currentServerAddress();
        if (!server.isBlank()) {
            String p2w = P2wServerPolicy.INSTANCE.registryStatusForServer(server);
            if (!p2w.isBlank()) {
                return "Community: " + p2w;
            }
        }
        int plugins = DupedbManager.INSTANCE.getDiscoveredPluginCount();
        return plugins > 0 ? plugins + " plugin(s) discovered" : "No scan data yet";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (collapsed || button != 0) {
            return false;
        }
        DupedbSettings s = manager.getSettings();
        int tx = x + UiTokens.BODY_INSET;
        int ty = y + bodyTopOffset() + UiTokens.UI_GAP;
        int sw = width - UiTokens.BODY_INSET * 2;
        int inner = sw - UiTokens.SP_4;
        int rx = tx + UiTokens.SP_2;
        int body = ty + UiTokens.SP_3;

        if (clickToggle(mouseX, mouseY, rx, body, inner)) {
            DupedbOverlay.INSTANCE.toggleOverlayVisible();
            manager.chatFeedbackConfigToggle("DupeDB overlay " + (s.overlayVisible ? "shown" : "hidden"));
            return true;
        }
        if (PanelKeybinds.clickRow(mouseX, mouseY, rx, body + TOGGLE_H + UiTokens.UI_GAP, inner)) {
            capturingOverlayHotkey = true;
            return true;
        }
        int y2 = body + TOGGLE_H + UiTokens.UI_GAP + PanelKeybinds.ROW_H + UiTokens.UI_GAP;
        if (clickToggle(mouseX, mouseY, rx, y2, inner)) {
            s.moduleChatFeedback = !s.moduleChatFeedback;
            DupedbConfigManager.save(s);
            manager.chatFeedbackConfigToggle("Chat feedback " + (s.moduleChatFeedback ? "on" : "off"));
            return true;
        }
        int y3 = y2 + TOGGLE_H + UiTokens.UI_GAP;
        if (PanelOverlayActions.clickOpenOverlay(mouseX, mouseY, rx, y3, inner)) {
            PanelOverlayActions.openOverlay(DupedbOverlay.INSTANCE);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!capturingOverlayHotkey) {
            return false;
        }
        manager.getSettings().overlayToggleKey = PanelKeybinds.captureKey(keyCode);
        DupedbConfigManager.save(manager.getSettings());
        capturingOverlayHotkey = false;
        return true;
    }

    @Override
    public void onModuleHidden() {
        capturingOverlayHotkey = false;
    }

    @Override
    public boolean hasFocusedTextInput() {
        return isVisible() && capturingOverlayHotkey;
    }

    private static boolean clickToggle(double mx, double my, int sx, int sy, int inner) {
        return mx >= sx && mx <= sx + inner && my >= sy && my <= sy + TOGGLE_H;
    }
}
