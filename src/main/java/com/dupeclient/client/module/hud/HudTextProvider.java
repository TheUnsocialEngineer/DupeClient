package com.dupeclient.client.module.hud;

import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface HudTextProvider {
    String text(Minecraft client, HudManager hud);
}
