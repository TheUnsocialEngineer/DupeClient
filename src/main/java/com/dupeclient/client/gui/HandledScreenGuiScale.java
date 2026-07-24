package com.dupeclient.client.gui;

import com.dupeclient.client.compat.ModCompat;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.world.inventory.Slot;
import org.joml.Matrix3x2fStack;

/**
 * Scales {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen} rendering without changing global GUI scale.
 */
public final class HandledScreenGuiScale {
    public static final float[] SCALE_PRESETS = {1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
    /** Creative tab buttons extend above/below the panel background. */
    public static final int CREATIVE_TAB_BAND = 32;
    /** Vanilla double-chest width; larger panels get an auto-scale boost when enabled. */
    public static final int LARGE_PANEL_THRESHOLD = 276;

    private static final ThreadLocal<int[]> CURRENT_PANEL = ThreadLocal.withInitial(() -> new int[2]);

    private HandledScreenGuiScale() {
    }

    public static void bindPanelSize(int backgroundWidth, int backgroundHeight) {
        int[] panel = CURRENT_PANEL.get();
        panel[0] = backgroundWidth;
        panel[1] = backgroundHeight;
    }

    public static void clearPanelSize() {
        int[] panel = CURRENT_PANEL.get();
        panel[0] = 0;
        panel[1] = 0;
    }

    public static boolean isActive() {
        PacketUtilsSettings settings = PacketUtilsManager.INSTANCE.getSettings();
        return settings.handledScreenScaleEnabled && settings.handledScreenScale > 1.001f;
    }

    public static float getScale() {
        PacketUtilsSettings settings = PacketUtilsManager.INSTANCE.getSettings();
        if (!settings.handledScreenScaleEnabled) {
            return 1.0f;
        }
        float scale = clamp(settings.handledScreenScale);
        if (!settings.handledScreenAutoScaleLarge) {
            return scale;
        }
        int[] panel = CURRENT_PANEL.get();
        int backgroundWidth = panel[0];
        int backgroundHeight = panel[1];
        if (backgroundWidth <= 0 && backgroundHeight <= 0) {
            return scale;
        }
        int maxDim = Math.max(backgroundWidth, backgroundHeight);
        if (maxDim > LARGE_PANEL_THRESHOLD) {
            float boost = Math.min(0.45f, (maxDim - LARGE_PANEL_THRESHOLD) / 520f);
            if (ModCompat.expectsModdedContainerWidgets()) {
                boost = Math.min(0.55f, boost + 0.08f);
            }
            scale = clamp(scale + boost);
        }
        return scale;
    }

    public static float clamp(float scale) {
        return Math.max(1.0f, Math.min(2.0f, scale));
    }

    public static float cyclePreset(float current) {
        float clamped = clamp(current);
        for (int i = 0; i < SCALE_PRESETS.length; i++) {
            if (Math.abs(SCALE_PRESETS[i] - clamped) < 0.02f) {
                return SCALE_PRESETS[(i + 1) % SCALE_PRESETS.length];
            }
        }
        return 1.5f;
    }

    public static String formatScale(float scale) {
        if (Math.abs(scale - Math.round(scale)) < 0.02f) {
            return Integer.toString(Math.round(scale)) + "x";
        }
        return String.format(java.util.Locale.ROOT, "%.2fx", scale);
    }

    public static void syncGuiPosition(int[] outXY, int backgroundWidth, int backgroundHeight, int screenWidth, int screenHeight) {
        outXY[0] = (screenWidth - backgroundWidth) / 2;
        outXY[1] = (screenHeight - backgroundHeight) / 2;
    }

    /** Scale around the panel center in absolute screen coordinates (for {@code drawBackground}). */
    public static void pushScaleScreen(GuiGraphics context, int guiLeft, int guiTop, int backgroundWidth, int backgroundHeight) {
        float scale = getScale();
        if (scale <= 1.001f) {
            return;
        }
        float centerX = guiLeft + backgroundWidth / 2.0f;
        float centerY = guiTop + backgroundHeight / 2.0f;
        applyScaleAround(context, centerX, centerY, scale);
    }

    /**
     * Scale around the panel center in GUI-local coordinates (after {@code translate(x, y)} in {@code renderMain}).
     */
    public static void pushScaleLocal(GuiGraphics context, int backgroundWidth, int backgroundHeight) {
        float scale = getScale();
        if (scale <= 1.001f) {
            return;
        }
        float centerX = backgroundWidth / 2.0f;
        float centerY = backgroundHeight / 2.0f;
        applyScaleAround(context, centerX, centerY, scale);
    }

    private static void applyScaleAround(GuiGraphics context, float centerX, float centerY, float scale) {
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(centerX, centerY);
        matrices.scale(scale, scale);
        matrices.translate(-centerX, -centerY);
    }

    public static void popScale(GuiGraphics context) {
        if (!isActive()) {
            return;
        }
        context.pose().popMatrix();
    }

    /** Maps a GUI-local point to screen space using the same transform as rendering. */
    public static float[] localToScreen(float localX, float localY, int guiLeft, int guiTop, int backgroundWidth, int backgroundHeight) {
        float scale = getScale();
        float centerX = guiLeft + backgroundWidth / 2.0f;
        float centerY = guiTop + backgroundHeight / 2.0f;
        if (scale <= 1.001f) {
            return new float[] {guiLeft + localX, guiTop + localY};
        }
        return new float[] {
                centerX + (localX - backgroundWidth / 2.0f) * scale,
                centerY + (localY - backgroundHeight / 2.0f) * scale
        };
    }

    /** Inverse of {@link #localToScreen} for hit-testing. */
    public static float[] screenToLocal(double screenX, double screenY, int guiLeft, int guiTop, int backgroundWidth, int backgroundHeight) {
        float scale = getScale();
        float centerX = guiLeft + backgroundWidth / 2.0f;
        float centerY = guiTop + backgroundHeight / 2.0f;
        if (scale <= 1.001f) {
            return new float[] {(float) (screenX - guiLeft), (float) (screenY - guiTop)};
        }
        return new float[] {
                backgroundWidth / 2.0f + (float) ((screenX - centerX) / scale),
                backgroundHeight / 2.0f + (float) ((screenY - centerY) / scale)
        };
    }

    /**
     * GUI-local bounds test using screen-space mouse coordinates (matches {@code HandledScreen#isPointWithinBounds}).
     */
    public static boolean isPointWithinScaledBounds(
            double screenX,
            double screenY,
            int localX,
            int localY,
            int localWidth,
            int localHeight,
            int guiLeft,
            int guiTop,
            int backgroundWidth,
            int backgroundHeight) {
        float[] local = screenToLocal(screenX, screenY, guiLeft, guiTop, backgroundWidth, backgroundHeight);
        return local[0] >= localX - 1
                && local[0] < localX + localWidth + 1
                && local[1] >= localY - 1
                && local[1] < localY + localHeight + 1;
    }

    /**
     * Hit-test for a creative inventory tab when {@link #isActive()}.
     * Accepts either gui-linear offsets ({@code click - guiOrigin}) or absolute screen coordinates.
     */
    public static boolean isCreativeTabHit(
            double mouseX,
            double mouseY,
            int tabX,
            int tabY,
            int tabWidth,
            int tabHeight,
            int guiLeft,
            int guiTop,
            int backgroundWidth,
            int backgroundHeight) {
        return creativeTabHitAt(
                        mouseX, mouseY, tabX, tabY, tabWidth, tabHeight,
                        guiLeft, guiTop, backgroundWidth, backgroundHeight, true)
                || creativeTabHitAt(
                        mouseX, mouseY, tabX, tabY, tabWidth, tabHeight,
                        guiLeft, guiTop, backgroundWidth, backgroundHeight, false);
    }

    private static boolean creativeTabHitAt(
            double mouseX,
            double mouseY,
            int tabX,
            int tabY,
            int tabWidth,
            int tabHeight,
            int guiLeft,
            int guiTop,
            int backgroundWidth,
            int backgroundHeight,
            boolean guiOffset) {
        double screenX = guiOffset ? mouseX + guiLeft : mouseX;
        double screenY = guiOffset ? mouseY + guiTop : mouseY;
        float[] local = screenToLocal(screenX, screenY, guiLeft, guiTop, backgroundWidth, backgroundHeight);
        return local[0] >= tabX
                && local[0] <= tabX + tabWidth
                && local[1] >= tabY
                && local[1] <= tabY + tabHeight;
    }

    /**
     * Converts screen coords to fake screen coords for slot hit tests (subtracts guiLeft/guiTop internally).
     */
    public static double[] toFakeScreenCoords(
            double screenX,
            double screenY,
            int guiLeft,
            int guiTop,
            int backgroundWidth,
            int backgroundHeight) {
        float scale = getScale();
        if (scale <= 1.001f) {
            return new double[] {screenX, screenY};
        }
        double centerX = guiLeft + backgroundWidth / 2.0;
        double centerY = guiTop + backgroundHeight / 2.0;
        return new double[] {
                centerX + (screenX - centerX) / scale,
                centerY + (screenY - centerY) / scale
        };
    }

    /** Right edge of the scaled panel in screen coordinates. */
    public static int scaledPanelRight(int guiLeft, int backgroundWidth) {
        float scale = getScale();
        if (scale <= 1.001f) {
            return guiLeft + backgroundWidth;
        }
        double halfW = backgroundWidth / 2.0;
        double centerX = guiLeft + halfW;
        return (int) Math.ceil(centerX + halfW * scale);
    }

    /** Extra X offset so status effects sit right of the scaled panel (vanilla uses unscaled right edge). */
    public static int effectColumnOffsetX(int guiLeft, int backgroundWidth) {
        float scale = getScale();
        if (scale <= 1.001f) {
            return 0;
        }
        return scaledPanelRight(guiLeft, backgroundWidth) - (guiLeft + backgroundWidth);
    }

    public static boolean isClickOutsideScaled(
            double screenX,
            double screenY,
            int guiLeft,
            int guiTop,
            int backgroundWidth,
            int backgroundHeight) {
        float scale = getScale();
        if (scale <= 1.001f) {
            return screenX < guiLeft
                    || screenY < guiTop
                    || screenX >= guiLeft + backgroundWidth
                    || screenY >= guiTop + backgroundHeight;
        }
        double centerX = guiLeft + backgroundWidth / 2.0;
        double centerY = guiTop + backgroundHeight / 2.0;
        double halfW = backgroundWidth / 2.0;
        double halfH = backgroundHeight / 2.0;
        double left = centerX - halfW * scale;
        double top = centerY - halfH * scale;
        double right = centerX + halfW * scale;
        double bottom = centerY + halfH * scale;
        return screenX < left || screenY < top || screenX >= right || screenY >= bottom;
    }

    /** Screen-space hit box for an 18×18 slot after scaling (matches vanilla ±1 px padding). */
    public static boolean isPointOverScaledSlot(
            double screenX,
            double screenY,
            Slot slot,
            int guiLeft,
            int guiTop,
            int backgroundWidth,
            int backgroundHeight) {
        float scale = getScale();
        float[] topLeft = localToScreen(slot.x, slot.y, guiLeft, guiTop, backgroundWidth, backgroundHeight);
        double size = 16.0 * scale;
        return screenX >= topLeft[0] - 1.0
                && screenX < topLeft[0] + size + 1.0
                && screenY >= topLeft[1] - 1.0
                && screenY < topLeft[1] + size + 1.0;
    }

    /** UI Utils buttons/fields use fixed screen coordinates and must not follow panel scaling. */
    public static boolean isScreenFixedOverlayWidget(GuiEventListener child) {
        String name = child.getClass().getName();
        return name.startsWith("com.ui_utils.gui.")
                || name.startsWith("com.dupeclient.client.module.packet.fabricator.");
    }

    /** Third-party mod widgets that should follow bigger-container scaling when on/near the panel. */
    public static boolean isThirdPartyContainerWidget(GuiEventListener child) {
        String name = child.getClass().getName();
        return name.startsWith("com.chaosmrp.axiom")
                || name.contains(".axiom.")
                || name.startsWith("meteordevelopment.meteorclient")
                || name.contains(".voxy.")
                || name.startsWith("dev.voxy.");
    }

    public static boolean shouldLayoutWithPanel(GuiEventListener child) {
        if (isScreenFixedOverlayWidget(child)) {
            return false;
        }
        return true;
    }

    /** Whether a widget should be repositioned with the scaled panel bounds. */
    public static boolean shouldScaleWithPanel(
            GuiEventListener child, int guiLeft, int guiTop, int backgroundWidth, int backgroundHeight) {
        if (!(child instanceof AbstractWidget widget)) {
            return false;
        }
        if (isScreenFixedOverlayWidget(child)) {
            return false;
        }
        if (!isThirdPartyContainerWidget(child)) {
            return true;
        }
        int margin = ModCompat.expectsModdedContainerWidgets() ? 72 : 48;
        return widget.getX() >= guiLeft - margin
                && widget.getY() >= guiTop - margin
                && widget.getX() <= guiLeft + backgroundWidth + margin
                && widget.getY() <= guiTop + backgroundHeight + margin;
    }

    /** Restores a screen-fixed overlay widget to its init-time screen position and size. */
    public static void pinScreenFixedWidget(AbstractWidget widget, java.util.Map<GuiEventListener, int[]> screenBases) {
        int[] base = screenBases.computeIfAbsent(widget, ignored -> new int[] {
            widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()
        });
        widget.setX(base[0]);
        widget.setY(base[1]);
        widget.setWidth(base[2]);
        widget.setHeight(base[3]);
    }

    /** Captures a widget's unscaled GUI-local bounds for {@link #layoutWidget}. */
    public static int[] captureWidgetLocalBounds(
            AbstractWidget widget, int guiLeft, int guiTop, int backgroundWidth, int backgroundHeight) {
        int dx = widget.getX() - guiLeft;
        int dy = widget.getY() - guiTop;
        float scale = getScale();
        if (!isActive() || scale <= 1.001f) {
            return new int[] {dx, dy, widget.getWidth(), widget.getHeight()};
        }
        float[] inverse = screenToLocal(widget.getX(), widget.getY(), guiLeft, guiTop, backgroundWidth, backgroundHeight);
        int lx = Math.round(inverse[0]);
        int ly = Math.round(inverse[1]);
        // After layoutWidget the widget sits in scaled screen space; delta-from-guiLeft no longer matches local coords.
        if (Math.abs(lx - dx) > 2 || Math.abs(ly - dy) > 2) {
            return new int[] {
                    lx,
                    ly,
                    Math.max(1, Math.round(widget.getWidth() / scale)),
                    Math.max(1, Math.round(widget.getHeight() / scale))
            };
        }
        return new int[] {dx, dy, widget.getWidth(), widget.getHeight()};
    }

    /** Repositions a child widget from unscaled GUI-local layout coords to scaled screen coords. */
    public static void layoutWidget(
            AbstractWidget widget,
            int localX,
            int localY,
            int localWidth,
            int localHeight,
            int guiLeft,
            int guiTop,
            int backgroundWidth,
            int backgroundHeight) {
        float scale = getScale();
        float[] pos = localToScreen(localX, localY, guiLeft, guiTop, backgroundWidth, backgroundHeight);
        widget.setX((int) pos[0]);
        widget.setY((int) pos[1]);
        widget.setWidth(Math.max(1, Math.round(localWidth * scale)));
        widget.setHeight(Math.max(1, Math.round(localHeight * scale)));
    }
}
