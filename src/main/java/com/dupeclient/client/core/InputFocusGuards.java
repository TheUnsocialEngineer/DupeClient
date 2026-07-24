package com.dupeclient.client.core;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.ClientGuiScreen;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Central checks so global hotkeys do not fire while the player is typing in chat or other text UI.
 */
public final class InputFocusGuards {
    private InputFocusGuards() {
    }

    public static boolean isTypingScreen(@Nullable Screen screen) {
        if (screen == null) {
            return false;
        }
        if (screen instanceof ChatScreen || screen instanceof InBedChatScreen) {
            return true;
        }
        String name = screen.getClass().getSimpleName();
        return name.contains("Chat")
                || name.contains("Sign")
                || name.contains("Anvil")
                || name.contains("BookEdit")
                || name.contains("CommandBlock");
    }

    public static boolean hasAnyTextInputFocus(Minecraft client) {
        if (client == null) {
            return false;
        }
        Screen screen = client.screen;
        if (isTypingScreen(screen)) {
            return true;
        }
        if (screenConsumesKeyboard(screen)) {
            return true;
        }
        if (screenHasFocusedTextInput(screen)) {
            return true;
        }
        return IngameOverlayHost.anyHasTextFocus();
    }

    public static boolean shouldBlockGlobalHotkeys(Minecraft client) {
        return hasAnyTextInputFocus(client);
    }

    /** Block overlay/module toggle hotkeys while any text field or key-capture UI has focus. */
    public static boolean shouldBlockOverlayToggleHotkeys(Minecraft client) {
        return shouldBlockGlobalHotkeys(client);
    }

    private static boolean screenConsumesKeyboard(@Nullable Screen screen) {
        return screen instanceof KeyboardConsumingScreen consuming && consuming.consumesGlobalHotkeys();
    }

    private static boolean screenHasFocusedTextInput(@Nullable Screen screen) {
        if (screen == null) {
            return false;
        }
        if (screen instanceof ClientGuiScreen) {
            return DupeClient.getGuiManager().hasFocusedTextInput();
        }
        GuiEventListener focused = screen.getFocused();
        if (isTextInputElement(focused) && isElementFocused(focused)) {
            return true;
        }
        return scanForFocusedTextInput(screen);
    }

    private static boolean scanForFocusedTextInput(ContainerEventHandler parent) {
        for (GuiEventListener child : parent.children()) {
            if (isTextInputElement(child) && isElementFocused(child)) {
                return true;
            }
            if (child instanceof ContainerEventHandler nested && scanForFocusedTextInput(nested)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTextInputElement(@Nullable GuiEventListener element) {
        if (element == null) {
            return false;
        }
        if (element instanceof EditBox) {
            return true;
        }
        String simple = element.getClass().getSimpleName();
        return simple.contains("TextField")
                || simple.contains("EditBox")
                || simple.contains("TextInput")
                || simple.contains("ChatField")
                || simple.contains("SnbtTextArea");
    }

    private static boolean isElementFocused(GuiEventListener element) {
        if (element instanceof AbstractWidget widget) {
            return widget.isFocused();
        }
        return false;
    }
}
