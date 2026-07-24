package com.dupeclient.client.module.waypoint;

import com.dupeclient.client.module.cape.DupeClientPresenceConfigManager;
import com.dupeclient.client.module.social.DupeClientSocialFriendsManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class DupeClientWaypointManager {
    public static final DupeClientWaypointManager INSTANCE = new DupeClientWaypointManager();

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DupeClient-Waypoints");
        t.setDaemon(true);
        return t;
    });

    private final List<DupeClientWaypoint> localWaypoints = new ArrayList<>();
    private final List<SharedDupeClientWaypoint> sharedWaypoints = new ArrayList<>();
    private WaypointShareAudience defaultShareAudience = WaypointShareAudience.FRIENDS_ONLY;
    private volatile boolean syncDirty = true;
    private volatile long lastFetchMs;
    private volatile long lastSyncMs;
    private volatile long nextFetchAllowedMs;
    private volatile int consecutiveFetchFailures;
    private final AtomicBoolean syncInFlight = new AtomicBoolean();
    private final AtomicBoolean fetchInFlight = new AtomicBoolean();
    private int tickCounter;

    private static final long FETCH_INTERVAL_MS = 30_000L;
    private static final long FETCH_FAILURE_BASE_MS = 10_000L;
    private static final long FETCH_FAILURE_MAX_MS = 120_000L;

    private DupeClientWaypointManager() {
    }

    public void initialize() {
        reloadLocal();
    }

    public void reloadLocal() {
        DupeClientWaypointStore.Data data = DupeClientWaypointStore.load();
        localWaypoints.clear();
        localWaypoints.addAll(data.waypoints());
        defaultShareAudience = data.defaultShareAudience();
        syncDirty = true;
    }

    public List<DupeClientWaypoint> localWaypoints() {
        return Collections.unmodifiableList(localWaypoints);
    }

    public List<SharedDupeClientWaypoint> sharedWaypoints() {
        return Collections.unmodifiableList(sharedWaypoints);
    }

    public WaypointShareAudience defaultShareAudience() {
        return defaultShareAudience;
    }

    public void setDefaultShareAudience(WaypointShareAudience audience) {
        defaultShareAudience = audience == null ? WaypointShareAudience.FRIENDS_ONLY : audience;
        DupeClientWaypointStore.save(localWaypoints, defaultShareAudience);
    }

    public Optional<DupeClientWaypoint> findLocal(String id) {
        for (DupeClientWaypoint wp : localWaypoints) {
            if (wp.id().equals(id)) {
                return Optional.of(wp);
            }
        }
        return Optional.empty();
    }

    public void add(DupeClientWaypoint waypoint) {
        localWaypoints.add(waypoint);
        persistLocal();
    }

    public void update(DupeClientWaypoint waypoint) {
        for (int i = 0; i < localWaypoints.size(); i++) {
            if (localWaypoints.get(i).id().equals(waypoint.id())) {
                localWaypoints.set(i, waypoint);
                persistLocal();
                return;
            }
        }
        localWaypoints.add(waypoint);
        persistLocal();
    }

    public void delete(String id) {
        localWaypoints.removeIf(wp -> wp.id().equals(id));
        persistLocal();
    }

    public void applyShareAudienceToAll(WaypointShareAudience audience) {
        WaypointShareAudience target = audience == null ? WaypointShareAudience.FRIENDS_ONLY : audience;
        for (int i = 0; i < localWaypoints.size(); i++) {
            localWaypoints.set(i, localWaypoints.get(i).withShareAudience(target));
        }
        persistLocal();
    }

    public List<SharedDupeClientWaypoint> visibleWaypoints(Minecraft client) {
        ArrayList<SharedDupeClientWaypoint> out = new ArrayList<>();
        UUID self = selfUuid(client);
        String dim = currentDimensionKey(client);
        for (DupeClientWaypoint wp : localWaypoints) {
            if (!matchesDimension(wp.dimension(), dim)) {
                continue;
            }
            out.add(new SharedDupeClientWaypoint(wp, self, client != null && client.player != null ? client.player.getName().getString() : "", true));
        }
        boolean friendsOnlyView = Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().waypointsFriendsOnlyView);
        for (SharedDupeClientWaypoint row : sharedWaypoints) {
            if (row.ownedBySelf()) {
                continue;
            }
            if (!matchesDimension(row.waypoint().dimension(), dim)) {
                continue;
            }
            if (friendsOnlyView && !DupeClientSocialFriendsManager.isFriend(row.ownerUuid())) {
                continue;
            }
            out.add(row);
        }
        return out;
    }

    public void tick(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        if (!Boolean.TRUE.equals(DupeClientPresenceConfigManager.get().enabled)) {
            return;
        }
        tickCounter++;
        long now = System.currentTimeMillis();
        if (syncDirty && now - lastSyncMs >= 5_000L) {
            scheduleSync(client);
        }
        if (now < nextFetchAllowedMs) {
            return;
        }
        if (now - lastFetchMs >= FETCH_INTERVAL_MS || (tickCounter >= 200 && lastFetchMs == 0)) {
            tickCounter = 0;
            scheduleFetch(client);
        }
    }

    public void markSyncDirty() {
        syncDirty = true;
        lastSyncMs = 0;
    }

    public void requestFetchNow() {
        lastFetchMs = 0;
        tickCounter = 200;
    }

    public static String currentDimensionKey(Minecraft client) {
        if (client == null || client.level == null) {
            return "";
        }
        ResourceKey<Level> key = client.level.dimension();
        return key.identifier().toString();
    }

    public static UUID selfUuid(Minecraft client) {
        if (client == null) {
            return null;
        }
        if (client.player != null) {
            return client.player.getUUID();
        }
        if (client.getUser() != null) {
            return client.getUser().getProfileId();
        }
        return null;
    }

    private void persistLocal() {
        DupeClientWaypointStore.save(localWaypoints, defaultShareAudience);
        syncDirty = true;
    }

    private void scheduleSync(Minecraft client) {
        UUID self = selfUuid(client);
        if (self == null || !syncInFlight.compareAndSet(false, true)) {
            return;
        }
        List<DupeClientWaypoint> snapshot = List.copyOf(localWaypoints);
        EXEC.execute(() -> {
            try {
                String username = client != null && client.player != null ? client.player.getName().getString() : "";
                int code = DupeClientWaypointSync.syncBlocking(self, username, snapshot);
                if (code >= 200 && code < 300 || code == 204) {
                    syncDirty = false;
                    lastSyncMs = System.currentTimeMillis();
                }
            } finally {
                syncInFlight.set(false);
            }
        });
    }

    private void scheduleFetch(Minecraft client) {
        UUID self = selfUuid(client);
        if (self == null || !fetchInFlight.compareAndSet(false, true)) {
            return;
        }
        EXEC.execute(() -> {
            try {
                List<SharedDupeClientWaypoint> rows = DupeClientWaypointSync.fetchBlocking(self);
                consecutiveFetchFailures = 0;
                nextFetchAllowedMs = 0L;
                Minecraft mc = Minecraft.getInstance();
                Runnable apply = () -> {
                    sharedWaypoints.clear();
                    sharedWaypoints.addAll(rows);
                    lastFetchMs = System.currentTimeMillis();
                };
                if (mc != null) {
                    mc.execute(apply);
                } else {
                    apply.run();
                }
            } catch (Exception ignored) {
                int failures = Math.min(8, consecutiveFetchFailures + 1);
                consecutiveFetchFailures = failures;
                long backoff = Math.min(FETCH_FAILURE_MAX_MS, FETCH_FAILURE_BASE_MS << Math.min(failures - 1, 3));
                nextFetchAllowedMs = System.currentTimeMillis() + backoff;
                lastFetchMs = System.currentTimeMillis();
            } finally {
                fetchInFlight.set(false);
            }
        });
    }

    private static boolean matchesDimension(String waypointDim, String currentDim) {
        if (waypointDim == null || waypointDim.isBlank()) {
            return true;
        }
        if (currentDim == null || currentDim.isBlank()) {
            return true;
        }
        if (waypointDim.equalsIgnoreCase(currentDim)) {
            return true;
        }
        return dimensionToken(waypointDim).equalsIgnoreCase(dimensionToken(currentDim));
    }

    private static String dimensionToken(String dim) {
        String trimmed = dim.trim().toLowerCase(Locale.ROOT);
        int colon = trimmed.indexOf(':');
        return colon >= 0 ? trimmed.substring(colon + 1) : trimmed;
    }
}
