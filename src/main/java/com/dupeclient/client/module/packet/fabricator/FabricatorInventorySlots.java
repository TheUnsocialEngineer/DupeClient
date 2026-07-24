package com.dupeclient.client.module.packet.fabricator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Converts between user-visible slot ids (0–40 player, 100+ container) and handler slot ids.
 * Ported from YungLight {@code PackUtilInventoryHelper}.
 */
public final class FabricatorInventorySlots {
    public static final int FIRST_GUI_SLOT = 100;

    private FabricatorInventorySlots() {
    }

    /** Prefer the open {@link HandledScreen} handler (player inventory, chest, etc.). */
    public static ScreenHandler activeHandler(MinecraftClient client) {
        if (client == null || client.player == null) {
            return null;
        }
        if (client.currentScreen instanceof HandledScreen<?> handled) {
            return handled.getScreenHandler();
        }
        return client.player.currentScreenHandler;
    }

    public static int toHandlerSlot(MinecraftClient client, int userVisibleSlot) {
        ScreenHandler handler = activeHandler(client);
        if (client == null || client.player == null || handler == null) {
            return -1;
        }
        if (userVisibleSlot < 0) {
            return -1;
        }
        if (userVisibleSlot < client.player.getInventory().size()) {
            for (Slot slot : handler.slots) {
                if (playerInventoryIndex(client.player, slot) == userVisibleSlot) {
                    return slot.id;
                }
            }
        }
        if (userVisibleSlot >= FIRST_GUI_SLOT) {
            int extraOrdinal = userVisibleSlot - FIRST_GUI_SLOT;
            for (Slot slot : handler.slots) {
                if (slot == null || isPlayerInventorySlot(client.player, slot)) {
                    continue;
                }
                if (extraOrdinal == 0) {
                    return slot.id;
                }
                extraOrdinal--;
            }
        }
        return -1;
    }

    public static int toUserVisibleSlot(MinecraftClient client, int handlerSlotId) {
        ScreenHandler handler = activeHandler(client);
        if (client == null || client.player == null || handler == null) {
            return handlerSlotId;
        }
        if (handlerSlotId < 0 || handlerSlotId >= handler.slots.size()) {
            return handlerSlotId;
        }
        Slot slot = handler.slots.get(handlerSlotId);
        int playerSlot = playerInventoryIndex(client.player, slot);
        if (playerSlot >= 0) {
            return playerSlot;
        }
        int extraOrdinal = 0;
        for (Slot current : handler.slots) {
            if (current == null || isPlayerInventorySlot(client.player, current)) {
                continue;
            }
            if (current.id == handlerSlotId) {
                return FIRST_GUI_SLOT + extraOrdinal;
            }
            extraOrdinal++;
        }
        return handlerSlotId;
    }

    public static int resolveHandlerSlot(MinecraftClient client, int configuredSlot) {
        return toHandlerSlot(client, configuredSlot);
    }

    public static boolean isValidHandlerSlot(ScreenHandler handler, int slotId) {
        return handler != null && slotId >= 0 && slotId < handler.slots.size();
    }

    public static boolean isPlayerInventorySlot(ClientPlayerEntity player, Slot slot) {
        return playerInventoryIndex(player, slot) >= 0;
    }

    public static Integer findSlotByItem(MinecraftClient client, FabricatorItemMatcher matcher) {
        if (client == null || client.player == null || !matcher.hasQuery()) {
            return null;
        }
        ScreenHandler handler = activeHandler(client);
        if (handler == null) {
            return null;
        }
        Integer best = null;
        int bestScore = -1;
        for (Slot slot : handler.slots) {
            if (slot == null || slot.getStack().isEmpty()) {
                continue;
            }
            int visible = toUserVisibleSlot(client, slot.id);
            int score = matcher.score(slot.getStack(), visible);
            if (score > bestScore) {
                bestScore = score;
                best = slot.id;
            }
        }
        return bestScore >= 0 ? best : null;
    }

    private static int playerInventoryIndex(ClientPlayerEntity player, Slot slot) {
        if (player == null || slot == null || slot.inventory != player.getInventory()) {
            return -1;
        }
        int index = slot.getIndex();
        return index >= 0 && index < player.getInventory().size() ? index : -1;
    }
}
