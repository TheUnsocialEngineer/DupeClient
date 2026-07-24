package com.dupeclient.client.mixin;

import com.dupeclient.client.module.security.nochatrestrictions.NoChatRestrictionsGate;
import com.dupeclient.client.module.security.nochatrestrictions.NoChatRestrictionsUserApiService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftClientNoChatRestrictionsMixin {
    @Inject(method = "createUserApiService", at = @At("RETURN"), cancellable = true)
    private void dupeClient$wrapUserApiService(
            YggdrasilAuthenticationService authService,
            GameConfig runArgs,
            CallbackInfoReturnable<UserApiService> cir) {
        if (!NoChatRestrictionsGate.active()) {
            return;
        }
        UserApiService original = cir.getReturnValue();
        if (original != null && !(original instanceof NoChatRestrictionsUserApiService)) {
            cir.setReturnValue(new NoChatRestrictionsUserApiService(original));
        }
    }

    @Inject(method = "userProperties", at = @At("RETURN"), cancellable = true)
    private void dupeClient$forceUserProperties(CallbackInfoReturnable<UserApiService.UserProperties> cir) {
        if (NoChatRestrictionsGate.active()) {
            cir.setReturnValue(NoChatRestrictionsUserApiService.forcedProperties());
        }
    }

    @Inject(method = "isNameBanned", at = @At("HEAD"), cancellable = true)
    private void dupeClient$bypassUsernameBan(CallbackInfoReturnable<Boolean> cir) {
        if (NoChatRestrictionsGate.active()) {
            cir.setReturnValue(false);
        }
    }
}
