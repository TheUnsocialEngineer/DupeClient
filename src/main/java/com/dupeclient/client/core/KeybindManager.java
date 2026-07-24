package com.dupeclient.client.core;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindManager {
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("dupeclient", "general"));
    public static final KeyBinding OPEN_GUI_KEY = new KeyBinding(
            "key.dupeclient.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_CONTROL,
            CATEGORY
    );

    /** Opens the macro editor (new or load from within the editor). Rebind in Controls. */
    public static final KeyBinding OPEN_MACRO_EDITOR_KEY = new KeyBinding(
            "key.dupeclient.open_macro_editor",
            InputUtil.Type.KEYSYM,
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
