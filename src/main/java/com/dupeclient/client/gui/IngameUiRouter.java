package com.dupeclient.client.gui;

import com.dupeclient.client.core.session.HubModuleRules;
import com.dupeclient.client.gui.ClientGuiScreen;
import com.dupeclient.client.gui.DupeMainMenuScreen;
import com.dupeclient.client.gui.StartupBlockedScreen;
import com.dupeclient.client.gui.SocialScreen;
import com.dupeclient.client.gui.WaypointsScreen;
import com.dupeclient.client.gui.overlay.IngameModuleOverlayScreen;
import com.dupeclient.client.module.hud.HudEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Opens in-game DupeClient UIs as plain vanilla {@link Screen}s.
 */
public final class IngameUiRouter {
    private IngameUiRouter() {
    }

    public static void openClientGui(Minecraft client) {
        if (client == null) {
            return;
        }
        client.setScreen(new ClientGuiScreen(client.screen));
    }

    public static void openSocial(Screen from) {
        if (!HubModuleRules.socialFeaturesAllowed()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(new SocialScreen(from));
        }
    }

    public static void openWaypoints(Screen from) {
        if (!HubModuleRules.socialFeaturesAllowed()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(new WaypointsScreen(from));
        }
    }

    public static void openHudEditor(Screen from) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(new HudEditorScreen(from));
        }
    }

    /** Closes DupeClient screens when leaving a world or disconnecting from a server. */
    public static void closeClientScreensOnLeave(Minecraft client) {
        if (client == null) {
            return;
        }
        Screen current = client.screen;
        if (current == null || !shouldCloseOnPlaySessionLeave(current)) {
            return;
        }
        client.setScreen(null);
    }

    private static boolean shouldCloseOnPlaySessionLeave(Screen screen) {
        if (screen instanceof IngameModuleOverlayScreen) {
            return true;
        }
        if (screen instanceof DupeMainMenuScreen || screen instanceof StartupBlockedScreen) {
            return false;
        }
        Package pkg = screen.getClass().getPackage();
        return pkg != null && pkg.getName().startsWith("com.dupeclient");
    }
}
