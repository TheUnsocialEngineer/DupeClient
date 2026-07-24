package com.dupeclient.client.module.dupedb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.dupeclient.client.config.DupeClientConfigDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DupedbConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_DUPEDB);

    private DupedbConfigManager() {
    }

    public static DupedbSettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new DupedbSettings();
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            DupedbSettings loaded = GSON.fromJson(raw, DupedbSettings.class);
            return loaded != null ? loaded : new DupedbSettings();
        } catch (IOException ignored) {
            return new DupedbSettings();
        }
    }

    public static void save(DupedbSettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
