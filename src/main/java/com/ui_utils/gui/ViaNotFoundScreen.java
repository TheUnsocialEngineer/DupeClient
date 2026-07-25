package com.ui_utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ViaNotFoundScreen extends Screen {
    private final Screen parent;
    private final String message;

    public ViaNotFoundScreen(Screen parent, String message) {
        super(Component.literal("ViaFabricPlus"));
        this.parent = parent;
        this.message = message;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.addRenderableWidget(Button.builder(Component.literal("OK"), button -> Minecraft.getInstance()
                        .setScreen(this.parent))
                .bounds(centerX - 50, centerY + 20, 100, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        context.centeredText(
                this.font, Component.literal(this.message), this.width / 2, this.height / 2, 0xFF5555);
    }
}
