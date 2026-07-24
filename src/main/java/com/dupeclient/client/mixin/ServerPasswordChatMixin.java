package com.dupeclient.client.mixin;

import com.dupeclient.client.module.serverpassword.ServerPasswordManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ServerPasswordChatMixin {
    @Inject(method = "sendChatCommand", at = @At("HEAD"))
    private void dupeclient$onSendChatCommand(String command, CallbackInfo ci) {
        ServerPasswordManager.INSTANCE.onOutgoingChat("/" + command);
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    private void dupeclient$onSendChatMessage(String message, CallbackInfo ci) {
        ServerPasswordManager.INSTANCE.onOutgoingChat(message);
    }
}
