package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.overlay.IngameModuleOverlayScreen;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.module.packet.sniffer.PacketWorkbenchScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws module overlays above hub / menu screens. Input is routed via {@link MousePopOutOverlayMixin}
 * and {@link KeyboardFabricatorMixin} — {@link Screen} no longer declares mouse methods in 1.21.11.
 */
@Mixin(Screen.class)
public abstract class ScreenFabricatorOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void dupeclient$renderModuleOverlays(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen instanceof AbstractContainerScreen
                || screen instanceof IngameModuleOverlayScreen
                || screen instanceof PacketWorkbenchScreen
                || screen instanceof ConnectScreen
                || screen instanceof LevelLoadingScreen
                || screen instanceof ProgressScreen) {
            return;
        }
        IngameOverlayHost.renderAll(context, mouseX, mouseY, delta);
    }
}
