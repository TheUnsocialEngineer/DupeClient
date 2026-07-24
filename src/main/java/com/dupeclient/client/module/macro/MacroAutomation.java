package com.dupeclient.client.module.macro;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side helpers for macro steps that move inventory via shift-clicks or use nearby blocks.
 */
public final class MacroAutomation {
    /** Preset ids for {@link MacroStep#blockPreset} / graph node {@code blockPreset}, in UI cycle order. */
    public static final String[] BLOCK_PRESET_CYCLE = {
            "CHEST",
            "ENDER_CHEST",
            "BARREL",
            "SHULKER_BOX",
            "CRAFTING_TABLE",
            "FURNACE",
            "BLAST_FURNACE",
            "SMOKER",
            "ANVIL",
            "SMITHING_TABLE",
            "GRINDSTONE",
            "LECTERN",
            "STONECUTTER",
            "ENCHANTING_TABLE",
            "BELL",
            "HOPPER",
            "DISPENSER",
            "DROPPER",
            "BREWING_STAND",
            "CARTOGRAPHY_TABLE",
            "LOOM",
            "OTHER"
    };

    private MacroAutomation() {
    }

    public static String normalizeBlockPreset(String raw) {
        if (raw == null || raw.isBlank()) {
            return "CHEST";
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        for (String p : BLOCK_PRESET_CYCLE) {
            if (p.equals(t)) {
                return p;
            }
        }
        return "CHEST";
    }

    public static String nextBlockPreset(String current) {
        String cur = normalizeBlockPreset(current);
        for (int i = 0; i < BLOCK_PRESET_CYCLE.length; i++) {
            if (BLOCK_PRESET_CYCLE[i].equals(cur)) {
                return BLOCK_PRESET_CYCLE[(i + 1) % BLOCK_PRESET_CYCLE.length];
            }
        }
        return BLOCK_PRESET_CYCLE[1];
    }

    public static Identifier parseItemId(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (!s.contains(":")) {
            s = "minecraft:" + s;
        }
        return Identifier.tryParse(s);
    }

    public static Item resolveItem(String raw) {
        if (isAnyItem(raw)) {
            return Items.AIR;
        }
        Identifier id = parseItemId(raw);
        if (id == null) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.getValue(id);
    }

    public static boolean isAnyItem(@Nullable String raw) {
        if (raw == null) {
            return false;
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        return t.equals("*") || t.equals("any") || t.equals("#any") || t.equals("all_items");
    }

    /**
     * First non-empty slot on the source side for shift-click quick move (any item type).
     */
    @Nullable
    public static Slot findAnyQuickMoveSourceSlot(AbstractContainerMenu handler, Player player, boolean takeFromContainer) {
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasItem()) {
                continue;
            }
            boolean isPlayer = slot.container == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            return slot;
        }
        return null;
    }

    public static boolean hasNonPlayerInventorySlots(AbstractContainerMenu handler, Player player) {
        for (Slot slot : handler.slots) {
            if (slot != null && slot.container != player.getInventory()) {
                return true;
            }
        }
        return false;
    }

    /**
     * First slot on the source side matching {@code item} for a shift-click quick move.
     *
     * @param takeFromContainer {@code true} = move from container into player; {@code false} = from player into container
     */
    public static Slot findQuickMoveSourceSlot(AbstractContainerMenu handler, Player player, Item item, boolean takeFromContainer) {
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasItem()) {
                continue;
            }
            boolean isPlayer = slot.container == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            if (slot.getItem().is(item)) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Smallest stack on the source side regardless of item type.
     */
    @Nullable
    public static Slot findSmallestAnyStackUnderCap(
            AbstractContainerMenu handler, Player player, boolean takeFromContainer, int maxCount) {
        Slot best = null;
        int bestCount = Integer.MAX_VALUE;
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasItem()) {
                continue;
            }
            boolean isPlayer = slot.container == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            int c = slot.getItem().getCount();
            if (c > maxCount) {
                continue;
            }
            if (c < bestCount) {
                bestCount = c;
                best = slot;
            }
        }
        return best;
    }

    /**
     * Smallest stack on the source side (any item), including stacks larger than {@code maxCount}.
     */
    @Nullable
    public static Slot findSmallestAnyStack(AbstractContainerMenu handler, Player player, boolean takeFromContainer) {
        Slot best = null;
        int bestCount = Integer.MAX_VALUE;
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasItem()) {
                continue;
            }
            boolean isPlayer = slot.container == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            int c = slot.getItem().getCount();
            if (c < bestCount) {
                bestCount = c;
                best = slot;
            }
        }
        return best;
    }

    /**
     * Smallest matching stack whose count does not exceed {@code maxCount} (for precise-ish moves).
     */
    public static Slot findSmallestStackUnderCap(AbstractContainerMenu handler, Player player, Item item, boolean takeFromContainer, int maxCount) {
        Slot best = null;
        int bestCount = Integer.MAX_VALUE;
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasItem()) {
                continue;
            }
            boolean isPlayer = slot.container == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            if (!slot.getItem().is(item)) {
                continue;
            }
            int c = slot.getItem().getCount();
            if (c > maxCount) {
                continue;
            }
            if (c < bestCount) {
                bestCount = c;
                best = slot;
            }
        }
        return best;
    }

    /**
     * Smallest matching stack (including stacks larger than {@code maxCount}) — used to finish a remainder with one overshooting shift-click.
     */
    public static Slot findSmallestMatchingStack(AbstractContainerMenu handler, Player player, Item item, boolean takeFromContainer) {
        Slot best = null;
        int bestCount = Integer.MAX_VALUE;
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasItem()) {
                continue;
            }
            boolean isPlayer = slot.container == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            if (!slot.getItem().is(item)) {
                continue;
            }
            int c = slot.getItem().getCount();
            if (c < bestCount) {
                bestCount = c;
                best = slot;
            }
        }
        return best;
    }

    public static boolean blockPresetMatches(String preset, String customBlockId, BlockState state) {
        String p = normalizeBlockPreset(preset);
        return switch (p) {
            case "CHEST" -> state.getBlock() instanceof ChestBlock || state.getBlock() instanceof TrappedChestBlock;
            case "ENDER_CHEST" -> state.is(Blocks.ENDER_CHEST);
            case "BARREL" -> state.is(Blocks.BARREL);
            case "SHULKER_BOX" -> state.getBlock() instanceof ShulkerBoxBlock;
            case "CRAFTING_TABLE" -> state.is(Blocks.CRAFTING_TABLE);
            case "FURNACE" -> state.is(Blocks.FURNACE);
            case "BLAST_FURNACE" -> state.is(Blocks.BLAST_FURNACE);
            case "SMOKER" -> state.is(Blocks.SMOKER);
            case "ANVIL" -> state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL);
            case "SMITHING_TABLE" -> state.is(Blocks.SMITHING_TABLE);
            case "GRINDSTONE" -> state.is(Blocks.GRINDSTONE);
            case "LECTERN" -> state.is(Blocks.LECTERN);
            case "STONECUTTER" -> state.is(Blocks.STONECUTTER);
            case "ENCHANTING_TABLE" -> state.is(Blocks.ENCHANTING_TABLE);
            case "BELL" -> state.is(Blocks.BELL);
            case "HOPPER" -> state.is(Blocks.HOPPER);
            case "DISPENSER" -> state.is(Blocks.DISPENSER);
            case "DROPPER" -> state.is(Blocks.DROPPER);
            case "BREWING_STAND" -> state.is(Blocks.BREWING_STAND);
            case "CARTOGRAPHY_TABLE" -> state.is(Blocks.CARTOGRAPHY_TABLE);
            case "LOOM" -> state.is(Blocks.LOOM);
            case "OTHER" -> {
                Identifier id = Identifier.tryParse(customBlockId == null ? "" : customBlockId.trim());
                if (id == null) {
                    yield false;
                }
                Block b = BuiltInRegistries.BLOCK.getValue(id);
                yield b != null && b != Blocks.AIR && state.is(b);
            }
            default -> false;
        };
    }

    /**
     * Finds the nearest block matching the preset within a Euclidean radius (in blocks) from the player.
     */
    @Nullable
    public static BlockPos findNearestMatchingBlock(Minecraft client, String preset, String customBlockId, int radiusBlocks) {
        LocalPlayer player = client.player;
        Level world = client.level;
        if (player == null || world == null) {
            return null;
        }
        int r = Math.max(1, radiusBlocks);
        int rSq = r * r;
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestD2 = Double.MAX_VALUE;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > rSq) {
                        continue;
                    }
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!world.hasChunkAt(m)) {
                        continue;
                    }
                    BlockState st = world.getBlockState(m);
                    if (st.isAir()) {
                        continue;
                    }
                    if (!blockPresetMatches(preset, customBlockId, st)) {
                        continue;
                    }
                    double d2 = player.distanceToSqr(Vec3.atCenterOf(m));
                    if (d2 < bestD2) {
                        bestD2 = d2;
                        best = m.immutable();
                    }
                }
            }
        }
        return best;
    }

    public static void lookToward(LocalPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) (Mth.atan2(dx, dz) * (180f / Math.PI));
        yaw = Mth.wrapDegrees(yaw);
        player.setYRot(yaw);
        player.setYBodyRot(yaw);
        player.setYHeadRot(yaw);
    }

    /**
     * Right-click / “use” with the item in the main hotbar slot {@code slot0To8} (0 = leftmost, 8 = rightmost),
     * then restores the player’s selected hotbar slot.
     */
    public static boolean tryUseHotbarSlot(Minecraft client, int slot0To8) {
        if (client.player == null || client.gameMode == null) {
            return false;
        }
        int slot = Mth.clamp(slot0To8, 0, 8);
        Inventory inv = client.player.getInventory();
        int prev = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        InteractionResult r = client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        inv.setSelectedSlot(prev);
        return r.consumesAction();
    }

    public static boolean tryInteractBlock(Minecraft client, BlockPos pos) {
        if (client.player == null || client.gameMode == null || client.level == null) {
            return false;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        lookToward(client.player, center);
        BlockHitResult hit = new BlockHitResult(center, Direction.UP, pos.immutable(), false);
        return client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit).consumesAction();
    }

    public static void quickMoveSlot(Minecraft client, AbstractContainerMenu handler, Slot slot) {
        if (client.player == null || client.gameMode == null || slot == null) {
            return;
        }
        clickSlot(client, handler, slot.index, ContainerInput.QUICK_MOVE, 0);
    }

    public static void clickSlot(
            Minecraft client,
            AbstractContainerMenu handler,
            int slotId,
            ContainerInput action,
            int button) {
        if (client.player == null || client.gameMode == null || handler == null || action == null) {
            return;
        }
        client.gameMode.handleContainerInput(handler.containerId, slotId, button, action, client.player);
    }
}
