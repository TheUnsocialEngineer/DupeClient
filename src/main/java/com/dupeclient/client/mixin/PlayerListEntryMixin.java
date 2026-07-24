package com.dupeclient.client.mixin;

import com.dupeclient.client.module.cape.DupeClientCapeApplicator;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void dupeClient$overrideDupeCape(CallbackInfoReturnable<SkinTextures> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerListEntry self = (PlayerListEntry) (Object) this;
        GameProfile profile = self.getProfile();
        DupeClientCapeApplicator.maybeOverrideCape(client, profile, cir);
    }
}
