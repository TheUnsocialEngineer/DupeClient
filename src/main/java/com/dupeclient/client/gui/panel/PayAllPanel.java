package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.module.payall.PayAllManager;
import com.dupeclient.client.module.payall.PayAllOverlay;
import com.dupeclient.client.module.payall.PayAllSettings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PayAllPanel extends Panel {
    private static final int TOGGLE_H = UiTokens.ROW_STEP;
    private static final int HINT_H = 10;

    private final PayAllManager manager = PayAllManager.INSTANCE;
    private boolean capturingOverlayHotkey;

    public PayAllPanel(int x, int y) {
        super("payall", Component.literal("PayAll"), x, y, 280, 140);
    }

    private static int cardHeight() {
        return UiTokens.CARD_CONTENT_TOP
                + TOGGLE_H + UiTokens.UI_GAP
                + TOGGLE_H + UiTokens.UI_GAP
                + PanelKeybinds.ROW_H + UiTokens.UI_GAP
                + TOGGLE_H + UiTokens.UI_GAP
                + PanelOverlayActions.BTN_H + UiTokens.UI_GAP
                + HINT_H + 8;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (collapsed) {
            return;
        }
        Font tr = net.minecraft.client.Minecraft.getInstance().font;
        PayAllSettings s = manager.getSettings();
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
                tr, context, rx, body, inner, "Enabled", s.enabled,
                smoothToggle("payall.on", s.enabled, delta));

        int y1 = body + TOGGLE_H + UiTokens.UI_GAP;
        UiComponents.drawOptionToggle(
                tr, context, rx, y1, inner, "Show overlay", s.overlayVisible,
                smoothToggle("payall.vis", s.overlayVisible, delta));

        int y2 = y1 + TOGGLE_H + UiTokens.UI_GAP;
        PanelKeybinds.drawRow(tr, context, rx, y2, inner, "Overlay hotkey", s.overlayToggleKey, capturingOverlayHotkey);

        int y3 = y2 + PanelKeybinds.ROW_H + UiTokens.UI_GAP;
        UiComponents.drawOptionToggle(
                tr, context, rx, y3, inner, "Chat feedback", s.moduleChatFeedback,
                smoothToggle("payall.chatFb", s.moduleChatFeedback, delta));

        int y4 = y3 + TOGGLE_H + UiTokens.UI_GAP;
        PanelOverlayActions.drawOpenOverlayButton(tr, context, rx, y4, inner, "Open PayAll overlay");
        context.drawString(tr, Component.literal("Targets and payouts are configured in the PayAll overlay."), rx, y4 + PanelOverlayActions.BTN_H + UiTokens.UI_GAP, UiTokens.TEXT_DIM);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (collapsed || button != 0) {
            return false;
        }
        PayAllSettings s = manager.getSettings();
        int tx = x + UiTokens.BODY_INSET;
        int ty = y + bodyTopOffset() + UiTokens.UI_GAP;
        int sw = width - UiTokens.BODY_INSET * 2;
        int inner = sw - UiTokens.SP_4;
        int rx = tx + UiTokens.SP_2;
        int body = ty + UiTokens.SP_3;

        if (clickToggle(mouseX, mouseY, rx, body, inner)) {
            s.enabled = !s.enabled;
            if (!s.enabled) {
                PayAllOverlay.INSTANCE.setOverlayVisible(false);
            }
            manager.saveSettings();
            manager.moduleFeedbackConfigToggle("PayAll " + (s.enabled ? "enabled" : "disabled"));
            return true;
        }
        if (clickToggle(mouseX, mouseY, rx, body + TOGGLE_H + UiTokens.UI_GAP, inner)) {
            PayAllOverlay.INSTANCE.toggleOverlayVisible();
            manager.moduleFeedbackConfigToggle("PayAll overlay " + (s.overlayVisible ? "shown" : "hidden"));
            return true;
        }
        if (PanelKeybinds.clickRow(mouseX, mouseY, rx, body + (TOGGLE_H + UiTokens.UI_GAP) * 2, inner)) {
            capturingOverlayHotkey = true;
            return true;
        }
        if (clickToggle(mouseX, mouseY, rx, body + (TOGGLE_H + UiTokens.UI_GAP) * 2 + PanelKeybinds.ROW_H + UiTokens.UI_GAP, inner)) {
            boolean next = !s.moduleChatFeedback;
            manager.setModuleChatFeedback(next);
            manager.moduleFeedbackConfigToggle("Chat feedback " + (next ? "on" : "off"));
            return true;
        }
        int y4 = body + (TOGGLE_H + UiTokens.UI_GAP) * 2 + PanelKeybinds.ROW_H + UiTokens.UI_GAP + TOGGLE_H + UiTokens.UI_GAP;
        if (PanelOverlayActions.clickOpenOverlay(mouseX, mouseY, rx, y4, inner)) {
            PanelOverlayActions.openOverlay(PayAllOverlay.INSTANCE);
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
        manager.saveSettings();
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
