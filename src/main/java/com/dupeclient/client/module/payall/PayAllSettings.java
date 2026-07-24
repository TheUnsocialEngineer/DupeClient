package com.dupeclient.client.module.payall;

import org.lwjgl.glfw.GLFW;

public final class PayAllSettings {
    public boolean enabled = true;
    public boolean overlayVisible = false;
    public int overlayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int overlayX = 16;
    public int overlayY = 48;
    public boolean moduleChatFeedback = true;
    /** When true, tab-list staff (Security rank detection) are never paid and auto-added to exclusions. */
    public boolean excludeStaff = true;
}
