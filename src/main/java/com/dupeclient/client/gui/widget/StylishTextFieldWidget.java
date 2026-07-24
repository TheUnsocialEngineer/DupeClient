package com.dupeclient.client.gui.widget;

import com.dupeclient.client.gui.modern.ModernTextInputChrome;
import com.dupeclient.client.gui.modern.UiTokens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Text field with rounded midnight chrome, placeholder, focus glow, and full edit shortcuts.
 */
public class StylishTextFieldWidget extends EditBox {
    private static final int TEXT_H = 8;
    private static final int TEXT_PAD_X = ModernTextInputChrome.PAD_X;

    private String placeholder = "";

    public StylishTextFieldWidget(Font renderer, int x, int y, int width, int height, Component hint) {
        super(renderer, x, y, width, Math.max(height, ModernTextInputChrome.MIN_HEIGHT), hint);
        setBordered(false);
        setMaxLength(512);
        setEditable(true);
        setTextColor(UiTokens.TEXT);
        setTextColorUneditable(UiTokens.SLATE_300);
        if (hint != null) {
            placeholder = hint.getString();
        }
    }

    public static StylishTextFieldWidget create(Font renderer, int x, int y, int width, Component placeholder) {
        return create(renderer, x, y, width, ModernTextInputChrome.MIN_HEIGHT, placeholder);
    }

    public static StylishTextFieldWidget create(
            Font renderer, int x, int y, int width, int height, Component placeholder) {
        StylishTextFieldWidget field = new StylishTextFieldWidget(renderer, x, y, width, height, placeholder);
        if (placeholder != null) {
            field.setPlaceholder(placeholder.getString());
        }
        return field;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        int outerX = getX();
        int outerY = getY();
        int outerW = getWidth();
        int outerH = getHeight();

        ModernTextInputChrome.drawField(context, outerX, outerY, outerW, outerH, isFocused());

        if (getValue().isEmpty() && !isFocused() && !placeholder.isEmpty()) {
            Font tr = Minecraft.getInstance().font;
            int textY = ModernTextInputChrome.textY(outerY, outerH);
            ModernTextInputChrome.drawPlaceholder(
                    tr,
                    context,
                    outerX + TEXT_PAD_X,
                    textY,
                    outerW - TEXT_PAD_X * 2,
                    placeholder);
            return;
        }

        int padY = (outerH - TEXT_H) / 2;
        setX(outerX + TEXT_PAD_X);
        setY(outerY + padY);
        setWidth(Math.max(4, outerW - TEXT_PAD_X * 2));
        setHeight(TEXT_H);

        super.extractWidgetRenderState(context, mouseX, mouseY, deltaTicks);

        setX(outerX);
        setY(outerY);
        setWidth(outerW);
        setHeight(outerH);
    }
}
