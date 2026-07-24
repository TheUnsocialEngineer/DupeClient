package com.dupeclient.client.module.cape;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared cape override for {@code PlayerListEntry#getSkinTextures()} and
 * {@link net.minecraft.client.network.AbstractClientPlayerEntity#getSkin()} (1.21+ world rendering).
 */
public final class DupeClientCapeApplicator {
    private static final Identifier DUPECLIENT_CAPE_ASSET_ID = Identifier.of("dupeclient", "cape");
    private static final Identifier DUPECLIENT_CAPE_TEXTURE_PATH = Identifier.of("dupeclient", "textures/cape.png");
    private static final AssetInfo.TextureAsset CAPE =
            new AssetInfo.TextureAssetInfo(DUPECLIENT_CAPE_ASSET_ID, DUPECLIENT_CAPE_TEXTURE_PATH);

    private static final ConcurrentHashMap<UUID, CachedOverride> CACHE = new ConcurrentHashMap<>();

    private DupeClientCapeApplicator() {
    }

    public static void maybeOverrideCape(MinecraftClient client, @Nullable GameProfile profile, CallbackInfoReturnable<SkinTextures> cir) {
        if (client == null || profile == null || profile.id() == null) {
            return;
        }
        UUID id = profile.id();
        boolean isLocal = client.player != null && id.equals(client.player.getUuid());
        if (!isLocal && !DupeClientCapeTracker.isDupeClientUser(id)) {
            CACHE.remove(id);
            return;
        }
        SkinTextures textures = cir.getReturnValue();
        if (textures == null) {
            return;
        }
        AssetInfo.TextureAsset body = textures.body();
        CachedOverride cached = CACHE.get(id);
        if (cached != null && cached.body == body && cached.model == textures.model() && cached.secure == textures.secure()) {
            cir.setReturnValue(cached.overridden);
            return;
        }
        SkinTextures overridden = new SkinTextures(
                body,
                CAPE,
                CAPE,
                textures.model(),
                textures.secure());
        CACHE.put(id, new CachedOverride(body, textures.model(), textures.secure(), overridden));
        cir.setReturnValue(overridden);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private record CachedOverride(AssetInfo.TextureAsset body, PlayerSkinType model, boolean secure, SkinTextures overridden) {
    }
}
