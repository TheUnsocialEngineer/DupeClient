package com.dupeclient.client.mixin;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.DupeMainMenuScreen;
import com.dupeclient.client.gui.StartupBlockedScreen;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.core.session.SessionGate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    public Screen currentScreen;

    @Unique
    private boolean dupeClient$redirectingScreen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void dupeClient$clearOverlayCapture(Screen screen, CallbackInfo ci) {
        IngameOverlayHost.onScreenChanged(screen);
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void dupeClient$enforceSessionGate(Screen screen, CallbackInfo ci) {
        if (dupeClient$redirectingScreen) {
            return;
        }
        if (!SessionGate.isGameBlocked()) {
            return;
        }
        if (screen instanceof StartupBlockedScreen) {
            return;
        }
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (screen == null && !client.isRunning()) {
            return;
        }
        ci.cancel();
        dupeClient$redirectingScreen = true;
        try {
            client.setScreen(new StartupBlockedScreen());
        } finally {
            dupeClient$redirectingScreen = false;
        }
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void dupeClient$replaceVanillaTitle(Screen screen, CallbackInfo ci) {
        if (dupeClient$redirectingScreen) {
            return;
        }
        if (SessionGate.isGameBlocked()) {
            return;
        }

        if (currentScreen instanceof TitleScreen && !(currentScreen instanceof DupeMainMenuScreen)) {
            dupeClient$redirectingScreen = true;
            try {
                MinecraftClient client = (MinecraftClient) (Object) this;
                client.setScreen(DupeClient.createMainMenu());
            } finally {
                dupeClient$redirectingScreen = false;
            }
        }
    }
}
