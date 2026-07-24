package com.dupeclient.client.module.packet.sniffer;

import com.dupeclient.client.mixin.ClientConnectionInvoker;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.fabricator.ClickSlotPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;

public final class PacketReplayer {
    private PacketReplayer() {
    }

    public static boolean replay(PacketSnifferEntry entry) {
        if (entry == null || entry.packet == null) {
            PacketSnifferManager.INSTANCE.feedback("No packet data to replay");
            return false;
        }
        if (!entry.canReplay()) {
            PacketSnifferManager.INSTANCE.feedback("Only C2S packets can be replayed");
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.getConnection() == null) {
            PacketSnifferManager.INSTANCE.feedback("Join a world to replay packets");
            return false;
        }
        return sendC2s(client, entry.packet);
    }

    public static boolean sendC2s(Minecraft client, Packet<?> packet) {
        if (client == null || packet == null) {
            return false;
        }
        Packet<?> toSend = packet;
        if (packet instanceof ServerboundContainerClickPacket click && client.player != null && client.player.containerMenu != null) {
            ServerboundContainerClickPacket refreshed = ClickSlotPackets.refresh(click, client.player.containerMenu);
            if (refreshed != null) {
                toSend = refreshed;
            }
        }
        PacketUtilsManager.INSTANCE.sendBypass(client, toSend);
        PacketMoveCodec.applyClientPrediction(client, toSend);
        PacketSnifferManager.INSTANCE.feedback("Sent C2S " + PacketSnifferEntry.nameOf(toSend));
        return true;
    }

    public static boolean sendEdited(Minecraft client, Packet<?> packet) {
        return sendC2s(client, packet);
    }

    public static boolean injectS2c(Minecraft client, Packet<?> packet) {
        if (client == null || client.getConnection() == null || packet == null) {
            return false;
        }
        Connection connection = client.getConnection().getConnection();
        if (connection == null || !connection.isConnected()) {
            PacketSnifferManager.INSTANCE.feedback("Connection not open");
            return false;
        }
        PacketListener listener = connection.getPacketListener();
        if (!(listener instanceof ClientGamePacketListener playListener)) {
            PacketSnifferManager.INSTANCE.feedback("S2C inject only supported in play phase");
            return false;
        }
        PacketUtilsManager.setIncomingHookBypassed(true);
        try {
            ((ClientConnectionInvoker) connection).dupeclient$invokeHandlePacket(packet, playListener);
        } finally {
            PacketUtilsManager.setIncomingHookBypassed(false);
        }
        PacketSnifferManager.INSTANCE.feedback("Injected S2C " + PacketSnifferEntry.nameOf(packet));
        return true;
    }
}
