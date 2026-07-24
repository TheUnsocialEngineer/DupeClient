package com.dupeclient.client.module.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.dupeclient.client.config.DupeClientConfigDir;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SecurityStaffStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_SECURITY_STAFF);
    private static final Type LIST_TYPE = new TypeToken<List<StaffWatchEntry>>() {}.getType();

    private SecurityStaffStore() {
    }

    public static Map<String, StaffWatchEntry> load() {
        Map<String, StaffWatchEntry> out = new LinkedHashMap<>();
        if (!Files.exists(PATH)) {
            return out;
        }
        try {
            String raw = Files.readString(PATH);
            List<StaffWatchEntry> entries = GSON.fromJson(raw, LIST_TYPE);
            if (entries == null) {
                return out;
            }
            for (StaffWatchEntry entry : entries) {
                if (entry == null || entry.uuid == null || entry.uuid.isBlank()) {
                    continue;
                }
                out.put(entry.uuid, entry);
            }
        } catch (IOException ignored) {
        }
        return out;
    }

    public static void save(Map<String, StaffWatchEntry> entries) {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(new ArrayList<>(entries.values())));
        } catch (IOException ignored) {
        }
    }
}
