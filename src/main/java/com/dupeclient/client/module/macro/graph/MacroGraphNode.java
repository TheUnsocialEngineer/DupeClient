package com.dupeclient.client.module.macro.graph;

/**
 * One node on the macro graph canvas. {@link #type} matches {@link com.dupeclient.client.module.macro.MacroStepType}
 * names or palette stub ids (compiled to runnable steps).
 */
public final class MacroGraphNode {
    public String id = "";
    public String type = "";
    public String category = "";
    public double x;
    public double y;
    public int ticks;
    public String text = "";
    /** For {@code MOVE_FORWARD}: {@code TICKS} or {@code BLOCKS}. */
    public String moveForwardMeasure = "TICKS";
    /** When {@link #moveForwardMeasure} is {@code BLOCKS}, horizontal block distance target. */
    public int moveForwardBlocks = 1;
    /**
     * For {@code MOVE_FORWARD}: before holding forward, snap yaw to {@code N}/{@code E}/{@code S}/{@code W},
     * or use {@code PLAYER} to walk in whatever direction the camera is already facing.
     */
    public String walkFacing = "S";
    /** With {@code MOVE_FORWARD}: optional keys held with forward (see {@link com.dupeclient.client.module.macro.MacroStep}). */
    public String moveAuxHoldKeyId = "";
    public String moveAuxHoldKey2Id = "";
    /**
     * When {@code type} is {@link MacroGraphTypes#REPEAT}: if false, the editor shows one mint Repeat output and the
     * compiler may infer Continue at End; set true (double-click the node) to show the orange Continue port.
     */
    public boolean repeatShowNextPort = false;

    /** {@link com.dupeclient.client.module.macro.MacroStepType#GUI_ITEM}: {@code PUT} or {@code TAKE}. */
    public String guiItemMode = "PUT";
    public String guiItemId = "";
    /** When true, shift-move any item type (not only {@link #guiItemId}). */
    public boolean guiItemAnyItem = false;
    /** {@code -1} = all matching stacks from the source side. */
    public int guiItemCount = 1;
    /** When true, {@link #guiItemCount} is ignored at runtime. */
    public boolean guiItemAmountAll = false;
    /** Client ticks after each shift-click; {@code 0} = burst (many clicks per tick). */
    public int guiItemDelayTicks = 0;

    /** {@link com.dupeclient.client.module.macro.MacroStepType#BLOCK_INTERACT}. */
    public String blockPreset = "CHEST";
    public String blockCustomId = "";
    /** Euclidean search radius in blocks (default 10). */
    public int blockSearchRadius = 10;
    public int blockNavigateMaxTicks = 400;
    /**
     * Entity type id (e.g. {@code minecraft:cow}). Used by {@code WAIT_LOOK_ENTITY}; optional note on other nodes.
     */
    public String entityTypeId = "";

    /** {@link com.dupeclient.client.module.macro.MacroStepType#USE_HOTBAR_ITEM}: {@code 0}–{@code 8}. */
    public int hotbarSlot = 0;

    /** {@link com.dupeclient.client.module.macro.MacroStepType#KEY_HOLD}. */
    public String holdKeyId = "ATTACK";

    /** {@link com.dupeclient.client.module.macro.MacroStepType#DROP_ITEM}. */
    public boolean dropFullStack = false;

    public String fabricatorSlot = "0";
    public int fabricatorTimes = 1;
    public int fabricatorActionIndex = 0;

    /** {@link com.dupeclient.client.module.macro.MacroStepType#CLICK_SLOT}. */
    public int clickSlotId = 0;
    public String clickSlotAction = "QUICK_MOVE";
    public int clickSlotButton = 0;

    /** {@link com.dupeclient.client.module.macro.MacroStepType#PRESS_BUTTON}. */
    public int pressKeyCode = org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
    public int pressKeyModifiers = 0;
}
