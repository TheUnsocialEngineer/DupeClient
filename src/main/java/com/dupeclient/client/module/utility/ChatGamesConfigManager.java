package com.dupeclient.client.module.utility;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChatGamesConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_CHAT_GAMES);

    private ChatGamesConfigManager() {
    }

    public static ChatGamesSettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new ChatGamesSettings();
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            ChatGamesSettings loaded = GSON.fromJson(raw, ChatGamesSettings.class);
            if (loaded == null) {
                return new ChatGamesSettings();
            }
            var root = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            if (!root.has("disableOnLeave")) {
                loaded.disableOnLeave = true;
            }
            return loaded;
        } catch (IOException | RuntimeException ignored) {
            return new ChatGamesSettings();
        }
    }

    public static void save(ChatGamesSettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
