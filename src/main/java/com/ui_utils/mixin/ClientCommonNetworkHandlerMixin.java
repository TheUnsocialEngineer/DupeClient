package com.ui_utils.mixin;

import com.ui_utils.MainClient;
import com.ui_utils.ResourcePackUiUtils;
import com.ui_utils.SharedVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    public abstract void send(Packet<?> packet);

    @Inject(at = @At("HEAD"), method = "handleResourcePackPush", cancellable = true)
    public void onResourcePackSend(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        ResourcePackUiUtils.Action action = ResourcePackUiUtils.actionFor(packet);
        if (action == ResourcePackUiUtils.Action.NONE) {
            return;
        }
        UUID packId = packet.id();
        if (action == ResourcePackUiUtils.Action.DECLINED) {
            this.send(ResourcePackUiUtils.statusPacket(packId, ServerboundResourcePackPacket.Action.DECLINED));
            MainClient.LOGGER.info(
                    "[UI Utils]: Resource pack declined, URL: {}",
                    packet.url() == null ? "<no url>" : packet.url());
            ci.cancel();
            return;
        }
        this.send(ResourcePackUiUtils.statusPacket(packId, ServerboundResourcePackPacket.Action.ACCEPTED));
        this.send(ResourcePackUiUtils.statusPacket(packId, ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        MainClient.LOGGER.info(
                "[UI Utils]: Required resource pack bypassed, URL: {}",
                packet.url() == null ? "<no url>" : packet.url());
        ci.cancel();
    }
}
