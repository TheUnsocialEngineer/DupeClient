package com.dupeclient.client.mixin;

import com.dupeclient.client.core.session.HubModuleRules;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketUtilsManager.class)
public abstract class PacketUtilsHubGateMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void dupeclient$blockHubPacketUtils(Minecraft client, CallbackInfo ci) {
        if (!HubModuleRules.exploitFeaturesAllowed()) {
            ci.cancel();
        }
    }
}
