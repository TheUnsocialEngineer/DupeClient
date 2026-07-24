package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.overlay.IngameModuleOverlayScreen;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
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
@Mixin(Keyboard.class)
public abstract class KeyboardFabricatorMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayKey(long window, int action, KeyInput input, CallbackInfo ci) {
        if (IngameModuleOverlayScreen.isShowing(client.currentScreen)) {
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
        if (!IngameOverlayHost.shouldRouteOverlayMouse(client)) {
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

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayChar(long window, CharInput input, CallbackInfo ci) {
        if (IngameModuleOverlayScreen.isShowing(client.currentScreen)) {
            if (IngameOverlayHost.onCharTyped(input.codepoint())) {
                ci.cancel();
            }
            return;
        }
        if (client.currentScreen instanceof HandledScreen<?>) {
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
        if (IngameOverlayHost.shouldRouteOverlayMouse(client)
                && IngameOverlayHost.onFocusedOverlayCharTyped(input.codepoint())) {
            ci.cancel();
        }
    }
}
