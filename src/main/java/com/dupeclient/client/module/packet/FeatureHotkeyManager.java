package com.dupeclient.client.module.packet;

import com.dupeclient.client.core.InputFocusGuards;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.HashMap;
import java.util.Map;

public class FeatureHotkeyManager {
    private final Map<Integer, Boolean> keyState = new HashMap<>();

    public boolean consumePress(MinecraftClient client, int keyCode) {
        if (keyCode == -1 || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN || client == null || client.getWindow() == null) {
            return false;
        }
        boolean down = InputUtil.isKeyPressed(client.getWindow(), keyCode);
        boolean wasDown = keyState.getOrDefault(keyCode, false);
        keyState.put(keyCode, down);
        if (InputFocusGuards.shouldBlockGlobalHotkeys(client)) {
            return false;
        }
        return down && !wasDown;
    }
}
