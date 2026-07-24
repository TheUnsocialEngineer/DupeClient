package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.overlay.IngameModuleOverlayScreen;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keyboard input for in-game module overlays while playing (no screen open), on the overlay host
 * screen, and on inventory screens.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardFabricatorMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayKey(long window, int action, KeyEvent input, CallbackInfo ci) {
        if (IngameModuleOverlayScreen.isShowing(minecraft.screen)) {
            if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) {
                return;
            }
            if (IngameOverlayHost.onKeyPressed(input.key())) {
                ci.cancel();
                return;
            }
            if (input.key() == GLFW.GLFW_KEY_ESCAPE && IngameOverlayHost.hasAnyActive()) {
                IngameOverlayHost.hideAllOverlays();
                ci.cancel();
            }
            return;
        }
        if (!IngameOverlayHost.shouldRouteOverlayMouse(minecraft)) {
            return;
        }
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) {
            return;
        }
        if (IngameOverlayHost.onFocusedOverlayKeyPressed(input.key())) {
            ci.cancel();
            return;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && IngameOverlayHost.hasAnyActive()) {
            IngameOverlayHost.hideAllOverlays();
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayChar(long window, CharacterEvent input, CallbackInfo ci) {
        if (IngameModuleOverlayScreen.isShowing(minecraft.screen)) {
            if (IngameOverlayHost.onCharTyped(input.codepoint())) {
                ci.cancel();
            }
            return;
        }
        if (minecraft.screen instanceof AbstractContainerScreen<?>) {
            if (IngameOverlayHost.onFocusedOverlayCharTyped(input.codepoint())) {
                ci.cancel();
                return;
            }
            if (!PacketFabricatorOverlay.INSTANCE.isModuleEnabled()) {
                return;
            }
            if (PacketFabricatorOverlay.INSTANCE.charTyped(input.codepoint())) {
                ci.cancel();
            }
            return;
        }
        if (IngameOverlayHost.shouldRouteOverlayMouse(minecraft)
                && IngameOverlayHost.onFocusedOverlayCharTyped(input.codepoint())) {
            ci.cancel();
        }
    }
}
