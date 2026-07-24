package com.dupeclient.client.module.packet.fabricator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Converts between user-visible slot ids (0–40 player, 100+ container) and handler slot ids.
 * Ported from YungLight {@code PackUtilInventoryHelper}.
 */
public final class FabricatorInventorySlots {
    public static final int FIRST_GUI_SLOT = 100;

    private FabricatorInventorySlots() {
    }

    /** Prefer the open {@link AbstractContainerScreen} handler (player inventory, chest, etc.). */
    public static AbstractContainerMenu activeHandler(Minecraft client) {
        if (client == null || client.player == null) {
            return null;
        }
        if (client.screen instanceof AbstractContainerScreen<?> handled) {
            return handled.getMenu();
        }
        return client.player.containerMenu;
    }

    public static int toHandlerSlot(Minecraft client, int userVisibleSlot) {
        AbstractContainerMenu handler = activeHandler(client);
        if (client == null || client.player == null || handler == null) {
            return -1;
        }
        if (userVisibleSlot < 0) {
            return -1;
        }
        if (userVisibleSlot < client.player.getInventory().getContainerSize()) {
            for (Slot slot : handler.slots) {
                if (playerInventoryIndex(client.player, slot) == userVisibleSlot) {
                    return slot.index;
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
                    return slot.index;
                }
                extraOrdinal--;
            }
        }
        return -1;
    }

    public static int toUserVisibleSlot(Minecraft client, int handlerSlotId) {
        AbstractContainerMenu handler = activeHandler(client);
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
            if (current.index == handlerSlotId) {
                return FIRST_GUI_SLOT + extraOrdinal;
            }
            extraOrdinal++;
        }
        return handlerSlotId;
    }

    public static int resolveHandlerSlot(Minecraft client, int configuredSlot) {
        return toHandlerSlot(client, configuredSlot);
    }

    public static boolean isValidHandlerSlot(AbstractContainerMenu handler, int slotId) {
        return handler != null && slotId >= 0 && slotId < handler.slots.size();
    }

    public static boolean isPlayerInventorySlot(LocalPlayer player, Slot slot) {
        return playerInventoryIndex(player, slot) >= 0;
    }

    public static Integer findSlotByItem(Minecraft client, FabricatorItemMatcher matcher) {
        if (client == null || client.player == null || !matcher.hasQuery()) {
            return null;
        }
        AbstractContainerMenu handler = activeHandler(client);
        if (handler == null) {
            return null;
        }
        Integer best = null;
        int bestScore = -1;
        for (Slot slot : handler.slots) {
            if (slot == null || slot.getItem().isEmpty()) {
                continue;
            }
            int visible = toUserVisibleSlot(client, slot.index);
            int score = matcher.score(slot.getItem(), visible);
            if (score > bestScore) {
                bestScore = score;
                best = slot.index;
            }
        }
        return bestScore >= 0 ? best : null;
    }

    private static int playerInventoryIndex(LocalPlayer player, Slot slot) {
        if (player == null || slot == null || slot.container != player.getInventory()) {
            return -1;
        }
        int index = slot.getContainerSlot();
        return index >= 0 && index < player.getInventory().getContainerSize() ? index : -1;
    }
}
