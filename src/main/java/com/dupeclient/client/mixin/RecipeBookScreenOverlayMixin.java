package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Survival inventory ({@link net.minecraft.client.gui.screens.inventory.InventoryScreen}) overrides
 * {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen#render} via {@link AbstractRecipeBookScreen}
 * and never hits {@link HandledScreenModuleOverlayMixin}.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class RecipeBookScreenOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void dupeclient$renderModuleOverlays(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        IngameOverlayHost.renderAll(context, mouseX, mouseY, delta);
    }
}
