package com.dupeclient.client.mixin;

import com.dupeclient.client.ui.mui.MuiClientGuiFragment;
import com.dupeclient.client.ui.mui.MuiHostedScreenFragment;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.MuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * mVUS routes {@code Screen#mouseMoved} into the Modern UI host (window chrome, edge resize, etc.).
 * For Dupe UIs the interactive surface is {@link icyllis.modernui.mc.MinecraftSurfaceView} inside
 * the fragment; forwarding {@code mouseMoved} here makes the client behave as if the window were
 * being resized, which matches the "GUI tied to the mouse" / corner-cursor report.
 */
@Mixin(targets = "icyllis.modernui.mc.fabric.SimpleScreen")
public abstract class MuiSimpleScreenSuppressHostMouseMixin {
    @Inject(method = "mouseMoved", at = @At("HEAD"), cancellable = true)
    private void dupe$cancelHostMouseForDupeSurfaceContent(double mouseX, double mouseY, CallbackInfo ci) {
        MuiScreen mui = (MuiScreen) (Object) this;
        Fragment f = mui.getFragment();
        if (f instanceof MuiClientGuiFragment || f instanceof MuiHostedScreenFragment) {
            ci.cancel();
        }
    }
}
