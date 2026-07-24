package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.SecurityManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Entity.class})
public abstract class EntityStaffGlowMixin {
    @Inject(method={"isGlowing"}, at={@At(value="HEAD")}, cancellable=true)
    private void dupeclient$staffGlow(CallbackInfoReturnable<Boolean> cir) {
        if (!SecurityManager.INSTANCE.isStaffGlowActive()) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity player)) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == player) {
            return;
        }
        if (SecurityManager.INSTANCE.isStaffPlayer(client, player)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}

