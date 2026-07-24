package com.dupeclient.client.multiplayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class OfflineAccountSkins {
    private static final Map<UUID, SkinTextures> CACHE = new ConcurrentHashMap<>();

    private OfflineAccountSkins() {
    }

    static SkinTextures texturesFor(MinecraftClient client, OfflineAccount account) {
        SkinTextures cached = CACHE.get(account.uuid());
        if (cached != null) {
            return cached;
        }
        requestLoad(client, account);
        return DefaultSkinHelper.getSkinTextures(account.uuid());
    }

    static void prefetchAll(MinecraftClient client, Iterable<OfflineAccount> accounts) {
        for (OfflineAccount account : accounts) {
            requestLoad(client, account);
        }
    }

    private static void requestLoad(MinecraftClient client, OfflineAccount account) {
        if (client == null || CACHE.containsKey(account.uuid())) {
            return;
        }
        GameProfile profile = new GameProfile(account.uuid(), account.username());
        client.getSkinProvider().fetchSkinTextures(profile).thenAccept(opt ->
            opt.ifPresent(skin -> CACHE.put(account.uuid(), skin))
        );
    }
}
