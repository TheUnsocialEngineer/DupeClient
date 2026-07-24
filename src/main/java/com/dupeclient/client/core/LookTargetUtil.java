package com.dupeclient.client.core;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class LookTargetUtil {
    private LookTargetUtil() {
    }

    @Nullable
    public static LookTarget pick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        World world = client.world;
        if (player == null || world == null) {
            return null;
        }

        Vec3d start = player.getEyePos();
        Vec3d rot = player.getRotationVec(1.0f);
        double blockReach = Math.max(0.0, player.getBlockInteractionRange());
        double entityReach = Math.max(0.0, player.getEntityInteractionRange());

        Vec3d blockEnd = start.add(rot.multiply(blockReach));
        BlockHitResult rayBlock = world.raycast(new RaycastContext(
                start,
                blockEnd,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player));
        if (rayBlock.getType() != HitResult.Type.BLOCK) {
            rayBlock = null;
        }

        Vec3d entityEnd = start.add(rot.multiply(entityReach));
        Box searchBox = player.getBoundingBox().stretch(rot.multiply(entityReach)).expand(1.25, 1.25, 1.25);
        EntityHitResult rayEntity = ProjectileUtil.getEntityCollision(
                world,
                player,
                start,
                entityEnd,
                searchBox,
                LookTargetUtil::pickableTarget,
                0.25f);

        // Prefer Minecraft's current crosshair target so automation matches what the player visibly targets.
        HitResult vanillaTarget = client.crosshairTarget;
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
            double db = blockCandidate.getPos().squaredDistanceTo(start);
            double de = entityCandidate.getPos().squaredDistanceTo(start);
            if (de <= db + 1.0e-7) {
                return new LookTarget(null, entityCandidate);
            }
            return new LookTarget(blockCandidate, null);
        }
        return new LookTarget(blockCandidate, entityCandidate);
    }

    private static boolean pickableTarget(Entity e) {
        return e != null && e.isAlive() && e.canHit() && !e.isSpectator();
    }

    @Nullable
    private static BlockHitResult nearerBlock(Vec3d start, @Nullable BlockHitResult a, @Nullable BlockHitResult b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.getPos().squaredDistanceTo(start) <= b.getPos().squaredDistanceTo(start) ? a : b;
    }

    @Nullable
    private static EntityHitResult nearerEntity(Vec3d start, @Nullable EntityHitResult a, @Nullable EntityHitResult b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.getPos().squaredDistanceTo(start) <= b.getPos().squaredDistanceTo(start) ? a : b;
    }

    @Nullable
    public static String describe(MinecraftClient client) {
        LookTarget t = pick(client);
        if (t == null) {
            return null;
        }
        if (t.entity() != null) {
            Entity e = t.entity().getEntity();
            Identifier id = Registries.ENTITY_TYPE.getId(e.getType());
            if (id == null) {
                return "Looking: entity";
            }
            return "Looking: " + id;
        }
        if (t.block() != null && client.world != null) {
            BlockState st = client.world.getBlockState(t.block().getBlockPos());
            Identifier id = Registries.BLOCK.getId(st.getBlock());
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
