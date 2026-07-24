package com.dupeclient.client.module.payall;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PayAllConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_PAY_EVERYONE);

    private PayAllConfigManager() {
    }

    public static PayAllSettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new PayAllSettings();
        }
        try {
            PayAllSettings loaded = GSON.fromJson(Files.readString(CONFIG_PATH), PayAllSettings.class);
            return loaded != null ? loaded : new PayAllSettings();
        } catch (IOException | RuntimeException ignored) {
            return new PayAllSettings();
        }
    }

    public static void save(PayAllSettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
