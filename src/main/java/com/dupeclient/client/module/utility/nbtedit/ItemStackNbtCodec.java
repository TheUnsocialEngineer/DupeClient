package com.dupeclient.client.module.utility.nbtedit;

import com.mojang.serialization.DynamicOps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryWrapper;

import java.util.stream.Collectors;

/** Encodes/decodes item stacks as SNBT using the vanilla item stack codec (1.21+ component format). */
public final class ItemStackNbtCodec {
    private ItemStackNbtCodec() {
    }

    public static RegistryWrapper.WrapperLookup registries(MinecraftClient client) {
        if (client.world != null) {
            return client.world.getRegistryManager();
        }
        return client.getNetworkHandler() != null
                ? client.getNetworkHandler().getRegistryManager()
                : MinecraftClient.getInstance().getNetworkHandler().getRegistryManager();
    }

    public static DynamicOps<NbtElement> nbtOps(RegistryWrapper.WrapperLookup lookup) {
        return lookup.getOps(NbtOps.INSTANCE);
    }

    public static NbtCompound toCompound(ItemStack stack, RegistryWrapper.WrapperLookup lookup) {
        return (NbtCompound) ItemStack.CODEC.encodeStart(nbtOps(lookup), stack)
                .getOrThrow(msg -> new IllegalStateException("Failed to encode item stack: " + msg));
    }

    public static String toSnbt(ItemStack stack, RegistryWrapper.WrapperLookup lookup) {
        return NbtHelper.toFormattedString(toCompound(stack, lookup), true);
    }

    public static ItemStack fromSnbt(String snbt, RegistryWrapper.WrapperLookup lookup) throws Exception {
        NbtCompound compound = StringNbtReader.readCompound(snbt);
        return fromCompound(compound, lookup);
    }

    public static ItemStack fromCompound(NbtCompound compound, RegistryWrapper.WrapperLookup lookup) throws Exception {
        return ItemStack.CODEC.parse(nbtOps(lookup), compound)
                .getOrThrow(msg -> new IllegalArgumentException("Failed to parse item stack: " + msg));
    }

    public static String toGiveCommand(ItemStack stack, RegistryWrapper.WrapperLookup lookup) {
        NbtCompound compound = toCompound(stack, lookup);
        String id = compound.contains("id") ? compound.getString("id").orElse("minecraft:stone") : "minecraft:stone";
        int count = compound.contains("count") ? compound.getInt("count").orElse(1) : 1;
        if (!compound.contains("components")) {
            if (count > 1) {
                return "/give @s " + id + " " + count;
            }
            return "/give @s " + id;
        }
        NbtCompound components = compound.getCompound("components").orElseGet(NbtCompound::new);
        if (components.isEmpty()) {
            if (count > 1) {
                return "/give @s " + id + " " + count;
            }
            return "/give @s " + id;
        }
        String bracket = components.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + NbtHelper.toFormattedString(entry.getValue(), false))
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
        return stack.getCount() + "x " + stack.getItem().getName().getString();
    }
}
