package com.dupeclient.client.module.macro;

import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.core.session.HubModuleRules;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/**
 * Global run hotkeys stored on {@link MacroDefinition}: when no screen is open, pressing the key starts the macro,
 * or stops it if that macro is already running.
 */
public final class MacroQuickPlay {
    private static volatile boolean dirty = true;
    private static final Map<Long, String> packedToMacroId = new HashMap<>();
    private static final Map<Long, Boolean> edgeWasDown = new HashMap<>();
    private static int rescanCooldown;

    private MacroQuickPlay() {
    }

    public static void markDirty() {
        dirty = true;
    }

    public static void disableForStaffLock() {
        packedToMacroId.clear();
        edgeWasDown.clear();
        dirty = true;
    }

    public static void tick(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        if (!HubModuleRules.exploitFeaturesAllowed()) {
            return;
        }
        if (InputFocusGuards.shouldBlockGlobalHotkeys(client)) {
            return;
        }
        if (client.screen != null) {
            return;
        }
        if (dirty || --rescanCooldown <= 0) {
            rescanCooldown = 40;
            rebuild();
            dirty = false;
        }
        long win = client.getWindow().handle();
        for (Map.Entry<Long, String> e : packedToMacroId.entrySet()) {
            long packed = e.getKey();
            if (packed < 0) {
                continue;
            }
            int key = (int) (packed & 0xFFFF_FFFFL);
            int modsWanted = (int) (packed >>> 32);
            if (!glfwModsSatisfied(win, modsWanted)) {
                continue;
            }
            boolean down = modsWanted == 0
                    ? InputConstants.isKeyDown(client.getWindow(), key)
                    : GLFW.glfwGetKey(win, key) == GLFW.GLFW_PRESS;
            boolean was = edgeWasDown.getOrDefault(packed, false);
            if (down && !was) {
                String id = e.getValue();
                if (MacroEngine.INSTANCE.isRunning() && id.equals(MacroEngine.INSTANCE.getActiveMacroId())) {
                    MacroEngine.INSTANCE.stop(client);
                } else {
                    MacroEngine.INSTANCE.start(client, id);
                }
            }
            edgeWasDown.put(packed, down);
        }
    }

    private static void rebuild() {
        packedToMacroId.clear();
        edgeWasDown.clear();
        MacroStorage.prepare();
        for (String id : MacroStorage.listMacroIds()) {
            try {
                MacroDefinition d = MacroStorage.load(id);
                d.normalize();
                if (d.hotkeyKey < 0) {
                    continue;
                }
                long packed = MacroDefinition.packHotkey(d.hotkeyKey, d.hotkeyMods);
                if (packed >= 0) {
                    packedToMacroId.put(packed, id);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean glfwModsSatisfied(long window, int required) {
        if (required == 0) {
            return true;
        }
        int cur = 0;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
            cur |= GLFW.GLFW_MOD_SHIFT;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
            cur |= GLFW.GLFW_MOD_CONTROL;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS) {
            cur |= GLFW.GLFW_MOD_ALT;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SUPER) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SUPER) == GLFW.GLFW_PRESS) {
            cur |= GLFW.GLFW_MOD_SUPER;
        }
        return (cur & required) == required;
    }
}
