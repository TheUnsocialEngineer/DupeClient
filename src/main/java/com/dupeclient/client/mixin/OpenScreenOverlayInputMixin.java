package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.ClientGuiScreen;
import com.dupeclient.client.gui.MacroEditorScreen;
import com.dupeclient.client.gui.SocialScreen;
import com.dupeclient.client.gui.overlay.IngameModuleOverlayScreen;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.module.hud.HudEditorScreen;
import com.dupeclient.client.module.packet.sniffer.PacketWorkbenchScreen;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes module overlay mouse input on open {@link Screen}s. Screens with custom handlers
 * (hub, social, HUD editor) call {@link IngameOverlayHost} themselves before their own logic.
 */
@Mixin(ContainerEventHandler.class)
public interface OpenScreenOverlayInputMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!dupeclient$shouldRouteOverlayOn((ContainerEventHandler) (Object) this)) {
            return;
        }
        if (IngameOverlayHost.onScreenOverlayMouseClicked(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayMouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        if (!dupeclient$shouldRouteOverlayOn((ContainerEventHandler) (Object) this)) {
            return;
        }
        if (IngameOverlayHost.onScreenOverlayMouseReleased(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayMouseDragged(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (!dupeclient$shouldRouteOverlayOn((ContainerEventHandler) (Object) this)) {
            return;
        }
        if (IngameOverlayHost.onScreenOverlayMouseDragged(click.x(), click.y(), click.button())) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayMouseScrolled(
            double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        if (!dupeclient$shouldRouteOverlayOn((ContainerEventHandler) (Object) this)) {
            return;
        }
        if (IngameOverlayHost.onScreenOverlayMouseScrolled(mouseX, mouseY, horizontal, vertical)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    private static boolean dupeclient$shouldRouteOverlayOn(ContainerEventHandler element) {
        if (!(element instanceof Screen screen)) {
            return false;
        }
        return !(screen instanceof AbstractContainerScreen<?>)
                && !(screen instanceof IngameModuleOverlayScreen)
                && !(screen instanceof PacketWorkbenchScreen)
                && !(screen instanceof ClientGuiScreen)
                && !(screen instanceof SocialScreen)
                && !(screen instanceof HudEditorScreen)
                && !(screen instanceof MacroEditorScreen);
    }
}
