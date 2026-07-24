package com.dupeclient.client.mixin;

import com.dupeclient.client.module.packet.fabricator.SlotIdOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenSlotIdsMixin {
    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void dupeclient$drawSlotId(GuiGraphics context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        SlotIdOverlay.renderSlot((AbstractContainerScreen<?>) (Object) this, context, slot);
    }
}
