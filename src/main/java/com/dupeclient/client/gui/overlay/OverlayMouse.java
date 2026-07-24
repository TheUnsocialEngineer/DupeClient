package com.dupeclient.client.gui.overlay;

import net.minecraft.client.Minecraft;

public final class OverlayMouse {
    private OverlayMouse() {
    }

    public static double scaledX(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            return 0;
        }
        return client.mouseHandler.xpos()
                * client.getWindow().getGuiScaledWidth()
                / (double) client.getWindow().getScreenWidth();
    }

    public static double scaledY(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            return 0;
        }
        return client.mouseHandler.ypos()
                * client.getWindow().getGuiScaledHeight()
                / (double) client.getWindow().getScreenHeight();
    }
}
