package com.dupeclient.client.mixin;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.ProjectionMatrix3;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.render.GameRenderer;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("buffers")
    BufferBuilderStorage dupeclient$buffers();

    @Accessor("fogRenderer")
    FogRenderer dupeclient$fogRenderer();

    @Accessor("hudProjectionMatrix")
    ProjectionMatrix3 dupeclient$hudProjectionMatrix();

    @Accessor("guiRenderer")
    GuiRenderer dupeclient$guiRenderer();
}
