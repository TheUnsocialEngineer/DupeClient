package com.ui_utils.gui;

import com.ui_utils.SharedVariables;
import com.ui_utils.features.CommandSystem;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ChatTextFieldWidget extends CustomTextFieldWidget {
    private static final Minecraft MC = Minecraft.getInstance();

    public ChatTextFieldWidget(Font textRenderer, int x, int y, int width, int height, Component text) {
        super(textRenderer, x, y, width, height, text);
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.key() == GLFW.GLFW_KEY_ENTER) {
            String message = getValue();
            String toggleCmd = SharedVariables.commandPrefix + "toggleuiutils";
            if (message.equalsIgnoreCase(toggleCmd)) {
                SharedVariables.enabled = !SharedVariables.enabled;
                if (MC.player != null) {
                    MC.player.displayClientMessage(
                            Component.literal("§7UI-Utils: " + (SharedVariables.enabled ? "ON" : "OFF")), false);
                }
                setValue("");
                return true;
            }
            if (!message.isEmpty()) {
                if (message.startsWith(SharedVariables.commandPrefix)) {
                    String result = CommandSystem.execute(message.substring(SharedVariables.commandPrefix.length()));
                    if (MC.player != null && result != null && !result.isEmpty()) {
                        for (String line : result.split("\n")) {
                            MC.player.displayClientMessage(Component.literal(line), false);
                        }
                    }
                } else if (MC.getConnection() != null) {
                    if (message.startsWith("/")) {
                        MC.getConnection().sendCommand(message.replaceFirst(Pattern.quote("/"), ""));
                    } else {
                        MC.getConnection().sendChat(message);
                    }
                }
            }
            setValue("");
            return true;
        }
        return super.keyPressed(keyInput);
    }
}
