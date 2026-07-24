package com.dupeclient.client.gui;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.modern.HubShell;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.gui.render.UiNativeRenderer;
import com.dupeclient.client.gui.panel.Panel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * DupeClient hub: responsive shell (rail / pills), glassy overlay, single scrollable module.
 */
public class ClientGuiScreen extends Screen {
    private final Screen returnScreen;
    private final HubShell hub = new HubShell();

    public ClientGuiScreen(Screen returnScreen) {
        super(Text.literal("DupeClient"));
        this.returnScreen = returnScreen;
    }

    private void applyFullWindowBounds() {
        MinecraftClient c = client;
        if (c == null || c.getWindow() == null) {
            return;
        }
        int sw = c.getWindow().getScaledWidth();
        int sh = c.getWindow().getScaledHeight();
        if (sw <= 0 || sh <= 0) {
            return;
        }
        this.width = sw;
        this.height = sh;
        hub.syncViewport(sw, sh);
    }

    @Override
    public void resize(int width, int height) {
        MinecraftClient c = client;
        if (c != null && c.getWindow() != null) {
            int sw = c.getWindow().getScaledWidth();
            int sh = c.getWindow().getScaledHeight();
            super.resize(sw, sh);
        } else {
            super.resize(width, height);
        }
        applyFullWindowBounds();
    }

    @Override
    protected void init() {
        clearChildren();
        applyFullWindowBounds();
        hub.setReserveTopForVanillaCloseButton(false);
        hub.onScreenOpen();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        applyFullWindowBounds();
        super.renderBackground(context, mouseX, mouseY, deltaTicks);
        UiDraw.fillMidnightBackground(context, this.width, this.height);
        hub.render(context, this.textRenderer, mouseX, mouseY, deltaTicks, this.width, this.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        applyFullWindowBounds();
        hub.updateNavHover(mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        applyFullWindowBounds();
        if (client != null) {
            hub.applyEmbeddedLayout(client.textRenderer);
        }
        if (IngameOverlayHost.onScreenOverlayMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        double mx = click.x();
        double my = click.y();
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        if (hub.handleNavClick(mx, my, click.button())) {
            return true;
        }
        if (hub.handlePanelClick(mx, my, click.button())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (IngameOverlayHost.onScreenOverlayMouseReleased(click.x(), click.y(), click.button())) {
            return true;
        }
        for (Panel panel : DupeClient.getGuiManager().getPanels()) {
            if (panel.isVisible()) {
                panel.mouseReleased(click.x(), click.y(), click.button());
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (IngameOverlayHost.onScreenOverlayMouseDragged(click.x(), click.y(), click.button())) {
            return true;
        }
        for (Panel panel : DupeClient.getGuiManager().getPanels()) {
            if (panel.isVisible() && panel.mouseDragged(click.x(), click.y(), click.button())) {
                return true;
            }
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        applyFullWindowBounds();
        if (IngameOverlayHost.onScreenOverlayMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return hub.handleContentScroll(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        for (Panel panel : DupeClient.getGuiManager().getPanels()) {
            if (panel.isVisible() && panel.keyPressed(keyInput.key(), keyInput.scancode(), keyInput.modifiers())) {
                return true;
            }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (super.charTyped(charInput)) {
            return true;
        }
        for (Panel panel : DupeClient.getGuiManager().getPanels()) {
            if (panel.isVisible() && panel.charTyped(charInput.codepoint(), charInput.modifiers())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        applyFullWindowBounds();
        super.tick();
        hub.tick();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(returnScreen);
        }
    }

    @Override
    public void removed() {
        hub.onRemoved();
        UiNativeRenderer.trimCache(48);
        super.removed();
    }
}
