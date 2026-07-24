package com.dupeclient.client.gui.macro;

import com.dupeclient.client.gui.widget.StylishCyclingButtonWidget;
import com.dupeclient.client.module.macro.MacroAutomation;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Minecraft 1.21.10: stylish cycling widgets for the macro editor inspector.
 */
public final class MacroCyclingStringWidgets {
    private MacroCyclingStringWidgets() {
    }

    public static StylishCyclingButtonWidget blockPreset(
            int x,
            int y,
            int w,
            int h,
            Text label,
            BiConsumer<StylishCyclingButtonWidget, String> onChange) {
        return new StylishCyclingButtonWidget(
                x, y, w, h, label, Arrays.asList(MacroAutomation.BLOCK_PRESET_CYCLE), "CHEST", onChange);
    }

    public static StylishCyclingButtonWidget holdKeyCycle(
            int x,
            int y,
            int w,
            int h,
            List<String> values,
            Text label,
            BiConsumer<StylishCyclingButtonWidget, String> onChange) {
        return new StylishCyclingButtonWidget(x, y, w, h, label, values, values.getFirst(), onChange);
    }

    public static StylishCyclingButtonWidget hotbarSlotCycle(
            int x,
            int y,
            int w,
            int h,
            List<String> values,
            Text label,
            BiConsumer<StylishCyclingButtonWidget, String> onChange) {
        return new StylishCyclingButtonWidget(x, y, w, h, label, values, "0", onChange);
    }
}
