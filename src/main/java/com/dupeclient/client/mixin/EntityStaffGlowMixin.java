package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.SecurityManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Entity.class})
public abstract class EntityStaffGlowMixin {
    @Inject(method={"isCurrentlyGlowing"}, at={@At(value="HEAD")}, cancellable=true)
    private void dupeclient$staffGlow(CallbackInfoReturnable<Boolean> cir) {
        if (!SecurityManager.INSTANCE.isStaffGlowActive()) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == player) {
            return;
        }
        if (SecurityManager.INSTANCE.isStaffPlayer(client, player)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}

