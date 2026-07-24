package com.dupeclient.client.module.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.dupeclient.client.config.DupeClientConfigDir;

import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PacketUtilsConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_PACKET_UTILS);

    private PacketUtilsConfigManager() {
    }

    public static PacketUtilsSettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new PacketUtilsSettings();
        }
        try {
            String raw = Files.readString(CONFIG_PATH);
            PacketUtilsSettings loaded = GSON.fromJson(raw, PacketUtilsSettings.class);
            if (loaded == null) {
                return new PacketUtilsSettings();
            }
            if (loaded.packetDelayC2sClassNames == null) {
                loaded.packetDelayC2sClassNames = new ArrayList<>();
            }
            if (loaded.packetDelayS2cClassNames == null) {
                loaded.packetDelayS2cClassNames = new ArrayList<>();
            }
            var root = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            if (!root.has("disableActiveOnLeave")) {
                loaded.disableActiveOnLeave = true;
            }
            if (loaded.configVersion < 2) {
                loaded.uiUtilsOverlayEnabled = true;
                loaded.configVersion = 2;
            }
            if (loaded.configVersion < 3) {
                loaded.uiUtilsOverlayEnabled = true;
                loaded.configVersion = 3;
            }
            if (loaded.configVersion < 4) {
                loaded.configVersion = 4;
            }
            if (loaded.configVersion < 5) {
                loaded.uiUtilsOverlayEnabled = true;
                loaded.configVersion = 5;
                save(loaded);
            }
            return loaded;
        } catch (IOException ignored) {
            return new PacketUtilsSettings();
        }
    }

    public static void save(PacketUtilsSettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
