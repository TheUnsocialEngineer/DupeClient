package com.dupeclient.client.module.fuzzer.economy;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EconomyFuzzerConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_ECONOMY_FUZZER);

    private EconomyFuzzerConfigManager() {
    }

    public static EconomyFuzzerSettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new EconomyFuzzerSettings();
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            EconomyFuzzerSettings loaded = GSON.fromJson(raw, EconomyFuzzerSettings.class);
            return loaded != null ? loaded : new EconomyFuzzerSettings();
        } catch (IOException | RuntimeException ignored) {
            return new EconomyFuzzerSettings();
        }
    }

    public static void save(EconomyFuzzerSettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
