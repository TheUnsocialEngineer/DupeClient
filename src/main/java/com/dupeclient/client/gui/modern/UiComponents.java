package com.dupeclient.client.gui.modern;

import com.dupeclient.client.gui.modern.ModernTextInputChrome;
import com.dupeclient.client.gui.modern.theme.MidnightPalette;
import com.dupeclient.client.gui.modern.theme.MidnightShapes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;

/**
 * Composable UI primitives — modern card layout (Tailwind / CSS-like spacing and radii).
 */
public final class UiComponents {
    public enum PillActionStyle {
        PRIMARY_MINT,
        PRIMARY_BLUE,
        SECONDARY_SLATE
    }

    private UiComponents() {
    }

    private static int lerpArgb(int c0, int c1, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int a0 = (c0 >>> 24) & 0xFF;
        int r0 = (c0 >> 16) & 0xFF;
        int g0 = (c0 >> 8) & 0xFF;
        int b0 = c0 & 0xFF;
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a = (int) (a0 + (a1 - a0) * t);
        int r = (int) (r0 + (r1 - r0) * t);
        int g = (int) (g0 + (g1 - g0) * t);
        int b = (int) (b0 + (b1 - b0) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blendSidebarSelected() {
        return lerpArgb(MidnightPalette.SIDEBAR_ACTIVE_L, MidnightPalette.SIDEBAR_ACTIVE_R, 0.45f);
    }

    public static void drawSlotField(DrawContext c, int x, int y, int w, int h, int fill, int border) {
        int rr = MidnightShapes.controlRadius(h);
        MidnightShapes.fillRoundedFrame(c, x, y, w, h, rr, fill, border);
    }

    public static void drawSection(TextRenderer tr, DrawContext c, int x, int y, int w, int h, String title, boolean collapsed) {
        int rr = MidnightShapes.surfaceRadius(w, h);
        UiDraw.cardElevated(c, x, y, w, h, rr);
        int headerH = UiTokens.CARD_CONTENT_TOP - 4;
        c.fill(x + 1, y + headerH, x + w - 1, y + headerH + 1, MidnightPalette.BORDER_LIGHT);
        c.drawTextWithShadow(tr, Text.literal(title), x + UiTokens.SP_4, y + UiTokens.SP_3 + 2, MidnightPalette.TEXT_PRIMARY);
        String chevron = collapsed ? "›" : "⌄";
        c.drawTextWithShadow(tr, Text.literal(chevron), x + w - UiTokens.SP_4 - 4, y + UiTokens.SP_3 + 2, MidnightPalette.TEXT_MUTED);
    }

    public static void drawSectionCard(
            TextRenderer tr, DrawContext c, int x, int y, int w, int h, String title, boolean sectionCollapsed) {
        drawSection(tr, c, x, y, w, h, title, sectionCollapsed);
    }

    public static void drawDropdownRow(TextRenderer tr, DrawContext c, int x, int y, int w, int h, String label) {
        int rr = MidnightShapes.controlRadius(h);
        int fill = lerpArgb(MidnightPalette.CHROME_BOT, MidnightPalette.CHROME_TOP, 0.35f);
        MidnightShapes.fillRoundedFrame(c, x, y, w, h, rr, fill, MidnightPalette.BORDER_LIGHT);
        c.drawTextWithShadow(tr, Text.literal(label), x + UiTokens.SP_3, y + (h - 8) / 2, MidnightPalette.TEXT_PRIMARY);
        c.drawTextWithShadow(tr, Text.literal("⌄"), x + w - UiTokens.SP_3 - 4, y + (h - 8) / 2, MidnightPalette.TEXT_MUTED);
    }

    public static void drawInfoCard(TextRenderer tr, DrawContext c, int x, int y, int w, int h, String title) {
        if (title == null || title.isBlank()) {
            drawSurfaceCard(c, x, y, w, h);
        } else {
            drawSection(tr, c, x, y, w, h, title.trim(), false);
        }
    }

    /** First control row Y inside a titled {@link #drawInfoCard} (below the title band). */
    public static int titledCardBodyY(int cardTop) {
        return cardTop + UiTokens.CARD_CONTENT_TOP;
    }

    /** Card without a title band — hub panels already show the module name in the sidebar. */
    public static void drawSurfaceCard(DrawContext c, int x, int y, int w, int h) {
        UiDraw.cardElevated(c, x, y, w, h, MidnightShapes.surfaceRadius(w, h));
    }

    public static void drawNavSectionLabel(TextRenderer tr, DrawContext c, int x, int y, int w, String label) {
        c.drawTextWithShadow(tr, Text.literal(label.toUpperCase(Locale.ROOT)), x + UiTokens.SP_2, y, UiTokens.MINT_300);
        c.fill(x, y + 10, x + w - UiTokens.SP_2, y + 11, UiTokens.argb(0x88, UiTokens.SLATE_600));
    }

    public static void drawOptionToggle(TextRenderer tr, DrawContext c, int x, int y, int rowW, String label, boolean enabled) {
        drawOptionToggle(tr, c, x, y, rowW, label, enabled, enabled ? 1f : 0f);
    }

    public static void drawOptionToggle(
            TextRenderer tr, DrawContext c, int x, int y, int rowW, String label, boolean enabled, float knobT) {
        int tw = UiTokens.TOGGLE_TRACK_W;
        int th = UiTokens.TOGGLE_TRACK_H;
        int tx = x + rowW - tw;
        int ty = y + (UiTokens.ROW_STEP - th) / 2;
        float t = MathHelper.clamp(knobT, 0f, 1f);
        float colorT = Math.round(t * 31f) / 31f;
        int track = lerpArgb(MidnightPalette.TOGGLE_OFF, MidnightPalette.GREEN, colorT);
        MidnightShapes.fillRoundedRect(c, tx, ty, tw, th, MidnightShapes.pillRadius(th), track);

        int knobR = 6;
        float travel = tw - knobR * 2 - 4;
        int knobCx = (int) (tx + knobR + 2 + travel * t + 0.5f);
        int knobCy = ty + th / 2;
        MidnightShapes.fillDisk(c, knobCx, knobCy, knobR, 0xFFF4F4F5);

        int labelMax = Math.max(8, rowW - tw - UiTokens.SP_4);
        String shown = tr.trimToWidth(label, labelMax);
        c.drawTextWithShadow(tr, Text.literal(shown), x, y + (UiTokens.ROW_STEP - 8) / 2, MidnightPalette.TEXT_PRIMARY);
    }

    public static void drawPillKeybind(
            TextRenderer tr, DrawContext c, int x, int y, int w, int rowH, String label, String value, boolean hot) {
        drawPillKeybindEx(tr, c, x, y, w, rowH, label, value, hot, 98);
    }

    public static void drawPillKeybindEx(
            TextRenderer tr, DrawContext c, int x, int y, int w, int rowH, String label, String value, boolean hot) {
        drawPillKeybindEx(tr, c, x, y, w, rowH, label, value, hot, 98);
    }

    public static void drawPillKeybindEx(
            TextRenderer tr, DrawContext c, int x, int y, int w, int rowH, String label, String value, boolean hot, int bindW) {
        int labelW = w - bindW - UiTokens.SP_2;
        String shown = tr.trimToWidth(label, Math.max(8, labelW));
        c.drawTextWithShadow(tr, Text.literal(shown), x, y + (rowH - 8) / 2, MidnightPalette.TEXT_SECONDARY);
        int bx = x + labelW + UiTokens.SP_2;
        int fill = hot ? MidnightPalette.PANEL_FILL_RAISED : MidnightPalette.PANEL_FILL;
        int edge = hot ? MidnightPalette.BORDER_FOCUS : MidnightPalette.BORDER_LIGHT;
        drawSlotField(c, bx, y, bindW, rowH, fill, edge);
        c.drawCenteredTextWithShadow(tr, Text.literal(value), bx + bindW / 2, y + (rowH - 8) / 2, MidnightPalette.TEXT_PRIMARY);
    }

    public static void drawPillActionButton(TextRenderer tr, DrawContext c, int x, int y, int w, int h, String label) {
        drawPillActionButton(tr, c, x, y, w, h, label, PillActionStyle.PRIMARY_MINT);
    }

    public static void drawPillActionButton(
            TextRenderer tr, DrawContext c, int x, int y, int w, int h, String label, PillActionStyle style) {
        int rr = MidnightShapes.controlRadius(h);
        switch (style) {
            case PRIMARY_BLUE -> {
                int fill = lerpArgb(MidnightPalette.BLUE_L, MidnightPalette.BLUE_R, 0.42f);
                MidnightShapes.fillRoundedFrame(c, x, y, w, h, rr, fill, lerpArgb(MidnightPalette.BLUE_L, 0x55FFFFFF, 0.25f));
            }
            case SECONDARY_SLATE -> {
                MidnightShapes.fillRoundedFrame(
                        c, x, y, w, h, rr,
                        MidnightPalette.PANEL_FILL_RAISED,
                        MidnightPalette.BORDER_LIGHT);
            }
            default -> {
                int fill = lerpArgb(MidnightPalette.GREEN, MidnightPalette.GREEN_DIM, 0.35f);
                MidnightShapes.fillRoundedFrame(c, x, y, w, h, rr, fill, lerpArgb(MidnightPalette.GREEN, 0x44FFFFFF, 0.2f));
            }
        }
        c.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + (h - 8) / 2, MidnightPalette.TEXT_PRIMARY);
    }

    public static void drawSegmentTab(TextRenderer tr, DrawContext c, int bx, int by, int tw, int th, String tabLabel, boolean active) {
        int rr = MidnightShapes.controlRadius(th);
        if (active) {
            int fill = lerpArgb(MidnightPalette.BLUE_L, MidnightPalette.BLUE_R, 0.42f);
            MidnightShapes.fillRoundedFrame(c, bx, by, tw, th, rr, fill, MidnightPalette.alphaRgb(0x55, 0x3B82F6));
            c.drawCenteredTextWithShadow(tr, Text.literal(tabLabel), bx + tw / 2, by + (th - 8) / 2, MidnightPalette.TEXT_PRIMARY);
        } else {
            MidnightShapes.fillRoundedFrame(c, bx, by, tw, th, rr, MidnightPalette.PANEL_FILL, MidnightPalette.BORDER_LIGHT);
            c.drawCenteredTextWithShadow(tr, Text.literal(tabLabel), bx + tw / 2, by + (th - 8) / 2, MidnightPalette.TEXT_MUTED);
        }
    }

    public static void drawValueSlider(
            TextRenderer tr,
            DrawContext c,
            int x,
            int y,
            int w,
            double t,
            String textLabel,
            String valueText,
            boolean active) {
        t = MathHelper.clamp(t, 0.0, 1.0);
        int barX = x + 76;
        int valueW = 36;
        int barW = w - 82 - valueW;
        c.drawTextWithShadow(tr, Text.literal(textLabel), x, y + 1, MidnightPalette.TEXT_SECONDARY);
        int track = MidnightPalette.CHROME_BOT;
        MidnightShapes.fillRoundedRect(c, barX, y + 3, barW, 6, 3, track);
        int fillW = (int) ((barW - 2) * t);
        if (fillW > 0) {
            MidnightShapes.fillRoundedRect(c, barX + 1, y + 4, fillW, 4, 2, MidnightPalette.GREEN);
        }
        int knobCx = barX + (int) (barW * t);
        int knobCy = y + 6;
        MidnightShapes.fillDisk(c, knobCx, knobCy, 5, active ? MidnightPalette.GREEN : MidnightPalette.TOGGLE_OFF);
        int tw0 = tr.getWidth(valueText);
        c.drawTextWithShadow(tr, Text.literal(valueText), barX + barW + valueW - tw0, y + 1, MidnightPalette.PATH_GREEN);
    }

    public static void drawAccentLabel(TextRenderer tr, DrawContext c, int x, int y, String text) {
        c.drawTextWithShadow(tr, Text.literal(text), x, y, MidnightPalette.PATH_GREEN);
    }

    public static void drawTextField(TextRenderer tr, DrawContext c, int x, int y, int w, int h, String value, boolean focused) {
        ModernTextInputChrome.drawField(c, x, y, w, h, focused);
        String v = value == null ? "" : value;
        int textX = x + ModernTextInputChrome.PAD_X;
        int textY = ModernTextInputChrome.textY(y, h);
        String shown = tr.trimToWidth(v, Math.max(4, w - ModernTextInputChrome.PAD_X * 2));
        c.drawTextWithShadow(tr, Text.literal(shown), textX, textY, MidnightPalette.TEXT_PRIMARY);
        if (focused && ModernTextInputChrome.caretVisible()) {
            int caretX = textX + tr.getWidth(shown);
            c.fill(caretX, textY - 1, caretX + 1, textY + 9, ModernTextInputChrome.CARET_COLOR);
        }
    }

    public static void drawInlineTextField(TextRenderer tr, DrawContext c, com.dupeclient.client.gui.widget.InlineTextField field) {
        field.render(c, tr);
    }

    public static void drawLabeledValueSlider(
            TextRenderer tr,
            DrawContext c,
            int x,
            int y,
            int w,
            double value,
            double min,
            double max,
            String label,
            int labelOffsetX,
            int valueBoxW,
            boolean active) {
        drawLabeledValueSlider(
                tr, c, x, y, w, value, min, max, label, labelOffsetX, valueBoxW, active,
                String.valueOf((int) Math.round(value)));
    }

    public static void drawLabeledValueSlider(
            TextRenderer tr,
            DrawContext c,
            int x,
            int y,
            int w,
            double value,
            double min,
            double max,
            String label,
            int labelOffsetX,
            int valueBoxW,
            boolean active,
            String valueDisplay) {
        double t = (value - min) / (max - min);
        drawValueSlider(tr, c, x, y, w, MathHelper.clamp(t, 0.0, 1.0), label, valueDisplay, active);
    }

    public static void drawListRowBack(DrawContext c, int x, int y, int w, int h, boolean selected) {
        int fill = selected ? MidnightPalette.alphaRgb(0x55, 0x22C55E) : MidnightPalette.alphaRgb(0x35, 0x18181B);
        MidnightShapes.fillRoundedRect(c, x, y, w, h, MidnightShapes.controlRadius(h), fill);
    }

    public static void drawNavItem(
            TextRenderer tr, DrawContext c, int x, int y, int w, int h, String label, boolean selected, boolean hovered) {
        int r = MidnightShapes.controlRadius(h);
        if (selected) {
            MidnightShapes.fillRoundedFrame(
                    c, x, y, w, h, r, blendSidebarSelected(), MidnightPalette.alphaRgb(0x66, 0x52525B));
        } else {
            int inner = hovered ? MidnightPalette.PANEL_FILL_RAISED : MidnightPalette.SIDEBAR_IDLE;
            MidnightShapes.fillRoundedFrame(c, x, y, w, h, r, inner, MidnightPalette.SIDEBAR_IDLE_BORDER);
        }
        int twLabel = tr.getWidth(label);
        int lx = x + (w - twLabel) / 2;
        int col = selected ? MidnightPalette.TEXT_PRIMARY : MidnightPalette.TEXT_SECONDARY;
        c.drawTextWithShadow(tr, Text.literal(label), lx, y + (h - 8) / 2, col);
    }

    public static void drawNavPill(
            TextRenderer tr, DrawContext c, int x, int y, int w, int h, String label, boolean selected, boolean hovered) {
        drawNavItem(tr, c, x, y, w, h, label, selected, hovered);
    }

    /** Compact setting row height used by packet fabricator / sniffer options panels. */
    public static final int FAB_SET_ROW = 12;
    public static final int FAB_VALUE_W = 72;
    public static final int FAB_BTN_H = 16;

    /** Fabricator-style label + ON/OFF chip (right-aligned). */
    public static void drawFabricatorSettingToggle(
            TextRenderer tr, DrawContext c, int x, int y, int w, String label, boolean on) {
        c.drawTextWithShadow(tr, Text.literal(label), x, y + 2, 0xFFA1A1AA);
        String state = on ? "ON" : "OFF";
        int sw = tr.getWidth(state) + 10;
        int sx = x + w - sw;
        int color = on ? 0xFF34D399 : 0xFF52525B;
        c.fill(sx, y, x + w, y + FAB_SET_ROW, 0xFF27272A);
        c.drawTextWithShadow(tr, Text.literal(state), sx + 5, y + 2, color);
    }

    /** Fabricator-style label + clickable value box (right-aligned). */
    public static void drawFabricatorSettingValue(
            TextRenderer tr, DrawContext c, int x, int y, int w, String label, String value) {
        c.drawTextWithShadow(tr, Text.literal(label), x, y + 2, 0xFFA1A1AA);
        int vx = x + w - FAB_VALUE_W;
        c.fill(vx, y, x + w, y + FAB_SET_ROW, 0xFF27272A);
        String shown = tr.trimToWidth(value == null ? "" : value, FAB_VALUE_W - 8);
        c.drawTextWithShadow(tr, Text.literal(shown), vx + 4, y + 2, 0xFFE5E5E5);
    }

    /** Fabricator-style flat action button. */
    public static void drawFabricatorButton(
            TextRenderer tr, DrawContext c, int x, int y, int w, int h, String label, int mouseX, int mouseY, boolean enabled) {
        boolean hover = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int bg = !enabled ? 0xFF1E1E22 : (hover ? 0xFF3F3F46 : 0xFF27272A);
        int fg = enabled ? 0xFFE5E5E5 : 0xFF52525B;
        c.fill(x, y, x + w, y + h, bg);
        int tw = tr.getWidth(label);
        c.drawTextWithShadow(tr, Text.literal(label), x + (w - tw) / 2, y + (h - 8) / 2, fg);
    }

    /** Title-bar pop-out toggle (segment tab). */
    public static void drawOverlayPopButton(
            TextRenderer tr, DrawContext c, int x, int y, int w, int h, boolean popOut) {
        drawSegmentTab(tr, c, x, y, w, h, popOut ? "Pop" : "Lock", popOut);
    }

    /**
     * Social / presence list row — elevated card with avatar, stacked metadata, optional join chip.
     */
    public static void drawPresenceUserCard(
            TextRenderer tr,
            DrawContext c,
            MinecraftClient mc,
            int x,
            int y,
            int w,
            int h,
            String username,
            String uuidShort,
            String serverLine,
            String coordsLine,
            boolean friend,
            boolean hovered,
            boolean showJoin,
            net.minecraft.entity.player.SkinTextures skin) {
        int rr = MidnightShapes.controlRadius(h);
        int fill = hovered ? MidnightPalette.PANEL_FILL_RAISED : MidnightPalette.PANEL_FILL;
        MidnightShapes.fillRoundedFrame(c, x, y, w, h, rr, fill, hovered ? MidnightPalette.BORDER_FOCUS : MidnightPalette.BORDER_LIGHT);
        if (friend) {
            MidnightShapes.fillRoundedRect(c, x + 1, y + 1, 3, h - 2, 2, MidnightPalette.GREEN);
        }

        int avatar = 32;
        int ax = x + UiTokens.SP_3;
        int ay = y + (h - avatar) / 2;
        if (mc != null && skin != null) {
            net.minecraft.client.gui.PlayerSkinDrawer.draw(c, skin, ax, ay, avatar);
        }

        int textX = ax + avatar + UiTokens.SP_3;
        int joinW = showJoin ? 52 : 0;
        int joinH = 20;
        int textMax = Math.max(48, w - (textX - x) - joinW - UiTokens.SP_4);

        int nameColor = friend ? UiTokens.MINT_300 : MidnightPalette.TEXT_PRIMARY;
        String name = tr.trimToWidth(username, textMax);
        c.drawTextWithShadow(tr, Text.literal(name), textX, y + UiTokens.SP_2 + 1, nameColor);

        String uuid = tr.trimToWidth(uuidShort, textMax);
        c.drawTextWithShadow(tr, Text.literal(uuid), textX, y + UiTokens.SP_2 + 12, MidnightPalette.TEXT_MUTED);

        int metaY = y + UiTokens.SP_2 + 23;
        if (serverLine != null && !serverLine.isBlank()) {
            c.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(serverLine, textMax)), textX, metaY, MidnightPalette.TEXT_SECONDARY);
            metaY += 10;
        }
        if (coordsLine != null && !coordsLine.isBlank()) {
            c.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(coordsLine, textMax)), textX, metaY, MidnightPalette.PATH_GREEN);
        }

        if (showJoin) {
            int jx = x + w - joinW - UiTokens.SP_2;
            int jy = y + (h - joinH) / 2;
            drawPillActionButton(tr, c, jx, jy, joinW, joinH, "Join", PillActionStyle.PRIMARY_BLUE);
        }
    }
}
