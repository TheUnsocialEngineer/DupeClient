package com.dupeclient.client.module.utility;

import org.lwjgl.glfw.GLFW;

public final class ChatGamesSettings {
    public boolean enabled = false;
    public boolean overlayVisible = false;
    public int overlayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int overlayX = 20;
    public int overlayY = 120;
    public boolean chatFeedback = false;
    public int toggleKey = -1;
    public boolean mathsOnly = true;
    public boolean wordGames = true;
    public int cooldownSeconds = 2;
    /** When true, chat games turns off after leaving a world or disconnecting from a server. */
    public boolean disableOnLeave = true;
}
