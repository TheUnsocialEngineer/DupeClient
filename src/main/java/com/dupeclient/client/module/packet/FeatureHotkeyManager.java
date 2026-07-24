package com.dupeclient.client.module.packet;

import com.dupeclient.client.core.InputFocusGuards;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

public class FeatureHotkeyManager {
    private final Map<Integer, Boolean> keyState = new HashMap<>();

    public boolean consumePress(Minecraft client, int keyCode) {
        if (keyCode == -1 || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN || client == null || client.getWindow() == null) {
            return false;
        }
        boolean down = InputConstants.isKeyDown(client.getWindow(), keyCode);
        boolean wasDown = keyState.getOrDefault(keyCode, false);
        keyState.put(keyCode, down);
        if (InputFocusGuards.shouldBlockGlobalHotkeys(client)) {
            return false;
        }
        return down && !wasDown;
    }
}
