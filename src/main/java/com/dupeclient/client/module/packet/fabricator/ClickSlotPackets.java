package com.dupeclient.client.module.packet.fabricator;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;

/**
 * Builds and refreshes {@link ClickSlotC2SPacket} for the active screen handler (1.21.11 sync hash).
 */
public final class ClickSlotPackets {
    private ClickSlotPackets() {
    }

    public static ClickSlotC2SPacket create(
            int syncId,
            int revision,
            int slot,
            int button,
            SlotActionType action) {
        return new ClickSlotC2SPacket(
                syncId,
                revision,
                (short) slot,
                (byte) button,
                action,
                new Int2ObjectArrayMap<>(),
                ItemStackHash.EMPTY);
    }

    public static ClickSlotC2SPacket refresh(ClickSlotC2SPacket packet, ScreenHandler handler) {
        if (packet == null || handler == null) {
            return packet;
        }
        return create(
                handler.syncId,
                handler.getRevision(),
                packet.slot(),
                packet.button(),
                packet.actionType());
    }
}
