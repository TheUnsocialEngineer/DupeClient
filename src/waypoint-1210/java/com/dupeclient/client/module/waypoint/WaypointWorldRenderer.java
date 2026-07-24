package com.dupeclient.client.module.waypoint;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.cape.DupeClientPresenceConfigManager;
import com.dupeclient.client.module.waypoint.DupeClientWaypoint;
import com.dupeclient.client.module.waypoint.DupeClientWaypointManager;
import com.dupeclient.client.module.waypoint.SharedDupeClientWaypoint;
import com.dupeclient.client.module.waypoint.WaypointShape;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

public final class WaypointWorldRenderer {
    private static final float LABEL_SCALE = 0.022f;
    private static final float HOVER_BLOCKS = 5.0f;
    private static final float LABEL_GAP = 1.5f;
    private static final float SOLID_ALPHA = 0.92f;

    private WaypointWorldRenderer() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WaypointWorldRenderer::renderWorld);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void renderWorld(WorldRenderContext context) {
        if (!Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().showSharedWaypointsInWorld)) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {
            return;
        }
        GameRenderer gameRenderer = context.gameRenderer();
        Camera camera = gameRenderer.getCamera();
        float tickDelta = camera.getLastTickProgress();
        Vec3d cam = client.player.getCameraPosVec(tickDelta);
        MatrixStack matrices = context.matrices();
        matrices.push();
        try {
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            VertexConsumer solids = consumers.getBuffer(RenderLayer.getDebugFilledBox());
            VertexConsumer quads = consumers.getBuffer(RenderLayer.getDebugQuads());
            VertexConsumer triangles = consumers.getBuffer(RenderLayer.getDebugTriangleFan());
            double px = MathHelper.lerp((double)tickDelta, (double)client.player.lastRenderX, (double)client.player.getX());
            double py = MathHelper.lerp((double)tickDelta, (double)client.player.lastRenderY, (double)client.player.getY());
            double pz = MathHelper.lerp((double)tickDelta, (double)client.player.lastRenderZ, (double)client.player.getZ());
            for (SharedDupeClientWaypoint row : DupeClientWaypointManager.INSTANCE.visibleWaypoints(client)) {
                DupeClientWaypoint wp = row.waypoint();
                float r = (float)(wp.colorArgb() >> 16 & 0xFF) / 255.0f;
                float g = (float)(wp.colorArgb() >> 8 & 0xFF) / 255.0f;
                float b = (float)(wp.colorArgb() & 0xFF) / 255.0f;
                float wx = (float)wp.x() + 0.5f;
                float groundY = wp.y();
                float hoverY = groundY + 5.0f;
                float wz = (float)wp.z() + 0.5f;
                float x = wx - (float)cam.x;
                float y = hoverY - (float)cam.y;
                float z = wz - (float)cam.z;
                WaypointWorldRenderer.drawShape(solids, quads, triangles, matrix, wp.shape(), x, y, z, r, g, b, 0.92f);
                double dx = (double)wx - px;
                double dy = (double)groundY + 0.5 - py;
                double dz = (double)wz - pz;
                int dist = Math.round((float)Math.sqrt(dx * dx + dy * dy + dz * dz));
                String label = wp.name() + "  " + dist + "m";
                float labelY = hoverY + WaypointWorldRenderer.shapeTopOffset(wp.shape()) + 1.5f;
                WaypointWorldRenderer.drawWorldLabel(matrices, client.textRenderer, consumers, camera, cam, label, wx, labelY, wz, wp.colorArgb() | 0xFF000000);
            }
        }
        catch (Exception ex) {
            DupeClient.LOGGER.debug("Waypoint world render failed", (Throwable)ex);
        }
        finally {
            matrices.pop();
        }
    }

    public static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
    }

    private static void drawWorldLabel(MatrixStack matrices, TextRenderer textRenderer, VertexConsumerProvider consumers, Camera camera, Vec3d cam, String text, float wx, float wy, float wz, int color) {
        matrices.push();
        matrices.translate((double)wx - cam.x, (double)wy - cam.y, (double)wz - cam.z);
        matrices.multiply((Quaternionfc)RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply((Quaternionfc)RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.scale(-0.022f, -0.022f, 0.022f);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        MutableText literal = Text.literal(text);
        float tx = (float)(-textRenderer.getWidth((StringVisitable)literal)) / 2.0f;
        textRenderer.draw((Text)literal, tx, 0.0f, color, true, matrix, consumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        matrices.pop();
    }

    private static float shapeTopOffset(WaypointShape shape) {
        return switch (shape) {
            default -> throw new MatchException(null, null);
            case WaypointShape.BEACON -> 12.0f;
            case WaypointShape.CUBE -> 2.0f;
            case WaypointShape.DIAMOND -> 2.0f;
            case WaypointShape.STAR -> 1.5f;
            case WaypointShape.RING -> 0.35f;
        };
    }

    private static void drawShape(VertexConsumer solids, VertexConsumer quads, VertexConsumer triangles, Matrix4f matrix, WaypointShape shape, float x, float y, float z, float r, float g, float b, float a) {
        switch (shape) {
            case BEACON: {
                float t = 0.14f;
                WaypointWorldRenderer.solidBox(solids, matrix, x - t, y, z - t, x + t, y + 12.0f, z + t, r, g, b, a);
                float cap = 0.55f;
                float capH = 0.35f;
                WaypointWorldRenderer.solidBox(solids, matrix, x - cap, y + 12.0f - capH, z - t, x + cap, y + 12.0f + capH, z + t, r, g, b, a);
                WaypointWorldRenderer.solidBox(solids, matrix, x - t, y + 12.0f - capH, z - cap, x + t, y + 12.0f + capH, z + cap, r, g, b, a);
                break;
            }
            case CUBE: {
                WaypointWorldRenderer.solidBox(solids, matrix, x - 0.5f, y, z - 0.5f, x + 0.5f, y + 2.0f, z + 0.5f, r, g, b, a);
                break;
            }
            case DIAMOND: {
                WaypointWorldRenderer.drawSolidDiamond(triangles, matrix, x, y, z, r, g, b, a);
                break;
            }
            case STAR: {
                WaypointWorldRenderer.drawSolidStar(triangles, matrix, x, y + 1.5f, z, r, g, b, a);
                break;
            }
            case RING: {
                WaypointWorldRenderer.drawSolidRing(quads, matrix, x, y + 0.05f, z, r, g, b, a);
            }
        }
    }

    private static void drawSolidDiamond(VertexConsumer triangles, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a) {
        float topY = y + 2.0f;
        float midY = y + 1.0f;
        float east = x + 0.75f;
        float west = x - 0.75f;
        float north = z - 0.75f;
        float south = z + 0.75f;
        WaypointWorldRenderer.solidTriangle(triangles, matrix, x, topY, z, east, midY, z, south, midY, z, r, g, b, a);
        WaypointWorldRenderer.solidTriangle(triangles, matrix, x, topY, z, south, midY, z, west, midY, z, r, g, b, a);
        WaypointWorldRenderer.solidTriangle(triangles, matrix, x, topY, z, west, midY, z, north, midY, z, r, g, b, a);
        WaypointWorldRenderer.solidTriangle(triangles, matrix, x, topY, z, north, midY, z, east, midY, z, r, g, b, a);
        WaypointWorldRenderer.solidTriangle(triangles, matrix, x, y, z, south, midY, z, east, midY, z, r, g, b, a);
        WaypointWorldRenderer.solidTriangle(triangles, matrix, x, y, z, west, midY, z, south, midY, z, r, g, b, a);
        WaypointWorldRenderer.solidTriangle(triangles, matrix, x, y, z, north, midY, z, west, midY, z, r, g, b, a);
        WaypointWorldRenderer.solidTriangle(triangles, matrix, x, y, z, east, midY, z, north, midY, z, r, g, b, a);
    }

    private static void drawSolidStar(VertexConsumer triangles, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a) {
        for (int i = 0; i < 5; ++i) {
            double ang = Math.toRadians(i * 72 - 90);
            double ang2 = Math.toRadians(i * 72 - 90 + 36);
            float x1 = x + (float)(Math.cos(ang) * 0.9);
            float z1 = z + (float)(Math.sin(ang) * 0.9);
            float x2 = x + (float)(Math.cos(ang2) * 0.35);
            float z2 = z + (float)(Math.sin(ang2) * 0.35);
            WaypointWorldRenderer.solidTriangle(triangles, matrix, x, y, z, x1, y, z1, x2, y, z2, r, g, b, a);
        }
    }

    private static void drawSolidRing(VertexConsumer quads, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a) {
        int segments = 20;
        float outer = 1.0f;
        float inner = 0.55f;
        float thickness = 0.28f;
        float y0 = y;
        float y1 = y + thickness;
        for (int i = 0; i < segments; ++i) {
            double a0 = Math.PI * 2 * (double)i / (double)segments;
            double a1 = Math.PI * 2 * (double)(i + 1) / (double)segments;
            float ox0 = x + (float)Math.cos(a0) * outer;
            float oz0 = z + (float)Math.sin(a0) * outer;
            float ox1 = x + (float)Math.cos(a1) * outer;
            float oz1 = z + (float)Math.sin(a1) * outer;
            float ix0 = x + (float)Math.cos(a0) * inner;
            float iz0 = z + (float)Math.sin(a0) * inner;
            float ix1 = x + (float)Math.cos(a1) * inner;
            float iz1 = z + (float)Math.sin(a1) * inner;
            WaypointWorldRenderer.solidQuad(quads, matrix, ox0, y0, oz0, ox1, y0, oz1, ix1, y0, iz1, ix0, y0, iz0, r, g, b, a);
            WaypointWorldRenderer.solidQuad(quads, matrix, ox0, y1, oz0, ix0, y1, iz0, ix1, y1, iz1, ox1, y1, oz1, r, g, b, a);
            WaypointWorldRenderer.solidQuad(quads, matrix, ox0, y0, oz0, ox0, y1, oz0, ox1, y1, oz1, ox1, y0, oz1, r, g, b, a);
            WaypointWorldRenderer.solidQuad(quads, matrix, ix0, y0, iz0, ix1, y1, iz1, ix1, y0, iz1, ix0, y1, iz0, r, g, b, a);
        }
    }

    private static void solidBox(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a) {
        WaypointWorldRenderer.solidQuad(consumer, matrix, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
        WaypointWorldRenderer.solidQuad(consumer, matrix, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        WaypointWorldRenderer.solidQuad(consumer, matrix, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
        WaypointWorldRenderer.solidQuad(consumer, matrix, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
        WaypointWorldRenderer.solidQuad(consumer, matrix, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, r, g, b, a);
        WaypointWorldRenderer.solidQuad(consumer, matrix, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, r, g, b, a);
    }

    private static void solidQuad(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float b, float a) {
        consumer.vertex(matrix, x0, y0, z0).color(r, g, b, a);
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        consumer.vertex(matrix, x3, y3, z3).color(r, g, b, a);
    }

    private static void solidTriangle(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        consumer.vertex(matrix, x0, y0, z0).color(r, g, b, a);
        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }
}

