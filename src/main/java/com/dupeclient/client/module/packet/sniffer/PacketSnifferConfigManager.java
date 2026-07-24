package com.dupeclient.client.module.packet.sniffer;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PacketSnifferConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_PACKET_SNIFFER);

    private PacketSnifferConfigManager() {
    }

    public static PacketSnifferSettings load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new PacketSnifferSettings();
        }
        try {
            PacketSnifferSettings loaded = GSON.fromJson(Files.readString(CONFIG_PATH), PacketSnifferSettings.class);
            return loaded != null ? loaded : new PacketSnifferSettings();
        } catch (IOException | RuntimeException ignored) {
            return new PacketSnifferSettings();
        }
    }

    public static void save(PacketSnifferSettings settings) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(settings));
        } catch (IOException ignored) {
        }
    }
}
