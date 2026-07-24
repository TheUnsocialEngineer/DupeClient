package com.dupeclient.client.module.acaudit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AcAuditSlotOverlay {
    private AcAuditSlotOverlay() {
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor context, int guiX, int guiY, int backgroundHeight, Slot focusedSlot) {
        AcAuditSettings settings = AcAuditManager.INSTANCE.getSettings();
        if (!settings.enabled || !settings.rawSlotOverlayEnabled) {
            return;
        }
        Font tr = Minecraft.getInstance().font;
        int color = settings.slotOverlayColor;
        boolean shadow = settings.slotOverlayShadow;
        AbstractContainerMenu handler = screen.getMenu();

        for (Slot slot : handler.slots) {
            context.text(tr, String.valueOf(slot.index), guiX + slot.x, guiY + slot.y, color, shadow);
        }

        if (settings.slotOverlayShowSyncId) {
            context.text(tr,
                    "syncId " + handler.containerId + "  rev " + handler.getStateId(),
                    guiX,
                    guiY - 10,
                    color,
                    shadow);
        }

        if (settings.slotOverlayShowHoveredItem && focusedSlot != null) {
            ItemStack stack = focusedSlot.getItem();
            String line = "slot " + focusedSlot.index + ": "
                    + (stack.isEmpty() ? "(empty)" : BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount());
            context.text(tr, line, guiX, guiY + backgroundHeight + 2, color, shadow);
        }
    }
}
