package com.ui_utils;

import com.dupeclient.client.gui.ClientGuiScreen;
import com.dupeclient.client.gui.DupeClientUtilityScreen;
import com.dupeclient.client.gui.MacroEditorScreen;
import com.dupeclient.client.gui.overlay.IngameModuleOverlayScreen;
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
        return true;
    }

    public static boolean shouldRenderSyncPanel(Screen screen) {
        return SharedVariables.enabled
                && MinecraftClient.getInstance().player != null
                && screen instanceof HandledScreen;
    }
}
