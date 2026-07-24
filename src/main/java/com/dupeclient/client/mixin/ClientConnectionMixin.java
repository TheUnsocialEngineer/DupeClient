package com.dupeclient.client.mixin;

import com.dupeclient.client.module.acaudit.AcAuditManager;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferManager;
import com.dupeclient.client.module.security.SecurityManager;
import com.dupeclient.client.module.security.SecurityPacketContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ClientConnectionMixin {
    // MC 1.21+: handlePacket is static — injector callbacks must be static (Mixin requirement).
    @Inject(
            method = "genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V",
            at = @At("HEAD")
    )
    private static void dupeClient$handlePacketHead(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        SecurityPacketContext.push(packet);
    }

    @Inject(
            method = "genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V",
            at = @At("RETURN")
    )
    private static void dupeClient$handlePacketReturn(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        SecurityPacketContext.pop();
    }

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void dupeClient$interceptIncoming(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        PacketSnifferManager sniffer = PacketSnifferManager.INSTANCE;
        if (sniffer.shouldBlockIncoming(packet)) {
            ci.cancel();
            return;
        }
        if (sniffer.getSettings().enabled) {
            sniffer.observeIncoming(packet);
        }
        AcAuditManager audit = AcAuditManager.INSTANCE;
        if (audit.getSettings().enabled) {
            audit.observeIncoming(packet);
        }
        if (SecurityManager.INSTANCE.onIncomingPacket(packet)) {
            ci.cancel();
            return;
        }
        Connection connection = (Connection) (Object) this;
        if (PacketUtilsManager.INSTANCE.onIncomingPacket(connection, packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void dupeClient$interceptOutgoing(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        PacketSnifferManager sniffer = PacketSnifferManager.INSTANCE;
        if (sniffer.shouldBlockOutgoing(packet)) {
            ci.cancel();
            return;
        }
        if (sniffer.getSettings().enabled) {
            sniffer.observeOutgoing(packet);
        }
        AcAuditManager audit = AcAuditManager.INSTANCE;
        if (audit.getSettings().enabled) {
            audit.observeOutgoing(packet);
        }
        if (SecurityManager.INSTANCE.onOutgoingPacket(packet)) {
            ci.cancel();
            return;
        }
        Connection connection = (Connection) (Object) this;
        if (PacketUtilsManager.INSTANCE.onOutgoingPacket(connection, packet, callbacks, flush)) {
            ci.cancel();
        }
    }
}
