package com.dupeclient.client.mixin;

import com.dupeclient.client.module.cape.DupeClientCapeApplicator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void dupeClient$overrideDupeCapeOnEntity(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        DupeClientCapeApplicator.maybeOverrideCape(MinecraftClient.getInstance(), self.getGameProfile(), cir);
    }
}
