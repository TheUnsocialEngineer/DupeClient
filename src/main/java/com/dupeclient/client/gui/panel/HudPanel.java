package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.IngameUiRouter;
import com.dupeclient.client.module.hud.HudManager;
import com.dupeclient.client.module.hud.HudSettings;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * HUD controls inside the modules GUI (no commands needed).
 */
public final class HudPanel extends Panel {
    private static final int BTN_H = 18;
    private static final int GAP = 6;
    private static final int CARD_H = 132;
    private static final int BIND_W = 88;

    private CaptureMode captureMode = CaptureMode.NONE;

    public HudPanel(int x, int y) {
        super("hud", Component.literal("HUD"), x, y, 340, 168);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (collapsed) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font tr = mc.font;
        HudSettings settings = HudManager.INSTANCE.settings();

        int cardX = x + 8;
        int cardY = y + bodyTopOffset() + 8;
        int cardW = width - 16;
        UiComponents.drawSurfaceCard(context, cardX, cardY, cardW, CARD_H);

        int cx = cardX + 10;
        int cy = cardY + 10;
        int cw = cardW - 20;

        boolean active = HudManager.INSTANCE.isActive();
        int stateColor = active ? UiTokens.ACCENT : 0xFFFF9A7A;
        context.drawString(tr, Component.literal("Overlay"), cx, cy, UiTokens.TEXT_DIM);
        context.drawString(tr, Component.literal(active ? "Enabled" : "Disabled"), cx + 52, cy, stateColor);
        cy += 16;

        UiComponents.drawPillActionButton(tr, context, cx, cy, cw, BTN_H, "Open HUD Editor", UiComponents.PillActionStyle.PRIMARY_BLUE);
        cy += BTN_H + GAP;

        int third = (cw - GAP * 2) / 3;
        UiComponents.drawPillActionButton(
                tr, context, cx, cy, third, BTN_H,
                active ? "Disable" : "Enable",
                active ? UiComponents.PillActionStyle.SECONDARY_SLATE : UiComponents.PillActionStyle.PRIMARY_MINT);
        UiComponents.drawPillActionButton(tr, context, cx + third + GAP, cy, third, BTN_H, "Reset", UiComponents.PillActionStyle.SECONDARY_SLATE);
        UiComponents.drawPillActionButton(tr, context, cx + (third + GAP) * 2, cy, third, BTN_H, "Reload", UiComponents.PillActionStyle.SECONDARY_SLATE);
        cy += BTN_H + GAP;

        boolean listen = captureMode == CaptureMode.HUD_EDITOR_KEY;
        UiComponents.drawPillKeybindEx(
                tr, context, cx, cy, cw, BTN_H, "Editor hotkey",
                listen ? "Press key…" : keyName(settings.editorOpenKey), listen, BIND_W);

        height = bodyTopOffset() + 8 + CARD_H + 10;
        if (captureMode != CaptureMode.NONE) {
            context.drawString(tr,
                    Component.literal("Press a key (ESC clears bind)"),
                    cx, y + height - 10, UiTokens.ACCENT);
            height += 10;
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
        int cardX = x + 8;
        int cardY = y + bodyTopOffset() + 8;
        int cardW = width - 16;
        int cx = cardX + 10;
        int cy = cardY + 10 + 16;
        int cw = cardW - 20;

        if (rect(mouseX, mouseY, cx, cy, cw, BTN_H)) {
            IngameUiRouter.openHudEditor(Minecraft.getInstance().screen);
            return true;
        }
        cy += BTN_H + GAP;
        int third = (cw - GAP * 2) / 3;
        if (rect(mouseX, mouseY, cx, cy, third, BTN_H)) {
            HudManager.INSTANCE.setActive(!HudManager.INSTANCE.isActive());
            return true;
        }
        if (rect(mouseX, mouseY, cx + third + GAP, cy, third, BTN_H)) {
            HudManager.INSTANCE.resetToDefaultElements();
            return true;
        }
        if (rect(mouseX, mouseY, cx + (third + GAP) * 2, cy, third, BTN_H)) {
            HudManager.INSTANCE.load();
            return true;
        }
        cy += BTN_H + GAP;
        if (clickBindValue(mouseX, mouseY, cx, cy, cw)) {
            captureMode = CaptureMode.HUD_EDITOR_KEY;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (captureMode == CaptureMode.NONE) {
            return false;
        }
        int key = keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
        if (captureMode == CaptureMode.HUD_EDITOR_KEY) {
            HudSettings settings = HudManager.INSTANCE.settings();
            settings.editorOpenKey = key == GLFW.GLFW_KEY_UNKNOWN ? -1 : key;
            HudManager.INSTANCE.save();
            captureMode = CaptureMode.NONE;
            return true;
        }
        return false;
    }

    @Override
    public void onModuleHidden() {
        captureMode = CaptureMode.NONE;
    }

    @Override
    public boolean hasFocusedTextInput() {
        return isVisible() && captureMode != CaptureMode.NONE;
    }

    private static boolean rect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean clickBindValue(double mouseX, double mouseY, int x, int y, int w) {
        int bx = x + w - BIND_W;
        return rect(mouseX, mouseY, bx, y, BIND_W, BTN_H);
    }

    private String keyName(int keyCode) {
        if (keyCode < 0 || keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return "UNBOUND";
        }
        String glfw = GLFW.glfwGetKeyName(keyCode, 0);
        if (glfw != null) {
            return glfw.toUpperCase(Locale.ROOT);
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            default -> "KEY_" + keyCode;
        };
    }

    private enum CaptureMode {
        NONE,
        HUD_EDITOR_KEY
    }
}
