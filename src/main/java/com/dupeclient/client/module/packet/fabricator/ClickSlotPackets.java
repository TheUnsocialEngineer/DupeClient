package com.dupeclient.client.module.packet.fabricator;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

/**
 * Builds and refreshes {@link ServerboundContainerClickPacket} for the active screen handler (1.21.11 sync hash).
 */
public final class ClickSlotPackets {
    private ClickSlotPackets() {
    }

    public static ServerboundContainerClickPacket create(
            int syncId,
            int revision,
            int slot,
            int button,
            ContainerInput action) {
        return new ServerboundContainerClickPacket(
                syncId,
                revision,
                (short) slot,
                (byte) button,
                action,
                new Int2ObjectArrayMap<>(),
                HashedStack.EMPTY);
    }

    public static ServerboundContainerClickPacket refresh(ServerboundContainerClickPacket packet, AbstractContainerMenu handler) {
        if (packet == null || handler == null) {
            return packet;
        }
        return create(
                handler.containerId,
                handler.getStateId(),
                packet.slotNum(),
                packet.buttonNum(),
                packet.containerInput());
    }
}
