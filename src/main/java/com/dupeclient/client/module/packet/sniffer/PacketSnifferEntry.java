package com.dupeclient.client.module.packet.sniffer;

import com.dupeclient.client.module.packet.PacketUtils;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;

public final class PacketSnifferEntry {
    public final long id;
    public final long timeMs;
    public final PacketDirection direction;
    public final String name;
    public final String detail;
    @Nullable
    public final Packet<?> packet;
    public final String editableText;

    public PacketSnifferEntry(
            long id,
            long timeMs,
            PacketDirection direction,
            String name,
            String detail,
            @Nullable Packet<?> packet,
            String editableText) {
        this.id = id;
        this.timeMs = timeMs;
        this.direction = direction;
        this.name = name;
        this.detail = detail == null ? "" : detail;
        this.packet = packet;
        this.editableText = editableText == null ? "" : editableText;
    }

    public static String nameOf(Packet<?> packet) {
        return packet == null ? "?" : PacketUtils.getPacketTypeName(packet);
    }

    public boolean canSend() {
        return isC2s() && (packet != null || !editableText.isBlank());
    }

    public boolean isC2s() {
        return direction == PacketDirection.C2S;
    }

    public boolean canReplay() {
        return isC2s() && packet != null;
    }

    public boolean canFabricate() {
        return isC2s();
    }

    public String displayLine() {
        if (detail.isBlank()) {
            return direction.label + " " + name;
        }
        return direction.label + " " + name + " — " + detail;
    }
}
