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
        if (parent instanceof JoinMultiplayerScreen multiplayer) {
            client.setScreen(multiplayer);
            return;
        }
        client.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
    }
}
