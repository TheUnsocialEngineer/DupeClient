package com.dupeclient.client.module.utility.nbtedit;

import com.dupeclient.client.gui.overlay.EditableTextBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Scrollable multiline SNBT editor with a high character limit (no vanilla TextField cap).
 */
public final class SnbtTextAreaWidget extends AbstractWidget {
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_CHARS = 262_144;

    private final StringBuilder text = new StringBuilder();
    private int cursor;
    private int scrollLine;

    public SnbtTextAreaWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public String text() {
        return text.toString();
    }

    public void setText(String value) {
        text.setLength(0);
        if (value != null) {
            text.append(trim(value));
        }
        cursor = text.length();
        scrollLine = 0;
    }

    public boolean handleKey(KeyEvent input) {
        if (!isFocused()) {
            return false;
        }
        int keyCode = input.key();
        int modifiers = EditableTextBuffer.liveModifiers();
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            cursor = text.length();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            copyAll();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            paste();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursor > 0) {
                text.deleteCharAt(cursor - 1);
                cursor--;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursor < text.length()) {
                text.deleteCharAt(cursor);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            insert('\n');
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            cursor = Math.max(0, cursor - 1);
            ensureCursorVisible();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            cursor = Math.min(text.length(), cursor + 1);
            ensureCursorVisible();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            moveVertical(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            moveVertical(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursor = startOfLine(lineOf(cursor));
            ensureCursorVisible();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            cursor = endOfLine(lineOf(cursor));
            ensureCursorVisible();
            return true;
        }
        return false;
    }

    public boolean handleChar(char chr) {
        if (!isFocused() || Character.isISOControl(chr)) {
            return false;
        }
        insert(chr);
        return true;
    }

    public boolean handleScroll(double verticalAmount) {
        if (!isFocused()) {
            return false;
        }
        int visibleLines = Math.max(1, (height - 6) / LINE_HEIGHT);
        int maxScroll = Math.max(0, lineCount() - visibleLines);
        if (verticalAmount > 0) {
            scrollLine = Math.max(0, scrollLine - 1);
        } else if (verticalAmount < 0) {
            scrollLine = Math.min(maxScroll, scrollLine + 1);
        }
        return true;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        Minecraft client = Minecraft.getInstance();
        Font tr = client.font;
        context.fill(getX(), getY(), getX() + width, getY() + height, 0xCC0F172A);
        context.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF334155);
        context.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFF334155);
        context.fill(getX(), getY(), getX() + 1, getY() + height, 0xFF334155);
        context.fill(getX() + width - 1, getY(), getX() + width, getY() + height, 0xFF334155);

        int visibleLines = Math.max(1, (height - 6) / LINE_HEIGHT);
        int lineCount = lineCount();
        scrollLine = Math.min(scrollLine, Math.max(0, lineCount - visibleLines));

        int drawY = getY() + 4;
        int cursorLine = lineOf(cursor);
        int cursorCol = columnOf(cursor);
        for (int i = 0; i < visibleLines; i++) {
            int lineIdx = scrollLine + i;
            if (lineIdx >= lineCount) {
                break;
            }
            String line = lineAt(lineIdx);
            context.text(tr, Component.literal(tr.plainSubstrByWidth(line, width - 10)), getX() + 5, drawY, 0xFFE2E8F0);
            drawY += LINE_HEIGHT;
        }

        if (isFocused()) {
            int caretY = getY() + 4 + (cursorLine - scrollLine) * LINE_HEIGHT;
            if (cursorLine >= scrollLine && cursorLine < scrollLine + visibleLines) {
                String before = lineAt(cursorLine).substring(0, Math.min(cursorCol, lineAt(cursorLine).length()));
                int caretX = getX() + 5 + tr.width(before);
                context.fill(caretX, caretY - 1, caretX + 1, caretY + 9, 0xFF4ADE80);
            }
        }
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        if (!visible || !active) {
            return;
        }
        double mouseX = click.x();
        double mouseY = click.y();
        boolean inside = mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
        if (inside && click.button() == 0) {
            setFocused(true);
            moveCursorToClick(mouseX, mouseY);
        } else if (!inside) {
            setFocused(false);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }

    private void insert(char ch) {
        if (text.length() >= MAX_CHARS) {
            return;
        }
        text.insert(cursor, ch);
        cursor++;
        ensureCursorVisible();
    }

    private void paste() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.keyboardHandler == null) {
            return;
        }
        String clip = client.keyboardHandler.getClipboard();
        if (clip == null || clip.isEmpty()) {
            return;
        }
        String cleaned = trim(clip.replace("\r", ""));
        int room = MAX_CHARS - text.length();
        if (room <= 0) {
            return;
        }
        if (cleaned.length() > room) {
            cleaned = cleaned.substring(0, room);
        }
        text.insert(cursor, cleaned);
        cursor += cleaned.length();
        ensureCursorVisible();
    }

    private void copyAll() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.keyboardHandler != null) {
            client.keyboardHandler.setClipboard(text.toString());
        }
    }

    private void moveCursorToClick(double mouseX, double mouseY) {
        Font tr = Minecraft.getInstance().font;
        int relY = (int) mouseY - (getY() + 4);
        int lineIdx = scrollLine + Math.max(0, relY / LINE_HEIGHT);
        lineIdx = Math.min(lineIdx, Math.max(0, lineCount() - 1));
        String line = lineAt(lineIdx);
        int relX = (int) mouseX - (getX() + 5);
        int best = line.length();
        for (int i = 0; i <= line.length(); i++) {
            if (tr.width(line.substring(0, i)) > relX) {
                best = Math.max(0, i - 1);
                break;
            }
        }
        cursor = startOfLine(lineIdx) + best;
        ensureCursorVisible();
    }

    private void moveVertical(int delta) {
        int line = lineOf(cursor);
        int col = columnOf(cursor);
        int nextLine = Math.max(0, Math.min(lineCount() - 1, line + delta));
        int nextCol = Math.min(col, lineAt(nextLine).length());
        cursor = startOfLine(nextLine) + nextCol;
        ensureCursorVisible();
    }

    private void ensureCursorVisible() {
        int line = lineOf(cursor);
        int visibleLines = Math.max(1, (height - 6) / LINE_HEIGHT);
        if (line < scrollLine) {
            scrollLine = line;
        } else if (line >= scrollLine + visibleLines) {
            scrollLine = line - visibleLines + 1;
        }
    }

    private int lineCount() {
        int count = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    private int lineOf(int index) {
        int line = 0;
        for (int i = 0; i < Math.min(index, text.length()); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private int columnOf(int index) {
        return index - startOfLine(lineOf(index));
    }

    private int startOfLine(int line) {
        int current = 0;
        int seen = 0;
        while (current < text.length() && seen < line) {
            if (text.charAt(current) == '\n') {
                seen++;
            }
            current++;
        }
        return current;
    }

    private int endOfLine(int line) {
        int start = startOfLine(line);
        int end = start;
        while (end < text.length() && text.charAt(end) != '\n') {
            end++;
        }
        return end;
    }

    private String lineAt(int line) {
        int start = startOfLine(line);
        int end = endOfLine(line);
        return text.substring(start, end);
    }

    private static String trim(String value) {
        if (value.length() <= MAX_CHARS) {
            return value;
        }
        return value.substring(0, MAX_CHARS);
    }
}
