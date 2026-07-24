package com.dupeclient.client.mixin;

import com.dupeclient.client.module.cape.DupeClientCapeApplicator;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerListEntryMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void dupeClient$overrideDupeCape(CallbackInfoReturnable<PlayerSkin> cir) {
        Minecraft client = Minecraft.getInstance();
        PlayerInfo self = (PlayerInfo) (Object) this;
        GameProfile profile = self.getProfile();
        DupeClientCapeApplicator.maybeOverrideCape(client, profile, cir);
    }
}
