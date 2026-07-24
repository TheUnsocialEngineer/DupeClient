package com.dupeclient.client.ui.mui;

import icyllis.modernui.mc.MinecraftSurfaceView;
import net.minecraft.client.MinecraftClient;
import icyllis.modernui.view.View;

/**
 * Keeps the last pointer position in the surface view's local coordinates (as delivered by
 * Modern UI), which match {@link MinecraftSurfaceView} rendering. {@link MinecraftClient#mouse}
 * is not always in the same space as the mVUS host, so we prefer events over polling the client.
 */
final class MuiSurfacePointerState {
    volatile float localX = Float.NaN;
    volatile float localY = Float.NaN;

    void record(View v, float lx, float ly) {
        if (v == null) {
            return;
        }
        if (v.getWidth() > 0 && v.getHeight() > 0) {
            this.localX = lx;
            this.localY = ly;
        }
    }

    double[] surfaceForDraw(MinecraftSurfaceView v, int surfaceW, int surfaceH, MinecraftClient mc) {
        if (v != null
                && v.getWidth() > 0
                && v.getHeight() > 0
                && !Float.isNaN(localX)
                && !Float.isNaN(localY)) {
            return new double[] {
                    localX * surfaceW / (double) v.getWidth(),
                    localY * surfaceH / (double) v.getHeight()
            };
        }
        return MuiSurfaceMouseMapper.windowToSurface(v, surfaceW, surfaceH, mc);
    }
}
