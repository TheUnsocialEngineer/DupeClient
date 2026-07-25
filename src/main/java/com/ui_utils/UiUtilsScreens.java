package com.ui_utils;

import com.dupeclient.client.gui.ClientGuiScreen;
import com.dupeclient.client.gui.DupeClientUtilityScreen;
import com.dupeclient.client.gui.MacroEditorScreen;
import com.dupeclient.client.gui.SocialScreen;
import com.dupeclient.client.gui.WaypointsScreen;
import com.dupeclient.client.gui.overlay.IngameModuleOverlayScreen;
import com.dupeclient.client.module.dupedb.search.ServerScannerScreen;
import com.dupeclient.client.module.dupedb.search.ServerSearchAuthScreen;
import com.dupeclient.client.module.hud.HudEditorScreen;
import com.dupeclient.client.module.packet.sniffer.PacketWorkbenchScreen;
import com.dupeclient.client.module.serverpassword.ServerPasswordScreen;
import com.dupeclient.client.multiplayer.OfflineAccountsScreen;
import com.dupeclient.client.multiplayer.ProxiesScreen;
import com.dupeclient.client.multiplayer.SsidLoginScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

/**
 * Decides which {@link Screen}s receive the UI Utils button overlay.
 */
public final class UiUtilsScreens {
    private UiUtilsScreens() {
    }

    public static boolean isOverlayEnabled() {
        return SharedVariables.enabled;
    }

    public static boolean shouldAttachWidgets(Screen screen) {
        if (!SharedVariables.enabled || screen == null) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return false;
        }
        if (screen instanceof ChatScreen) {
            return false;
        }
        if (screen instanceof TitleScreen) {
            return false;
        }
        if (screen instanceof IngameModuleOverlayScreen) {
            return false;
        }
        if (screen instanceof ClientGuiScreen) {
            return false;
        }
        if (screen instanceof MacroEditorScreen) {
            return false;
        }
        if (screen instanceof DupeClientUtilityScreen) {
            return false;
        }
        if (screen instanceof HudEditorScreen) {
            return false;
        }
        if (screen instanceof ServerPasswordScreen) {
            return false;
        }
        if (screen instanceof ServerSearchAuthScreen) {
            return false;
        }
        if (screen instanceof ServerScannerScreen) {
            return false;
        }
        if (screen instanceof ProxiesScreen) {
            return false;
        }
        if (screen instanceof OfflineAccountsScreen) {
            return false;
        }
        if (screen instanceof SsidLoginScreen) {
            return false;
        }
        if (screen instanceof SocialScreen) {
            return false;
        }
        if (screen instanceof WaypointsScreen) {
            return false;
        }
        if (screen instanceof PacketWorkbenchScreen) {
            return false;
        }
        return true;
    }

    public static boolean shouldRenderSyncPanel(Screen screen) {
        return SharedVariables.enabled
                && MinecraftClient.getInstance().player != null
                && screen instanceof HandledScreen;
    }
}
