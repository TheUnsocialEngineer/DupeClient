package com.dupeclient.client.module.packet.fabricator;

import net.minecraft.screen.slot.SlotActionType;

/** Maps fabricator actions to vanilla slot action types. */
public enum FabricatorSlotAction {
    PICKUP,
    QUICK_MOVE,
    SWAP,
    CLONE,
    THROW,
    QUICK_CRAFT,
    PICKUP_ALL,
    DROP_ITEM,
    DROP_STACK;

    public boolean usesFixedButton() {
        return fixedButton() >= 0;
    }

    public int fixedButton() {
        return switch (this) {
            case QUICK_MOVE, PICKUP_ALL, DROP_ITEM -> 0;
            case CLONE -> 2;
            case DROP_STACK -> 1;
            default -> -1;
        };
    }

    public SlotActionType toVanilla() {
        return switch (this) {
            case PICKUP -> SlotActionType.PICKUP;
            case QUICK_MOVE -> SlotActionType.QUICK_MOVE;
            case SWAP -> SlotActionType.SWAP;
            case CLONE -> SlotActionType.CLONE;
            case THROW, DROP_ITEM, DROP_STACK -> SlotActionType.THROW;
            case QUICK_CRAFT -> SlotActionType.QUICK_CRAFT;
            case PICKUP_ALL -> SlotActionType.PICKUP_ALL;
        };
    }
}
