package com.dupeclient.client.module.packet.fabricator;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.overlay.OverlayTextField;
import com.dupeclient.client.module.packet.PacketUtils;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Delay tab UI for {@link PacketFabricatorOverlay} — holds selected packet types until disabled or flushed.
 */
public final class FabricatorDelayTab {
    private static final int GAP = 5;
    private static final int ROW_H = UiComponents.FAB_SET_ROW;
    private static final int TAB_H = 14;
    private static final int PICKER_ROW_H = 16;
    private static final int LIST_H = 72;
    private static final int LIST_LINE = 9;
    private static final int LIST_VISIBLE = LIST_H / LIST_LINE;
    private static final int FIELD_H = 18;

    private static List<String> sortedC2sNames;
    private static List<String> sortedS2cNames;

    private boolean pickerOpen;
    private DelaySide delaySide = DelaySide.C2S;
    private final OverlayTextField search = OverlayTextField.create(64);
    private boolean searchFocused;
    private int includedScroll;
    private int excludedScroll;

    private int searchX;
    private int searchY;
    private int searchW;
    private int exListX;
    private int exListY;
    private int exListW;
    private int exListH;
    private int inListX;
    private int inListY;
    private int inListW;
    private int inListH;
    private int flushY;

    int contentHeight(boolean pickerOpen) {
        int h = ROW_H + GAP
                + ROW_H + GAP
                + ROW_H + GAP
                + TAB_H + GAP
                + PICKER_ROW_H + GAP;
        if (pickerOpen) {
            h += FIELD_H + GAP + ROW_H + GAP + 10 + GAP + LIST_H + GAP;
        }
        h += 10 + GAP + PICKER_ROW_H + GAP;
        return h;
    }

    boolean hasTextFocus() {
        return searchFocused;
    }

    boolean isPickerOpen() {
        return pickerOpen;
    }

    void onHide() {
        pickerOpen = false;
        searchFocused = false;
        search.clear();
        PacketUtilsManager.INSTANCE.setTextInputFocused(false);
    }

    void render(GuiGraphicsExtractor context, Font tr, int tx, int ty, int innerW, int mouseX, int mouseY) {
        PacketUtilsSettings s = PacketUtilsManager.INSTANCE.getSettings();
        int y = ty;

        UiComponents.drawFabricatorSettingToggle(tr, context, tx, y, innerW, "Delay enabled", s.packetDelayEnabled);
        y += ROW_H + GAP;
        UiComponents.drawFabricatorSettingToggle(tr, context, tx, y, innerW, "Log packets", s.logPacketNamesOnDelay);
        y += ROW_H + GAP;
        UiComponents.drawFabricatorSettingToggle(
                tr, context, tx, y, innerW, "Blocked chat", s.packetDelayBlockedChatNotify);
        y += ROW_H + GAP;

        int half = (innerW - GAP) / 2;
        UiComponents.drawSegmentTab(tr, context, tx, y, half, TAB_H, "C2S", delaySide == DelaySide.C2S);
        UiComponents.drawSegmentTab(tr, context, tx + half + GAP, y, half, TAB_H, "S2C", delaySide == DelaySide.S2C);
        y += TAB_H + GAP;

        String summary = delaySide == DelaySide.C2S
                ? (s.packetDelayC2sClassNames == null || s.packetDelayC2sClassNames.isEmpty()
                        ? "no C2S packets"
                        : s.packetDelayC2sClassNames.size() + " C2S packet(s)")
                : (s.packetDelayS2cClassNames.isEmpty()
                        ? "no S2C packets"
                        : s.packetDelayS2cClassNames.size() + " S2C packet(s)");
        String pickerLabel = (pickerOpen ? "Packets v " : "Packets > ") + summary;
        drawField(context, tr, tx, y, innerW - 50, pickerLabel, pickerOpen);
        UiComponents.drawFabricatorButton(tr, context, tx + innerW - 46, y, 46, PICKER_ROW_H, "Clear", mouseX, mouseY, true);
        y += PICKER_ROW_H + GAP;

        if (pickerOpen) {
            searchX = tx;
            searchY = y;
            searchW = innerW;
            String searchShown = search.isEmpty() && !searchFocused ? "Search packets…" : search.text();
            drawField(context, tr, searchX, searchY, searchW, searchShown, searchFocused);
            y += FIELD_H + GAP;

            context.text(tr, Component.literal("Excluded"), tx, y, 0xFFA1A1AA);
            context.text(tr, Component.literal("Included"), tx + half + GAP, y, 0xFFA1A1AA);
            y += 10 + GAP;

            ensureSortedNames();
            List<String> included = filterNames(selectedNames(s), search.text());
            List<String> excluded = filterNames(excludedPool(s), search.text());
            int maxInc = Math.max(0, included.size() - LIST_VISIBLE);
            includedScroll = Math.max(0, Math.min(includedScroll, maxInc));
            int maxExc = Math.max(0, excluded.size() - LIST_VISIBLE);
            excludedScroll = Math.max(0, Math.min(excludedScroll, maxExc));

            exListX = tx;
            exListY = y;
            exListW = half;
            exListH = LIST_H;
            inListX = tx + half + GAP;
            inListY = y;
            inListW = half;
            inListH = LIST_H;
            drawListPanel(context, exListX, exListY, exListW, exListH);
            drawListPanel(context, inListX, inListY, inListW, inListH);
            for (int r = 0; r < LIST_VISIBLE; r++) {
                int excIdx = excludedScroll + r;
                if (excIdx < excluded.size()) {
                    context.text(
                            tr,
                            Component.literal(tr.plainSubstrByWidth(excluded.get(excIdx), exListW - 8)),
                            exListX + 4,
                            exListY + 2 + r * LIST_LINE,
                            0xFFF87171);
                }
                int incIdx = includedScroll + r;
                if (incIdx < included.size()) {
                    context.text(
                            tr,
                            Component.literal(tr.plainSubstrByWidth(included.get(incIdx), inListW - 8)),
                            inListX + 4,
                            inListY + 2 + r * LIST_LINE,
                            0xFF34D399);
                }
            }
            y += LIST_H + GAP;
        } else {
            searchW = 0;
            exListW = 0;
            inListW = 0;
        }

        String queued = "Queued: "
                + PacketUtilsManager.INSTANCE.packetDelayQueueSize()
                + " out / "
                + PacketUtilsManager.INSTANCE.packetDelayIncomingQueueSize()
                + " in";
        context.text(tr, Component.literal(queued), tx, y, 0xFFA1A1AA);
        y += 10 + GAP;

        flushY = y;
        UiComponents.drawFabricatorButton(tr, context, tx, flushY, innerW, PICKER_ROW_H, "Flush", mouseX, mouseY, true);
    }

    boolean mouseClicked(double mouseX, double mouseY, int button, int tx, int ty, int innerW) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        PacketUtilsSettings s = PacketUtilsManager.INSTANCE.getSettings();
        PacketUtilsManager mgr = PacketUtilsManager.INSTANCE;
        int y = ty;

        if (rect(mouseX, mouseY, tx, y, innerW, ROW_H)) {
            mgr.togglePacketDelayEnabled();
            return true;
        }
        y += ROW_H + GAP;
        if (rect(mouseX, mouseY, tx, y, innerW, ROW_H)) {
            s.logPacketNamesOnDelay = !s.logPacketNamesOnDelay;
            mgr.save();
            mgr.moduleFeedback("Packet delay log " + (s.logPacketNamesOnDelay ? "on" : "off"));
            return true;
        }
        y += ROW_H + GAP;
        if (rect(mouseX, mouseY, tx, y, innerW, ROW_H)) {
            s.packetDelayBlockedChatNotify = !s.packetDelayBlockedChatNotify;
            mgr.save();
            mgr.moduleFeedback("Blocked chat notify " + (s.packetDelayBlockedChatNotify ? "on" : "off"));
            return true;
        }
        y += ROW_H + GAP;
        int half = (innerW - GAP) / 2;
        if (rect(mouseX, mouseY, tx, y, half, TAB_H)) {
            delaySide = DelaySide.C2S;
            includedScroll = 0;
            excludedScroll = 0;
            return true;
        }
        if (rect(mouseX, mouseY, tx + half + GAP, y, half, TAB_H)) {
            delaySide = DelaySide.S2C;
            includedScroll = 0;
            excludedScroll = 0;
            return true;
        }
        y += TAB_H + GAP;
        if (rect(mouseX, mouseY, tx, y, innerW - 50, PICKER_ROW_H)) {
            pickerOpen = !pickerOpen;
            if (!pickerOpen) {
                searchFocused = false;
                search.clear();
                mgr.setTextInputFocused(false);
            }
            return true;
        }
        if (rect(mouseX, mouseY, tx + innerW - 46, y, 46, PICKER_ROW_H)) {
            if (delaySide == DelaySide.C2S) {
                s.packetDelayC2sClassNames.clear();
            } else {
                s.packetDelayS2cClassNames.clear();
            }
            mgr.save();
            mgr.moduleFeedback("Packet delay selection cleared.");
            return true;
        }
        y += PICKER_ROW_H + GAP;

        if (pickerOpen) {
            if (rect(mouseX, mouseY, searchX, searchY, searchW, FIELD_H)) {
                searchFocused = true;
                mgr.setTextInputFocused(true);
                return true;
            }
            List<String> target = delaySide == DelaySide.C2S ? s.packetDelayC2sClassNames : s.packetDelayS2cClassNames;
            if (rect(mouseX, mouseY, exListX, exListY, exListW, exListH)) {
                searchFocused = false;
                mgr.setTextInputFocused(false);
                int row = (int) ((mouseY - exListY - 2) / LIST_LINE);
                if (row >= 0 && row < LIST_VISIBLE) {
                    List<String> exc = filterNames(excludedPool(s), search.text());
                    int idx = excludedScroll + row;
                    if (idx >= 0 && idx < exc.size() && !target.contains(exc.get(idx))) {
                        target.add(exc.get(idx));
                        mgr.save();
                    }
                }
                return true;
            }
            if (rect(mouseX, mouseY, inListX, inListY, inListW, inListH)) {
                searchFocused = false;
                mgr.setTextInputFocused(false);
                int row = (int) ((mouseY - inListY - 2) / LIST_LINE);
                if (row >= 0 && row < LIST_VISIBLE) {
                    List<String> inc = filterNames(selectedNames(s), search.text());
                    int idx = includedScroll + row;
                    if (idx >= 0 && idx < inc.size()) {
                        while (target.remove(inc.get(idx))) {
                            // remove duplicates
                        }
                        mgr.save();
                    }
                }
                return true;
            }
            y += FIELD_H + GAP + 10 + GAP + LIST_H + GAP;
        }

        y += 10 + GAP;
        if (rect(mouseX, mouseY, tx, flushY, innerW, PICKER_ROW_H)) {
            mgr.flushPacketDelayQueue();
            return true;
        }

        if (searchFocused) {
            searchFocused = false;
            mgr.setTextInputFocused(false);
        }
        return false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (!pickerOpen) {
            return false;
        }
        PacketUtilsSettings s = PacketUtilsManager.INSTANCE.getSettings();
        int delta = (int) Math.signum(verticalAmount) * Math.max(1, (int) Math.ceil(Math.abs(verticalAmount)));
        if (rect(mouseX, mouseY, exListX, exListY, exListW, exListH)) {
            List<String> exc = filterNames(excludedPool(s), search.text());
            int maxScroll = Math.max(0, exc.size() - LIST_VISIBLE);
            excludedScroll = Math.max(0, Math.min(maxScroll, excludedScroll - delta));
            return true;
        }
        if (rect(mouseX, mouseY, inListX, inListY, inListW, inListH)) {
            List<String> inc = filterNames(selectedNames(s), search.text());
            int maxScroll = Math.max(0, inc.size() - LIST_VISIBLE);
            includedScroll = Math.max(0, Math.min(maxScroll, includedScroll - delta));
            return true;
        }
        return false;
    }

    boolean keyPressed(int keyCode) {
        if (!searchFocused) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            searchFocused = false;
            PacketUtilsManager.INSTANCE.setTextInputFocused(false);
            return true;
        }
        return search.keyPressed(keyCode);
    }

    boolean charTyped(int codePoint) {
        if (!searchFocused) {
            return false;
        }
        return search.charTyped(codePoint);
    }

    private static void drawField(GuiGraphicsExtractor c, Font tr, int x, int y, int w, String value, boolean focused) {
        if (focused) {
            c.fill(x - 1, y - 1, x + w + 1, y + FIELD_H + 1, 0x5534D399);
        }
        int bg = focused ? 0xFF3F3F46 : 0xFF27272A;
        int border = focused ? 0xFF34D399 : 0xFF52525B;
        c.fill(x, y, x + w, y + FIELD_H, bg);
        c.fill(x, y, x + w, y + 1, border);
        c.fill(x, y + FIELD_H - 1, x + w, y + FIELD_H, border);
        c.fill(x, y, x + 1, y + FIELD_H, border);
        c.fill(x + w - 1, y, x + w, y + FIELD_H, border);
        String display = tr.plainSubstrByWidth(value == null || value.isBlank() ? " " : value, w - 10);
        c.text(tr, Component.literal(display), x + 5, y + 5, focused ? 0xFFFFFFFF : 0xFFE5E5E5);
    }

    private static void drawListPanel(GuiGraphicsExtractor c, int x, int y, int w, int h) {
        c.fill(x, y, x + w, y + h, 0xFF27272A);
        c.fill(x, y, x + w, y + 1, 0xFF52525B);
        c.fill(x, y + h - 1, x + w, y + h, 0xFF52525B);
        c.fill(x, y, x + 1, y + h, 0xFF52525B);
        c.fill(x + w - 1, y, x + w, y + h, 0xFF52525B);
    }

    private List<String> selectedNames(PacketUtilsSettings s) {
        List<String> raw = new ArrayList<>(delaySide == DelaySide.C2S ? s.packetDelayC2sClassNames : s.packetDelayS2cClassNames);
        raw.sort(String.CASE_INSENSITIVE_ORDER);
        return raw;
    }

    private List<String> excludedPool(PacketUtilsSettings s) {
        List<String> all = delaySide == DelaySide.C2S ? sortedC2sNames : sortedS2cNames;
        List<String> sel = delaySide == DelaySide.C2S ? s.packetDelayC2sClassNames : s.packetDelayS2cClassNames;
        Set<String> set = new HashSet<>(sel);
        List<String> out = new ArrayList<>();
        for (String n : all) {
            if (!set.contains(n)) {
                out.add(n);
            }
        }
        return out;
    }

    private static void ensureSortedNames() {
        if (sortedC2sNames != null) {
            return;
        }
        sortedC2sNames = new ArrayList<>();
        for (var c : PacketUtils.getC2SPackets()) {
            String n = PacketUtils.getName(c);
            if (n != null && !n.isBlank()) {
                sortedC2sNames.add(n);
            }
        }
        Collections.sort(sortedC2sNames);
        sortedS2cNames = new ArrayList<>();
        for (var c : PacketUtils.getS2CPackets()) {
            String n = PacketUtils.getName(c);
            if (n != null && !n.isBlank()) {
                sortedS2cNames.add(n);
            }
        }
        Collections.sort(sortedS2cNames);
    }

    private static List<String> filterNames(List<String> all, String query) {
        if (query == null || query.isBlank()) {
            return all;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String n : all) {
            if (n.toLowerCase(Locale.ROOT).contains(q)) {
                out.add(n);
            }
        }
        return out;
    }

    private static boolean rect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private enum DelaySide {
        C2S,
        S2C
    }
}
