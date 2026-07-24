package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Module overlays on inventory screens. Mouse is intercepted only when the cursor is on a panel
 * and the overlay consumes the event — slot clicks keep vanilla handling.
 */
@Mixin(value = AbstractContainerScreen.class, priority = 2090)
public abstract class HandledScreenModuleOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void dupeclient$renderModuleOverlays(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        IngameOverlayHost.renderAll(context, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (IngameOverlayHost.onHandledScreenMouseClicked(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayMouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        if (IngameOverlayHost.onHandledScreenMouseReleased(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayMouseDragged(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (IngameOverlayHost.onHandledScreenMouseDragged(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        if (IngameOverlayHost.onHandledScreenMouseScrolled(mouseX, mouseY, horizontal, vertical)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void dupeclient$moduleOverlayKey(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (IngameOverlayHost.onFocusedOverlayKeyPressed(input.key())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
