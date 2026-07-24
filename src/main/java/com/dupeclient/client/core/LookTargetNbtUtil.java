package com.dupeclient.client.core;

import com.dupeclient.client.core.LookTargetUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side NBT snapshot for the block or entity the player is looking at.
 * Data is limited to what the local world already knows (no extra server queries).
 */
public final class LookTargetNbtUtil {
    private LookTargetNbtUtil() {
    }

    public record CaptureResult(String kind, String summary, String snbt, CompoundTag compound) {
    }

    @Nullable
    public static CaptureResult capture(Minecraft client) {
        LookTargetUtil.LookTarget target = LookTargetUtil.pick(client);
        if (target == null) {
            return null;
        }
        if (target.entity() != null) {
            return captureEntity(client, target.entity().getEntity());
        }
        if (target.block() != null && client.level != null) {
            return captureBlock(client.level, target.block().getBlockPos());
        }
        return null;
    }

    @Nullable
    private static CaptureResult captureEntity(Minecraft client, Entity entity) {
        ClientLevel world = client.level;
        if (world == null) {
            return null;
        }
        HolderLookup.Provider registries = world.registryAccess();
        TagValueOutput view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        entity.saveWithoutId(view);
        CompoundTag compound = view.buildResult();
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String type = typeId == null ? "unknown" : typeId.toString();
        String summary = "entity " + type + " @ "
            + formatPos(entity.blockPosition())
            + " (id=" + entity.getId() + ", uuid=" + entity.getStringUUID() + ")";
        return new CaptureResult("entity", summary, formatSnbt(compound), compound);
    }

    @Nullable
    private static CaptureResult captureBlock(ClientLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        HolderLookup.Provider registries = world.registryAccess();

        CompoundTag compound;
        String summary;
        if (blockEntity != null) {
            compound = blockEntity.saveWithoutMetadata(registries);
            Identifier typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
            String type = typeId == null ? "block_entity" : typeId.toString();
            summary = "block_entity " + type + " @ " + formatPos(pos);
        } else {
            compound = new CompoundTag();
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            compound.putString("id", blockId == null ? "unknown" : blockId.toString());
            compound.putInt("x", pos.getX());
            compound.putInt("y", pos.getY());
            compound.putInt("z", pos.getZ());
            CompoundTag properties = new CompoundTag();
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

    private static <T extends Comparable<T>> void appendProperty(CompoundTag properties, Property<T> property, BlockState state) {
        T value = state.getValue(property);
        properties.putString(property.getName(), property.getName(value));
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static String formatSnbt(CompoundTag compound) {
        return NbtUtils.prettyPrint(compound, true);
    }
}
