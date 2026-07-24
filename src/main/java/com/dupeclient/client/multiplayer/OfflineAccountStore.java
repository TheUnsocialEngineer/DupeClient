package com.dupeclient.client.multiplayer;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OfflineAccountStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_OFFLINE_ACCOUNTS);
    private static final Type LIST_TYPE = new TypeToken<List<StoredAccount>>() {}.getType();

    private OfflineAccountStore() {
    }

    public static List<OfflineAccount> load() {
        List<OfflineAccount> out = new ArrayList<>();
        if (!Files.exists(PATH)) {
            return out;
        }
        try {
            String raw = Files.readString(PATH);
            List<StoredAccount> stored = GSON.fromJson(raw, LIST_TYPE);
            if (stored == null) {
                return out;
            }
            for (StoredAccount entry : stored) {
                if (entry == null || entry.username == null || entry.username.isBlank()) continue;
                UUID uuid = entry.uuid == null || entry.uuid.isBlank()
                    ? OfflineAccount.ofUsername(entry.username).uuid()
                    : UUID.fromString(entry.uuid);
                out.add(new OfflineAccount(entry.username.trim(), uuid));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void save(List<OfflineAccount> accounts) {
        try {
            Files.createDirectories(PATH.getParent());
            List<StoredAccount> stored = new ArrayList<>();
            for (OfflineAccount account : accounts) {
                StoredAccount row = new StoredAccount();
                row.username = account.username();
                row.uuid = account.uuid().toString();
                stored.add(row);
            }
            Files.writeString(PATH, GSON.toJson(stored));
        } catch (IOException ignored) {
        }
    }

    private static final class StoredAccount {
        String username;
        String uuid;
    }
}
