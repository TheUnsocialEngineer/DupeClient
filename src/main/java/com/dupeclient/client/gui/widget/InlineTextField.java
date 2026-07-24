package com.dupeclient.client.gui.widget;

import com.dupeclient.client.gui.modern.ModernTextInputChrome;
import com.dupeclient.client.gui.overlay.EditableTextBuffer;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntPredicate;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Single-line text field for panels and overlays — cursor, selection, clipboard, modern chrome.
 */
public final class InlineTextField {
    private final EditableTextBuffer buffer;
    private String placeholder = "";
    private boolean focused;
    private int x;
    private int y;
    private int w = 120;
    private int h = ModernTextInputChrome.MIN_HEIGHT;
    private IntPredicate charFilter = codePoint -> true;

    public InlineTextField(int maxLength) {
        this.buffer = new EditableTextBuffer(maxLength);
    }

    public String getText() {
        return buffer.text();
    }

    public void setText(String value) {
        buffer.setText(value);
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
    }

    public void setCharFilter(IntPredicate charFilter) {
        this.charFilter = charFilter == null ? codePoint -> true : charFilter;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(24, w);
        this.h = Math.max(ModernTextInputChrome.MIN_HEIGHT, h);
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) {
            buffer.clearSelection();
        }
    }

    public void blur() {
        setFocused(false);
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (contains(mouseX, mouseY)) {
            focused = true;
            Font tr = net.minecraft.client.Minecraft.getInstance().font;
            buffer.setCursorFromClick(tr, (int) mouseX, x + ModernTextInputChrome.PAD_X, w - ModernTextInputChrome.PAD_X * 2);
            return true;
        }
        if (focused) {
            focused = false;
            buffer.clearSelection();
        }
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (!focused) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            blur();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            blur();
            return true;
        }
        return buffer.handleKey(keyCode);
    }

    public boolean charTyped(int codePoint) {
        if (!focused || !Character.isValidCodePoint(codePoint) || Character.isISOControl(codePoint)) {
            return false;
        }
        if (codePoint < 32 || codePoint >= 127) {
            return false;
        }
        if (!charFilter.test(codePoint)) {
            return false;
        }
        return buffer.handleCodePoint((char) codePoint);
    }

    public void render(GuiGraphicsExtractor context, Font tr) {
        ModernTextInputChrome.drawField(context, x, y, w, h, focused);
        int textX = x + ModernTextInputChrome.PAD_X;
        int textY = ModernTextInputChrome.textY(y, h);
        int textMaxW = Math.max(4, w - ModernTextInputChrome.PAD_X * 2);
        if (buffer.isEmpty() && !focused && !placeholder.isEmpty()) {
            ModernTextInputChrome.drawPlaceholder(tr, context, textX, textY, textMaxW, placeholder);
            return;
        }
        buffer.draw(tr, context, textX, textY, textMaxW, ModernTextInputChrome.TEXT_COLOR, focused);
    }
}
