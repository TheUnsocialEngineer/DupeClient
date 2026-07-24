package com.dupeclient.client.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Transparent full-screen host for in-game module overlays (same interaction model as
 * {@link com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay} on {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen}).
 */
public final class IngameModuleOverlayScreen extends Screen {
    private static final IngameModuleOverlayScreen INSTANCE = new IngameModuleOverlayScreen();

    private IngameModuleOverlayScreen() {
        super(Component.empty());
    }

    public static IngameModuleOverlayScreen get() {
        return INSTANCE;
    }

    public static boolean isShowing(Screen screen) {
        return screen instanceof IngameModuleOverlayScreen;
    }

    private void syncBounds() {
        Minecraft mc = minecraft;
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        width = mc.getWindow().getGuiScaledWidth();
        height = mc.getWindow().getGuiScaledHeight();
    }

    @Override
    public void resize(int width, int height) {
        Minecraft mc = minecraft;
        if (mc != null && mc.getWindow() != null) {
            super.resize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        } else {
            super.resize(width, height);
        }
        syncBounds();
    }

    @Override
    protected void init() {
        clearWidgets();
        syncBounds();
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        // Do not darken or blur the world — overlays draw in {@link #render}.
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        syncBounds();
        IngameOverlayHost.renderAll(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        if (IngameOverlayHost.onScreenOverlayMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        if (IngameOverlayHost.onMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (IngameOverlayHost.onScreenOverlayMouseReleased(click.x(), click.y(), click.button())) {
            return true;
        }
        return IngameOverlayHost.onMouseReleased(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (IngameOverlayHost.onScreenOverlayMouseDragged(click.x(), click.y(), click.button())) {
            return true;
        }
        return IngameOverlayHost.onMouseDragged(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (IngameOverlayHost.onScreenOverlayMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return IngameOverlayHost.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (IngameOverlayHost.onKeyPressed(input.key())) {
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && IngameOverlayHost.hasAnyActive()) {
            IngameOverlayHost.hideAllOverlays();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (IngameOverlayHost.onCharTyped(input.codepoint())) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
