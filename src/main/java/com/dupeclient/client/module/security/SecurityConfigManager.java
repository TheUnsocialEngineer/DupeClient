package com.dupeclient.client.module.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.dupeclient.client.config.DupeClientConfigDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SecurityConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_SECURITY);

    private SecurityConfigManager() {
    }

    public static SecuritySettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new SecuritySettings();
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            SecuritySettings loaded = GSON.fromJson(raw, SecuritySettings.class);
            if (loaded == null) {
                return new SecuritySettings();
            }
            if (!root.has("keyResolutionServerMarkedOnly")) {
                loaded.keyResolutionServerMarkedOnly = true;
            }
            if (!root.has("keyResolutionBlockSignEditorOnKeyProbe")) {
                loaded.keyResolutionBlockSignEditorOnKeyProbe = false;
            }
            if (!root.has("opsecFakeDefaultKeybinds")) {
                loaded.opsecFakeDefaultKeybinds = true;
            }
            if (!root.has("opsecBrandMode") || loaded.opsecBrandMode == null) {
                loaded.opsecBrandMode = SecuritySettings.OpsecBrandMode.FABRIC;
            }
            if (!root.has("opsecWhitelistMode") || loaded.opsecWhitelistMode == null) {
                loaded.opsecWhitelistMode = SecuritySettings.OpsecWhitelistMode.AUTO;
            }
            if (!root.has("opsecWhitelistedModsCsv") || loaded.opsecWhitelistedModsCsv == null) {
                loaded.opsecWhitelistedModsCsv = "voicechat,xaero,minimap";
            }
            return loaded;
        } catch (IOException | RuntimeException ignored) {
            return new SecuritySettings();
        }
    }

    public static void save(SecuritySettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
