package com.dupeclient.client.module.packet.fabricator;

import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/**
 * Draws per-slot fabricator IDs on {@link HandledScreen}s (0–40 player, 100+ container).
 */
public final class SlotIdOverlay {
    private static final int SLOT_PX = 18;
    private static final int CORNER_PAD = 1;
    private static final float LABEL_SCALE = 0.5f;
    private static final int PLAYER_COLOR = 0xFF6EE7B7;
    private static final int CONTAINER_COLOR = 0xFF7DD3FC;
    private static final int DISABLED_COLOR = 0xFF9CA3AF;

    private SlotIdOverlay() {
    }

    public static void renderSlot(HandledScreen<?> screen, DrawContext context, Slot slot) {
        PacketUtilsSettings settings = PacketUtilsManager.INSTANCE.getSettings();
        if (!settings.slotIdsOverlayEnabled || slot == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || screen.getScreenHandler() == null) {
            return;
        }
        TextRenderer tr = screen.getTextRenderer();
        int visible = FabricatorInventorySlots.toUserVisibleSlot(client, slot.id);
        int color = slotColor(client.player, slot, visible);
        drawCornerLabel(
                context,
                tr,
                slot.x,
                slot.y,
                slot.x + CORNER_PAD,
                slot.y + CORNER_PAD,
                Integer.toString(visible),
                color);
    }

    private static int slotColor(ClientPlayerEntity player, Slot slot, int visible) {
        if (!slot.isEnabled()) {
            return DISABLED_COLOR;
        }
        if (visible < FabricatorInventorySlots.FIRST_GUI_SLOT) {
            return PLAYER_COLOR;
        }
        return CONTAINER_COLOR;
    }

    private static void drawCornerLabel(
            DrawContext context,
            TextRenderer tr,
            int slotLeft,
            int slotTop,
            int x,
            int y,
            String text,
            int textColor) {
        int tw = tr.getWidth(text);
        int th = 8;
        int bw = Math.max(1, (int) (tw * LABEL_SCALE) + 2);
        int bh = Math.max(1, (int) (th * LABEL_SCALE) + 1);

        int bx = x;
        int by = y;
        int bx2 = Math.min(slotLeft + SLOT_PX, bx + bw);
        int by2 = Math.min(slotTop + SLOT_PX, by + bh);
        context.fill(bx, by, bx2, by2, 0xD8000000);
        context.fill(bx, by, bx2, by + 1, textColor);
        context.fill(bx, by, bx + 1, by2, textColor);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(bx + 1, by + 1);
        matrices.scale(LABEL_SCALE, LABEL_SCALE);
        context.drawTextWithShadow(tr, Text.literal(text), 0, 0, textColor);
        matrices.popMatrix();
    }
}
