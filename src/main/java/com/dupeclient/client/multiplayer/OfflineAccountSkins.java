package com.dupeclient.client.multiplayer;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

final class OfflineAccountSkins {
    private static final Map<UUID, PlayerSkin> CACHE = new ConcurrentHashMap<>();

    private OfflineAccountSkins() {
    }

    static PlayerSkin texturesFor(Minecraft client, OfflineAccount account) {
        PlayerSkin cached = CACHE.get(account.uuid());
        if (cached != null) {
            return cached;
        }
        requestLoad(client, account);
        return DefaultPlayerSkin.get(account.uuid());
    }

    static void prefetchAll(Minecraft client, Iterable<OfflineAccount> accounts) {
        for (OfflineAccount account : accounts) {
            requestLoad(client, account);
        }
    }

    private static void requestLoad(Minecraft client, OfflineAccount account) {
        if (client == null || CACHE.containsKey(account.uuid())) {
            return;
        }
        GameProfile profile = new GameProfile(account.uuid(), account.username());
        client.getSkinManager().get(profile).thenAccept(opt ->
            opt.ifPresent(skin -> CACHE.put(account.uuid(), skin))
        );
    }
}
