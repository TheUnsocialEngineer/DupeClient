package com.dupeclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VisualSettingsConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_VISUAL);

    private VisualSettingsConfigManager() {
    }

    public static VisualSettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new VisualSettings();
        }

        try {
            String raw = Files.readString(CONFIG_PATH);
            VisualSettings settings = GSON.fromJson(raw, VisualSettings.class);
            return settings != null ? settings : new VisualSettings();
        } catch (IOException ignored) {
            return new VisualSettings();
        }
    }

    public static void save(VisualSettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
