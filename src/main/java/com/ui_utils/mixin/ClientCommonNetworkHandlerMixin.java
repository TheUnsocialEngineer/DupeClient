package com.ui_utils.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ui_utils.MainClient;
import com.ui_utils.SharedVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    public abstract void send(Packet<?> packet);

    @Inject(at = @At("HEAD"), method = "handleResourcePackPush", cancellable = true)
    public void onResourcePackSend(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (SharedVariables.bypassResourcePack && (packet.required() || SharedVariables.resourcePackForceDeny)) {
            this.send(new ServerboundResourcePackPacket(Minecraft.getInstance().getUser().getProfileId(), ServerboundResourcePackPacket.Action.ACCEPTED));
            this.send(new ServerboundResourcePackPacket(Minecraft.getInstance().getUser().getProfileId(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
            MainClient.LOGGER.info(
                    "[UI Utils]: Required Resource Pack Bypassed, Message: " +
                            (packet.prompt().isEmpty() ? "<no message>" : packet.prompt().toString()) +
                            ", URL: " + (packet.url() == null ? "<no url>" : packet.url())
            );
            ci.cancel();
        }
    }
}
