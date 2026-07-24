package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scales the survival-inventory player preview with the rest of the scaled {@link InventoryScreen} GUI.
 * {@link InventoryScreen#drawEntity} is static in 1.21.11, so callbacks here must be static too.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenGuiScaleMixin {
    @Inject(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V",
            at = @At("HEAD"))
    private static void dupeclient$scalePlayerModelHead(
            DrawContext context,
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
        if (!(MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> screen)) {
            return;
        }
        HandledScreenAccessor gui = (HandledScreenAccessor) screen;
        HandledScreenGuiScale.pushScaleScreen(
                context, gui.getX(), gui.getY(), gui.getBackgroundWidth(), gui.getBackgroundHeight());
    }

    @Inject(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V",
            at = @At("RETURN"))
    private static void dupeclient$scalePlayerModelTail(
            DrawContext context,
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
