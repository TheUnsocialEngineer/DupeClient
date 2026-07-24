package com.dupeclient.client.module.security;

import net.minecraft.network.protocol.Packet;

/**
 * OpSec-style {@code PacketContext}: marks when the client is inside inbound packet handling so content can be
 * tagged. Best-effort; see {@link SecurityKeyResolution} for mod-probe rules that do not depend on marking.
 */
public final class SecurityPacketContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<String> PACKET_NAME = ThreadLocal.withInitial(() -> "unknown");

    private SecurityPacketContext() {
    }

    public static void push(Packet<?> packet) {
        setPacketName(packet);
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void pop() {
        int d = DEPTH.get();
        if (d > 0) {
            DEPTH.set(d - 1);
        }
    }

    public static boolean isProcessingPacket() {
        return DEPTH.get() > 0;
    }

    public static String getPacketName() {
        return PACKET_NAME.get();
    }

    public static void setPacketName(Object packet) {
        if (packet instanceof Packet<?> p) {
            try {
                PACKET_NAME.set(p.type().id().toString());
            } catch (Exception e) {
                PACKET_NAME.set("unknown");
            }
        }
    }
}
