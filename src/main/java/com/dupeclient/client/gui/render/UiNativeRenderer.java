package com.dupeclient.client.gui.render;

import com.dupeclient.client.DupeClient;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * GPU-backed UI shapes. Textures use cleared pixels + premultiplied alpha to avoid colored corner halos.
 */
public final class UiNativeRenderer {
    private static final int MAX_CACHE_ENTRIES = 200;
    private static final int AA_SAMPLES = 2;
    /** Below this alpha, use CPU fills (texture filtering breaks on translucent UI). */
    private static final int TEXTURE_ALPHA_MIN = 0xF0;
    private static final Identifier BASE_ID = Identifier.fromNamespaceAndPath(DupeClient.MOD_ID, "ui_native");

    private enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private static final class CachedShape {
        final Identifier id;
        final DynamicTexture texture;
        final int size;

        CachedShape(Identifier id, DynamicTexture texture, int size) {
            this.id = id;
            this.texture = texture;
            this.size = size;
        }
    }

    private static final Map<Long, CachedShape> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CachedShape> eldest) {
            if (size() <= MAX_CACHE_ENTRIES) {
                return false;
            }
            evict(eldest.getValue());
            return true;
        }
    };

    private static int cacheSeq;
    private static boolean ready;

    private UiNativeRenderer() {
    }

    public static void ensureReady() {
        if (ready && Minecraft.getInstance() != null) {
            return;
        }
        ready = Minecraft.getInstance() != null;
    }

    public static void clearCache() {
        Minecraft client = Minecraft.getInstance();
        for (CachedShape shape : CACHE.values()) {
            evict(client, shape);
        }
        CACHE.clear();
        cacheSeq = 0;
        ready = false;
    }

    private static void evict(CachedShape shape) {
        evict(Minecraft.getInstance(), shape);
    }

    private static void evict(Minecraft client, CachedShape shape) {
        if (shape == null) {
            return;
        }
        if (client != null) {
            client.getTextureManager().release(shape.id);
        }
        shape.texture.close();
    }

    public static void fillRoundedRect(GuiGraphicsExtractor context, int x, int y, int w, int h, int rad, int argb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int rr = Math.min(rad, Math.min(w / 2, h / 2));
        if (rr <= 0) {
            context.fill(x, y, x + w, y + h, argb);
            return;
        }
        if (((argb >>> 24) & 0xFF) < TEXTURE_ALPHA_MIN) {
            fillRoundedRectSoftware(context, x, y, w, h, rr, argb);
            return;
        }
        if (!ensureDraw(context, x, y, w, h, rr, argb)) {
            fillRoundedRectSoftware(context, x, y, w, h, rr, argb);
        }
    }

    public static void fillDisk(GuiGraphicsExtractor context, int cx, int cy, int r, int argb) {
        if (r <= 0) {
            return;
        }
        if (((argb >>> 24) & 0xFF) < TEXTURE_ALPHA_MIN) {
            context.fill(cx - r, cy - r, cx + r + 1, cy + r + 1, argb);
            return;
        }
        CachedShape shape = getDisk(r, argb);
        if (shape == null) {
            context.fill(cx - r, cy - r, cx + r + 1, cy + r + 1, argb);
            return;
        }
        int d = shape.size;
        blitShape(context, shape, cx - r, cy - r, d, d);
    }

    private static boolean ensureDraw(GuiGraphicsExtractor context, int x, int y, int w, int h, int rr, int argb) {
        ensureReady();
        if (!ready) {
            return false;
        }
        context.fill(x + rr, y, x + w - rr, y + h, argb);
        context.fill(x, y + rr, x + rr, y + h - rr, argb);
        context.fill(x + w - rr, y + rr, x + w, y + h - rr, argb);

        drawCorner(context, x, y, rr, argb, Corner.TOP_LEFT);
        drawCorner(context, x + w - rr, y, rr, argb, Corner.TOP_RIGHT);
        drawCorner(context, x, y + h - rr, rr, argb, Corner.BOTTOM_LEFT);
        drawCorner(context, x + w - rr, y + h - rr, rr, argb, Corner.BOTTOM_RIGHT);
        return true;
    }

    private static void drawCorner(GuiGraphicsExtractor context, int x, int y, int r, int argb, Corner corner) {
        CachedShape shape = getCorner(r, argb, corner);
        if (shape == null) {
            return;
        }
        blitShape(context, shape, x, y, r, r);
    }

    private static void blitShape(GuiGraphicsExtractor context, CachedShape shape, int x, int y, int w, int h) {
        int s = shape.size;
        context.blit(RenderPipelines.GUI_TEXTURED, shape.id, x, y, 0.0f, 0.0f, w, h, s, s);
    }

    /** Software path — no texture filtering (safe for translucent sidebar pills). */
    private static void fillRoundedRectSoftware(GuiGraphicsExtractor c, int x, int y, int w, int h, int rr, int argb) {
        c.fill(x + rr, y, x + w - rr, y + h, argb);
        c.fill(x, y + rr, x + rr, y + h - rr, argb);
        c.fill(x + w - rr, y + rr, x + w, y + h - rr, argb);
        fillQuarterSoftware(c, x, y, rr, argb, Corner.TOP_LEFT);
        fillQuarterSoftware(c, x + w - rr, y, rr, argb, Corner.TOP_RIGHT);
        fillQuarterSoftware(c, x, y + h - rr, rr, argb, Corner.BOTTOM_LEFT);
        fillQuarterSoftware(c, x + w - rr, y + h - rr, rr, argb, Corner.BOTTOM_RIGHT);
    }

    private static void fillQuarterSoftware(GuiGraphicsExtractor c, int left, int top, int rr, int argb, Corner corner) {
        int r2 = rr * rr;
        double cx;
        double cy;
        switch (corner) {
            case TOP_RIGHT -> {
                cx = left + 0.5;
                cy = top + rr - 0.5;
            }
            case BOTTOM_LEFT -> {
                cx = left + rr - 0.5;
                cy = top + 0.5;
            }
            case BOTTOM_RIGHT -> {
                cx = left + 0.5;
                cy = top + 0.5;
            }
            default -> {
                cx = left + rr - 0.5;
                cy = top + rr - 0.5;
            }
        }
        int xEnd = left + rr;
        int yEnd = top + rr;
        for (int py = top; py < yEnd; py++) {
            int dy = py - (int) cy;
            int inner = r2 - dy * dy;
            if (inner < 0) {
                continue;
            }
            int hx = (int) Math.floor(Math.sqrt(inner));
            int x0;
            int x1;
            switch (corner) {
                case TOP_RIGHT, BOTTOM_RIGHT -> {
                    x0 = left;
                    x1 = Math.min(xEnd, (int) cx + hx + 1);
                }
                default -> {
                    x0 = Math.max(left, (int) cx - hx);
                    x1 = xEnd;
                }
            }
            if (x0 < x1) {
                c.fill(x0, py, x1, py + 1, argb);
            }
        }
    }

    private static CachedShape getCorner(int r, int argb, Corner corner) {
        long key = ((long) r << 36) | ((long) corner.ordinal() << 32) | (argb & 0xFFFFFFFFL);
        return getOrBake(key, () -> bakeCorner(r, argb, corner));
    }

    private static CachedShape getDisk(int r, int argb) {
        long key = (1L << 63) | ((long) r << 32) | (argb & 0xFFFFFFFFL);
        return getOrBake(key, () -> bakeDisk(r, argb));
    }

    private static CachedShape getOrBake(long key, Supplier<NativeImage> baker) {
        CachedShape hit = CACHE.get(key);
        if (hit != null) {
            return hit;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        NativeImage image;
        try {
            image = baker.get();
        } catch (Exception e) {
            DupeClient.LOGGER.warn("UI shape bake failed", e);
            return null;
        }
        if (image == null) {
            return null;
        }
        int size = image.getWidth();
        String label = "dupeclient-ui-" + (cacheSeq++);
        DynamicTexture texture = new DynamicTexture(() -> label, image);
        Identifier id = BASE_ID.withSuffix(label);
        client.getTextureManager().register(id, texture);
        CachedShape cached = new CachedShape(id, texture, size);
        CACHE.put(key, cached);
        return cached;
    }

    private static void clearImage(NativeImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                img.setPixel(px, py, 0);
            }
        }
    }

    private static NativeImage bakeCorner(int r, int argb, Corner corner) {
        NativeImage img = new NativeImage(r, r, false);
        clearImage(img);
        float step = 1.0f / AA_SAMPLES;
        for (int py = 0; py < r; py++) {
            for (int px = 0; px < r; px++) {
                float cov = 0f;
                int n = 0;
                for (int sy = 0; sy < AA_SAMPLES; sy++) {
                    for (int sx = 0; sx < AA_SAMPLES; sx++) {
                        cov += cornerCoverage(px + (sx + 0.5f) * step, py + (sy + 0.5f) * step, r, corner);
                        n++;
                    }
                }
                cov /= n;
                if (cov > 0f) {
                    img.setPixel(px, py, premultipliedArgb(argb, cov));
                }
            }
        }
        return img;
    }

    private static float cornerCoverage(float px, float py, int r, Corner corner) {
        if (px < 0 || py < 0 || px >= r || py >= r) {
            return 0f;
        }
        double cx;
        double cy;
        switch (corner) {
            case TOP_RIGHT -> {
                cx = 0.5;
                cy = r - 0.5;
            }
            case BOTTOM_LEFT -> {
                cx = r - 0.5;
                cy = 0.5;
            }
            case BOTTOM_RIGHT -> {
                cx = 0.5;
                cy = 0.5;
            }
            default -> {
                cx = r - 0.5;
                cy = r - 0.5;
            }
        }
        double dx = px - cx;
        double dy = py - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        return Mth.clamp((float) (r - dist + 0.65f), 0f, 1f);
    }

    private static NativeImage bakeDisk(int r, int argb) {
        int d = r * 2 + 1;
        NativeImage img = new NativeImage(d, d, false);
        clearImage(img);
        float center = (d - 1) * 0.5f;
        float radius = r + 0.01f;
        float step = 1.0f / AA_SAMPLES;
        for (int py = 0; py < d; py++) {
            for (int px = 0; px < d; px++) {
                float cov = 0f;
                int n = 0;
                for (int sy = 0; sy < AA_SAMPLES; sy++) {
                    for (int sx = 0; sx < AA_SAMPLES; sx++) {
                        float sxp = px + (sx + 0.5f) * step;
                        float syp = py + (sy + 0.5f) * step;
                        float dx = sxp - center;
                        float dy = syp - center;
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);
                        cov += Mth.clamp(radius - dist + 0.65f, 0f, 1f);
                        n++;
                    }
                }
                cov /= n;
                if (cov > 0f) {
                    img.setPixel(px, py, premultipliedArgb(argb, cov));
                }
            }
        }
        return img;
    }

    /** Premultiplied ARGB — prevents RGB fringe when the GPU linear-filters edges. */
    private static int premultipliedArgb(int argb, float coverage) {
        int baseA = (argb >>> 24) & 0xFF;
        int a = Math.min(255, Math.max(0, (int) (baseA * coverage)));
        if (a == 0) {
            return 0;
        }
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        r = r * a / 255;
        g = g * a / 255;
        b = b * a / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void trimCache(int keep) {
        int target = Math.max(16, keep);
        Iterator<Map.Entry<Long, CachedShape>> it = CACHE.entrySet().iterator();
        while (CACHE.size() > target && it.hasNext()) {
            Map.Entry<Long, CachedShape> e = it.next();
            evict(e.getValue());
            it.remove();
        }
    }
}
