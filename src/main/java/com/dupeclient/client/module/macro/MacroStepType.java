package com.dupeclient.client.module.macro;

import java.util.Locale;

public enum MacroStepType {
    WAIT_TICKS,
    SEND_CHAT,
    /** {@code client.setScreen(null)} — closes the overlay immediately; does not send {@code CloseHandledScreenC2S}. */
    CLOSE_SCREEN,
    /** Player {@code closeHandledScreen()} — vanilla container close + {@code CloseHandledScreenC2S} packet. */
    CLOSE_GUI,
    UI_UTILS_TOGGLE_DELAY,
    UI_UTILS_FLUSH_QUEUE,
    PACKET_DELAY_TOGGLE,
    PACKET_DELAY_FLUSH,
    FABRICATOR_SEND,
    /** Hold the forward key for {@link com.dupeclient.client.module.macro.MacroStep#ticks} client ticks (walk duration). */
    MOVE_FORWARD,
    /** Add {@link com.dupeclient.client.module.macro.MacroStep#ticks} to the client player yaw (degrees, ±). */
    LOOK_TURN,
    /**
     * Shift-click items between the open container GUI and the player inventory.
     * See {@link com.dupeclient.client.module.macro.MacroStep#guiItemMode}, {@link com.dupeclient.client.module.macro.MacroStep#guiItemId},
     * {@link com.dupeclient.client.module.macro.MacroStep#guiItemCount}, {@link com.dupeclient.client.module.macro.MacroStep#guiItemDelayTicks}.
     */
    GUI_ITEM,
    /**
     * Click a handler slot while a container GUI is open.
     * See {@link com.dupeclient.client.module.macro.MacroStep#clickSlotId},
     * {@link com.dupeclient.client.module.macro.MacroStep#clickSlotAction},
     * {@link com.dupeclient.client.module.macro.MacroStep#clickSlotButton}.
     */
    CLICK_SLOT,
    /**
     * Find nearest block matching {@link com.dupeclient.client.module.macro.MacroStep#blockPreset}, walk toward it if needed,
     * then right-click (use) the block.
     */
    BLOCK_INTERACT,
    /** Right-click (use) the stack in the main hotbar column: slot {@code 0} = leftmost, {@code 8} = rightmost. */
    USE_HOTBAR_ITEM,
    /**
     * Hold a vanilla key binding for {@link com.dupeclient.client.module.macro.MacroStep#ticks} client ticks.
     * Key id in {@link com.dupeclient.client.module.macro.MacroStep#holdKeyId} (see {@link MacroHoldKeys}).
     */
    KEY_HOLD,
    /**
     * Press any keyboard key once (GLFW key code + modifiers).
     * See {@link com.dupeclient.client.module.macro.MacroStep#pressKeyCode},
     * {@link com.dupeclient.client.module.macro.MacroStep#pressKeyModifiers}.
     */
    PRESS_BUTTON,
    /** Add {@link com.dupeclient.client.module.macro.MacroStep#ticks} to pitch (degrees, clamped). */
    LOOK_PITCH,
    /** Set selected hotbar column index {@code 0}–{@code 8} without using the item. */
    HOTBAR_SELECT,
    /** Drop from the selected hotbar stack; {@link com.dupeclient.client.module.macro.MacroStep#dropFullStack}. */
    DROP_ITEM,
    /**
     * Stall until the view ray targets a block whose id matches {@link com.dupeclient.client.module.macro.MacroStep#blockCustomId}.
     * {@link com.dupeclient.client.module.macro.MacroStep#ticks} is max wait in client ticks; {@code 0} = wait indefinitely.
     */
    WAIT_LOOK_BLOCK,
    /**
     * Stall until the view ray targets an entity whose type id matches {@link com.dupeclient.client.module.macro.MacroStep#entityTypeId}.
     * {@link com.dupeclient.client.module.macro.MacroStep#ticks} is max wait in client ticks; {@code 0} = wait indefinitely.
     */
    WAIT_LOOK_ENTITY,
    UNKNOWN;

    /** Types exposed in the macro editor (linear graph precursor). */
    public static MacroStepType[] editorPalette() {
        return new MacroStepType[]{WAIT_TICKS, SEND_CHAT, CLOSE_SCREEN, CLOSE_GUI, UI_UTILS_TOGGLE_DELAY, UI_UTILS_FLUSH_QUEUE, PACKET_DELAY_TOGGLE, PACKET_DELAY_FLUSH, FABRICATOR_SEND, MOVE_FORWARD, LOOK_TURN, LOOK_PITCH, KEY_HOLD, PRESS_BUTTON, GUI_ITEM, CLICK_SLOT, BLOCK_INTERACT, USE_HOTBAR_ITEM, HOTBAR_SELECT, DROP_ITEM, WAIT_LOOK_BLOCK, WAIT_LOOK_ENTITY};
    }

    public MacroStepType nextInEditor() {
        MacroStepType[] p = editorPalette();
        for (int i = 0; i < p.length; i++) {
            if (p[i] == this) {
                return p[(i + 1) % p.length];
            }
        }
        return p[0];
    }

    public static MacroStepType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return MacroStepType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
