package com.dupeclient.client.module.packet.fabricator;

import net.minecraft.world.inventory.ContainerInput;

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

    public ContainerInput toVanilla() {
        return switch (this) {
            case PICKUP -> ContainerInput.PICKUP;
            case QUICK_MOVE -> ContainerInput.QUICK_MOVE;
            case SWAP -> ContainerInput.SWAP;
            case CLONE -> ContainerInput.CLONE;
            case THROW, DROP_ITEM, DROP_STACK -> ContainerInput.THROW;
            case QUICK_CRAFT -> ContainerInput.QUICK_CRAFT;
            case PICKUP_ALL -> ContainerInput.PICKUP_ALL;
        };
    }
}
