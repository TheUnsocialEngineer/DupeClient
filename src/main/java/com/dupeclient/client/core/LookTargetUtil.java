package com.dupeclient.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class LookTargetUtil {
    private LookTargetUtil() {
    }

    @Nullable
    public static LookTarget pick(Minecraft client) {
        LocalPlayer player = client.player;
        Level world = client.level;
        if (player == null || world == null) {
            return null;
        }

        Vec3 start = player.getEyePosition();
        Vec3 rot = player.getViewVector(1.0f);
        double blockReach = Math.max(0.0, player.blockInteractionRange());
        double entityReach = Math.max(0.0, player.entityInteractionRange());

        Vec3 blockEnd = start.add(rot.scale(blockReach));
        BlockHitResult rayBlock = world.clip(new ClipContext(
                start,
                blockEnd,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player));
        if (rayBlock.getType() != HitResult.Type.BLOCK) {
            rayBlock = null;
        }

        Vec3 entityEnd = start.add(rot.scale(entityReach));
        AABB searchBox = player.getBoundingBox().expandTowards(rot.scale(entityReach)).inflate(1.25, 1.25, 1.25);
        EntityHitResult rayEntity = ProjectileUtil.getEntityHitResult(
                world,
                player,
                start,
                entityEnd,
                searchBox,
                LookTargetUtil::pickableTarget,
                0.25f);

        // Prefer Minecraft's current crosshair target so automation matches what the player visibly targets.
        HitResult vanillaTarget = client.hitResult;
        if (vanillaTarget instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            if (pickableTarget(e)) {
                return new LookTarget(null, ehr);
            }
        } else if (vanillaTarget instanceof BlockHitResult bhr && vanillaTarget.getType() == HitResult.Type.BLOCK) {
            return new LookTarget(bhr, null);
        }

        // If crosshairTarget is unavailable or MISS, merge raycast results and pick nearest.
        BlockHitResult vanillaBlock = null;
        EntityHitResult vanillaEntity = null;

        BlockHitResult blockCandidate = nearerBlock(start, rayBlock, vanillaBlock);
        EntityHitResult entityCandidate = nearerEntity(start, rayEntity, vanillaEntity);

        if (blockCandidate != null && entityCandidate != null) {
            double db = blockCandidate.getLocation().distanceToSqr(start);
            double de = entityCandidate.getLocation().distanceToSqr(start);
            if (de <= db + 1.0e-7) {
                return new LookTarget(null, entityCandidate);
            }
            return new LookTarget(blockCandidate, null);
        }
        return new LookTarget(blockCandidate, entityCandidate);
    }

    private static boolean pickableTarget(Entity e) {
        return e != null && e.isAlive() && e.isPickable() && !e.isSpectator();
    }

    @Nullable
    private static BlockHitResult nearerBlock(Vec3 start, @Nullable BlockHitResult a, @Nullable BlockHitResult b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.getLocation().distanceToSqr(start) <= b.getLocation().distanceToSqr(start) ? a : b;
    }

    @Nullable
    private static EntityHitResult nearerEntity(Vec3 start, @Nullable EntityHitResult a, @Nullable EntityHitResult b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.getLocation().distanceToSqr(start) <= b.getLocation().distanceToSqr(start) ? a : b;
    }

    @Nullable
    public static String describe(Minecraft client) {
        LookTarget t = pick(client);
        if (t == null) {
            return null;
        }
        if (t.entity() != null) {
            Entity e = t.entity().getEntity();
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            if (id == null) {
                return "Looking: entity";
            }
            return "Looking: " + id;
        }
        if (t.block() != null && client.level != null) {
            BlockState st = client.level.getBlockState(t.block().getBlockPos());
            Identifier id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
            if (id == null) {
                return "Looking: block";
            }
            return "Looking: " + id;
        }
        return "Looking: none";
    }

    public record LookTarget(@Nullable BlockHitResult block, @Nullable EntityHitResult entity) {
    }
}
