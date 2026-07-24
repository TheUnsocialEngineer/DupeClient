package com.dupeclient.client.mixin;

import com.dupeclient.client.module.packet.fabricator.SlotIdOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenSlotIdsMixin {
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void dupeclient$drawSlotId(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        SlotIdOverlay.renderSlot((HandledScreen<?>) (Object) this, context, slot);
    }
}
