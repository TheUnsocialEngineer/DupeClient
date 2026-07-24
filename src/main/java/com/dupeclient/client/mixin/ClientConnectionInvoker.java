package com.dupeclient.client.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Connection.class)
public interface ClientConnectionInvoker {
    @Invoker("genericsFtw")
    void dupeclient$invokeHandlePacket(Packet<?> packet, PacketListener listener);
}
