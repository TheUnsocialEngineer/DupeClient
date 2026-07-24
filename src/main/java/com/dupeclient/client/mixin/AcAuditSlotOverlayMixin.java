package com.dupeclient.client.mixin;

import com.dupeclient.client.module.acaudit.AcAuditSlotOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class AcAuditSlotOverlayMixin {
    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    protected int backgroundHeight;

    @Shadow
    protected Slot focusedSlot;

    @Inject(method = "render", at = @At("TAIL"))
    private void dupeclient$acAuditSlotOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AcAuditSlotOverlay.render((HandledScreen<?>) (Object) this, context, this.x, this.y, this.backgroundHeight, this.focusedSlot);
    }
}
