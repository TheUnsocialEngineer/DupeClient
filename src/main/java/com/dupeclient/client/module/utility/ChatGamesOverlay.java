package com.dupeclient.client.module.utility;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.overlay.AbstractDraggableOverlay;
import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ChatGamesOverlay extends AbstractDraggableOverlay implements IngameModuleOverlay {
    public static final ChatGamesOverlay INSTANCE = new ChatGamesOverlay();

    private static final int PANEL_W = 268;
    private static final int PANEL_H = 148;
    private static final int TITLE_H = 12;
    private static final int ROW = UiTokens.ROW_STEP;
    private static final int SLIDER_H = 16;

    private final ChatGamesManager manager = ChatGamesManager.INSTANCE;
    private boolean draggingSlider;

    private ChatGamesOverlay() {
    }

    @Override
    public String id() {
        return "chat_games";
    }

    @Override
    public boolean isModuleEnabled() {
        return manager.getSettings().enabled;
    }

    @Override
    public boolean isOverlayVisible() {
        return manager.getSettings().overlayVisible;
    }

    @Override
    public void setOverlayVisible(boolean visible) {
        if (visible) {
            com.dupeclient.client.gui.overlay.IngameOverlayHost.onModuleOverlayOpening(this);
        }
        manager.getSettings().overlayVisible = visible;
        manager.save();
    }

    @Override
    public int overlayX() {
        return manager.getSettings().overlayX;
    }

    @Override
    public int overlayY() {
        return manager.getSettings().overlayY;
    }

    @Override
    public void setOverlayPosition(int x, int y) {
        ChatGamesSettings s = manager.getSettings();
        s.overlayX = x;
        s.overlayY = y;
        manager.save();
    }

    @Override
    public int panelWidth() {
        return PANEL_W;
    }

    @Override
    public int panelHeight() {
        return PANEL_H;
    }

    @Override
    public boolean hasTextFocus() {
        return false;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!isActive()) {
            return;
        }
        ChatGamesSettings s = manager.getSettings();
        int px = overlayX();
        int py = overlayY();
        Minecraft mc = Minecraft.getInstance();
        Font tr = mc.font;

        context.fill(px, py, px + PANEL_W, py + PANEL_H, 0xE018181B);
        context.fill(px, py, px + PANEL_W, py + TITLE_H, 0xFF27272A);
        context.text(tr, Component.literal("Chat Games"), px + 6, py + 2, 0xFF60A5FA);

        int rx = px + 8;
        int inner = PANEL_W - 16;
        int y = py + TITLE_H + 6;

        UiComponents.drawOptionToggle(
                tr, context, rx, y, inner, "Maths only", s.mathsOnly, s.mathsOnly ? 1f : 0f);
        y += ROW + UiTokens.UI_GAP;

        UiComponents.drawOptionToggle(
                tr, context, rx, y, inner, "Word games", s.wordGames, s.wordGames ? 1f : 0f);
        y += ROW + UiTokens.UI_GAP;

        UiComponents.drawLabeledValueSlider(
                tr,
                context,
                rx,
                y,
                inner,
                s.cooldownSeconds,
                1,
                30,
                "Cooldown (s)",
                86,
                40,
                draggingSlider,
                String.valueOf(s.cooldownSeconds));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isActive()) {
            return false;
        }
        int px = overlayX();
        int py = overlayY();
        if (!containsPoint(mouseX, mouseY)) {
            return false;
        }
        if (beginTitleDrag(mouseX, mouseY, button, px, py, PANEL_W, TITLE_H)) {
            return true;
        }
        ChatGamesSettings s = manager.getSettings();
        int rx = px + 8;
        int inner = PANEL_W - 16;
        int y = py + TITLE_H + 6;

        if (inRect(mouseX, mouseY, rx, y, inner, ROW)) {
            s.mathsOnly = !s.mathsOnly;
            manager.save();
            return true;
        }
        y += ROW + UiTokens.UI_GAP;
        if (inRect(mouseX, mouseY, rx, y, inner, ROW)) {
            s.wordGames = !s.wordGames;
            manager.save();
            return true;
        }
        y += ROW + UiTokens.UI_GAP;
        int barX = rx + 86;
        int barW = inner - 92 - 40;
        if (inRect(mouseX, mouseY, barX, y + 1, barW, 8)) {
            draggingSlider = true;
            s.cooldownSeconds = (int) Math.round(sliderValue(mouseX, barX, barW, 1, 30));
            manager.save();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        endTitleDrag(button);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingSlider = false;
        }
        return dragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (updateTitleDrag(mouseX, mouseY, button)) {
            return true;
        }
        if (!draggingSlider || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        ChatGamesSettings s = manager.getSettings();
        int rx = overlayX() + 8;
        int inner = PANEL_W - 16;
        int y = overlayY() + TITLE_H + 6 + (ROW + UiTokens.UI_GAP) * 2;
        int barX = rx + 86;
        int barW = inner - 92 - 40;
        s.cooldownSeconds = (int) Math.round(sliderValue(mouseX, barX, barW, 1, 30));
        manager.save();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        return false;
    }

    @Override
    public boolean charTyped(int codePoint) {
        return false;
    }

    private static double sliderValue(double mouseX, int x, int w, double min, double max) {
        double t = (mouseX - x) / w;
        t = Math.max(0.0, Math.min(1.0, t));
        return min + (max - min) * t;
    }
}
