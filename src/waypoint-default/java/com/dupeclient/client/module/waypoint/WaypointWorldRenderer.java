package com.dupeclient.client.module.waypoint;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.cape.DupeClientPresenceConfigManager;
import com.dupeclient.client.module.waypoint.DupeClientWaypoint;
import com.dupeclient.client.module.waypoint.DupeClientWaypointManager;
import com.dupeclient.client.module.waypoint.SharedDupeClientWaypoint;
import com.dupeclient.client.module.waypoint.WaypointShape;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class WaypointWorldRenderer {
    private static final float HOVER_BLOCKS = 5.0f;
    private static final float LABEL_GAP = 1.5f;
    private static final float SOLID_ALPHA = 0.92f;

    private WaypointWorldRenderer() {
    }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(WaypointWorldRenderer::collectSubmits);
    }

    private static void collectSubmits(LevelRenderContext context) {
        if (!Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().showSharedWaypointsInWorld)) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        SubmitNodeCollector collector = context.submitNodeCollector();
        if (collector == null) {
            return;
        }
        CameraRenderState camera = context.levelState().cameraRenderState;
        if (camera == null) {
            return;
        }

        try {
            float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            double px = Mth.lerp(tickDelta, client.player.xOld, client.player.getX());
            double py = Mth.lerp(tickDelta, client.player.yOld, client.player.getY());
            double pz = Mth.lerp(tickDelta, client.player.zOld, client.player.getZ());
            DrawableGizmoPrimitives gizmos = new DrawableGizmoPrimitives();

            for (SharedDupeClientWaypoint row : DupeClientWaypointManager.INSTANCE.visibleWaypoints(client)) {
                DupeClientWaypoint wp = row.waypoint();
                int color = colorWithAlpha(wp.colorArgb(), SOLID_ALPHA);
                float wx = (float) wp.x() + 0.5f;
                float groundY = wp.y();
                float hoverY = groundY + HOVER_BLOCKS;
                float wz = (float) wp.z() + 0.5f;

                drawShape(gizmos, wp.shape(), wx, hoverY, wz, color);

                double dx = wx - px;
                double dy = groundY + 0.5 - py;
                double dz = wz - pz;
                int dist = Math.round((float) Math.sqrt(dx * dx + dy * dy + dz * dz));
                String label = wp.name() + "  " + dist + "m";
                float labelY = hoverY + shapeTopOffset(wp.shape()) + LABEL_GAP;
                collector.submitNameTag(
                        context.poseStack(),
                        new Vec3(wx, labelY, wz),
                        0,
                        Component.literal(label),
                        true,
                        0xF000F0,
                        camera);
            }

            gizmos.submit(collector, camera, false);
        } catch (Exception ex) {
            DupeClient.LOGGER.debug("Waypoint world render failed", ex);
        }
    }

    public static void renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
    }

    private static int colorWithAlpha(int argb, float alpha) {
        int rgb = argb & 0x00FFFFFF;
        int a = Mth.clamp((int) (alpha * 255.0f), 0, 255);
        return rgb | (a << 24);
    }

    private static float shapeTopOffset(WaypointShape shape) {
        return switch (shape) {
            case BEACON -> 12.0f;
            case CUBE -> 2.0f;
            case DIAMOND -> 2.0f;
            case STAR -> 1.5f;
            case RING -> 0.35f;
        };
    }

    private static void drawShape(
            DrawableGizmoPrimitives gizmos, WaypointShape shape, float x, float y, float z, int color) {
        switch (shape) {
            case BEACON -> {
                float t = 0.14f;
                solidBox(gizmos, x - t, y, z - t, x + t, y + 12.0f, z + t, color);
                float cap = 0.55f;
                float capH = 0.35f;
                solidBox(gizmos, x - cap, y + 12.0f - capH, z - t, x + cap, y + 12.0f + capH, z + t, color);
                solidBox(gizmos, x - t, y + 12.0f - capH, z - cap, x + t, y + 12.0f + capH, z + cap, color);
            }
            case CUBE -> solidBox(gizmos, x - 0.5f, y, z - 0.5f, x + 0.5f, y + 2.0f, z + 0.5f, color);
            case DIAMOND -> drawSolidDiamond(gizmos, x, y, z, color);
            case STAR -> drawSolidStar(gizmos, x, y + 1.5f, z, color);
            case RING -> drawSolidRing(gizmos, x, y + 0.05f, z, color);
        }
    }

    private static void drawSolidDiamond(DrawableGizmoPrimitives gizmos, float x, float y, float z, int color) {
        float topY = y + 2.0f;
        float midY = y + 1.0f;
        float east = x + 0.75f;
        float west = x - 0.75f;
        float north = z - 0.75f;
        float south = z + 0.75f;
        solidTriangle(gizmos, x, topY, z, east, midY, z, south, midY, z, color);
        solidTriangle(gizmos, x, topY, z, south, midY, z, west, midY, z, color);
        solidTriangle(gizmos, x, topY, z, west, midY, z, north, midY, z, color);
        solidTriangle(gizmos, x, topY, z, north, midY, z, east, midY, z, color);
        solidTriangle(gizmos, x, y, z, south, midY, z, east, midY, z, color);
        solidTriangle(gizmos, x, y, z, west, midY, z, south, midY, z, color);
        solidTriangle(gizmos, x, y, z, north, midY, z, west, midY, z, color);
        solidTriangle(gizmos, x, y, z, east, midY, z, north, midY, z, color);
    }

    private static void drawSolidStar(DrawableGizmoPrimitives gizmos, float x, float y, float z, int color) {
        for (int i = 0; i < 5; ++i) {
            double ang = Math.toRadians(i * 72 - 90);
            double ang2 = Math.toRadians(i * 72 - 90 + 36);
            float x1 = x + (float) (Math.cos(ang) * 0.9);
            float z1 = z + (float) (Math.sin(ang) * 0.9);
            float x2 = x + (float) (Math.cos(ang2) * 0.35);
            float z2 = z + (float) (Math.sin(ang2) * 0.35);
            solidTriangle(gizmos, x, y, z, x1, y, z1, x2, y, z2, color);
        }
    }

    private static void drawSolidRing(DrawableGizmoPrimitives gizmos, float x, float y, float z, int color) {
        int segments = 20;
        float outer = 1.0f;
        float inner = 0.55f;
        float thickness = 0.28f;
        float y0 = y;
        float y1 = y + thickness;
        for (int i = 0; i < segments; ++i) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            float ox0 = x + (float) Math.cos(a0) * outer;
            float oz0 = z + (float) Math.sin(a0) * outer;
            float ox1 = x + (float) Math.cos(a1) * outer;
            float oz1 = z + (float) Math.sin(a1) * outer;
            float ix0 = x + (float) Math.cos(a0) * inner;
            float iz0 = z + (float) Math.sin(a0) * inner;
            float ix1 = x + (float) Math.cos(a1) * inner;
            float iz1 = z + (float) Math.sin(a1) * inner;
            solidQuad(gizmos, ox0, y0, oz0, ox1, y0, oz1, ix1, y0, iz1, ix0, y0, iz0, color);
            solidQuad(gizmos, ox0, y1, oz0, ix0, y1, iz0, ix1, y1, iz1, ox1, y1, oz1, color);
            solidQuad(gizmos, ox0, y0, oz0, ox0, y1, oz0, ox1, y1, oz1, ox1, y0, oz1, color);
            solidQuad(gizmos, ix0, y0, iz0, ix1, y1, iz1, ix1, y0, iz1, ix0, y1, iz0, color);
        }
    }

    private static void solidBox(
            DrawableGizmoPrimitives gizmos,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int color) {
        solidQuad(gizmos, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, color);
        solidQuad(gizmos, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, color);
        solidQuad(gizmos, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, color);
        solidQuad(gizmos, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, color);
        solidQuad(gizmos, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, color);
        solidQuad(gizmos, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, color);
    }

    private static void solidQuad(
            DrawableGizmoPrimitives gizmos,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            int color) {
        gizmos.addQuad(
                new Vec3(x0, y0, z0),
                new Vec3(x1, y1, z1),
                new Vec3(x2, y2, z2),
                new Vec3(x3, y3, z3),
                color);
    }

    private static void solidTriangle(
            DrawableGizmoPrimitives gizmos,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            int color) {
        gizmos.addTriangleFan(new Vec3[] {
            new Vec3(x0, y0, z0), new Vec3(x1, y1, z1), new Vec3(x2, y2, z2)
        }, color);
    }
}
