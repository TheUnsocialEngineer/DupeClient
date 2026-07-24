package com.dupeclient.client.mixin;

import com.dupeclient.client.module.acaudit.AcAuditSlotOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AcAuditSlotOverlayMixin {
    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    protected int imageHeight;

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "render", at = @At("TAIL"))
    private void dupeclient$acAuditSlotOverlay(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AcAuditSlotOverlay.render((AbstractContainerScreen<?>) (Object) this, context, this.leftPos, this.topPos, this.imageHeight, this.hoveredSlot);
    }
}
