package com.dupeclient.client.core;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.ClientGuiScreen;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
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
        if (screen instanceof ChatScreen || screen instanceof SleepingChatScreen) {
            return true;
        }
        String name = screen.getClass().getSimpleName();
        return name.contains("Chat")
                || name.contains("Sign")
                || name.contains("Anvil")
                || name.contains("BookEdit")
                || name.contains("CommandBlock");
    }

    public static boolean hasAnyTextInputFocus(MinecraftClient client) {
        if (client == null) {
            return false;
        }
        Screen screen = client.currentScreen;
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

    public static boolean shouldBlockGlobalHotkeys(MinecraftClient client) {
        return hasAnyTextInputFocus(client);
    }

    /** Block overlay/module toggle hotkeys while any text field or key-capture UI has focus. */
    public static boolean shouldBlockOverlayToggleHotkeys(MinecraftClient client) {
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
        Element focused = screen.getFocused();
        if (isTextInputElement(focused) && isElementFocused(focused)) {
            return true;
        }
        return scanForFocusedTextInput(screen);
    }

    private static boolean scanForFocusedTextInput(ParentElement parent) {
        for (Element child : parent.children()) {
            if (isTextInputElement(child) && isElementFocused(child)) {
                return true;
            }
            if (child instanceof ParentElement nested && scanForFocusedTextInput(nested)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTextInputElement(@Nullable Element element) {
        if (element == null) {
            return false;
        }
        if (element instanceof TextFieldWidget) {
            return true;
        }
        String simple = element.getClass().getSimpleName();
        return simple.contains("TextField")
                || simple.contains("EditBox")
                || simple.contains("TextInput")
                || simple.contains("ChatField")
                || simple.contains("SnbtTextArea");
    }

    private static boolean isElementFocused(Element element) {
        if (element instanceof ClickableWidget widget) {
            return widget.isFocused();
        }
        return false;
    }
}
