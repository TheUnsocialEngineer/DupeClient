package com.ui_utils.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ui_utils.SharedVariables;

@Mixin(Connection.class)
public class ClientConnectionMixin {

    // called when sending any packet
    @Inject(at = @At("HEAD"), method = "sendPacket", cancellable = true)
    public void sendImmediately(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flush, CallbackInfo ci) {
        // checks for if packets should be sent and if the packet is a gui related packet
        if (!SharedVariables.sendUIPackets && (packet instanceof ServerboundContainerClickPacket || packet instanceof ServerboundContainerButtonClickPacket)) {
            ci.cancel();
            return;
        }

        // checks for if packets should be delayed and if the packet is a gui related packet and is added to a list
        if (SharedVariables.delayUIPackets && (packet instanceof ServerboundContainerClickPacket || packet instanceof ServerboundContainerButtonClickPacket)) {
            SharedVariables.delayedUIPackets.add(packet);
            ci.cancel();
        }

        // cancels sign update packets if sign editing is disabled and re-enables sign editing
        if (!SharedVariables.shouldEditSign && (packet instanceof ServerboundSignUpdatePacket)) {
            SharedVariables.shouldEditSign = true;
            ci.cancel();
        }
    }
}
