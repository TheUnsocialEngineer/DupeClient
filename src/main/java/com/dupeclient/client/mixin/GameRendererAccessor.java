package com.dupeclient.client.mixin;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("renderBuffers")
    RenderBuffers dupeclient$buffers();

    @Accessor("fogRenderer")
    FogRenderer dupeclient$fogRenderer();

    @Accessor("hud3dProjectionMatrixBuffer")
    ProjectionMatrixBuffer dupeclient$hudProjectionMatrix();

    @Accessor("guiRenderer")
    GuiRenderer dupeclient$guiRenderer();
}
