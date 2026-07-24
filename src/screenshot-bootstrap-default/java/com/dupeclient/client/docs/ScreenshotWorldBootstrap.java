package com.dupeclient.client.docs;

import com.dupeclient.client.DupeClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;

public final class ScreenshotWorldBootstrap {
    private static final String WORLD_DIR = "dupeclient_docs";

    private static boolean attempted;

    private ScreenshotWorldBootstrap() {
    }

    public static void ensureWorld(net.minecraft.client.Minecraft client) {
        if (!ScreenshotCaptureMode.isActive() || client == null || attempted) {
            return;
        }
        if (client.player != null && client.level != null) {
            return;
        }
        attempted = true;
        WorldOpenFlows loader = client.createWorldOpenFlows();
        try {
            LevelStorageSource.LevelCandidates worlds = client.getLevelSource().findLevelCandidates();
            for (LevelStorageSource.LevelDirectory save : worlds) {
                if (WORLD_DIR.equalsIgnoreCase(save.path().getFileName().toString())) {
                    DupeClient.LOGGER.info("Loading screenshot world {}", WORLD_DIR);
                    loader.openWorld(WORLD_DIR, () -> DupeClient.LOGGER.warn("Screenshot world load cancelled"));
                    return;
                }
            }
        } catch (Exception e) {
            DupeClient.LOGGER.warn("Could not list worlds for screenshot capture: {}", e.toString());
        }
        createFlatWorld(loader);
    }

    private static void createFlatWorld(WorldOpenFlows loader) {
        LevelSettings info = new LevelSettings(
                "DupeClient Docs",
                GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true),
                true,
                WorldDataConfiguration.DEFAULT
        );
        DupeClient.LOGGER.info("Creating flat screenshot world {}", WORLD_DIR);
        loader.createFreshLevel(
                WORLD_DIR,
                info,
                WorldOptions.testWorldWithRandomSeed(),
                WorldPresets::createFlatWorldDimensions,
                (Screen) null
        );
    }
}
