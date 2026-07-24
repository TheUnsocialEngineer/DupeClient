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
        if (parent instanceof MultiplayerScreen multiplayer) {
            client.setScreen(multiplayer);
            return;
        }
        client.setScreen(new MultiplayerScreen(new TitleScreen()));
    }
}
