package com.dupeclient.client.core.session;

import com.dupeclient.client.docs.ScreenshotCaptureMode;
import com.dupeclient.client.gui.StartupBlockedScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class SessionGate {
    private SessionGate() {
    }

    public static boolean isGameBlocked() {
        return !SessionBootstrap.INSTANCE.isHealthy();
    }

    public static void tick(Minecraft client) {
        if (ScreenshotCaptureMode.isActive()) {
            return;
        }
        if (client == null || !isGameBlocked()) {
            return;
        }
        if (client.getConnection() != null) {
            client.getConnection().getConnection().disconnect(Component.literal("DupeClient startup check failed"));
        }
        if (client.gui.screen() instanceof StartupBlockedScreen) {
            return;
        }
        client.gui.setScreen(new StartupBlockedScreen());
    }
}
