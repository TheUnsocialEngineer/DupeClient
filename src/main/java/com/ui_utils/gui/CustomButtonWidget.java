package com.ui_utils.gui;

import com.ui_utils.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CustomButtonWidget
extends AbstractWidget {
    private final PressAction onPress;

    public CustomButtonWidget(int x, int y, int width, int height, Component message, PressAction onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        int bg = hovered ? -266326714 : -434823890;
        int border = hovered ? -10782552 : -12627080;
        context.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, -16117734);
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
        UITheme.drawBorder(context, this.getX(), this.getY(), this.width, this.height, border);
        if (hovered) {
            context.fill(this.getX(), this.getY() + this.height - 2, this.getX() + this.width, this.getY() + this.height, -12877066);
        }
        int textColor = this.active ? -328966 : -7429950;
        context.centeredText(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
    }

    public void onClick(MouseButtonEvent click, boolean doubleClick) {
        if (this.active && this.onPress != null) {
            this.onPress.onPress(this);
        }
    }

    protected void updateWidgetNarration(NarrationElementOutput builder) {
        this.defaultButtonNarrationText(builder);
    }

    public static CustomButtonWidget create(int x, int y, int width, Component message, PressAction onPress) {
        return new CustomButtonWidget(x, y, width, 16, message, onPress);
    }

    public static CustomButtonWidget createSmall(int x, int y, int width, Component message, PressAction onPress) {
        return new CustomButtonWidget(x, y, width, 14, message, onPress);
    }

    @FunctionalInterface
    public static interface PressAction {
        public void onPress(CustomButtonWidget var1);
    }
}

