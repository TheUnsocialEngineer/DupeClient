package com.dupeclient.client.gui.overlay;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Draggable in-game panel (Packet Fabricator style) shown while playing, with input priority over the world.
 */
public interface IngameModuleOverlay {
    String id();

    boolean isModuleEnabled();

    boolean isOverlayVisible();

    void setOverlayVisible(boolean visible);

    default void toggleOverlayVisible() {
        setOverlayVisible(!isOverlayVisible());
    }

    default boolean isActive() {
        return isModuleEnabled() && isOverlayVisible();
    }

    int overlayX();

    int overlayY();

    void setOverlayPosition(int x, int y);

    int panelWidth();

    int panelHeight();

    default boolean containsPoint(double mouseX, double mouseY) {
        return mouseX >= overlayX()
                && mouseX < overlayX() + panelWidth()
                && mouseY >= overlayY()
                && mouseY < overlayY() + panelHeight();
    }

    void render(GuiGraphics context, int mouseX, int mouseY, float delta);

    /** @return true if the event was consumed (blocks game input). */
    boolean mouseClicked(double mouseX, double mouseY, int button);

    boolean mouseReleased(double mouseX, double mouseY, int button);

    boolean mouseDragged(double mouseX, double mouseY, int button);

    default boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return false;
    }

    boolean keyPressed(int keyCode);

    boolean charTyped(int codePoint);

    boolean hasTextFocus();

    default int priority() {
        return 0;
    }

    /** When false, overlay is drawn on the HUD and does not open the full-screen input host. */
    default boolean blocksGameInput() {
        return true;
    }

    default boolean isDragging() {
        return false;
    }
}
