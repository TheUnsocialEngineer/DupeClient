package com.dupeclient.client.docs;

import com.dupeclient.client.DupeClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;

public final class ScreenshotWorldBootstrap {
    private static final String WORLD_DIR = "dupeclient_docs";

    private static boolean attempted;

    private ScreenshotWorldBootstrap() {
    }

    public static void ensureWorld(net.minecraft.client.MinecraftClient client) {
        if (!ScreenshotCaptureMode.isActive() || client == null || attempted) {
            return;
        }
        if (client.player != null && client.world != null) {
            return;
        }
        attempted = true;
        IntegratedServerLoader loader = client.createIntegratedServerLoader();
        try {
            LevelStorage.LevelList worlds = client.getLevelStorage().getLevelList();
            for (LevelStorage.LevelSave save : worlds) {
                if (WORLD_DIR.equalsIgnoreCase(save.path().getFileName().toString())) {
                    DupeClient.LOGGER.info("Loading screenshot world {}", WORLD_DIR);
                    loader.start(WORLD_DIR, () -> DupeClient.LOGGER.warn("Screenshot world load cancelled"));
                    return;
                }
            }
        } catch (Exception e) {
            DupeClient.LOGGER.warn("Could not list worlds for screenshot capture: {}", e.toString());
        }
        createFlatWorld(loader);
    }

    private static void createFlatWorld(IntegratedServerLoader loader) {
        LevelInfo info = new LevelInfo(
                "DupeClient Docs",
                GameMode.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                new GameRules(FeatureSet.empty()),
                DataConfiguration.SAFE_MODE
        );
        DupeClient.LOGGER.info("Creating flat screenshot world {}", WORLD_DIR);
        loader.createAndStart(
                WORLD_DIR,
                info,
                GeneratorOptions.createTestWorld(),
                WorldPresets::createTestOptions,
                (Screen) null
        );
    }
}
