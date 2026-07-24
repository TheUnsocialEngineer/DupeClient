package com.dupeclient.client.module.hud;

import net.minecraft.client.MinecraftClient;

@FunctionalInterface
public interface HudTextProvider {
    String text(MinecraftClient client, HudManager hud);
}
