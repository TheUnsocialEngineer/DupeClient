package com.dupeclient.client.module.macro;

/**
 * One step in a linear macro. Unknown fields are ignored by Gson.
 */
public final class MacroStep {
    public String type = "";
    /** For {@link MacroStepType#WAIT_TICKS} (wait length) or {@link MacroStepType#LOOK_TURN} (yaw delta in degrees). */
    public int ticks;
    /** For {@link MacroStepType#SEND_CHAT} — sent as chat or as command if it starts with /. */
    public String text = "";
    /**
     * For {@link MacroStepType#MOVE_FORWARD}: {@code TICKS} = hold forward for {@link #ticks} client ticks;
     * {@code BLOCKS} = hold forward until horizontal Chebyshev distance (max |dx|,|dz|) from step start reaches
     * {@link #moveDistanceBlocks} (approximate block units).
     */
    public String moveMeasure = "TICKS";
    /** Used when {@link #moveMeasure} is {@code BLOCKS}. */
    public int moveDistanceBlocks = 1;
    /** For {@link MacroStepType#MOVE_FORWARD}: {@code N}/{@code E}/{@code S}/{@code W}, or {@code PLAYER} for current look direction. */
    public String walkFacing = "S";
    /**
     * For {@link MacroStepType#MOVE_FORWARD} only: optional extra vanilla keys held together with forward
     * (e.g. {@code USE} to place blocks, {@code SNEAK} for edge sneak). Unknown ids are ignored at runtime.
     */
    public String moveAuxHoldKeyId = "";
    public String moveAuxHoldKey2Id = "";

    /** For {@link MacroStepType#GUI_ITEM}: {@code PUT} into the container or {@code TAKE} out of it. */
    public String guiItemMode = "PUT";
    /** Item id (e.g. {@code minecraft:diamond} or {@code diamond}). */
    public String guiItemId = "";
    /** Items to move; {@code -1} = keep shift-clicking until no matching stacks remain on the source side. */
    public int guiItemCount = 1;
    /**
     * For {@link MacroStepType#GUI_ITEM}: client ticks to wait after each shift-click before the next ({@code 0} =
     * no wait — up to 64 moves per tick for fast dump/deposit). Larger values slow the step for picky servers.
     */
    public int guiItemDelayTicks = 0;

    /** For {@link MacroStepType#BLOCK_INTERACT}: preset from {@link MacroAutomation#BLOCK_PRESET_CYCLE}. */
    public String blockPreset = "CHEST";
    /** When {@link #blockPreset} is {@code OTHER}: full block id (e.g. {@code minecraft:lever}). */
    public String blockCustomId = "";
    /** Search sphere radius in blocks around the player for a matching block (e.g. 10). */
    public int blockSearchRadius = 10;
    /** Max client ticks spent walking toward the block before the step gives up. */
    public int blockNavigateMaxTicks = 400;

    /** For {@link MacroStepType#USE_HOTBAR_ITEM}: main hotbar index {@code 0}–{@code 8} (left → right). */
    public int hotbarSlot = 0;

    /** For {@link MacroStepType#KEY_HOLD}: id such as {@code ATTACK}, {@code USE}, {@code JUMP} (see {@link MacroHoldKeys}). */
    public String holdKeyId = "ATTACK";

    /** For {@link MacroStepType#DROP_ITEM}: drop whole stack vs one item. */
    public boolean dropFullStack = false;

    /** For {@link MacroStepType#WAIT_LOOK_ENTITY}: entity type id (e.g. {@code minecraft:cow}). */
    public String entityTypeId = "";

    /** For {@link MacroStepType#FABRICATOR_SEND}: handler slot id or item name filter. */
    public String fabricatorSlot = "0";
    public int fabricatorTimes = 1;
    public int fabricatorActionIndex = 0;

    /** For {@link MacroStepType#CLICK_SLOT}: handler slot index (e.g. {@code -999} = outside cursor). */
    public int clickSlotId = 0;
    /** For {@link MacroStepType#CLICK_SLOT}: {@link net.minecraft.screen.slot.SlotActionType} name. */
    public String clickSlotAction = "QUICK_MOVE";
    /** For {@link MacroStepType#CLICK_SLOT}: mouse button ({@code 0} = left, {@code 1} = right). */
    public int clickSlotButton = 0;

    /** For {@link MacroStepType#PRESS_BUTTON}: GLFW key code ({@link org.lwjgl.glfw.GLFW}). */
    public int pressKeyCode = org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
    /** For {@link MacroStepType#PRESS_BUTTON}: GLFW modifier mask (shift/ctrl/alt/super). */
    public int pressKeyModifiers = 0;
}
