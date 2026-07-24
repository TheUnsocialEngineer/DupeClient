package com.dupeclient.client.module.macro;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

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
        return Registries.ITEM.get(id);
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
    public static Slot findAnyQuickMoveSourceSlot(ScreenHandler handler, PlayerEntity player, boolean takeFromContainer) {
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasStack()) {
                continue;
            }
            boolean isPlayer = slot.inventory == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            return slot;
        }
        return null;
    }

    public static boolean hasNonPlayerInventorySlots(ScreenHandler handler, PlayerEntity player) {
        for (Slot slot : handler.slots) {
            if (slot != null && slot.inventory != player.getInventory()) {
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
    public static Slot findQuickMoveSourceSlot(ScreenHandler handler, PlayerEntity player, Item item, boolean takeFromContainer) {
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasStack()) {
                continue;
            }
            boolean isPlayer = slot.inventory == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            if (slot.getStack().isOf(item)) {
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
            ScreenHandler handler, PlayerEntity player, boolean takeFromContainer, int maxCount) {
        Slot best = null;
        int bestCount = Integer.MAX_VALUE;
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasStack()) {
                continue;
            }
            boolean isPlayer = slot.inventory == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            int c = slot.getStack().getCount();
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
    public static Slot findSmallestAnyStack(ScreenHandler handler, PlayerEntity player, boolean takeFromContainer) {
        Slot best = null;
        int bestCount = Integer.MAX_VALUE;
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasStack()) {
                continue;
            }
            boolean isPlayer = slot.inventory == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            int c = slot.getStack().getCount();
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
    public static Slot findSmallestStackUnderCap(ScreenHandler handler, PlayerEntity player, Item item, boolean takeFromContainer, int maxCount) {
        Slot best = null;
        int bestCount = Integer.MAX_VALUE;
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasStack()) {
                continue;
            }
            boolean isPlayer = slot.inventory == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            if (!slot.getStack().isOf(item)) {
                continue;
            }
            int c = slot.getStack().getCount();
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
    public static Slot findSmallestMatchingStack(ScreenHandler handler, PlayerEntity player, Item item, boolean takeFromContainer) {
        Slot best = null;
        int bestCount = Integer.MAX_VALUE;
        for (Slot slot : handler.slots) {
            if (slot == null || !slot.hasStack()) {
                continue;
            }
            boolean isPlayer = slot.inventory == player.getInventory();
            if (takeFromContainer == isPlayer) {
                continue;
            }
            if (!slot.getStack().isOf(item)) {
                continue;
            }
            int c = slot.getStack().getCount();
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
            case "ENDER_CHEST" -> state.isOf(Blocks.ENDER_CHEST);
            case "BARREL" -> state.isOf(Blocks.BARREL);
            case "SHULKER_BOX" -> state.getBlock() instanceof ShulkerBoxBlock;
            case "CRAFTING_TABLE" -> state.isOf(Blocks.CRAFTING_TABLE);
            case "FURNACE" -> state.isOf(Blocks.FURNACE);
            case "BLAST_FURNACE" -> state.isOf(Blocks.BLAST_FURNACE);
            case "SMOKER" -> state.isOf(Blocks.SMOKER);
            case "ANVIL" -> state.isOf(Blocks.ANVIL) || state.isOf(Blocks.CHIPPED_ANVIL) || state.isOf(Blocks.DAMAGED_ANVIL);
            case "SMITHING_TABLE" -> state.isOf(Blocks.SMITHING_TABLE);
            case "GRINDSTONE" -> state.isOf(Blocks.GRINDSTONE);
            case "LECTERN" -> state.isOf(Blocks.LECTERN);
            case "STONECUTTER" -> state.isOf(Blocks.STONECUTTER);
            case "ENCHANTING_TABLE" -> state.isOf(Blocks.ENCHANTING_TABLE);
            case "BELL" -> state.isOf(Blocks.BELL);
            case "HOPPER" -> state.isOf(Blocks.HOPPER);
            case "DISPENSER" -> state.isOf(Blocks.DISPENSER);
            case "DROPPER" -> state.isOf(Blocks.DROPPER);
            case "BREWING_STAND" -> state.isOf(Blocks.BREWING_STAND);
            case "CARTOGRAPHY_TABLE" -> state.isOf(Blocks.CARTOGRAPHY_TABLE);
            case "LOOM" -> state.isOf(Blocks.LOOM);
            case "OTHER" -> {
                Identifier id = Identifier.tryParse(customBlockId == null ? "" : customBlockId.trim());
                if (id == null) {
                    yield false;
                }
                Block b = Registries.BLOCK.get(id);
                yield b != null && b != Blocks.AIR && state.isOf(b);
            }
            default -> false;
        };
    }

    /**
     * Finds the nearest block matching the preset within a Euclidean radius (in blocks) from the player.
     */
    @Nullable
    public static BlockPos findNearestMatchingBlock(MinecraftClient client, String preset, String customBlockId, int radiusBlocks) {
        ClientPlayerEntity player = client.player;
        World world = client.world;
        if (player == null || world == null) {
            return null;
        }
        int r = Math.max(1, radiusBlocks);
        int rSq = r * r;
        BlockPos origin = player.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        BlockPos best = null;
        double bestD2 = Double.MAX_VALUE;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > rSq) {
                        continue;
                    }
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!world.isChunkLoaded(m)) {
                        continue;
                    }
                    BlockState st = world.getBlockState(m);
                    if (st.isAir()) {
                        continue;
                    }
                    if (!blockPresetMatches(preset, customBlockId, st)) {
                        continue;
                    }
                    double d2 = player.squaredDistanceTo(Vec3d.ofCenter(m));
                    if (d2 < bestD2) {
                        bestD2 = d2;
                        best = m.toImmutable();
                    }
                }
            }
        }
        return best;
    }

    public static void lookToward(ClientPlayerEntity player, Vec3d target) {
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) (MathHelper.atan2(dx, dz) * (180f / Math.PI));
        yaw = MathHelper.wrapDegrees(yaw);
        player.setYaw(yaw);
        player.setBodyYaw(yaw);
        player.setHeadYaw(yaw);
    }

    /**
     * Right-click / “use” with the item in the main hotbar slot {@code slot0To8} (0 = leftmost, 8 = rightmost),
     * then restores the player’s selected hotbar slot.
     */
    public static boolean tryUseHotbarSlot(MinecraftClient client, int slot0To8) {
        if (client.player == null || client.interactionManager == null) {
            return false;
        }
        int slot = MathHelper.clamp(slot0To8, 0, 8);
        PlayerInventory inv = client.player.getInventory();
        int prev = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        ActionResult r = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        inv.setSelectedSlot(prev);
        return r.isAccepted();
    }

    public static boolean tryInteractBlock(MinecraftClient client, BlockPos pos) {
        if (client.player == null || client.interactionManager == null || client.world == null) {
            return false;
        }
        Vec3d center = Vec3d.ofCenter(pos);
        lookToward(client.player, center);
        BlockHitResult hit = new BlockHitResult(center, Direction.UP, pos.toImmutable(), false);
        return client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit).isAccepted();
    }

    public static void quickMoveSlot(MinecraftClient client, ScreenHandler handler, Slot slot) {
        if (client.player == null || client.interactionManager == null || slot == null) {
            return;
        }
        clickSlot(client, handler, slot.id, SlotActionType.QUICK_MOVE, 0);
    }

    public static void clickSlot(
            MinecraftClient client,
            ScreenHandler handler,
            int slotId,
            SlotActionType action,
            int button) {
        if (client.player == null || client.interactionManager == null || handler == null || action == null) {
            return;
        }
        client.interactionManager.clickSlot(handler.syncId, slotId, button, action, client.player);
    }
}
