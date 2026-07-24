package com.dupeclient.client.gui.widget;

import com.dupeclient.client.gui.modern.ModernTextInputChrome;
import com.dupeclient.client.gui.modern.UiTokens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Text field with rounded midnight chrome, placeholder, focus glow, and full edit shortcuts.
 */
public class StylishTextFieldWidget extends TextFieldWidget {
    private static final int TEXT_H = 8;
    private static final int TEXT_PAD_X = ModernTextInputChrome.PAD_X;

    private String placeholder = "";

    public StylishTextFieldWidget(TextRenderer renderer, int x, int y, int width, int height, Text hint) {
        super(renderer, x, y, width, Math.max(height, ModernTextInputChrome.MIN_HEIGHT), hint);
        setDrawsBackground(false);
        setMaxLength(512);
        setEditable(true);
        setEditableColor(UiTokens.TEXT);
        setUneditableColor(UiTokens.SLATE_300);
        if (hint != null) {
            placeholder = hint.getString();
        }
    }

    public static StylishTextFieldWidget create(TextRenderer renderer, int x, int y, int width, Text placeholder) {
        return create(renderer, x, y, width, ModernTextInputChrome.MIN_HEIGHT, placeholder);
    }

    public static StylishTextFieldWidget create(
            TextRenderer renderer, int x, int y, int width, int height, Text placeholder) {
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
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int outerX = getX();
        int outerY = getY();
        int outerW = getWidth();
        int outerH = getHeight();

        ModernTextInputChrome.drawField(context, outerX, outerY, outerW, outerH, isFocused());

        if (getText().isEmpty() && !isFocused() && !placeholder.isEmpty()) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
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

        super.renderWidget(context, mouseX, mouseY, deltaTicks);

        setX(outerX);
        setY(outerY);
        setWidth(outerW);
        setHeight(outerH);
    }
}
