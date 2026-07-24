package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player inventory draws effects via {@link EffectsInInventory} at {@code x + backgroundWidth}.
 * Bigger containers extend the panel visually; shift the effect column to match.
 */
@Mixin(EffectsInInventory.class)
public abstract class StatusEffectsDisplayMixin {
    @Shadow
    @Final
    private AbstractContainerScreen<?> screen;

    @Inject(method = "canSeeEffects", at = @At("HEAD"), cancellable = true)
    private void dupeclient$scaledHideCheck(CallbackInfoReturnable<Boolean> cir) {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        HandledScreenAccessor gui = (HandledScreenAccessor) screen;
        int effectX = HandledScreenGuiScale.scaledPanelRight(gui.getX(), gui.getImageWidth()) + 2;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        cir.setReturnValue(screenWidth - effectX < 32);
        cir.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void dupeclient$translateEffectColumnHead(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
        int offset = effectOffset();
        if (offset > 0) {
            context.pose().translate(offset, 0.0f);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void dupeclient$translateEffectColumnReturn(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
        int offset = effectOffset();
        if (offset > 0) {
            context.pose().translate(-offset, 0.0f);
        }
    }

    private int effectOffset() {
        if (!HandledScreenGuiScale.isActive()) {
            return 0;
        }
        HandledScreenAccessor gui = (HandledScreenAccessor) screen;
        return HandledScreenGuiScale.effectColumnOffsetX(gui.getX(), gui.getImageWidth());
    }
}
