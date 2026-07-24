package com.dupeclient.client.module.cape;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side set of player UUIDs known to be using DupeClient (from {@link DupeClientCapePresence} HTTP presence
 * plus the local player marked on join). Entries expire so offline peers do not keep cape overrides forever.
 */
public final class DupeClientCapeTracker {
    private static final long TTL_MS = 120_000L;
    private static final ConcurrentHashMap<UUID, Long> IDS = new ConcurrentHashMap<>();

    private DupeClientCapeTracker() {
    }

    public static void markDupeClient(UUID playerId) {
        if (playerId != null) {
            IDS.put(playerId, System.currentTimeMillis());
        }
    }

    /** Replace tracked peers with the latest presence query result (keeps local player if already marked). */
    public static void replaceOnline(Collection<UUID> online, UUID keepLocal) {
        long now = System.currentTimeMillis();
        IDS.clear();
        if (online != null) {
            for (UUID id : online) {
                if (id != null) {
                    IDS.put(id, now);
                }
            }
        }
        if (keepLocal != null) {
            IDS.put(keepLocal, now);
        }
    }

    public static boolean isDupeClientUser(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        Long seen = IDS.get(playerId);
        if (seen == null) {
            return false;
        }
        if (System.currentTimeMillis() - seen > TTL_MS) {
            IDS.remove(playerId, seen);
            return false;
        }
        return true;
    }

    public static void clear() {
        IDS.clear();
    }

    public static Set<UUID> snapshot() {
        return Set.copyOf(IDS.keySet());
    }
}
