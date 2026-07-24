package com.dupeclient.client.module.packet.sniffer;

import com.dupeclient.client.module.packet.sniffer.PacketDirection;

/** A packet type hidden from the sniffer log via log-exclude rules. */
public record LogHiddenPacket(PacketDirection direction, String name) {
}
