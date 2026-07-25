package com.dupeclient.client.multiplayer;

import net.minecraft.client.gui.screen.Screen;

/** Screens opened from the multiplayer header that store a return target. */
public interface MultiplayerNavigable {
    Screen getNavigationParent();
}
