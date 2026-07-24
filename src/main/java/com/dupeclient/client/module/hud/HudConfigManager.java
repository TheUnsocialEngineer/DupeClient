package com.dupeclient.client.module.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.dupeclient.client.config.DupeClientConfigDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HudConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_HUD);

    private HudConfigManager() {
    }

    public static HudPersistedState load() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            return new HudPersistedState();
        }
        try {
            String raw = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            HudPersistedState v = GSON.fromJson(raw, HudPersistedState.class);
            return v != null ? v : new HudPersistedState();
        } catch (IOException ignored) {
            return new HudPersistedState();
        }
    }

    public static void save(HudPersistedState state) {
        if (state == null) {
            return;
        }
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(state), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
