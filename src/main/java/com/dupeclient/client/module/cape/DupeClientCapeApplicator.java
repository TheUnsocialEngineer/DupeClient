package com.dupeclient.client.module.cape;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Shared cape override for {@code PlayerListEntry#getSkinTextures()} and
 * {@link net.minecraft.client.player.AbstractClientPlayer#getSkin()} (1.21+ world rendering).
 */
public final class DupeClientCapeApplicator {
    private static final Identifier DUPECLIENT_CAPE_ASSET_ID = Identifier.fromNamespaceAndPath("dupeclient", "cape");
    private static final Identifier DUPECLIENT_CAPE_TEXTURE_PATH = Identifier.fromNamespaceAndPath("dupeclient", "textures/cape.png");
    private static final ClientAsset.Texture CAPE =
            new ClientAsset.ResourceTexture(DUPECLIENT_CAPE_ASSET_ID, DUPECLIENT_CAPE_TEXTURE_PATH);

    private static final ConcurrentHashMap<UUID, CachedOverride> CACHE = new ConcurrentHashMap<>();

    private DupeClientCapeApplicator() {
    }

    public static void maybeOverrideCape(Minecraft client, @Nullable GameProfile profile, CallbackInfoReturnable<PlayerSkin> cir) {
        if (client == null || profile == null || profile.id() == null) {
            return;
        }
        UUID id = profile.id();
        boolean isLocal = client.player != null && id.equals(client.player.getUUID());
        if (!isLocal && !DupeClientCapeTracker.isDupeClientUser(id)) {
            CACHE.remove(id);
            return;
        }
        PlayerSkin textures = cir.getReturnValue();
        if (textures == null) {
            return;
        }
        ClientAsset.Texture body = textures.body();
        CachedOverride cached = CACHE.get(id);
        if (cached != null && cached.body == body && cached.model == textures.model() && cached.secure == textures.secure()) {
            cir.setReturnValue(cached.overridden);
            return;
        }
        PlayerSkin overridden = new PlayerSkin(
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

    private record CachedOverride(ClientAsset.Texture body, PlayerModelType model, boolean secure, PlayerSkin overridden) {
    }
}
