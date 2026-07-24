package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scales the survival-inventory player preview with the rest of the scaled {@link InventoryScreen} GUI.
 * {@link InventoryScreen#extractRenderState} is static in 1.21.11, so callbacks here must be static too.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenGuiScaleMixin {
    @Inject(
            method = "renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"))
    private static void dupeclient$scalePlayerModelHead(
            GuiGraphics context,
            int x1,
            int y1,
            int x2,
            int y2,
            int size,
            float scale,
            float mouseX,
            float mouseY,
            LivingEntity entity,
            CallbackInfo ci) {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        HandledScreenAccessor gui = (HandledScreenAccessor) screen;
        HandledScreenGuiScale.pushScaleScreen(
                context, gui.getX(), gui.getY(), gui.getImageWidth(), gui.getImageHeight());
    }

    @Inject(
            method = "renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphics;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("RETURN"))
    private static void dupeclient$scalePlayerModelTail(
            GuiGraphics context,
            int x1,
            int y1,
            int x2,
            int y2,
            int size,
            float scale,
            float mouseX,
            float mouseY,
            LivingEntity entity,
            CallbackInfo ci) {
        if (HandledScreenGuiScale.isActive()) {
            HandledScreenGuiScale.popScale(context);
        }
    }
}
