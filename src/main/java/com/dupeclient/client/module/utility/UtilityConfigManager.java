package com.dupeclient.client.module.utility;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UtilityConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_UTILITY);

    private UtilityConfigManager() {
    }

    public static UtilitySettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new UtilitySettings();
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            UtilitySettings loaded = GSON.fromJson(raw, UtilitySettings.class);
            if (loaded == null) {
                return new UtilitySettings();
            }
            if (loaded.selectedSubTab == null) {
                loaded.selectedSubTab = UtilitySubTab.CHAT_GAMES;
            }
            return loaded;
        } catch (IOException | RuntimeException ignored) {
            return new UtilitySettings();
        }
    }

    public static void save(UtilitySettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
