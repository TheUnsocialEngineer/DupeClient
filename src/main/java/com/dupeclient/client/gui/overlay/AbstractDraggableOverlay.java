package com.dupeclient.client.gui.overlay;

import org.lwjgl.glfw.GLFW;

/**
 * Shared title-bar drag behaviour for in-game module overlays.
 */
public abstract class AbstractDraggableOverlay implements IngameModuleOverlay {
    protected boolean dragging;
    protected double dragOffX;
    protected double dragOffY;

    protected boolean beginTitleDrag(double mouseX, double mouseY, int button, int px, int py, int w, int titleH) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (mouseX >= px && mouseX < px + w && mouseY >= py && mouseY < py + titleH) {
            dragging = true;
            dragOffX = mouseX - px;
            dragOffY = mouseY - py;
            return true;
        }
        return false;
    }

    protected boolean updateTitleDrag(double mouseX, double mouseY, int button) {
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        setOverlayPosition((int) (mouseX - dragOffX), (int) (mouseY - dragOffY));
        return true;
    }

    protected void endTitleDrag(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
        }
    }

    @Override
    public boolean isDragging() {
        return dragging;
    }

    protected static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
