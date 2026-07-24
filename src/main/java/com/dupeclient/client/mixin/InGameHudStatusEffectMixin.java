package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Non-inventory containers that still use the HUD effect column need the same horizontal offset
 * when bigger containers is active (player inventory uses {@link StatusEffectsDisplayMixin}).
 */
@Mixin(InGameHud.class)
public abstract class InGameHudStatusEffectMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"))
    private void dupeclient$offsetStatusEffectsHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        int offset = effectOffset();
        if (offset > 0) {
            context.getMatrices().translate(offset, 0.0f);
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("RETURN"))
    private void dupeclient$offsetStatusEffectsReturn(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        int offset = effectOffset();
        if (offset > 0) {
            context.getMatrices().translate(-offset, 0.0f);
        }
    }

    private int effectOffset() {
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) {
            return 0;
        }
        if (!HandledScreenGuiScale.isActive()) {
            return 0;
        }
        HandledScreenAccessor gui = (HandledScreenAccessor) screen;
        return HandledScreenGuiScale.effectColumnOffsetX(gui.getX(), gui.getBackgroundWidth());
    }
}
