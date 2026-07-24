package com.dupeclient.client.ui.mui;

import icyllis.modernui.mc.MinecraftSurfaceView;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the pixel size of a {@link MinecraftSurfaceView} draw buffer. {@code onDraw} receives
 * the MUI <em>view-root</em> w/h; the actual GL FBO is {@code mSurfaceWidth} / {@code mSurfaceHeight} on
 * the same view, which is what {@code Renderer#onSurfaceChanged} reports. Layout {@link
 * icyllis.modernui.view.View#getWidth()}/{@code getHeight()} are not a reliable stand-in and can
 * stay smaller than the FBO, leaving black margins.
 */
public final class MuiSurfaceBufferSize {
    private MuiSurfaceBufferSize() {
    }

    /**
     * @return a two-element array {@code {width, height}} in surface pixels, never 0/negative
     */
    public static int[] resolveForDraw(
            @Nullable MinecraftSurfaceView surface, int reportW, int reportH, double scaleInv) {
        if (reportW <= 0 || reportH <= 0) {
            return new int[] {1, 1};
        }
        int[] fromFields = tryReflectFboSize(surface);
        if (fromFields != null) {
            return fromFields;
        }
        if (surface != null) {
            int vw = surface.getWidth();
            int vh = surface.getHeight();
            if (vw > 0 && vh > 0) {
                // Expand only if the root callback is smaller than what layout measured (stamped UI);
                // do not return view size alone; it can be smaller than the FBO.
                return new int[] {Math.max(reportW, vw), Math.max(reportH, vh)};
            }
        }
        if (scaleInv > 0.01 && scaleInv < 0.999) {
            return new int[] {
                    (int) Math.ceil(reportW / scaleInv),
                    (int) Math.ceil(reportH / scaleInv)
            };
        }
        return new int[] {reportW, reportH};
    }

    @Nullable
    private static int[] tryReflectFboSize(@Nullable MinecraftSurfaceView surface) {
        if (surface == null) {
            return null;
        }
        try {
            var c = surface.getClass();
            var w = c.getDeclaredField("mSurfaceWidth");
            var h = c.getDeclaredField("mSurfaceHeight");
            w.setAccessible(true);
            h.setAccessible(true);
            int iw = w.getInt(surface);
            int ih = h.getInt(surface);
            if (iw > 0 && ih > 0) {
                return new int[] {iw, ih};
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
