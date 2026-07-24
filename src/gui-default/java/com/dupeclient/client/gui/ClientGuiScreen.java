package com.dupeclient.client.gui;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.modern.HubShell;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.gui.render.UiNativeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.dupeclient.client.gui.panel.Panel;
import org.jetbrains.annotations.Nullable;

/**
 * DupeClient hub: responsive shell (rail / pills), glassy overlay, single scrollable module.
 */
public class ClientGuiScreen extends Screen {
    private final Screen returnScreen;
    private final HubShell hub = new HubShell();

    public ClientGuiScreen(Screen returnScreen) {
        super(Component.literal("DupeClient"));
        this.returnScreen = returnScreen;
    }

    private void applyFullWindowBounds() {
        Minecraft c = minecraft;
        if (c == null || c.getWindow() == null) {
            return;
        }
        int sw = c.getWindow().getGuiScaledWidth();
        int sh = c.getWindow().getGuiScaledHeight();
        if (sw <= 0 || sh <= 0) {
            return;
        }
        this.width = sw;
        this.height = sh;
        hub.syncViewport(sw, sh);
    }

    @Override
    public void resize(int width, int height) {
        Minecraft c = minecraft;
        if (c != null && c.getWindow() != null) {
            int sw = c.getWindow().getGuiScaledWidth();
            int sh = c.getWindow().getGuiScaledHeight();
            super.resize(sw, sh);
        } else {
            super.resize(width, height);
        }
        applyFullWindowBounds();
    }

    @Override
    protected void init() {
        clearWidgets();
        applyFullWindowBounds();
        hub.setReserveTopForVanillaCloseButton(false);
        hub.onScreenOpen();
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        applyFullWindowBounds();
        super.renderBackground(context, mouseX, mouseY, deltaTicks);
        UiDraw.fillMidnightBackground(context, this.width, this.height);
        hub.render(context, this.font, mouseX, mouseY, deltaTicks, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        applyFullWindowBounds();
        hub.updateNavHover(mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        applyFullWindowBounds();
        if (minecraft != null) {
            hub.applyEmbeddedLayout(minecraft.font);
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
    public boolean mouseReleased(MouseButtonEvent click) {
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
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
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
    public boolean keyPressed(KeyEvent keyInput) {
        for (Panel panel : DupeClient.getGuiManager().getPanels()) {
            if (panel.isVisible() && panel.keyPressed(keyInput.key(), keyInput.scancode(), keyInput.modifiers())) {
                return true;
            }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharacterEvent charInput) {
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
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(returnScreen);
        }
    }

    @Override
    public void removed() {
        hub.onRemoved();
        UiNativeRenderer.trimCache(48);
        super.removed();
    }
}
