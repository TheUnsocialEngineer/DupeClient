package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Survival inventory ({@link net.minecraft.client.gui.screen.ingame.InventoryScreen}) overrides
 * {@link net.minecraft.client.gui.screen.ingame.HandledScreen#render} via {@link RecipeBookScreen}
 * and never hits {@link HandledScreenModuleOverlayMixin}.
 */
@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookScreenOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void dupeclient$renderModuleOverlays(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        IngameOverlayHost.renderAll(context, mouseX, mouseY, delta);
    }
}
