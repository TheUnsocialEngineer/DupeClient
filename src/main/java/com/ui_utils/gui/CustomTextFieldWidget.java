package com.ui_utils.gui;

import com.dupeclient.client.gui.modern.ModernTextInputChrome;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

/** UI Utils container fields — styled like DupeClient hub inputs. */
public class CustomTextFieldWidget extends StylishTextFieldWidget {
    public CustomTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
        if (text != null) {
            setPlaceholder(text.getString());
        }
    }

    public static CustomTextFieldWidget create(TextRenderer textRenderer, int x, int y, int width, Text placeholder) {
        return create(textRenderer, x, y, width, ModernTextInputChrome.MIN_HEIGHT, placeholder);
    }

    public static CustomTextFieldWidget create(
            TextRenderer textRenderer, int x, int y, int width, int height, Text placeholder) {
        CustomTextFieldWidget field = new CustomTextFieldWidget(textRenderer, x, y, width, height, placeholder);
        field.setMaxLength(255);
        return field;
    }
}
