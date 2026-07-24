package com.dupeclient.client.ui.mui;

import icyllis.modernui.mc.MinecraftSurfaceView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Maps the OS cursor into {@link MinecraftSurfaceView} render space. Uses view-local
 * (window-pixel) offset from {@link #getLocationInWindow} and the <b>same</b> buffer
 * {@code w}/{@code h} the renderer uses — not {@link android.view.View#getWidth()} on every
 * frame, because the MUI layout can re-measure during pointer moves and the ratio
 * {@code bufferW / view.getWidth()} will jump (looks like the whole GUI resizes with the cursor).
 */
public final class MuiSurfaceMouseMapper {
    private MuiSurfaceMouseMapper() {
    }

    /**
     * @param bufferW buffer width from {@code Renderer#onDraw} / {@code onSurfaceChanged}
     * @param bufferH same for height
     */
    public static double[] windowToSurface(
            @Nullable MinecraftSurfaceView v, int bufferW, int bufferH, MinecraftClient mc) {
        if (bufferW <= 0 || bufferH <= 0) {
            return new double[] {0, 0};
        }
        if (v == null) {
            return fallbackFullWindow(bufferW, bufferH, mc);
        }
        int[] loc = new int[2];
        v.getLocationInWindow(loc);
        double lx = mc.mouse.getX() - (double) loc[0];
        double ly = mc.mouse.getY() - (double) loc[1];
        // MinecraftSurfaceView: view client area and DrawContext buffer w/h are 1:1 in window
        // pixels. Do not scale by v.getWidth()/getHeight() — they can change during MUI relayout
        // on hover, which made the old ratio w/vW flicker and the whole UI look "resized" with the
        // pointer.
        double ox = MathHelper.clamp(lx, 0.0, (double) Math.max(0, bufferW - 1));
        double oy = MathHelper.clamp(ly, 0.0, (double) Math.max(0, bufferH - 1));
        return new double[] {ox, oy};
    }

    private static double[] fallbackFullWindow(int w, int h, MinecraftClient mc) {
        var win = mc.getWindow();
        if (w <= 0 || h <= 0 || win.getScaledWidth() <= 0 || win.getScaledHeight() <= 0) {
            return new double[] {0, 0};
        }
        return new double[] {
                mc.mouse.getScaledX(win) * w / (double) win.getScaledWidth(),
                mc.mouse.getScaledY(win) * h / (double) win.getScaledHeight()
        };
    }
}
