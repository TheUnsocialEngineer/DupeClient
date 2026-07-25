package com.dupeclient.client.multiplayer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;

public final class MultiplayerScreens {
    private MultiplayerScreens() {
    }

    public static void returnToMultiplayer(MinecraftClient client, Screen parent) {
        if (client == null) {
            return;
        }
        if (isInGame(client)) {
            client.setScreen(inGameReturnTarget(parent));
            return;
        }
        if (parent instanceof MultiplayerScreen multiplayer) {
            client.setScreen(multiplayer);
            return;
        }
        if (parent != null) {
            client.setScreen(parent);
            return;
        }
        client.setScreen(new MultiplayerScreen(new TitleScreen()));
    }

    private static boolean isInGame(MinecraftClient client) {
        return client.player != null && client.world != null;
    }

    /**
     * When a vault/search/proxies screen is opened while connected to a server, Esc should
     * return to gameplay (or the screen that opened it), not the multiplayer server list.
     */
    private static Screen inGameReturnTarget(Screen parent) {
        Screen cursor = parent;
        while (cursor != null) {
            if (cursor instanceof MultiplayerScreen) {
                return null;
            }
            if (cursor instanceof MultiplayerNavigable navigable) {
                Screen next = navigable.getNavigationParent();
                if (next == null || next instanceof MultiplayerScreen) {
                    return null;
                }
                cursor = next;
                continue;
            }
            return cursor;
        }
        return null;
    }
}
