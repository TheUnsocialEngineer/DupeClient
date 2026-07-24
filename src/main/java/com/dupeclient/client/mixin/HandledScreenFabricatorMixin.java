package com.dupeclient.client.mixin;

import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractContainerScreen.class, priority = 2100)
public abstract class HandledScreenFabricatorMixin {
    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"))
    private void dupeclient$fabricatorSlotPick(Slot slot, int slotId, int button, net.minecraft.world.inventory.ContainerInput actionType, CallbackInfo ci) {
        if (slot != null && PacketUtilsManager.INSTANCE.getSettings().fabricatorEnabled) {
            PacketFabricatorOverlay.INSTANCE.onSlotClick((AbstractContainerScreen<?>) (Object) this, slotId);
        }
    }
}
