package com.dupeclient.client.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Keeps a transparent overlay host screen open while a blocking module overlay is active,
 * so the cursor unlocks and clicks/scroll reach panels (HUD-only mouse is locked in FPP).
 */
public final class IngameOverlayScreens {
    private IngameOverlayScreens() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        Screen current = client.screen;
        boolean needHost = IngameOverlayHost.needsBlockingOverlayScreen();
        if (needHost) {
            if (current == null) {
                client.setScreen(IngameModuleOverlayScreen.get());
            }
            return;
        }
        if (IngameModuleOverlayScreen.isShowing(current)) {
            client.setScreen(null);
        }
    }
}
