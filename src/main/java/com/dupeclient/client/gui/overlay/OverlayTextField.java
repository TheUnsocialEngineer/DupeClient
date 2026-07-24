package com.dupeclient.client.gui.overlay;

import org.lwjgl.glfw.GLFW;

import java.util.function.IntPredicate;

/** Single-line overlay input with cursor, selection, and Ctrl+A/C/V/X shortcuts. */
public final class OverlayTextField {
    private final EditableTextBuffer buffer;
    private final IntPredicate charFilter;

    private OverlayTextField(int maxLength, IntPredicate charFilter) {
        this.buffer = new EditableTextBuffer(maxLength);
        this.charFilter = charFilter;
    }

    public static OverlayTextField create(int maxLength) {
        return new OverlayTextField(maxLength, codePoint -> codePoint >= 32 && codePoint < 127);
    }

    public static OverlayTextField digits(int maxLength) {
        return new OverlayTextField(maxLength, codePoint -> codePoint >= '0' && codePoint <= '9');
    }

    public String text() {
        return buffer.text();
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public void setText(String value) {
        buffer.setText(value == null ? "" : value);
    }

    public void clear() {
        buffer.clear();
    }

    /** @return true if the key was consumed */
    public boolean keyPressed(int keyCode) {
        return keyPressed(keyCode, null);
    }

    /** @return true if the key was consumed */
    public boolean keyPressed(int keyCode, Runnable onEnter) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (onEnter != null) {
                onEnter.run();
            }
            return true;
        }
        return buffer.handleKey(keyCode);
    }

    /** @return true if the character was consumed */
    public boolean charTyped(int codePoint) {
        if (!Character.isValidCodePoint(codePoint) || Character.isISOControl(codePoint)) {
            return false;
        }
        if (codePoint < 32 || codePoint >= 127) {
            return false;
        }
        if (!charFilter.test(codePoint)) {
            return true;
        }
        return buffer.handleCodePoint((char) codePoint);
    }
}
