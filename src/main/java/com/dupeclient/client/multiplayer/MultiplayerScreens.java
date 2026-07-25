package com.dupeclient.client.multiplayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

public final class MultiplayerScreens {
    private MultiplayerScreens() {
    }

    public static void returnToMultiplayer(Minecraft client, Screen parent) {
        if (client == null) {
            return;
        }
        if (isInGame(client)) {
            client.gui.setScreen(inGameReturnTarget(parent));
            return;
        }
        if (parent instanceof JoinMultiplayerScreen multiplayer) {
            client.gui.setScreen(multiplayer);
            return;
        }
        if (parent != null) {
            client.gui.setScreen(parent);
            return;
        }
        client.gui.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
    }

    private static boolean isInGame(Minecraft client) {
        return client.player != null && client.level != null;
    }

    /**
     * When a vault/search/proxies screen is opened while connected to a server, Esc should
     * return to gameplay (or the screen that opened it), not the multiplayer server list.
     */
    private static Screen inGameReturnTarget(Screen parent) {
        Screen cursor = parent;
        while (cursor != null) {
            if (cursor instanceof JoinMultiplayerScreen) {
                return null;
            }
            if (cursor instanceof MultiplayerNavigable navigable) {
                Screen next = navigable.getNavigationParent();
                if (next == null || next instanceof JoinMultiplayerScreen) {
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
