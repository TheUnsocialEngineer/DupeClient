package com.ui_utils.mixin;

import com.ui_utils.SharedVariables;
import com.ui_utils.features.CommandSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(at = @At("HEAD"), method = "handleChatInput", cancellable = true)
    public void sendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (!chatText.startsWith(SharedVariables.commandPrefix)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.gui.hud.getChat().addRecentChat(chatText);
        String result = CommandSystem.execute(chatText.substring(SharedVariables.commandPrefix.length()));
        if (mc.player != null && result != null && !result.isEmpty()) {
            for (String line : result.split("\n")) {
                mc.player.sendSystemMessage(Component.literal(line));
            }
        }
        mc.gui.setScreen(null);
        ci.cancel();
    }
}
