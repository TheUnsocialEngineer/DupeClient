package com.dupeclient.client.gui.overlay;

import net.minecraft.client.MinecraftClient;

public final class OverlayMouse {
    private OverlayMouse() {
    }

    public static double scaledX(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return 0;
        }
        return client.mouse.getX()
                * client.getWindow().getScaledWidth()
                / (double) client.getWindow().getWidth();
    }

    public static double scaledY(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return 0;
        }
        return client.mouse.getY()
                * client.getWindow().getScaledHeight()
                / (double) client.getWindow().getHeight();
    }
}
