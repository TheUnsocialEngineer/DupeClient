package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
@Mixin(Gui.class)
public abstract class InGameHudStatusEffectMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderEffects", at = @At("HEAD"))
    private void dupeclient$offsetStatusEffectsHead(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        int offset = effectOffset();
        if (offset > 0) {
            context.pose().translate(offset, 0.0f);
        }
    }

    @Inject(method = "renderEffects", at = @At("RETURN"))
    private void dupeclient$offsetStatusEffectsReturn(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        int offset = effectOffset();
        if (offset > 0) {
            context.pose().translate(-offset, 0.0f);
        }
    }

    private int effectOffset() {
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen)) {
            return 0;
        }
        if (!HandledScreenGuiScale.isActive()) {
            return 0;
        }
        HandledScreenAccessor gui = (HandledScreenAccessor) screen;
        return HandledScreenGuiScale.effectColumnOffsetX(gui.getX(), gui.getImageWidth());
    }
}
