package com.dupeclient.client.module.utility.crashes;

import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import com.dupeclient.client.module.packet.FeatureHotkeyManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class CrashesManager {
    public static final CrashesManager INSTANCE = new CrashesManager();

    private CrashesSettings settings = new CrashesSettings();
    private int chestPacketsSent;
    private int armorDelayCounter;
    private final FeatureHotkeyManager hotkeys = new FeatureHotkeyManager();
    private boolean textInputFocused;
    private int joinGraceTicks;

    private CrashesManager() {
    }

    public void initialize() {
        settings = CrashesConfigManager.load();
    }

    public CrashesSettings getSettings() {
        return settings;
    }

    public void save() {
        CrashesConfigManager.save(settings);
    }

    public int getChestPacketsSent() {
        return chestPacketsSent;
    }

    public void setTextInputFocused(boolean focused) {
        textInputFocused = focused;
    }

    public void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null || client.getConnection() == null) {
            return;
        }
        if (joinGraceTicks > 0) {
            joinGraceTicks--;
            return;
        }
        handleHotkeys(client);
        if (settings.chestCrashEnabled) {
            tickChestCrash(client);
        }
        if (settings.armorPlaceEnabled) {
            tickArmorPlace(client);
        }
    }

    private void handleHotkeys(Minecraft client) {
        if (textInputFocused) {
            return;
        }
        if (hotkeys.consumePress(client, settings.chestToggleKey)) {
            setChestCrashEnabled(!settings.chestCrashEnabled);
        }
        if (hotkeys.consumePress(client, settings.armorToggleKey)) {
            setArmorPlaceEnabled(!settings.armorPlaceEnabled);
        }
    }

    public void setChestCrashEnabled(boolean enabled) {
        if (enabled && P2wServerPolicy.INSTANCE.isModulesLocked()) {
            feedbackChest(Minecraft.getInstance(), "Modules locked on non-P2W server.");
            return;
        }
        if (settings.chestCrashEnabled == enabled) {
            return;
        }
        settings.chestCrashEnabled = enabled;
        if (enabled) {
            chestPacketsSent = 0;
            Minecraft client = Minecraft.getInstance();
            if (client != null && hasChestsWithBooksInRange(client)) {
                settings.chestCrashEnabled = false;
                feedbackChest(client, "Chests with written books in range - disabled to protect them.");
                save();
                return;
            }
            feedbackChest(Minecraft.getInstance(), "Chest crash enabled.");
        } else {
            feedbackChest(Minecraft.getInstance(), "Chest crash disabled.");
        }
        save();
    }

    public void setArmorPlaceEnabled(boolean enabled) {
        if (enabled && P2wServerPolicy.INSTANCE.isModulesLocked()) {
            feedbackArmor(Minecraft.getInstance(), "Modules locked on non-P2W server.");
            return;
        }
        if (settings.armorPlaceEnabled == enabled) {
            return;
        }
        settings.armorPlaceEnabled = enabled;
        armorDelayCounter = 0;
        feedbackArmor(Minecraft.getInstance(), enabled ? "Armor stand placer enabled." : "Armor stand placer disabled.");
        save();
    }

    public void onDisconnected() {
        onSessionLeave();
    }

    /** Called when entering a play session — brief grace period avoids packet floods during world load. */
    public void onSessionJoin() {
        joinGraceTicks = 40;
        chestPacketsSent = 0;
        armorDelayCounter = 0;
    }

    public void forceDisableAll() {
        boolean changed = false;
        if (settings.chestCrashEnabled) {
            settings.chestCrashEnabled = false;
            changed = true;
        }
        if (settings.armorPlaceEnabled) {
            settings.armorPlaceEnabled = false;
            changed = true;
        }
        chestPacketsSent = 0;
        armorDelayCounter = 0;
        if (changed) {
            save();
        }
    }

    /** Called when leaving a world or disconnecting from a server. */
    public void onSessionLeave() {
        boolean dirty = false;
        if (settings.chestDisableOnDisconnect && settings.chestCrashEnabled) {
            settings.chestCrashEnabled = false;
            chestPacketsSent = 0;
            dirty = true;
        }
        if (settings.armorDisableOnLeave && settings.armorPlaceEnabled) {
            settings.armorPlaceEnabled = false;
            armorDelayCounter = 0;
            dirty = true;
        }
        if (dirty) {
            save();
        }
    }

    private void tickChestCrash(Minecraft client) {
        if (hasChestsWithBooksInRange(client)) {
            settings.chestCrashEnabled = false;
            chestPacketsSent = 0;
            feedbackChest(client, "Chests with written books in range - disabled to protect them.");
            save();
            return;
        }

        List<BlockPos> chests = getChestsInRange(client);
        if (chests.isEmpty()) {
            return;
        }

        for (BlockPos pos : chests) {
            if (settings.chestPackets > 0 && chestPacketsSent >= settings.chestPackets) {
                settings.chestCrashEnabled = false;
                feedbackChest(client, "Chest crash finished (" + chestPacketsSent + " packets).");
                save();
                return;
            }
            sendOpenPacket(client, pos);
            chestPacketsSent++;
        }
    }

    private void tickArmorPlace(Minecraft client) {
        if (armorDelayCounter < settings.armorDelay) {
            armorDelayCounter++;
            return;
        }
        armorDelayCounter = 0;
        placeArmorStands(client);
    }

    private void placeArmorStands(Minecraft client) {
        int slot = ensureArmorStandInHotbar(client);
        if (slot == -1) {
            if (settings.armorDisableOnEmpty) {
                settings.armorPlaceEnabled = false;
                save();
            }
            feedbackArmor(client, "No armor stands in inventory.");
            return;
        }

        List<PlaceTarget> targets = collectPlaceTargets(client);
        if (targets.isEmpty()) {
            return;
        }

        var inv = client.player.getInventory();
        int prevSlot = inv.getSelectedSlot();
        var networkHandler = client.getConnection();

        inv.setSelectedSlot(slot);
        networkHandler.send(new ServerboundSetCarriedItemPacket(slot));

        int repeats = Math.max(1, settings.armorPacketsPerTick);
        for (int r = 0; r < repeats; r++) {
            for (PlaceTarget target : targets) {
                sendPlacePacket(client, target);
            }
        }

        inv.setSelectedSlot(prevSlot);
        networkHandler.send(new ServerboundSetCarriedItemPacket(prevSlot));
    }

    private int ensureArmorStandInHotbar(Minecraft client) {
        Predicate<ItemStack> isStand = s -> s.getItem() == Items.ARMOR_STAND;
        var inv = client.player.getInventory();

        int slot = findSlot(inv, 0, 8, isStand);
        if (slot != -1) {
            return slot;
        }

        int hotbarSlot = findEmptyHotbarSlot(inv);
        if (hotbarSlot == -1) {
            return -1;
        }

        slot = findSlot(inv, 9, 35, isStand);
        if (slot == -1) {
            return -1;
        }

        if (client.gameMode != null) {
            client.gameMode.handleInventoryMouseClick(
                    client.player.inventoryMenu.containerId,
                    slot,
                    hotbarSlot,
                    ClickType.SWAP,
                    client.player);
        }
        return hotbarSlot;
    }

    private static int findSlot(net.minecraft.world.entity.player.Inventory inv, int start, int end, Predicate<ItemStack> predicate) {
        for (int i = start; i <= end; i++) {
            if (predicate.test(inv.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int findEmptyHotbarSlot(net.minecraft.world.entity.player.Inventory inv) {
        for (int i = 0; i <= 8; i++) {
            if (inv.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private List<PlaceTarget> collectPlaceTargets(Minecraft client) {
        BlockPos playerPos = client.player.blockPosition();
        Direction dir = client.player.getDirection();
        int len = Math.max(1, Math.min(6, settings.armorLength));
        int v = Math.max(1, Math.min(6, settings.armorVerticality));
        List<PlaceTarget> out = new ArrayList<>();
        for (int h = 0; h < v; h++) {
            for (int i = 1; i <= len; i++) {
                BlockPos at = playerPos.relative(dir, i).above(h);
                out.add(new PlaceTarget(at.below(), Direction.UP));
                out.add(new PlaceTarget(at, dir.getOpposite()));
            }
        }
        return out;
    }

    private static void sendPlacePacket(Minecraft client, PlaceTarget target) {
        Direction face = target.face();
        Vec3 hitPos = Vec3.atCenterOf(target.blockPos())
                .add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(hitPos, face, target.blockPos(), false);
        client.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0));
    }

    private boolean hasChestsWithBooksInRange(Minecraft client) {
        double rangeSq = (double) settings.chestRange * settings.chestRange;
        for (BlockEntity blockEntity : blockEntitiesInRange(client, settings.chestRange)) {
            if (!isChest(blockEntity)) {
                continue;
            }
            if (squaredDistanceToPlayer(client, blockEntity.getBlockPos()) > rangeSq) {
                continue;
            }
            if (hasWrittenBook(blockEntity)) {
                return true;
            }
        }
        return false;
    }

    private List<BlockPos> getChestsInRange(Minecraft client) {
        List<BlockPos> result = new ArrayList<>();
        double rangeSq = (double) settings.chestRange * settings.chestRange;
        for (BlockEntity blockEntity : blockEntitiesInRange(client, settings.chestRange)) {
            if (!isChest(blockEntity)) {
                continue;
            }
            BlockPos pos = blockEntity.getBlockPos();
            if (squaredDistanceToPlayer(client, pos) > rangeSq) {
                continue;
            }
            if (hasWrittenBook(blockEntity)) {
                continue;
            }
            if (settings.chestOnlyWithWrittenBook) {
                continue;
            }
            result.add(pos);
        }
        return result;
    }

    private static boolean isChest(BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity || blockEntity instanceof TrappedChestBlockEntity;
    }

    private static boolean hasWrittenBook(BlockEntity blockEntity) {
        if (blockEntity instanceof Container inv) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (stack.getItem() == Items.WRITTEN_BOOK) {
                    return true;
                }
                if (stack.getItem() == Items.WRITABLE_BOOK) {
                    var content = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
                    if (content != null && !content.pages().isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void sendOpenPacket(Minecraft client, BlockPos pos) {
        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(pos).add(0, 0.5, 0),
                Direction.UP,
                pos,
                false);
        client.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, 0));
    }

    private static double squaredDistanceToPlayer(Minecraft client, BlockPos pos) {
        return pos.distToLowCornerSqr(client.player.getX(), client.player.getY(), client.player.getZ());
    }

    private static Iterable<BlockEntity> blockEntitiesInRange(Minecraft client, int rangeBlocks) {
        List<BlockEntity> out = new ArrayList<>();
        ClientLevel world = client.level;
        if (world == null) {
            return out;
        }
        BlockPos center = client.player.blockPosition();
        int chunkRadius = (rangeBlocks >> 4) + 1;
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                LevelChunk chunk = world.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) {
                    continue;
                }
                out.addAll(chunk.getBlockEntities().values());
            }
        }
        return out;
    }

    public void feedbackChest(Minecraft client, String message) {
        sendFeedback(client, settings.chestChatFeedback, "[Chest Crash] ", message);
    }

    public void feedbackArmor(Minecraft client, String message) {
        sendFeedback(client, settings.armorChatFeedback, "[Armor Placer] ", message);
    }

    public void feedbackChestConfigToggle(String message) {
        sendFeedback(Minecraft.getInstance(), true, "[Chest Crash] ", message);
    }

    public void feedbackArmorConfigToggle(String message) {
        sendFeedback(Minecraft.getInstance(), true, "[Armor Placer] ", message);
    }

    private static void sendFeedback(Minecraft client, boolean enabled, String prefix, String message) {
        if (!enabled || client == null || message == null || message.isBlank()) {
            return;
        }
        Component line = Component.literal(prefix).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal(message).withStyle(ChatFormatting.GRAY));
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(line, false);
            }
        });
    }

    private record PlaceTarget(BlockPos blockPos, Direction face) {
    }
}
