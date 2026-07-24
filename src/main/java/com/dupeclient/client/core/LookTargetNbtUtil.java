package com.dupeclient.client.core;

import com.dupeclient.client.core.LookTargetUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.Property;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side NBT snapshot for the block or entity the player is looking at.
 * Data is limited to what the local world already knows (no extra server queries).
 */
public final class LookTargetNbtUtil {
    private LookTargetNbtUtil() {
    }

    public record CaptureResult(String kind, String summary, String snbt, NbtCompound compound) {
    }

    @Nullable
    public static CaptureResult capture(MinecraftClient client) {
        LookTargetUtil.LookTarget target = LookTargetUtil.pick(client);
        if (target == null) {
            return null;
        }
        if (target.entity() != null) {
            return captureEntity(client, target.entity().getEntity());
        }
        if (target.block() != null && client.world != null) {
            return captureBlock(client.world, target.block().getBlockPos());
        }
        return null;
    }

    @Nullable
    private static CaptureResult captureEntity(MinecraftClient client, Entity entity) {
        ClientWorld world = client.world;
        if (world == null) {
            return null;
        }
        RegistryWrapper.WrapperLookup registries = world.getRegistryManager();
        NbtWriteView view = NbtWriteView.create(ErrorReporter.EMPTY, registries);
        entity.writeData(view);
        NbtCompound compound = view.getNbt();
        Identifier typeId = Registries.ENTITY_TYPE.getId(entity.getType());
        String type = typeId == null ? "unknown" : typeId.toString();
        String summary = "entity " + type + " @ "
            + formatPos(entity.getBlockPos())
            + " (id=" + entity.getId() + ", uuid=" + entity.getUuidAsString() + ")";
        return new CaptureResult("entity", summary, formatSnbt(compound), compound);
    }

    @Nullable
    private static CaptureResult captureBlock(ClientWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        RegistryWrapper.WrapperLookup registries = world.getRegistryManager();

        NbtCompound compound;
        String summary;
        if (blockEntity != null) {
            compound = blockEntity.createNbt(registries);
            Identifier typeId = Registries.BLOCK_ENTITY_TYPE.getId(blockEntity.getType());
            String type = typeId == null ? "block_entity" : typeId.toString();
            summary = "block_entity " + type + " @ " + formatPos(pos);
        } else {
            compound = new NbtCompound();
            Identifier blockId = Registries.BLOCK.getId(state.getBlock());
            compound.putString("id", blockId == null ? "unknown" : blockId.toString());
            compound.putInt("x", pos.getX());
            compound.putInt("y", pos.getY());
            compound.putInt("z", pos.getZ());
            NbtCompound properties = new NbtCompound();
            for (Property<?> property : state.getProperties()) {
                appendProperty(properties, property, state);
            }
            if (!properties.isEmpty()) {
                compound.put("properties", properties);
            }
            summary = "block " + (blockId == null ? "unknown" : blockId.toString()) + " @ " + formatPos(pos);
        }

        return new CaptureResult(blockEntity == null ? "block" : "block_entity", summary, formatSnbt(compound), compound);
    }

    private static <T extends Comparable<T>> void appendProperty(NbtCompound properties, Property<T> property, BlockState state) {
        T value = state.get(property);
        properties.putString(property.getName(), property.name(value));
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static String formatSnbt(NbtCompound compound) {
        return NbtHelper.toFormattedString(compound, true);
    }
}
