package com.dupeclient.client.gui.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Transparent full-screen host for in-game module overlays (same interaction model as
 * {@link com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay} on {@link net.minecraft.client.gui.screen.ingame.HandledScreen}).
 */
public final class IngameModuleOverlayScreen extends Screen {
    private static final IngameModuleOverlayScreen INSTANCE = new IngameModuleOverlayScreen();

    private IngameModuleOverlayScreen() {
        super(Text.empty());
    }

    public static IngameModuleOverlayScreen get() {
        return INSTANCE;
    }

    public static boolean isShowing(Screen screen) {
        return screen instanceof IngameModuleOverlayScreen;
    }

    private void syncBounds() {
        MinecraftClient mc = client;
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        width = mc.getWindow().getScaledWidth();
        height = mc.getWindow().getScaledHeight();
    }

    @Override
    public void resize(MinecraftClient mc, int width, int height) {
        MinecraftClient client = this.client;
        if (client != null && client.getWindow() != null) {
            super.resize(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        } else {
            super.resize(mc, width, height);
        }
        syncBounds();
    }

    @Override
    protected void init() {
        clearChildren();
        syncBounds();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Do not darken or blur the world — overlays draw in {@link #render}.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        syncBounds();
        IngameOverlayHost.renderAll(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (IngameOverlayHost.onScreenOverlayMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        if (IngameOverlayHost.onMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (IngameOverlayHost.onScreenOverlayMouseReleased(click.x(), click.y(), click.button())) {
            return true;
        }
        return IngameOverlayHost.onMouseReleased(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
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
    public boolean keyPressed(KeyInput input) {
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
    public boolean charTyped(CharInput input) {
        if (IngameOverlayHost.onCharTyped(input.codepoint())) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
