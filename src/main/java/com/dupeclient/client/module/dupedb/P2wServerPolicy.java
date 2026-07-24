package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.utility.crashes.CrashesManager;
import com.dupeclient.client.module.dupedb.p2w.NonP2wDisclaimerScreen;
import com.dupeclient.client.module.dupedb.p2w.P2wAlertScreen;
import com.dupeclient.client.module.macro.MacroEngine;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.payall.PayAllManager;
import com.dupeclient.client.module.utility.ChatGamesManager;
import com.ui_utils.SharedVariables;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;

/** Loads community P2W/non-P2W lists and enforces module policy on join. */
public final class P2wServerPolicy {
    public static final P2wServerPolicy INSTANCE = new P2wServerPolicy();

    public enum ServerKind {
        UNKNOWN,
        P2W,
        NON_P2W
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DupeClient-P2wPolicy");
        t.setDaemon(true);
        return t;
    });

    private final AtomicInteger generation = new AtomicInteger();
    private volatile P2wPresenceApi.Registry registry = P2wPresenceApi.Registry.empty();
    private volatile ServerKind currentKind = ServerKind.UNKNOWN;
    private volatile int currentP2wScore = -1;
    private volatile boolean modulesLocked;
    private volatile String lockedServer = "";
    /** Server address whose policy UI the user already dismissed this session (avoids setScreen loops). */
    private volatile String policyUiDismissedForServer = "";
    private int enforceCooldownTicks;

    private P2wServerPolicy() {
    }

    public void onPlaySessionJoin() {
        generation.incrementAndGet();
        currentKind = ServerKind.UNKNOWN;
        currentP2wScore = -1;
        modulesLocked = false;
        lockedServer = "";
        policyUiDismissedForServer = "";
        enforceCooldownTicks = 0;
        P2wMarkManager.trackServerSession(P2wMarkManager.currentServerAddress());
        refreshRegistryAsync();
    }

    public void onDisconnected() {
        generation.incrementAndGet();
        modulesLocked = false;
        lockedServer = "";
        currentKind = ServerKind.UNKNOWN;
        currentP2wScore = -1;
        policyUiDismissedForServer = "";
        enforceCooldownTicks = 0;
        P2wMarkManager.trackServerSession("");
    }

    public String registryStatusForServer(String server) {
        return registry.statusForServer(server);
    }

    /** Called when the user dismisses a P2W / non-P2W policy screen. */
    public void onPolicyUiDismissed(String server) {
        if (server != null && !server.isBlank()) {
            policyUiDismissedForServer = server;
        }
    }

    public boolean isModulesLocked() {
        return modulesLocked;
    }

    public ServerKind getCurrentKind() {
        return currentKind;
    }

    public int getCurrentP2wScore() {
        return currentP2wScore;
    }

    public void refreshRegistryAsync() {
        int gen = generation.get();
        EXEC.execute(() -> {
            try {
                P2wPresenceApi.Registry loaded = P2wPresenceApi.fetchRegistry();
                if (generation.get() != gen) {
                    return;
                }
                registry = loaded;
                Minecraft client = Minecraft.getInstance();
                if (client != null) {
                    client.execute(() -> applyPolicyForCurrentServer(false));
                }
            } catch (Exception e) {
                DupeClient.LOGGER.debug("[P2W] registry fetch failed: {}", e.toString());
            }
        });
    }

    public void applyPolicyForCurrentServer(boolean afterLocalMark) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        String server = P2wMarkManager.currentServerAddress();
        if (server.isBlank() || "singleplayer".equals(server)) {
            modulesLocked = false;
            lockedServer = "";
            currentKind = ServerKind.UNKNOWN;
            P2wMarkManager.trackServerSession("");
            return;
        }
        P2wMarkManager.trackServerSession(server);

        Map<String, P2wPresenceApi.ServerMark> p2wMap = toMap(registry.p2w());
        Map<String, P2wPresenceApi.ServerMark> nonMap = toMap(registry.nonP2w());
        P2wPresenceApi.ServerMark p2w = p2wMap.get(server);
        P2wPresenceApi.ServerMark non = nonMap.get(server);

        if (non != null) {
            currentKind = ServerKind.NON_P2W;
            currentP2wScore = -1;
            modulesLocked = true;
            lockedServer = server;
            enforceCooldownTicks = 0;
            forceDisableAllModules(client);
            if (shouldShowNonP2wScreen(client, server, afterLocalMark)) {
                client.setScreen(new NonP2wDisclaimerScreen(server));
            }
            return;
        }

        modulesLocked = false;
        lockedServer = "";
        enforceCooldownTicks = 0;
        if (p2w != null) {
            currentKind = ServerKind.P2W;
            currentP2wScore = p2w.score();
            if (shouldShowP2wAlert(client, server, afterLocalMark)) {
                client.setScreen(new P2wAlertScreen(server, currentP2wScore));
            }
            return;
        }

        currentKind = ServerKind.UNKNOWN;
        currentP2wScore = -1;
    }

    public void enforce(Minecraft client) {
        if (!modulesLocked || client == null || client.player == null) {
            return;
        }
        String server = P2wMarkManager.currentServerAddress();
        if (!server.equals(lockedServer)) {
            return;
        }
        if (enforceCooldownTicks > 0) {
            enforceCooldownTicks--;
            return;
        }
        enforceCooldownTicks = 20;
        forceDisableAllModulesIfNeeded(client);
    }

    private boolean shouldShowNonP2wScreen(Minecraft client, String server, boolean afterLocalMark) {
        if (afterLocalMark) {
            policyUiDismissedForServer = "";
            return true;
        }
        if (server.equals(policyUiDismissedForServer)) {
            return false;
        }
        return !(client.screen instanceof NonP2wDisclaimerScreen);
    }

    private boolean shouldShowP2wAlert(Minecraft client, String server, boolean afterLocalMark) {
        if (afterLocalMark) {
            policyUiDismissedForServer = "";
            return true;
        }
        if (server.equals(policyUiDismissedForServer)) {
            return false;
        }
        return !(client.screen instanceof P2wAlertScreen);
    }

    private static Map<String, P2wPresenceApi.ServerMark> toMap(List<P2wPresenceApi.ServerMark> list) {
        Map<String, P2wPresenceApi.ServerMark> map = new HashMap<>();
        if (list == null) {
            return map;
        }
        for (P2wPresenceApi.ServerMark mark : list) {
            map.putIfAbsent(mark.server(), mark);
        }
        return map;
    }

    private static void forceDisableAllModulesIfNeeded(Minecraft client) {
        if (MacroEngine.INSTANCE.isRunning()) {
            MacroEngine.INSTANCE.stop(client);
        }
        PayAllManager.INSTANCE.cancelIfActive();
        CrashesManager.INSTANCE.forceDisableAll();
        ChatGamesManager.INSTANCE.forceDisable();
        PacketUtilsManager.INSTANCE.forceDisableAllFeatures();
        if (SharedVariables.enabled) {
            SharedVariables.enabled = false;
        }
        if (SharedVariables.delayUIPackets) {
            SharedVariables.delayUIPackets = false;
        }
    }

    private static void forceDisableAllModules(Minecraft client) {
        MacroEngine.INSTANCE.stop(client);
        PayAllManager.INSTANCE.cancelIfActive();
        CrashesManager.INSTANCE.forceDisableAll();
        ChatGamesManager.INSTANCE.forceDisable();
        PacketUtilsManager.INSTANCE.forceDisableAllFeatures();
        SharedVariables.enabled = false;
        SharedVariables.delayUIPackets = false;
    }
}
