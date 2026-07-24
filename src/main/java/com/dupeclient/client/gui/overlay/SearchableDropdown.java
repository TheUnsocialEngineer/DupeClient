package com.dupeclient.client.gui.overlay;

import com.dupeclient.client.gui.modern.ModernTextInputChrome;
import com.dupeclient.client.gui.overlay.EditableTextBuffer;
import com.dupeclient.client.gui.modern.theme.MidnightPalette;
import com.dupeclient.client.gui.modern.theme.MidnightShapes;
import com.dupeclient.client.module.packet.sniffer.PacketNameSearch;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Inline searchable combobox: closed field, optional search row, and scrollable results.
 */
public final class SearchableDropdown {
    public static final int SEARCH_H = 14;
    public static final int ROW_H = 11;
    public static final int AVATAR_ROW_H = 16;
    public static final int DEFAULT_VISIBLE = 6;
    private static final int AVATAR = 12;

    private final String placeholder;
    private final int visibleRows;

    private boolean allowCustomEntry;
    private boolean open;
    private boolean searchFocused;
    private boolean modernChrome;
    private boolean showPlayerAvatars;
    private final EditableTextBuffer search = new EditableTextBuffer(64);
    private int scroll;
    private String displayValue = "";

    private int fieldX;
    private int fieldY;
    private int fieldW;
    private int fieldH;
    private int searchX;
    private int searchY;
    private int searchW;
    private int listX;
    private int listY;
    private int listW;
    private int listH;

    public SearchableDropdown(String placeholder) {
        this(placeholder, DEFAULT_VISIBLE);
    }

    public SearchableDropdown(String placeholder, int visibleRows) {
        this.placeholder = placeholder == null ? "" : placeholder;
        this.visibleRows = Math.max(1, visibleRows);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean hasTextFocus() {
        return open && searchFocused;
    }

    public void setDisplayValue(String value) {
        displayValue = value == null ? "" : value;
    }

    public String displayValue() {
        return displayValue;
    }

    public void close() {
        open = false;
        searchFocused = false;
        search.clear();
        scroll = 0;
    }

    public void open() {
        open = true;
        searchFocused = true;
        scroll = 0;
    }

    public void setAllowCustomEntry(boolean allowCustomEntry) {
        this.allowCustomEntry = allowCustomEntry;
    }

    public void setModernChrome(boolean modernChrome) {
        this.modernChrome = modernChrome;
    }

    public void setShowPlayerAvatars(boolean showPlayerAvatars) {
        this.showPlayerAvatars = showPlayerAvatars;
    }

    private int rowH() {
        return showPlayerAvatars ? AVATAR_ROW_H : ROW_H;
    }

    public int extraHeight() {
        return open ? SEARCH_H + 4 + visibleRows * rowH() + 4 : 0;
    }

    public void render(
            GuiGraphicsExtractor context,
            Font tr,
            int x,
            int y,
            int w,
            int h,
            List<String> options,
            double mouseX,
            double mouseY) {
        fieldX = x;
        fieldY = y;
        fieldW = w;
        fieldH = h;

        String shown = displayValue.isBlank() ? placeholder : displayValue;
        drawField(context, tr, x, y, w, h, shown, open || searchFocused);
        String chevron = open ? "⌄" : "›";
        int chevronX = x + w - tr.width(chevron) - 6;
        context.text(tr, Component.literal(chevron), chevronX, y + (h - 8) / 2, MidnightPalette.TEXT_MUTED);

        if (open) {
            // Keep hitboxes current even before popup paint (scroll/click between frames).
            layoutPopupBounds();
        } else {
            searchW = 0;
            listW = 0;
            listH = 0;
        }
    }

    private void layoutPopupBounds() {
        int rh = rowH();
        searchX = fieldX;
        searchY = fieldY + fieldH + 3;
        searchW = fieldW;
        listX = fieldX;
        listY = searchY + SEARCH_H + 3;
        listW = fieldW;
        listH = visibleRows * rh;
    }

    /** Draws only the expanded search + list (call after the rest of the panel so it paints on top). */
    public void renderPopupLayer(
            GuiGraphicsExtractor context,
            Font tr,
            List<String> options,
            double mouseX,
            double mouseY) {
        if (open) {
            renderPopup(context, tr, options, mouseX, mouseY);
        }
    }

    private void renderPopup(
            GuiGraphicsExtractor context,
            Font tr,
            List<String> options,
            double mouseX,
            double mouseY) {
        layoutPopupBounds();
        String searchShown = search.isEmpty() && !searchFocused ? "Search…" : search.text();
        if (search.isEmpty() && !searchFocused) {
            drawField(context, tr, searchX, searchY, searchW, SEARCH_H, searchShown, searchFocused);
        } else {
            ModernTextInputChrome.drawField(context, searchX, searchY, searchW, SEARCH_H, searchFocused);
            int textX = searchX + ModernTextInputChrome.PAD_X;
            int textY = ModernTextInputChrome.textY(searchY, SEARCH_H);
            search.draw(tr, context, textX, textY, searchW - ModernTextInputChrome.PAD_X * 2, ModernTextInputChrome.TEXT_COLOR, searchFocused);
        }

        int rh = rowH();
        List<String> filtered = filter(options, search.text());
        int maxScroll = Math.max(0, filtered.size() - visibleRows);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        int rr = modernChrome ? MidnightShapes.controlRadius(listH) : 0;
        if (modernChrome) {
            MidnightShapes.fillRoundedFrame(
                    context, listX, listY, listW, listH, rr, 0xF70B1220, MidnightPalette.BORDER_LIGHT);
        } else {
            drawSquareBox(context, listX, listY, listW, listH, 0xF00F172B, 0xFF3A4A5E);
        }

        Minecraft client = Minecraft.getInstance();
        for (int row = 0; row < visibleRows; row++) {
            int idx = scroll + row;
            if (idx >= filtered.size()) {
                continue;
            }
            String name = filtered.get(idx);
            int rowY = listY + row * rh;
            boolean hot = mouseX >= listX && mouseX < listX + listW
                    && mouseY >= rowY && mouseY < rowY + rh;
            if (hot) {
                if (modernChrome) {
                    MidnightShapes.fillRoundedRect(
                            context, listX + 2, rowY + 1, listW - 4, rh - 2, 3, 0x553B82F6);
                } else {
                    context.fill(listX + 1, rowY + 1, listX + listW - 1, rowY + rh - 1, 0x553F3F46);
                }
            }

            int textX = listX + 5;
            if (showPlayerAvatars) {
                PlayerSkin skin = skinForName(client, name);
                int ay = rowY + (rh - AVATAR) / 2;
                if (skin != null) {
                    PlayerFaceExtractor.extractRenderState(context, skin, textX, ay, AVATAR);
                }
                textX += AVATAR + 4;
            }
            int maxText = Math.max(4, listX + listW - textX - 6);
            String rowText = tr.plainSubstrByWidth(name, maxText);
            context.text(
                    tr, Component.literal(rowText), textX, rowY + (rh - 8) / 2, MidnightPalette.TEXT_PRIMARY);
        }

        if (maxScroll > 0 && modernChrome) {
            int trackH = listH - 6;
            int thumbH = Math.max(10, trackH * visibleRows / Math.max(1, filtered.size()));
            int thumbY = listY + 3 + (trackH - thumbH) * scroll / Math.max(1, maxScroll);
            MidnightShapes.fillRoundedRect(context, listX + listW - 5, thumbY, 3, thumbH, 1, MidnightPalette.GREEN);
        }
    }

    /** Hit test for the field and open popup (may extend outside the parent panel). */
    public boolean hitsInteractive(double mouseX, double mouseY) {
        if (rect(mouseX, mouseY, fieldX, fieldY, fieldW, fieldH)) {
            return true;
        }
        if (!open) {
            return false;
        }
        return rect(mouseX, mouseY, searchX, searchY, searchW, SEARCH_H)
                || rect(mouseX, mouseY, listX, listY, listW, listH);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, List<String> options, Consumer<String> onSelect) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (rect(mouseX, mouseY, fieldX, fieldY, fieldW, fieldH)) {
            if (open) {
                close();
            } else {
                open();
            }
            return true;
        }
        if (!open) {
            return false;
        }
        if (rect(mouseX, mouseY, searchX, searchY, searchW, SEARCH_H)) {
            searchFocused = true;
            Font tr = Minecraft.getInstance().font;
            search.setCursorFromClick(tr, (int) mouseX, searchX + ModernTextInputChrome.PAD_X, searchW - ModernTextInputChrome.PAD_X * 2);
            return true;
        }
        if (rect(mouseX, mouseY, listX, listY, listW, listH)) {
            searchFocused = false;
            int rh = rowH();
            int row = (int) ((mouseY - listY) / rh);
            if (row >= 0 && row < visibleRows) {
                List<String> filtered = filter(options, search.text());
                int idx = scroll + row;
                if (idx >= 0 && idx < filtered.size()) {
                    if (onSelect != null) {
                        onSelect.accept(filtered.get(idx));
                    }
                    close();
                }
            }
            return true;
        }
        close();
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount, List<String> options) {
        if (!open) {
            return false;
        }
        if (listH <= 0) {
            layoutPopupBounds();
        }
        if (!hitsInteractive(mouseX, mouseY)) {
            return false;
        }
        return applyScroll(verticalAmount, options);
    }

    /** Scroll while open regardless of pointer position (panel-wide wheel absorption). */
    public boolean scrollOpenList(double verticalAmount, List<String> options) {
        if (!open) {
            return false;
        }
        return applyScroll(verticalAmount, options);
    }

    private boolean applyScroll(double verticalAmount, List<String> options) {
        if (verticalAmount == 0.0) {
            return false;
        }
        List<String> filtered = filter(options, search.text());
        int maxScroll = Math.max(0, filtered.size() - visibleRows);
        // At least one row per notch; scale up for trackpads / fast wheels.
        int lines = Math.max(1, (int) Math.ceil(Math.abs(verticalAmount)));
        int delta = verticalAmount > 0 ? -lines : lines;
        int next = Math.max(0, Math.min(maxScroll, scroll + delta));
        if (next == scroll) {
            return true; // still consume so the game / host screen doesn't steal the wheel
        }
        scroll = next;
        return true;
    }

    public boolean keyPressed(int keyCode) {
        return keyPressed(keyCode, null);
    }

    public boolean keyPressed(int keyCode, Consumer<String> onCustom) {
        if (!open || !searchFocused) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER && allowCustomEntry && !search.isEmpty() && onCustom != null) {
            onCustom.accept(search.text().trim());
            close();
            return true;
        }
        return search.handleKey(keyCode);
    }

    public boolean charTyped(int codePoint) {
        if (!open || !searchFocused) {
            return false;
        }
        if (search.handleCodePoint(codePoint)) {
            scroll = 0;
            return true;
        }
        return false;
    }

    private static List<String> filter(List<String> names, String query) {
        return new ArrayList<>(PacketNameSearch.filter(names, query));
    }

    private void drawField(
            GuiGraphicsExtractor context, Font tr, int x, int y, int w, int h, String text, boolean focused) {
        if (modernChrome) {
            ModernTextInputChrome.drawField(context, x, y, w, h, focused);
            String shown = tr.plainSubstrByWidth(text == null ? "" : text, Math.max(4, w - 20));
            context.text(tr, Component.literal(shown), x + ModernTextInputChrome.PAD_X, ModernTextInputChrome.textY(y, h), ModernTextInputChrome.TEXT_COLOR);
            return;
        }
        int bg = 0xFF18181F;
        int border = focused ? 0xFF6A9EFF : 0xFF3A4A5E;
        drawSquareBox(context, x, y, w, h, bg, border);
        String shown = tr.plainSubstrByWidth(text == null ? "" : text, Math.max(4, w - 14));
        context.text(tr, Component.literal(shown), x + 5, y + (h - 8) / 2, 0xFFE5E7EB);
    }

    private static void drawSquareBox(GuiGraphicsExtractor context, int x, int y, int w, int h, int fill, int border) {
        context.fill(x, y, x + w, y + h, fill);
        context.fill(x, y, x + w, y + 1, border);
        context.fill(x, y + h - 1, x + w, y + h, border);
        context.fill(x, y, x + 1, y + h, border);
        context.fill(x + w - 1, y, x + w, y + h, border);
    }

    private static boolean rect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Nullable
    private static PlayerSkin skinForName(Minecraft client, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        if (client != null && client.getConnection() != null) {
            for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {
                if (entry == null || entry.getProfile() == null) {
                    continue;
                }
                String entryName = entry.getProfile().name();
                if (entryName != null && entryName.equalsIgnoreCase(name)) {
                    return entry.getSkin();
                }
            }
        }
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name.toLowerCase(Locale.ROOT))
                .getBytes(StandardCharsets.UTF_8));
        return DefaultPlayerSkin.get(uuid);
    }
}
