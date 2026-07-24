package com.dupeclient.client.module.social;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.dupeclient.client.config.DupeClientConfigDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Local friend UUIDs: used to highlight rows and to respect {@link PresenceListAudience#FRIENDS_ONLY} list rows.
 */
public final class DupeClientSocialFriendsManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = DupeClientConfigDir.root().resolve(DupeClientConfigDir.FILE_SOCIAL_FRIENDS);

    private static final Set<UUID> friends = new LinkedHashSet<>();

    private DupeClientSocialFriendsManager() {
    }

    public static void initialize() {
        reload();
    }

    public static void reload() {
        friends.clear();
        if (!Files.exists(PATH)) {
            return;
        }
        try {
            String raw = Files.readString(PATH);
            DupeClientSocialFriendsData data = GSON.fromJson(raw, DupeClientSocialFriendsData.class);
            if (data == null || data.friendUuids == null) {
                return;
            }
            for (String s : data.friendUuids) {
                if (s == null || s.isBlank()) {
                    continue;
                }
                try {
                    friends.add(UUID.fromString(s.trim()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static Set<UUID> friendUuidSet() {
        return Collections.unmodifiableSet(friends);
    }

    public static boolean isFriend(UUID uuid) {
        return uuid != null && friends.contains(uuid);
    }

    public static void addFriend(UUID uuid) {
        if (uuid == null) {
            return;
        }
        friends.add(uuid);
        save();
    }

    public static void removeFriend(UUID uuid) {
        if (uuid == null) {
            return;
        }
        friends.remove(uuid);
        save();
    }

    /** Add if absent, remove if present. */
    public static void toggleFriend(UUID uuid) {
        if (uuid == null) {
            return;
        }
        if (friends.contains(uuid)) {
            friends.remove(uuid);
        } else {
            friends.add(uuid);
        }
        save();
    }

    public static void save() {
        DupeClientSocialFriendsData data = new DupeClientSocialFriendsData();
        for (UUID u : friends) {
            data.friendUuids.add(u.toString());
        }
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(data));
        } catch (IOException ignored) {
        }
    }
}
