package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.module.acaudit.AcAuditGitHubCard;
import com.dupeclient.client.module.acaudit.AcAuditManager;
import com.dupeclient.client.module.acaudit.AcAuditOverlay;
import com.dupeclient.client.module.acaudit.AcAuditSettings;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class AcAuditPanel extends Panel {
    private static final int TOGGLE_H = UiTokens.ROW_STEP;
    private static final int KEYBIND_H = 20;
    private static final int HINT_H = 10;
    private static final int SETTINGS_TOP_OFFSET = 5;

    private final AcAuditManager manager = AcAuditManager.INSTANCE;
    private CaptureMode captureMode = CaptureMode.IDLE;
    private int githubCardX;
    private int githubCardY;

    public AcAuditPanel(int x, int y) {
        super("ac_audit", Component.literal("AC Audit"), x, y, 280, 168);
    }

    private static int rowBlock(int... rowHeights) {
        int total = 0;
        for (int h : rowHeights) {
            total += h + UiTokens.UI_GAP;
        }
        return total;
    }

    private static int rowY(int bodyTop, int rowIndex, int... rowHeights) {
        int y = bodyTop;
        for (int i = 0; i < rowIndex; i++) {
            y += rowHeights[i] + UiTokens.UI_GAP;
        }
        return y;
    }

    private static int cardHeight() {
        return UiTokens.CARD_CONTENT_TOP + SETTINGS_TOP_OFFSET + rowBlock(TOGGLE_H, TOGGLE_H, KEYBIND_H, KEYBIND_H, TOGGLE_H) + HINT_H + 4;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (collapsed) {
            return;
        }
        Font tr = net.minecraft.client.Minecraft.getInstance().font;
        AcAuditSettings s = manager.getSettings();
        int tx = x + UiTokens.BODY_INSET;
        int ty = y + bodyTopOffset() + UiTokens.UI_GAP;
        int sw = width - UiTokens.BODY_INSET * 2;
        int inner = sw - UiTokens.SP_4;
        int rx = tx + UiTokens.SP_2;
        int cardTop = ty;
        int cardH = cardHeight();
        UiComponents.drawInfoCard(tr, context, tx, cardTop, sw, cardH, "AC Audit");
        githubCardX = tx + sw - AcAuditGitHubCard.width() - UiTokens.SP_2;
        githubCardY = cardTop + UiTokens.SP_2;
        AcAuditGitHubCard.render(context, tr, githubCardX, githubCardY, mouseX, mouseY);
        int body = UiComponents.titledCardBodyY(cardTop) + SETTINGS_TOP_OFFSET;
        int[] rows = {TOGGLE_H, TOGGLE_H, KEYBIND_H, KEYBIND_H, TOGGLE_H};

        int y0 = rowY(body, 0, rows);
        UiComponents.drawOptionToggle(
                tr, context, rx, y0, inner, "Enabled", s.enabled,
                smoothToggle("ac.en", s.enabled, delta));

        int y1 = rowY(body, 1, rows);
        UiComponents.drawOptionToggle(
                tr, context, rx, y1, inner, "Show overlay", s.overlayVisible,
                smoothToggle("ac.ov", s.overlayVisible, delta));

        int y2 = rowY(body, 2, rows);
        drawBindRow(tr, context, rx, y2, inner, "Overlay hotkey", s.overlayToggleKey, CaptureMode.OVERLAY);

        int y3 = rowY(body, 3, rows);
        drawBindRow(tr, context, rx, y3, inner, "Toggle hotkey", s.toggleKey, CaptureMode.TOGGLE);

        int y4 = rowY(body, 4, rows);
        UiComponents.drawOptionToggle(
                tr, context, rx, y4, inner, "Disable on leave", s.disableOnLeave,
                smoothToggle("ac.leave", s.disableOnLeave, delta));

        int hintY = y4 + TOGGLE_H + UiTokens.UI_GAP;
        context.text(tr, Component.literal("Diagnostics & probes — use in-game overlay"), rx, hintY, 0xFF9CA3AF);

        height = bodyTopOffset() + UiTokens.UI_GAP + cardH + UiTokens.SP_2;
        if (captureMode != CaptureMode.IDLE) {
            context.text(
                    tr,
                    Component.literal("Press key for " + captureMode.label + " (ESC to unbind)"),
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
        if (AcAuditGitHubCard.mouseClicked(mouseX, mouseY, githubCardX, githubCardY, button)) {
            return true;
        }
        AcAuditSettings s = manager.getSettings();
        int tx = x + UiTokens.BODY_INSET;
        int ty = y + bodyTopOffset() + UiTokens.UI_GAP;
        int sw = width - UiTokens.BODY_INSET * 2;
        int inner = sw - UiTokens.SP_4;
        int rx = tx + UiTokens.SP_2;
        int body = UiComponents.titledCardBodyY(ty) + SETTINGS_TOP_OFFSET;
        int[] rows = {TOGGLE_H, TOGGLE_H, KEYBIND_H, KEYBIND_H, TOGGLE_H};

        if (clickToggle(mouseX, mouseY, rx, rowY(body, 0, rows), inner)) {
            manager.setEnabled(!s.enabled);
            return true;
        }
        if (clickToggle(mouseX, mouseY, rx, rowY(body, 1, rows), inner)) {
            AcAuditOverlay.INSTANCE.toggleOverlayVisible();
            return true;
        }
        if (clickBind(mouseX, mouseY, rx, rowY(body, 2, rows), inner)) {
            captureMode = CaptureMode.OVERLAY;
            return true;
        }
        if (clickBind(mouseX, mouseY, rx, rowY(body, 3, rows), inner)) {
            captureMode = CaptureMode.TOGGLE;
            return true;
        }
        if (clickToggle(mouseX, mouseY, rx, rowY(body, 4, rows), inner)) {
            s.disableOnLeave = !s.disableOnLeave;
            manager.save();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (captureMode == CaptureMode.IDLE) {
            return false;
        }
        int key = keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
        if (captureMode == CaptureMode.OVERLAY) {
            manager.getSettings().overlayToggleKey = key;
        } else {
            manager.getSettings().toggleKey = key;
        }
        manager.save();
        captureMode = CaptureMode.IDLE;
        return true;
    }

    @Override
    public void onModuleHidden() {
        captureMode = CaptureMode.IDLE;
    }

    @Override
    public boolean hasFocusedTextInput() {
        return isVisible() && captureMode != CaptureMode.IDLE;
    }

    private void drawBindRow(
            Font tr, GuiGraphicsExtractor context, int x, int y, int w, String label, int keyCode, CaptureMode mode) {
        boolean listening = captureMode == mode;
        UiComponents.drawPillKeybind(
                tr, context, x, y, w, KEYBIND_H, label,
                listening ? "Press key..." : keyName(keyCode), listening);
    }

    private static boolean clickToggle(double mx, double my, int sx, int sy, int inner) {
        return mx >= sx && mx <= sx + inner && my >= sy && my <= sy + TOGGLE_H;
    }

    private static boolean clickBind(double mx, double my, int sx, int sy, int inner) {
        return mx >= sx + inner - 98 && mx <= sx + inner && my >= sy && my <= sy + KEYBIND_H;
    }

    private static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            return "None";
        }
        String name = GLFW.glfwGetKeyName(key, 0);
        return name != null ? name.toUpperCase(Locale.ROOT) : "Key " + key;
    }

    private enum CaptureMode {
        IDLE(""),
        OVERLAY("overlay hotkey"),
        TOGGLE("toggle hotkey");

        private final String label;

        CaptureMode(String label) {
            this.label = label;
        }
    }
}
