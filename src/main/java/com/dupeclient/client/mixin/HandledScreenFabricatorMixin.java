package com.dupeclient.client.mixin;

import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HandledScreen.class, priority = 2100)
public abstract class HandledScreenFabricatorMixin {
    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void dupeclient$fabricatorSlotPick(Slot slot, int slotId, int button, net.minecraft.screen.slot.SlotActionType actionType, CallbackInfo ci) {
        if (slot != null && PacketUtilsManager.INSTANCE.getSettings().fabricatorEnabled) {
            PacketFabricatorOverlay.INSTANCE.onSlotClick((HandledScreen<?>) (Object) this, slotId);
        }
    }
}
