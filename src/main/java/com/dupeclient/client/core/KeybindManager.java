package com.dupeclient.client.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindManager {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("dupeclient", "general"));
    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
            "key.dupeclient.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_CONTROL,
            CATEGORY
    );

    /** Opens the macro editor (new or load from within the editor). Rebind in Controls. */
    public static final KeyMapping OPEN_MACRO_EDITOR_KEY = new KeyMapping(
            "key.dupeclient.open_macro_editor",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            CATEGORY
    );

    private KeybindManager() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_GUI_KEY);
        KeyBindingHelper.registerKeyBinding(OPEN_MACRO_EDITOR_KEY);
    }
}
