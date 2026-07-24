package com.dupeclient.client.module.packet.fabricator;

import net.minecraft.world.inventory.ClickType;

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

    public ClickType toVanilla() {
        return switch (this) {
            case PICKUP -> ClickType.PICKUP;
            case QUICK_MOVE -> ClickType.QUICK_MOVE;
            case SWAP -> ClickType.SWAP;
            case CLONE -> ClickType.CLONE;
            case THROW, DROP_ITEM, DROP_STACK -> ClickType.THROW;
            case QUICK_CRAFT -> ClickType.QUICK_CRAFT;
            case PICKUP_ALL -> ClickType.PICKUP_ALL;
        };
    }
}
