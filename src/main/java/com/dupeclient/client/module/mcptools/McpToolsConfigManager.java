package com.dupeclient.client.module.mcptools;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class McpToolsConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_MCP_TOOLS);

    private McpToolsConfigManager() {
    }

    public static McpToolsSettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new McpToolsSettings();
        }
        try {
            McpToolsSettings loaded = GSON.fromJson(Files.readString(CONFIG_PATH), McpToolsSettings.class);
            if (loaded == null) {
                return new McpToolsSettings();
            }
            migrateSettings(loaded);
            return loaded;
        } catch (IOException | RuntimeException ignored) {
            return new McpToolsSettings();
        }
    }

    public static void save(McpToolsSettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }

    private static void migrateSettings(McpToolsSettings settings) {
        if (settings.lastMcVersion == null || settings.lastMcVersion.isBlank()) {
            McpToolsMcVersion migrated = McpToolsMcVersion.migrateLegacy(settings.lastVersion);
            settings.lastMcVersion = migrated.id;
            settings.lastVersion = migrated.protocol;
        }
        if (settings.botJoinDelaySec <= 0 || settings.botJoinDelaySec == 10) {
            settings.botJoinDelaySec = 2;
        }
    }
}
