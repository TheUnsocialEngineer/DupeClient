package com.ui_utils.gui;

import com.dupeclient.client.gui.modern.ModernTextInputChrome;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** UI Utils container fields — styled like DupeClient hub inputs. */
public class CustomTextFieldWidget extends StylishTextFieldWidget {
    public CustomTextFieldWidget(Font textRenderer, int x, int y, int width, int height, Component text) {
        super(textRenderer, x, y, width, height, text);
        if (text != null) {
            setPlaceholder(text.getString());
        }
    }

    public static CustomTextFieldWidget create(Font textRenderer, int x, int y, int width, Component placeholder) {
        return create(textRenderer, x, y, width, ModernTextInputChrome.MIN_HEIGHT, placeholder);
    }

    public static CustomTextFieldWidget create(
            Font textRenderer, int x, int y, int width, int height, Component placeholder) {
        CustomTextFieldWidget field = new CustomTextFieldWidget(textRenderer, x, y, width, height, placeholder);
        field.setMaxLength(255);
        return field;
    }
}
