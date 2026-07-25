package com.ui_utils.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ViaNotFoundScreen extends Screen {
    private final Screen parent;
    private final String message;

    public ViaNotFoundScreen(Screen parent, String message) {
        super(Text.literal("ViaFabricPlus"));
        this.parent = parent;
        this.message = message;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("OK"), button -> MinecraftClient.getInstance()
                        .setScreen(this.parent))
                .dimensions(centerX - 50, centerY + 20, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer, Text.literal(this.message), this.width / 2, this.height / 2, 0xFF5555);
    }
}
