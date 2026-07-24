package com.dupeclient.client.gui.overlay;

import com.dupeclient.client.gui.modern.ModernTextInputChrome;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Lightweight single-line text buffer with cursor, selection, and standard edit shortcuts.
 */
public final class EditableTextBuffer {
    private final StringBuilder text = new StringBuilder();
    private final int maxLength;
    private int cursor;
    private int selAnchor = -1;
    private int selExtent = -1;

    public EditableTextBuffer(int maxLength) {
        this.maxLength = Math.max(1, maxLength);
    }

    public String text() {
        return text.toString();
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    public int length() {
        return text.length();
    }

    public void clear() {
        text.setLength(0);
        cursor = 0;
        clearSelection();
    }

    public void setText(String value) {
        clear();
        if (value != null && !value.isEmpty()) {
            text.append(trimToMax(value));
            cursor = text.length();
        }
    }

    public void replaceAll(String value) {
        setText(value);
    }

    public boolean hasSelection() {
        return selAnchor >= 0 && selExtent >= 0 && selectionStart() != selectionEnd();
    }

    public int selectionStart() {
        if (selAnchor < 0 || selExtent < 0) {
            return cursor;
        }
        return Math.min(selAnchor, selExtent);
    }

    public int selectionEnd() {
        if (selAnchor < 0 || selExtent < 0) {
            return cursor;
        }
        return Math.max(selAnchor, selExtent);
    }

    public void selectAll() {
        selAnchor = 0;
        selExtent = text.length();
        cursor = text.length();
    }

    public void clearSelection() {
        selAnchor = -1;
        selExtent = -1;
    }

    public void collapseSelectionToCursor() {
        clearSelection();
    }

    public int cursor() {
        return cursor;
    }

    public void setCursor(int pos) {
        cursor = clampIndex(pos);
        clearSelection();
    }

    public void setCursorFromClick(Font tr, int clickX, int fieldX, int maxWidth) {
        int relX = clickX - fieldX;
        if (relX <= 0) {
            cursor = 0;
            clearSelection();
            return;
        }
        int best = text.length();
        for (int i = 0; i <= text.length(); i++) {
            int w = tr.width(text.substring(0, i));
            if (w > relX) {
                best = Math.max(0, i - 1);
                break;
            }
            if (i == text.length()) {
                best = text.length();
            }
        }
        cursor = best;
        clearSelection();
    }

    public boolean handleKey(int keyCode) {
        int mods = liveModifiers();
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0;

        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            selectAll();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            copySelection();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
            cutSelection();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            pasteClipboard();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            moveCursor(-1, shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            moveCursor(1, shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            setCursorWithSelection(0, shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            setCursorWithSelection(text.length(), shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursor > 0) {
                text.deleteCharAt(cursor - 1);
                cursor--;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursor < text.length()) {
                text.deleteCharAt(cursor);
            }
            return true;
        }
        return false;
    }

    public boolean handleCodePoint(int codePoint) {
        if (!Character.isValidCodePoint(codePoint) || Character.isISOControl(codePoint)) {
            return false;
        }
        if (codePoint < 32 || codePoint >= 127) {
            return false;
        }
        insertCodePoint((char) codePoint);
        return true;
    }

    public void insertCodePoint(char ch) {
        deleteSelection();
        if (text.length() >= maxLength) {
            return;
        }
        text.insert(cursor, ch);
        cursor++;
    }

    public void insert(String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        deleteSelection();
        String cleaned = trimToMax(value.replace("\r", "").replace("\n", " "));
        int room = maxLength - text.length();
        if (room <= 0) {
            return;
        }
        if (cleaned.length() > room) {
            cleaned = cleaned.substring(0, room);
        }
        text.insert(cursor, cleaned);
        cursor += cleaned.length();
    }

    public void draw(
            Font tr,
            GuiGraphics context,
            int x,
            int y,
            int maxWidth,
            int textColor,
            boolean focused) {
        String shown = text.toString();
        if (hasSelection() && focused) {
            int selStart = selectionStart();
            int selEnd = selectionEnd();
            String before = shown.substring(0, selStart);
            String selected = shown.substring(selStart, selEnd);
            int selX = x + tr.width(before);
            int selW = tr.width(selected);
            context.fill(selX - 1, y - 1, selX + selW + 1, y + 9, ModernTextInputChrome.SELECTION_COLOR);
        }
        context.drawString(tr, Component.literal(tr.plainSubstrByWidth(shown, maxWidth)), x, y, textColor);
        if (focused && ModernTextInputChrome.caretVisible()) {
            int caretX = x + tr.width(shown.substring(0, Math.min(cursor, shown.length())));
            context.fill(caretX, y - 1, caretX + 1, y + 9, ModernTextInputChrome.CARET_COLOR);
        }
    }

    private void moveCursor(int delta, boolean extendSelection) {
        int next = clampIndex(cursor + delta);
        if (extendSelection) {
            if (selAnchor < 0) {
                selAnchor = cursor;
            }
            cursor = next;
            selExtent = cursor;
        } else {
            cursor = next;
            clearSelection();
        }
    }

    private void setCursorWithSelection(int pos, boolean extendSelection) {
        int next = clampIndex(pos);
        if (extendSelection) {
            if (selAnchor < 0) {
                selAnchor = cursor;
            }
            cursor = next;
            selExtent = cursor;
        } else {
            cursor = next;
            clearSelection();
        }
    }

    private void deleteSelection() {
        if (!hasSelection()) {
            return;
        }
        int start = selectionStart();
        int end = selectionEnd();
        text.delete(start, end);
        cursor = start;
        clearSelection();
    }

    private void copySelection() {
        if (!hasSelection()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.keyboardHandler != null) {
            client.keyboardHandler.setClipboard(text.substring(selectionStart(), selectionEnd()));
        }
    }

    private void cutSelection() {
        copySelection();
        deleteSelection();
    }

    private void pasteClipboard() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.keyboardHandler == null) {
            return;
        }
        String clip = client.keyboardHandler.getClipboard();
        if (clip != null) {
            insert(clip);
        }
    }

    private String trimToMax(String value) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private int clampIndex(int index) {
        return Math.max(0, Math.min(text.length(), index));
    }

    public static int liveModifiers() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return 0;
        }
        long window = client.getWindow().handle();
        int mods = 0;
        if (pressed(window, GLFW.GLFW_KEY_LEFT_CONTROL) || pressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            mods |= GLFW.GLFW_MOD_CONTROL;
        }
        if (pressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) || pressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            mods |= GLFW.GLFW_MOD_SHIFT;
        }
        if (pressed(window, GLFW.GLFW_KEY_LEFT_ALT) || pressed(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
            mods |= GLFW.GLFW_MOD_ALT;
        }
        return mods;
    }

    public static boolean ctrlDown() {
        return (liveModifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    public static boolean shiftDown() {
        return (liveModifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    private static boolean pressed(long window, int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }
}
