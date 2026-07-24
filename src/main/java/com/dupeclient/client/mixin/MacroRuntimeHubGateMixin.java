package com.dupeclient.client.mixin;

import com.dupeclient.client.core.session.HubModuleRules;
import com.dupeclient.client.module.macro.MacroRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MacroRuntime.class)
public abstract class MacroRuntimeHubGateMixin {
    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void dupeclient$blockHubMacroStart(Minecraft client, String id, CallbackInfo ci) {
        if (!HubModuleRules.exploitFeaturesAllowed()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void dupeclient$blockHubMacroTick(Minecraft client, CallbackInfo ci) {
        if (!HubModuleRules.exploitFeaturesAllowed()) {
            if (((MacroRuntime) (Object) this).isRunning()) {
                ((MacroRuntime) (Object) this).stop(client);
            }
            ci.cancel();
        }
    }
}
