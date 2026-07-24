package com.dupeclient.client.module.packet.fabricator;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FabricatorPresetStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = DupeClientConfigDir.root().resolve("fabricator_presets.json");
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private FabricatorPresetStore() {
    }

    public static Map<String, String> loadAll() {
        if (!Files.exists(FILE)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> map = GSON.fromJson(Files.readString(FILE), MAP_TYPE);
            return map != null ? new LinkedHashMap<>(map) : new LinkedHashMap<>();
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    public static void saveAll(Map<String, String> presets) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(presets));
        } catch (IOException ignored) {
        }
    }

    public static void save(String name, String payload) {
        if (name == null || name.isBlank() || payload == null) {
            return;
        }
        Map<String, String> all = loadAll();
        all.put(name.trim(), payload);
        saveAll(all);
    }

    public static String load(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return loadAll().get(name.trim());
    }

    public static List<String> names() {
        return new ArrayList<>(loadAll().keySet());
    }

    public static void delete(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        Map<String, String> all = loadAll();
        all.remove(name.trim());
        saveAll(all);
    }
}
