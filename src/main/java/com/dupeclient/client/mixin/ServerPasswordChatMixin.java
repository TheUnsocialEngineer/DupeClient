package com.dupeclient.client.mixin;

import com.dupeclient.client.module.serverpassword.ServerPasswordManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ServerPasswordChatMixin {
    @Inject(method = "sendCommand", at = @At("HEAD"))
    private void dupeclient$onSendChatCommand(String command, CallbackInfo ci) {
        ServerPasswordManager.INSTANCE.onOutgoingChat("/" + command);
    }

    @Inject(method = "sendChat", at = @At("HEAD"))
    private void dupeclient$onSendChatMessage(String message, CallbackInfo ci) {
        ServerPasswordManager.INSTANCE.onOutgoingChat(message);
    }
}
