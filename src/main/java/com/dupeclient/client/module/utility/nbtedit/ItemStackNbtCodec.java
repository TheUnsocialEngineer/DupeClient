package com.dupeclient.client.module.utility.nbtedit;

import com.mojang.serialization.DynamicOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import java.util.stream.Collectors;

/** Encodes/decodes item stacks as SNBT using the vanilla item stack codec (1.21+ component format). */
public final class ItemStackNbtCodec {
    private ItemStackNbtCodec() {
    }

    public static HolderLookup.Provider registries(Minecraft client) {
        if (client.level != null) {
            return client.level.registryAccess();
        }
        return client.getConnection() != null
                ? client.getConnection().registryAccess()
                : Minecraft.getInstance().getConnection().registryAccess();
    }

    public static DynamicOps<Tag> nbtOps(HolderLookup.Provider lookup) {
        return lookup.createSerializationContext(NbtOps.INSTANCE);
    }

    public static CompoundTag toCompound(ItemStack stack, HolderLookup.Provider lookup) {
        return (CompoundTag) ItemStack.CODEC.encodeStart(nbtOps(lookup), stack)
                .getOrThrow(msg -> new IllegalStateException("Failed to encode item stack: " + msg));
    }

    public static String toSnbt(ItemStack stack, HolderLookup.Provider lookup) {
        return NbtUtils.prettyPrint(toCompound(stack, lookup), true);
    }

    public static ItemStack fromSnbt(String snbt, HolderLookup.Provider lookup) throws Exception {
        CompoundTag compound = TagParser.parseCompoundFully(snbt);
        return fromCompound(compound, lookup);
    }

    public static ItemStack fromCompound(CompoundTag compound, HolderLookup.Provider lookup) throws Exception {
        return ItemStack.CODEC.parse(nbtOps(lookup), compound)
                .getOrThrow(msg -> new IllegalArgumentException("Failed to parse item stack: " + msg));
    }

    public static String toGiveCommand(ItemStack stack, HolderLookup.Provider lookup) {
        CompoundTag compound = toCompound(stack, lookup);
        String id = compound.contains("id") ? compound.getString("id").orElse("minecraft:stone") : "minecraft:stone";
        int count = compound.contains("count") ? compound.getInt("count").orElse(1) : 1;
        if (!compound.contains("components")) {
            if (count > 1) {
                return "/give @s " + id + " " + count;
            }
            return "/give @s " + id;
        }
        CompoundTag components = compound.getCompound("components").orElseGet(CompoundTag::new);
        if (components.isEmpty()) {
            if (count > 1) {
                return "/give @s " + id + " " + count;
            }
            return "/give @s " + id;
        }
        String bracket = components.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + NbtUtils.prettyPrint(entry.getValue(), false))
                .collect(Collectors.joining(","));
        String base = "/give @s " + id + "[" + bracket + "]";
        if (count > 1) {
            return base + " " + count;
        }
        return base;
    }

    public static String itemSummary(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getCount() + "x " + stack.getHoverName().getString();
    }
}
