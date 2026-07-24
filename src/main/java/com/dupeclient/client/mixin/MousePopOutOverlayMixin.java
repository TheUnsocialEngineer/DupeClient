package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.overlay.IngameModuleOverlayScreen;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.gui.overlay.OverlayMouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes mouse input to in-game overlays while playing (no {@link net.minecraft.client.gui.screen.Screen} open),
 * and always routes scroll while our transparent overlay host screen is open (Screen scroll can miss panels).
 */
@Mixin(Mouse.class)
public abstract class MousePopOutOverlayMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (!IngameOverlayHost.shouldRouteOverlayMouse(client)) {
            return;
        }
        double mx = OverlayMouse.scaledX(client);
        double my = OverlayMouse.scaledY(client);
        int button = input.button();
        if (action == GLFW.GLFW_PRESS) {
            if (IngameOverlayHost.onHudMouseClicked(mx, my, button)) {
                ci.cancel();
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            if (IngameOverlayHost.onHudMouseReleased(mx, my, button)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void dupeclient$overlayDrag(long window, double x, double y, CallbackInfo ci) {
        if (!IngameOverlayHost.shouldRouteOverlayMouse(client) || !IngameOverlayHost.anyActiveDragging()) {
            return;
        }
        double mx = OverlayMouse.scaledX(client);
        double my = OverlayMouse.scaledY(client);
        IngameOverlayHost.onHudMouseDragged(mx, my, GLFW.GLFW_MOUSE_BUTTON_LEFT);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void dupeclient$overlayScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        boolean hostScreen = IngameModuleOverlayScreen.isShowing(client.currentScreen);
        if (!hostScreen && !IngameOverlayHost.shouldRouteOverlayMouse(client)) {
            return;
        }
        double mx = OverlayMouse.scaledX(client);
        double my = OverlayMouse.scaledY(client);
        boolean consumed = hostScreen
                ? IngameOverlayHost.onMouseScrolled(mx, my, horizontal, vertical)
                : IngameOverlayHost.onHudMouseScrolled(mx, my, horizontal, vertical);
        if (consumed) {
            ci.cancel();
        }
    }
}
