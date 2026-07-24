package com.dupeclient.client.module.packet.fabricator;

/** Inventory fabrication actions (ported from YungLight PackUtil fabricator). */
public enum FabricatorAction {
    CLICK("Click", true, false),
    QUICK_MOVE("Quick Move", false, false),
    PICKUP_ALL("Pickup All", false, false),
    DROP("Drop", false, true);

    public final String displayName;
    public final boolean usesClickButton;
    public final boolean usesDropToggle;

    FabricatorAction(String displayName, boolean usesClickButton, boolean usesDropToggle) {
        this.displayName = displayName;
        this.usesClickButton = usesClickButton;
        this.usesDropToggle = usesDropToggle;
    }

    public FabricatorSlotAction toSlotAction(boolean dropWholeStack, int clickButtonIndex) {
        return switch (this) {
            case CLICK -> FabricatorSlotAction.PICKUP;
            case QUICK_MOVE -> FabricatorSlotAction.QUICK_MOVE;
            case PICKUP_ALL -> FabricatorSlotAction.PICKUP_ALL;
            case DROP -> dropWholeStack ? FabricatorSlotAction.DROP_STACK : FabricatorSlotAction.DROP_ITEM;
        };
    }

    public int resolveButton(boolean dropWholeStack, int clickButtonIndex) {
        FabricatorSlotAction action = toSlotAction(dropWholeStack, clickButtonIndex);
        if (action.usesFixedButton()) {
            return action.fixedButton();
        }
        return Math.max(0, Math.min(1, clickButtonIndex));
    }
}
