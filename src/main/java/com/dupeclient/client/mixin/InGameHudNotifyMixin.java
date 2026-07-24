package com.dupeclient.client.mixin;

import com.dupeclient.client.core.notify.ClientNotificationHub;
import com.dupeclient.client.gui.overlay.ServerProfileCard;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudNotifyMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "render", at = @At("RETURN"))
    private void dupeclient$renderNotifications(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (minecraft.font == null) {
            return;
        }
        ClientNotificationHub.render(context, minecraft.font, context.guiWidth());
        ServerProfileCard.render(context, minecraft.font);
    }
}
