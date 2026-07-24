package com.dupeclient.client.core.session;

import com.dupeclient.client.docs.ScreenshotCaptureMode;
import com.dupeclient.client.gui.StartupBlockedScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class SessionGate {
    private SessionGate() {
    }

    public static boolean isGameBlocked() {
        return !SessionBootstrap.INSTANCE.isHealthy();
    }

    public static void tick(MinecraftClient client) {
        if (ScreenshotCaptureMode.isActive()) {
            return;
        }
        if (client == null || !isGameBlocked()) {
            return;
        }
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().getConnection().disconnect(Text.literal("DupeClient startup check failed"));
        }
        if (client.currentScreen instanceof StartupBlockedScreen) {
            return;
        }
        client.setScreen(new StartupBlockedScreen());
    }
}
