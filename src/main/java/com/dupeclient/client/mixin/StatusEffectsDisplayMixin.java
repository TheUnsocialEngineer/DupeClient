package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player inventory draws effects via {@link StatusEffectsDisplay} at {@code x + backgroundWidth}.
 * Bigger containers extend the panel visually; shift the effect column to match.
 */
@Mixin(StatusEffectsDisplay.class)
public abstract class StatusEffectsDisplayMixin {
    @Shadow
    @Final
    private HandledScreen<?> parent;

    @Inject(method = "shouldHideStatusEffectHud", at = @At("HEAD"), cancellable = true)
    private void dupeclient$scaledHideCheck(CallbackInfoReturnable<Boolean> cir) {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        HandledScreenAccessor gui = (HandledScreenAccessor) parent;
        int effectX = HandledScreenGuiScale.scaledPanelRight(gui.getX(), gui.getBackgroundWidth()) + 2;
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        cir.setReturnValue(screenWidth - effectX < 32);
        cir.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void dupeclient$translateEffectColumnHead(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        int offset = effectOffset();
        if (offset > 0) {
            context.getMatrices().translate(offset, 0.0f);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void dupeclient$translateEffectColumnReturn(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        int offset = effectOffset();
        if (offset > 0) {
            context.getMatrices().translate(-offset, 0.0f);
        }
    }

    private int effectOffset() {
        if (!HandledScreenGuiScale.isActive()) {
            return 0;
        }
        HandledScreenAccessor gui = (HandledScreenAccessor) parent;
        return HandledScreenGuiScale.effectColumnOffsetX(gui.getX(), gui.getBackgroundWidth());
    }
}
