package com.dupeclient.client.module.utility.crashes;

import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import com.dupeclient.client.module.packet.FeatureHotkeyManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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

    public void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.getNetworkHandler() == null) {
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

    private void handleHotkeys(MinecraftClient client) {
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
            feedbackChest(MinecraftClient.getInstance(), "Modules locked on non-P2W server.");
            return;
        }
        if (settings.chestCrashEnabled == enabled) {
            return;
        }
        settings.chestCrashEnabled = enabled;
        if (enabled) {
            chestPacketsSent = 0;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && hasChestsWithBooksInRange(client)) {
                settings.chestCrashEnabled = false;
                feedbackChest(client, "Chests with written books in range - disabled to protect them.");
                save();
                return;
            }
            feedbackChest(MinecraftClient.getInstance(), "Chest crash enabled.");
        } else {
            feedbackChest(MinecraftClient.getInstance(), "Chest crash disabled.");
        }
        save();
    }

    public void setArmorPlaceEnabled(boolean enabled) {
        if (enabled && P2wServerPolicy.INSTANCE.isModulesLocked()) {
            feedbackArmor(MinecraftClient.getInstance(), "Modules locked on non-P2W server.");
            return;
        }
        if (settings.armorPlaceEnabled == enabled) {
            return;
        }
        settings.armorPlaceEnabled = enabled;
        armorDelayCounter = 0;
        feedbackArmor(MinecraftClient.getInstance(), enabled ? "Armor stand placer enabled." : "Armor stand placer disabled.");
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

    private void tickChestCrash(MinecraftClient client) {
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

    private void tickArmorPlace(MinecraftClient client) {
        if (armorDelayCounter < settings.armorDelay) {
            armorDelayCounter++;
            return;
        }
        armorDelayCounter = 0;
        placeArmorStands(client);
    }

    private void placeArmorStands(MinecraftClient client) {
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
        var networkHandler = client.getNetworkHandler();

        inv.setSelectedSlot(slot);
        networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        int repeats = Math.max(1, settings.armorPacketsPerTick);
        for (int r = 0; r < repeats; r++) {
            for (PlaceTarget target : targets) {
                sendPlacePacket(client, target);
            }
        }

        inv.setSelectedSlot(prevSlot);
        networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
    }

    private int ensureArmorStandInHotbar(MinecraftClient client) {
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

        if (client.interactionManager != null) {
            client.interactionManager.clickSlot(
                    client.player.playerScreenHandler.syncId,
                    slot,
                    hotbarSlot,
                    SlotActionType.SWAP,
                    client.player);
        }
        return hotbarSlot;
    }

    private static int findSlot(net.minecraft.entity.player.PlayerInventory inv, int start, int end, Predicate<ItemStack> predicate) {
        for (int i = start; i <= end; i++) {
            if (predicate.test(inv.getStack(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int findEmptyHotbarSlot(net.minecraft.entity.player.PlayerInventory inv) {
        for (int i = 0; i <= 8; i++) {
            if (inv.getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private List<PlaceTarget> collectPlaceTargets(MinecraftClient client) {
        BlockPos playerPos = client.player.getBlockPos();
        Direction dir = client.player.getHorizontalFacing();
        int len = Math.max(1, Math.min(6, settings.armorLength));
        int v = Math.max(1, Math.min(6, settings.armorVerticality));
        List<PlaceTarget> out = new ArrayList<>();
        for (int h = 0; h < v; h++) {
            for (int i = 1; i <= len; i++) {
                BlockPos at = playerPos.offset(dir, i).up(h);
                out.add(new PlaceTarget(at.down(), Direction.UP));
                out.add(new PlaceTarget(at, dir.getOpposite()));
            }
        }
        return out;
    }

    private static void sendPlacePacket(MinecraftClient client, PlaceTarget target) {
        Direction face = target.face();
        Vec3d hitPos = Vec3d.ofCenter(target.blockPos())
                .add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(hitPos, face, target.blockPos(), false);
        client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 0));
    }

    private boolean hasChestsWithBooksInRange(MinecraftClient client) {
        double rangeSq = (double) settings.chestRange * settings.chestRange;
        for (BlockEntity blockEntity : blockEntitiesInRange(client, settings.chestRange)) {
            if (!isChest(blockEntity)) {
                continue;
            }
            if (squaredDistanceToPlayer(client, blockEntity.getPos()) > rangeSq) {
                continue;
            }
            if (hasWrittenBook(blockEntity)) {
                return true;
            }
        }
        return false;
    }

    private List<BlockPos> getChestsInRange(MinecraftClient client) {
        List<BlockPos> result = new ArrayList<>();
        double rangeSq = (double) settings.chestRange * settings.chestRange;
        for (BlockEntity blockEntity : blockEntitiesInRange(client, settings.chestRange)) {
            if (!isChest(blockEntity)) {
                continue;
            }
            BlockPos pos = blockEntity.getPos();
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
        if (blockEntity instanceof Inventory inv) {
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (stack.getItem() == Items.WRITTEN_BOOK) {
                    return true;
                }
                if (stack.getItem() == Items.WRITABLE_BOOK) {
                    var content = stack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
                    if (content != null && !content.pages().isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void sendOpenPacket(MinecraftClient client, BlockPos pos) {
        BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofCenter(pos).add(0, 0.5, 0),
                Direction.UP,
                pos,
                false);
        client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
    }

    private static double squaredDistanceToPlayer(MinecraftClient client, BlockPos pos) {
        return pos.getSquaredDistance(client.player.getX(), client.player.getY(), client.player.getZ());
    }

    private static Iterable<BlockEntity> blockEntitiesInRange(MinecraftClient client, int rangeBlocks) {
        List<BlockEntity> out = new ArrayList<>();
        ClientWorld world = client.world;
        if (world == null) {
            return out;
        }
        BlockPos center = client.player.getBlockPos();
        int chunkRadius = (rangeBlocks >> 4) + 1;
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx + dx, cz + dz);
                if (chunk == null) {
                    continue;
                }
                out.addAll(chunk.getBlockEntities().values());
            }
        }
        return out;
    }

    public void feedbackChest(MinecraftClient client, String message) {
        sendFeedback(client, settings.chestChatFeedback, "[Chest Crash] ", message);
    }

    public void feedbackArmor(MinecraftClient client, String message) {
        sendFeedback(client, settings.armorChatFeedback, "[Armor Placer] ", message);
    }

    public void feedbackChestConfigToggle(String message) {
        sendFeedback(MinecraftClient.getInstance(), true, "[Chest Crash] ", message);
    }

    public void feedbackArmorConfigToggle(String message) {
        sendFeedback(MinecraftClient.getInstance(), true, "[Armor Placer] ", message);
    }

    private static void sendFeedback(MinecraftClient client, boolean enabled, String prefix, String message) {
        if (!enabled || client == null || message == null || message.isBlank()) {
            return;
        }
        Text line = Text.literal(prefix).formatted(Formatting.RED, Formatting.BOLD)
                .append(Text.literal(message).formatted(Formatting.GRAY));
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(line, false);
            }
        });
    }

    private record PlaceTarget(BlockPos blockPos, Direction face) {
    }
}
