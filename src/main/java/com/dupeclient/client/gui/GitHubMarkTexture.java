package com.dupeclient.client.gui;

import com.dupeclient.client.DupeClient;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

/** Shared baked GitHub mark icon for attribution cards. */
public final class GitHubMarkTexture {
    private static final String RESOURCE_PATH = "/assets/dupeclient/textures/gui/github_mark.png";
    private static final Identifier RESOURCE = Identifier.fromNamespaceAndPath(DupeClient.MOD_ID, "textures/gui/github_mark.png");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(DupeClient.MOD_ID, "gui/github_mark");
    private static final int ICON_SIZE = 16;

    private static volatile boolean registered;
    private static volatile boolean preloadStarted;
    private static volatile NativeImage pendingIcon;

    private GitHubMarkTexture() {
    }

    public static Identifier texture() {
        return TEXTURE;
    }

    public static boolean isReady() {
        return registered;
    }

    public static void preloadAsync() {
        if (registered || preloadStarted) {
            return;
        }
        preloadStarted = true;
        Thread thread = new Thread(GitHubMarkTexture::decodeIconOffThread, "dupeclient-github-mark");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public static void ensureLoaded(Minecraft client) {
        if (registered || client == null) {
            return;
        }
        if (pendingIcon != null) {
            registerPending(client);
            return;
        }
        if (!preloadStarted) {
            preloadAsync();
        }
    }

    public static int iconSize() {
        return ICON_SIZE;
    }

    private static void decodeIconOffThread() {
        try (InputStream in = openResourceStream(null)) {
            if (in == null) {
                DupeClient.LOGGER.warn("GitHub mark texture missing at {}", RESOURCE);
                return;
            }
            NativeImage source = NativeImage.read(in);
            NativeImage icon = bakeIcon(source, ICON_SIZE);
            source.close();
            pendingIcon = icon;
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.execute(() -> {
                    Minecraft live = Minecraft.getInstance();
                    if (live != null) {
                        registerPending(live);
                    }
                });
            }
        } catch (IOException e) {
            DupeClient.LOGGER.warn("Failed to decode GitHub mark texture", e);
        }
    }

    private static void registerPending(Minecraft client) {
        if (registered) {
            return;
        }
        NativeImage icon = pendingIcon;
        if (icon == null) {
            return;
        }
        pendingIcon = null;
        DynamicTexture backed = new DynamicTexture(() -> "dupeclient-github-mark", icon);
        client.getTextureManager().register(TEXTURE, backed);
        backed.upload();
        registered = true;
    }

    private static InputStream openResourceStream(Minecraft client) throws IOException {
        InputStream fromClassLoader = DupeClient.class.getResourceAsStream(RESOURCE_PATH);
        if (fromClassLoader != null) {
            return fromClassLoader;
        }
        if (client == null) {
            return null;
        }
        Resource resource = client.getResourceManager().getResource(RESOURCE).orElse(null);
        if (resource == null) {
            return null;
        }
        return resource.open();
    }

    private static NativeImage bakeIcon(NativeImage source, int size) {
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        NativeImage out = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int sx = x * srcW / size;
                int sy = y * srcH / size;
                out.setPixel(x, y, blendOnWhite(source.getPixel(sx, sy)));
            }
        }
        return out;
    }

    private static int blendOnWhite(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha <= 8) {
            return 0xFFFFFFFF;
        }
        int srcR = (argb >>> 16) & 0xFF;
        int srcG = (argb >>> 8) & 0xFF;
        int srcB = argb & 0xFF;
        float a = alpha / 255.0F;
        int r = Math.round(srcR * a + 255.0F * (1.0F - a));
        int g = Math.round(srcG * a + 255.0F * (1.0F - a));
        int b = Math.round(srcB * a + 255.0F * (1.0F - a));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
