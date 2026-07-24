package com.ui_utils.gui;

import com.ui_utils.SharedVariables;
import com.ui_utils.features.CommandSystem;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ChatTextFieldWidget extends CustomTextFieldWidget {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    public ChatTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput.key() == GLFW.GLFW_KEY_ENTER) {
            String message = getText();
            String toggleCmd = SharedVariables.commandPrefix + "toggleuiutils";
            if (message.equalsIgnoreCase(toggleCmd)) {
                SharedVariables.enabled = !SharedVariables.enabled;
                if (MC.player != null) {
                    MC.player.sendMessage(
                            Text.literal("§7UI-Utils: " + (SharedVariables.enabled ? "ON" : "OFF")), false);
                }
                setText("");
                return true;
            }
            if (!message.isEmpty()) {
                if (message.startsWith(SharedVariables.commandPrefix)) {
                    String result = CommandSystem.execute(message.substring(SharedVariables.commandPrefix.length()));
                    if (MC.player != null && result != null && !result.isEmpty()) {
                        for (String line : result.split("\n")) {
                            MC.player.sendMessage(Text.literal(line), false);
                        }
                    }
                } else if (MC.getNetworkHandler() != null) {
                    if (message.startsWith("/")) {
                        MC.getNetworkHandler().sendChatCommand(message.replaceFirst(Pattern.quote("/"), ""));
                    } else {
                        MC.getNetworkHandler().sendChatMessage(message);
                    }
                }
            }
            setText("");
            return true;
        }
        return super.keyPressed(keyInput);
    }
}
