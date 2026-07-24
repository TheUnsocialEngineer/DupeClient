package com.dupeclient.client.module.packet.sniffer;

public enum PacketDirection {
    C2S("C2S"),
    S2C("S2C");

    public final String label;

    PacketDirection(String label) {
        this.label = label;
    }
}
