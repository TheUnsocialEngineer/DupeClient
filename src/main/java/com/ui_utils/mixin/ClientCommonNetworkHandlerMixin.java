package com.ui_utils.mixin;

import com.ui_utils.MainClient;
import com.ui_utils.ResourcePackUiUtils;
import com.ui_utils.SharedVariables;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Shadow
    @Final
    protected MinecraftClient client;

    @Shadow
    public abstract void sendPacket(Packet<?> packet);

    @Inject(at = @At("HEAD"), method = "onResourcePackSend", cancellable = true)
    public void onResourcePackSend(ResourcePackSendS2CPacket packet, CallbackInfo ci) {
        ResourcePackUiUtils.Action action = ResourcePackUiUtils.actionFor(packet);
        if (action == ResourcePackUiUtils.Action.NONE) {
            return;
        }
        UUID packId = packet.id();
        if (action == ResourcePackUiUtils.Action.DECLINED) {
            this.sendPacket(ResourcePackUiUtils.statusPacket(packId, ResourcePackStatusC2SPacket.Status.DECLINED));
            MainClient.LOGGER.info(
                    "[UI Utils]: Resource pack declined, URL: {}",
                    packet.url() == null ? "<no url>" : packet.url());
            ci.cancel();
            return;
        }
        this.sendPacket(ResourcePackUiUtils.statusPacket(packId, ResourcePackStatusC2SPacket.Status.ACCEPTED));
        this.sendPacket(ResourcePackUiUtils.statusPacket(packId, ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
        MainClient.LOGGER.info(
                "[UI Utils]: Required resource pack bypassed, URL: {}",
                packet.url() == null ? "<no url>" : packet.url());
        ci.cancel();
    }
}
