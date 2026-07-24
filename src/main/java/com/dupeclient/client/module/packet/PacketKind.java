package com.dupeclient.client.module.packet;

import net.minecraft.network.packet.Packet;

public enum PacketKind {
    MOVEMENT,
    INTERACTION,
    INVENTORY,
    COMMAND,
    CHAT,
    KEEP_ALIVE,
    CUSTOM_PAYLOAD,
    OTHER;

    public static PacketKind fromPacket(Packet<?> packet) {
        String name = packet.getClass().getSimpleName();
        if (name.contains("Move") || name.contains("Position") || name.contains("Look") || name.contains("PlayerInput")) {
            return MOVEMENT;
        }
        if (name.contains("Interact") || name.contains("UseItem") || name.contains("Attack") || name.contains("HandSwing")) {
            return INTERACTION;
        }
        if (name.contains("Slot") || name.contains("Inventory") || name.contains("Click")) {
            return INVENTORY;
        }
        if (name.contains("Command")) {
            return COMMAND;
        }
        if (name.contains("Chat")) {
            return CHAT;
        }
        if (name.contains("KeepAlive") || name.contains("Pong")) {
            return KEEP_ALIVE;
        }
        if (name.contains("CustomPayload")) {
            return CUSTOM_PAYLOAD;
        }
        return OTHER;
    }
}
