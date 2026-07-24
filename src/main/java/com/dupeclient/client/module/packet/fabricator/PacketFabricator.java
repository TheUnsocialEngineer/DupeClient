package com.dupeclient.client.module.packet.fabricator;

import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import com.ui_utils.SharedVariables;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

/** Builds and sends fabricated inventory click packets (YungLight fabricator backend). */
public final class PacketFabricator {
    public static final PacketFabricator INSTANCE = new PacketFabricator();

    private String lastStatus = "";

    private PacketFabricator() {
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String status) {
        lastStatus = status == null ? "" : status;
    }

    public PacketUtilsSettings settings() {
        return PacketUtilsManager.INSTANCE.getSettings();
    }

    public FabricatorAction currentAction() {
        PacketUtilsSettings s = settings();
        FabricatorAction[] values = FabricatorAction.values();
        int idx = Math.floorMod(s.fabricatorActionIndex, values.length);
        return values[idx];
    }

    public void cycleAction() {
        PacketUtilsSettings s = settings();
        s.fabricatorActionIndex = (s.fabricatorActionIndex + 1) % FabricatorAction.values().length;
        PacketUtilsManager.INSTANCE.save();
    }

    public void cycleClickButton() {
        PacketUtilsSettings s = settings();
        s.fabricatorClickButton = s.fabricatorClickButton == 0 ? 1 : 0;
        PacketUtilsManager.INSTANCE.save();
    }

    public void toggleDropWholeStack() {
        PacketUtilsSettings s = settings();
        s.fabricatorDropWholeStack = !s.fabricatorDropWholeStack;
        PacketUtilsManager.INSTANCE.save();
    }

    public void toggleMultiSlot() {
        PacketUtilsSettings s = settings();
        s.fabricatorMultiSlot = !s.fabricatorMultiSlot;
        if (!s.fabricatorMultiSlot && FabricatorSlotList.hasMultiple(s.fabricatorSlot)) {
            List<Integer> slots = FabricatorSlotList.parseVisible(s.fabricatorSlot);
            if (!slots.isEmpty()) {
                s.fabricatorSlot = Integer.toString(slots.getFirst());
            }
        }
        PacketUtilsManager.INSTANCE.save();
        PacketUtilsManager.INSTANCE.moduleFeedback("Multi-slot pick " + (s.fabricatorMultiSlot ? "ON" : "OFF"));
    }

    public SendResult send(boolean queue) {
        if (P2wServerPolicy.INSTANCE.isModulesLocked()) {
            lastStatus = "Modules locked on this server.";
            return SendResult.FAILED;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.getConnection() == null) {
            lastStatus = "Not in game.";
            return SendResult.FAILED;
        }
        AbstractContainerMenu handler = resolveHandler(client);
        if (handler == null) {
            lastStatus = "No screen handler (open inventory or a container).";
            return SendResult.FAILED;
        }
        List<Integer> handlerSlots;
        try {
            handlerSlots = resolveTargetSlots(client, handler);
        } catch (NumberFormatException e) {
            lastStatus = "Invalid number.";
            return SendResult.FAILED;
        }
        if (handlerSlots.isEmpty()) {
            return SendResult.FAILED;
        }
        int repeats;
        try {
            repeats = resolveRepeatCount();
        } catch (NumberFormatException e) {
            lastStatus = "Invalid repeat count.";
            return SendResult.FAILED;
        }
        if (repeats <= 0) {
            return SendResult.FAILED;
        }
        List<ServerboundContainerClickPacket> packets = buildSequence(client, handler, handlerSlots, repeats);
        if (packets.isEmpty()) {
            lastStatus = "Failed to build packet.";
            return SendResult.FAILED;
        }
        if (queue) {
            for (ServerboundContainerClickPacket packet : packets) {
                SharedVariables.delayedUIPackets.add(packet);
            }
            lastStatus = "Queued " + packets.size() + " packet(s).";
            PacketUtilsManager.INSTANCE.moduleFeedback(lastStatus);
            return SendResult.QUEUED;
        }
        if (FabricatorSendScheduler.INSTANCE.isActive()) {
            FabricatorSendScheduler.INSTANCE.stop("Previous send stopped.");
        }
        int flushed = flushDelayedUiPackets(client);
        if (packets.size() == 1) {
            ServerboundContainerClickPacket packet = ClickSlotPackets.refresh(packets.getFirst(), handler);
            if (packet != null) {
                PacketUtilsManager.INSTANCE.sendBypass(client, packet);
            }
            lastStatus = flushed > 0
                    ? "Sent " + flushed + " queued + 1 new packet."
                    : "Sent 1 packet.";
            PacketUtilsManager.INSTANCE.moduleFeedback(lastStatus);
            return SendResult.SENT;
        }
        FabricatorSendScheduler.INSTANCE.start(client, packets);
        if (flushed > 0) {
            lastStatus = "Sending " + packets.size() + " (after " + flushed + " queued flushed).";
        }
        return SendResult.SENDING;
    }

    public ServerboundContainerClickPacket buildPacket(Minecraft client, AbstractContainerMenu handler, int handlerSlot) {
        PacketUtilsSettings s = settings();
        FabricatorAction action = currentAction();
        FabricatorSlotAction slotAction = action.toSlotAction(s.fabricatorDropWholeStack, s.fabricatorClickButton);
        int button = action.resolveButton(s.fabricatorDropWholeStack, s.fabricatorClickButton);
        ContainerInput type = slotAction.toVanilla();
        ServerboundContainerClickPacket packet = ClickSlotPackets.create(
                handler.containerId,
                handler.getStateId(),
                handlerSlot,
                button,
                type);
        return ClickSlotPackets.refresh(packet, handler);
    }

    private List<ServerboundContainerClickPacket> buildSequence(
            Minecraft client, AbstractContainerMenu handler, List<Integer> handlerSlots, int repeats) {
        List<ServerboundContainerClickPacket> packets = new ArrayList<>(repeats * handlerSlots.size());
        for (int i = 0; i < repeats; i++) {
            for (int handlerSlot : handlerSlots) {
                ServerboundContainerClickPacket built = buildPacket(client, handler, handlerSlot);
                if (built == null) {
                    return List.of();
                }
                packets.add(built);
            }
        }
        return packets;
    }

    private List<Integer> resolveTargetSlots(Minecraft client, AbstractContainerMenu handler) {
        PacketUtilsSettings s = settings();
        String slotText = s.fabricatorSlot == null ? "" : s.fabricatorSlot.trim();
        String itemName = s.fabricatorItemName == null ? "" : s.fabricatorItemName.trim();

        if (!slotText.isEmpty() && (s.fabricatorMultiSlot || FabricatorSlotList.hasMultiple(slotText))) {
            List<Integer> visibleSlots = FabricatorSlotList.parseVisible(slotText);
            if (visibleSlots.isEmpty()) {
                lastStatus = "Enter at least one slot.";
                return List.of();
            }
            List<Integer> resolved = new ArrayList<>();
            for (int visible : visibleSlots) {
                int handlerSlot = FabricatorInventorySlots.resolveHandlerSlot(client, visible);
                if (!FabricatorInventorySlots.isValidHandlerSlot(handler, handlerSlot)) {
                    lastStatus = "Slot " + visible + " is not available in this screen.";
                    return List.of();
                }
                if (!itemName.isEmpty() && !slotMatchesItem(handler, handlerSlot, itemName)) {
                    lastStatus = "Slot " + visible + " does not contain '" + itemName + "'.";
                    return List.of();
                }
                resolved.add(handlerSlot);
            }
            return resolved;
        }

        Integer enteredSlot = null;
        if (!slotText.isEmpty()) {
            enteredSlot = Integer.parseInt(slotText);
        }
        int resolved = enteredSlot == null ? -1 : FabricatorInventorySlots.resolveHandlerSlot(client, enteredSlot);
        if (enteredSlot != null && !FabricatorInventorySlots.isValidHandlerSlot(handler, resolved)) {
            lastStatus = "Slot " + enteredSlot + " is not available in this screen.";
            return List.of();
        }
        if (enteredSlot != null) {
            if (!itemName.isEmpty() && !slotMatchesItem(handler, resolved, itemName)) {
                lastStatus = "Slot " + enteredSlot + " does not contain '" + itemName + "'.";
                return List.of();
            }
            return List.of(resolved);
        }
        if (!itemName.isEmpty()) {
            FabricatorItemMatcher matcher = FabricatorItemMatcher.parse(itemName);
            Integer found = FabricatorInventorySlots.findSlotByItem(client, matcher);
            if (found == null) {
                lastStatus = "No item matching '" + itemName + "' found.";
                return List.of();
            }
            return List.of(found);
        }
        lastStatus = "Enter a slot or item name.";
        return List.of();
    }

    private static boolean slotMatchesItem(AbstractContainerMenu handler, int handlerSlot, String itemQuery) {
        if (!FabricatorInventorySlots.isValidHandlerSlot(handler, handlerSlot)) {
            return false;
        }
        FabricatorItemMatcher matcher = FabricatorItemMatcher.parse(itemQuery);
        return matcher.score(handler.slots.get(handlerSlot).getItem(), handlerSlot) >= 0;
    }

    private int resolveRepeatCount() {
        PacketUtilsSettings s = settings();
        String raw = s.fabricatorTimes == null ? "" : s.fabricatorTimes.trim();
        if (raw.isEmpty()) {
            lastStatus = "Enter repeat count.";
            return -1;
        }
        int value = Integer.parseInt(raw);
        if (value < 1) {
            lastStatus = "Repeat count must be at least 1.";
            return -1;
        }
        return value;
    }

    private static AbstractContainerMenu resolveHandler(Minecraft client) {
        return FabricatorInventorySlots.activeHandler(client);
    }

    private static int flushDelayedUiPackets(Minecraft client) {
        if (SharedVariables.delayedUIPackets.isEmpty()) {
            return 0;
        }
        int n = SharedVariables.delayedUIPackets.size();
        PacketUtilsManager.INSTANCE.flushUiUtilsQueueNow(client);
        return n;
    }

    public enum SendResult {
        SENT,
        SENDING,
        QUEUED,
        FAILED
    }
}
