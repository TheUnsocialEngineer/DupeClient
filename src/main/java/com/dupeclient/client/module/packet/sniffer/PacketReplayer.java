package com.dupeclient.client.module.packet.sniffer;

import com.dupeclient.client.mixin.ClientConnectionInvoker;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.fabricator.ClickSlotPackets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;

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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            PacketSnifferManager.INSTANCE.feedback("Join a world to replay packets");
            return false;
        }
        return sendC2s(client, entry.packet);
    }

    public static boolean sendC2s(MinecraftClient client, Packet<?> packet) {
        if (client == null || packet == null) {
            return false;
        }
        Packet<?> toSend = packet;
        if (packet instanceof ClickSlotC2SPacket click && client.player != null && client.player.currentScreenHandler != null) {
            ClickSlotC2SPacket refreshed = ClickSlotPackets.refresh(click, client.player.currentScreenHandler);
            if (refreshed != null) {
                toSend = refreshed;
            }
        }
        PacketUtilsManager.INSTANCE.sendBypass(client, toSend);
        PacketMoveCodec.applyClientPrediction(client, toSend);
        PacketSnifferManager.INSTANCE.feedback("Sent C2S " + PacketSnifferEntry.nameOf(toSend));
        return true;
    }

    public static boolean sendEdited(MinecraftClient client, Packet<?> packet) {
        return sendC2s(client, packet);
    }

    public static boolean injectS2c(MinecraftClient client, Packet<?> packet) {
        if (client == null || client.getNetworkHandler() == null || packet == null) {
            return false;
        }
        ClientConnection connection = client.getNetworkHandler().getConnection();
        if (connection == null || !connection.isOpen()) {
            PacketSnifferManager.INSTANCE.feedback("Connection not open");
            return false;
        }
        PacketListener listener = connection.getPacketListener();
        if (!(listener instanceof ClientPlayPacketListener playListener)) {
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
