package com.dupeclient.client.module.acaudit;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class AcAuditSlotOverlay {
    private AcAuditSlotOverlay() {
    }

    public static void render(HandledScreen<?> screen, DrawContext context, int guiX, int guiY, int backgroundHeight, Slot focusedSlot) {
        AcAuditSettings settings = AcAuditManager.INSTANCE.getSettings();
        if (!settings.enabled || !settings.rawSlotOverlayEnabled) {
            return;
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int color = settings.slotOverlayColor;
        boolean shadow = settings.slotOverlayShadow;
        ScreenHandler handler = screen.getScreenHandler();

        for (Slot slot : handler.slots) {
            context.drawText(tr, String.valueOf(slot.id), guiX + slot.x, guiY + slot.y, color, shadow);
        }

        if (settings.slotOverlayShowSyncId) {
            context.drawText(tr,
                    "syncId " + handler.syncId + "  rev " + handler.getRevision(),
                    guiX,
                    guiY - 10,
                    color,
                    shadow);
        }

        if (settings.slotOverlayShowHoveredItem && focusedSlot != null) {
            ItemStack stack = focusedSlot.getStack();
            String line = "slot " + focusedSlot.id + ": "
                    + (stack.isEmpty() ? "(empty)" : Registries.ITEM.getId(stack.getItem()) + " x" + stack.getCount());
            context.drawText(tr, line, guiX, guiY + backgroundHeight + 2, color, shadow);
        }
    }
}
