package com.dupeclient.client.module.utility.crashes;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CrashesConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_CRASHES);

    private CrashesConfigManager() {
    }

    public static CrashesSettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new CrashesSettings();
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            var root = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            CrashesSettings loaded = GSON.fromJson(raw, CrashesSettings.class);
            if (loaded == null) {
                return new CrashesSettings();
            }
            if (root.has("moduleChatFeedback") && !root.has("chestChatFeedback")) {
                loaded.chestChatFeedback = loaded.moduleChatFeedback;
            }
            if (root.has("moduleChatFeedback") && !root.has("armorChatFeedback")) {
                loaded.armorChatFeedback = loaded.moduleChatFeedback;
            }
            if (!root.has("armorDisableOnLeave")) {
                loaded.armorDisableOnLeave = true;
            }
            return loaded;
        } catch (IOException | RuntimeException ignored) {
            return new CrashesSettings();
        }
    }

    public static void save(CrashesSettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
