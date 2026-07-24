package com.dupeclient.client.module.acaudit;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AcAuditConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_AC_AUDIT);

    private AcAuditConfigManager() {
    }

    public static AcAuditSettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new AcAuditSettings();
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            AcAuditSettings loaded = GSON.fromJson(raw, AcAuditSettings.class);
            return loaded != null ? loaded : new AcAuditSettings();
        } catch (IOException | RuntimeException ignored) {
            return new AcAuditSettings();
        }
    }

    public static void save(AcAuditSettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
