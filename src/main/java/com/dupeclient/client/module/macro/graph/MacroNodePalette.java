package com.dupeclient.client.module.macro.graph;

import java.util.ArrayList;
import java.util.List;

/** Sidebar palette: category + label + type id placed on the canvas when dropped. */
public final class MacroNodePalette {
    /**
     * @param holdKeyPreset when {@link #typeId()} is {@code KEY_HOLD}, default {@link com.dupeclient.client.module.macro.MacroHoldKeys} id; otherwise ignored
     */
    public record Entry(String category, String label, String typeId, String holdKeyPreset) {
        public Entry(String category, String label, String typeId) {
            this(category, label, typeId, "");
        }
    }

    private MacroNodePalette() {
    }

    public static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry("Control", "Start", MacroGraphTypes.START));
        out.add(new Entry("Control", "Delay (ticks)", "WAIT_TICKS"));
        out.add(new Entry("Control", "Wait: look at block", "WAIT_LOOK_BLOCK"));
        out.add(new Entry("Control", "Wait: look at entity", "WAIT_LOOK_ENTITY"));
        out.add(new Entry("Control", "End", MacroGraphTypes.END));
        out.add(new Entry("Logic", "Repeat (→ End)", MacroGraphTypes.REPEAT));
        out.add(new Entry("Movement", "Walk (hold W)", "MOVE_FORWARD"));
        out.add(new Entry("Movement", "Look / turn yaw (°)", "LOOK_TURN"));
        out.add(new Entry("Movement", "Look / pitch (°)", "LOOK_PITCH"));
        out.add(new Entry("Movement", "Strafe left (hold)", "KEY_HOLD", "LEFT"));
        out.add(new Entry("Movement", "Strafe right (hold)", "KEY_HOLD", "RIGHT"));
        out.add(new Entry("Movement", "Back (hold)", "KEY_HOLD", "BACK"));
        out.add(new Entry("Movement", "Jump (hold)", "KEY_HOLD", "JUMP"));
        out.add(new Entry("Movement", "Sneak (hold)", "KEY_HOLD", "SNEAK"));
        out.add(new Entry("Movement", "Sprint (hold)", "KEY_HOLD", "SPRINT"));
        out.add(new Entry("Chat", "Send chat", "SEND_CHAT"));
        out.add(new Entry("Utility", "Close screen (instant)", "CLOSE_SCREEN"));
        out.add(new Entry("Utility", "Close GUI (server)", "CLOSE_GUI"));
        out.add(new Entry("Utility", "UI delay toggle", "UI_UTILS_TOGGLE_DELAY"));
        out.add(new Entry("Utility", "UI flush queue", "UI_UTILS_FLUSH_QUEUE"));
        out.add(new Entry("Utility", "Packet delay toggle", "PACKET_DELAY_TOGGLE"));
        out.add(new Entry("Utility", "Packet delay flush", "PACKET_DELAY_FLUSH"));
        out.add(new Entry("Utility", "Fabricator send", "FABRICATOR_SEND"));
        out.add(new Entry("Item", "GUI item (in/out)", "GUI_ITEM"));
        out.add(new Entry("Item", "Click slot", "CLICK_SLOT"));
        out.add(new Entry("Item", "Hotbar: select slot", "HOTBAR_SELECT"));
        out.add(new Entry("Item", "Swap offhand (F)", "SWAP_OFFHAND"));
        out.add(new Entry("Item", "Drop held stack", "DROP_ITEM"));
        out.add(new Entry("Interact", "Block (walk + use)", "BLOCK_INTERACT"));
        out.add(new Entry("Interact", "Use hotbar item", "USE_HOTBAR_ITEM"));
        out.add(new Entry("Interact", "Attack / break (hold)", "KEY_HOLD", "ATTACK"));
        out.add(new Entry("Interact", "Use / place (hold)", "KEY_HOLD", "USE"));
        out.add(new Entry("Interact", "Pick block (hold)", "KEY_HOLD", "PICK_BLOCK"));
        out.add(new Entry("Interact", "Open inventory (hold)", "KEY_HOLD", "INVENTORY"));
        out.add(new Entry("Interact", "Hold key (custom)", "KEY_HOLD"));
        out.add(new Entry("Interact", "Press key", "PRESS_BUTTON"));
        return out;
    }
}
