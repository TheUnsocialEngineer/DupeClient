package com.dupeclient.server;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DupeServerCapeState {
    private static final Set<UUID> DUPE_CLIENT_PLAYERS = ConcurrentHashMap.newKeySet();

    private DupeServerCapeState() {
    }

    public static void mark(UUID playerId) {
        if (playerId != null) {
            DUPE_CLIENT_PLAYERS.add(playerId);
        }
    }

    public static void unmark(UUID playerId) {
        if (playerId != null) {
            DUPE_CLIENT_PLAYERS.remove(playerId);
        }
    }

    public static UUID[] snapshot() {
        return DUPE_CLIENT_PLAYERS.toArray(UUID[]::new);
    }

    public static void clearAll() {
        DUPE_CLIENT_PLAYERS.clear();
    }
}
