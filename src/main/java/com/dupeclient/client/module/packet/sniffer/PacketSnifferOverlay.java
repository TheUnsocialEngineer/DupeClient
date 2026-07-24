package com.dupeclient.client.module.packet.sniffer;

import com.dupeclient.client.core.notify.ClientNotificationHub;
import com.dupeclient.client.module.macro.MacroSnifferBridge;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.overlay.AbstractDraggableOverlay;
import com.dupeclient.client.gui.overlay.OverlayTextField;
import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import com.dupeclient.client.module.packet.PacketUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SniffCraft-style sniffer layout: log + packet data side-by-side, stats footer, collapsible options.
 */
public final class PacketSnifferOverlay extends AbstractDraggableOverlay implements IngameModuleOverlay {
    public static final PacketSnifferOverlay INSTANCE = new PacketSnifferOverlay();

    private static final int PANEL_W = 720;
    private static final int GAP = 4;
    private static final int PAD = 8;
    private static final int TITLE_H = 14;
    private static final int BTN_H = 14;
    private static final int TAB_H = 14;
    private static final int SEARCH_H = 12;
    private static final int LINE_H = 10;
    private static final int MAIN_H = 150;
    private static final int LOG_LINES = MAIN_H / LINE_H;
    private static final int DATA_LINES = LOG_LINES;
    private static final int SCROLLBAR_W = 8;
    private static final int STATS_H = 88;
    private static final int STATS_ROWS = 5;
    private static final int STATS_ROW_H = 11;
    private static final int OPTIONS_ROW_H = 14;
    private static final int OPTIONS_EXPANDED_H = 118;
    private static final int CTX_W = 156;
    private static final int CTX_ITEM_H = 16;
    private static final int PICKER_LIST_H = 56;
    private static final int PICKER_LIST_LINE = 9;
    private static final int PICKER_LIST_VISIBLE = PICKER_LIST_H / PICKER_LIST_LINE;
    private static final int LOG_COL_RATIO = 58;

    private static List<String> sortedC2sNames;
    private static List<String> sortedS2cNames;

    private final PacketSnifferManager manager = PacketSnifferManager.INSTANCE;
    private final OverlayTextField search = OverlayTextField.create(96);

    private FilterTab filterTab = FilterTab.ALL;
    private SnifferListMode listMode = SnifferListMode.LOG_EXCLUDE;
    private SnifferSide listSide = SnifferSide.C2S;
    private boolean optionsExpanded;
    private boolean settingsPickerOpen;
    private final OverlayTextField settingsSearch = OverlayTextField.create(64);
    private boolean settingsSearchFocused;
    private int settingsIncludedScroll;
    private int settingsExcludedScroll;

    private long selectedEntryId = -1;
    private int logScrollOffset;
    private int dataScrollOffset;
    private boolean searchFocused;
    private boolean logFocused;
    private boolean dataFocused;
    private boolean contextMenuOpen;
    private int contextMenuX;
    private int contextMenuY;
    private long contextMenuEntryId = -1;

    private boolean logScrollDragging;
    private int logScrollDragGrabY;
    private boolean dataScrollDragging;
    private int dataScrollDragGrabY;

    private int hitLogX;
    private int hitLogY;
    private int hitLogW;
    private int hitLogH;
    private int hitLogScrollX;
    private int hitLogScrollY;
    private int hitLogScrollH;
    private int hitLogScrollThumbY;
    private int hitLogScrollThumbH;
    private int hitDataX;
    private int hitDataY;
    private int hitDataW;
    private int hitDataH;
    private int hitDataScrollX;
    private int hitDataScrollY;
    private int hitDataScrollH;
    private int hitDataScrollThumbY;
    private int hitDataScrollThumbH;
    private int hitSearchX;
    private int hitSearchY;
    private int hitSearchW;
    private int hitTabAllX;
    private int hitTabC2sX;
    private int hitTabS2cX;
    private int hitTabY;
    private int hitTabW;
    private int hitPauseX;
    private int hitClearX;
    private int hitExportX;
    private int hitFabX;
    private int hitBtnY;
    private int hitBtnW;
    private int hitReplayX;
    private int hitEditX;
    private int hitOpenX;
    private int hitQueueX;
    private int hitActionY;
    private int hitActionW;
    private int hitOptionsExpandX;
    private int hitOptionsExpandY;
    private int hitOptionsExpandW;
    private int hitChipBlockX;
    private int hitChipKeepX;
    private int hitChipMoveX;
    private int hitChipFileX;
    private int hitChipConsoleX;
    private int hitChipDetailX;
    private int hitOptionsBodyY;
    private int hitOptionsBodyH;
    private int hitSettingsPickerX;
    private int hitSettingsPickerY;
    private int hitSettingsPickerW;
    private int hitSettingsPickerClearX;
    private int hitSettingsPickerClearW;
    private int hitSettingsSearchX;
    private int hitSettingsSearchY;
    private int hitSettingsSearchW;
    private int hitSettingsExListX;
    private int hitSettingsExListY;
    private int hitSettingsExListW;
    private int hitSettingsExListH;
    private int hitSettingsInListX;
    private int hitSettingsInListY;
    private int hitSettingsInListW;
    private int hitSettingsInListH;
    private int hitStatsX;
    private int hitStatsY;
    private int hitStatsW;
    private int hitStatsH;

    private final List<SettingsRowHit> settingsRowHits = new ArrayList<>();

    private PacketSnifferOverlay() {
    }

    @Override
    public String id() {
        return "packet_sniffer";
    }

    @Override
    public boolean isModuleEnabled() {
        return true;
    }

    @Override
    public boolean isOverlayVisible() {
        return manager.getSettings().overlayVisible;
    }

    @Override
    public void setOverlayVisible(boolean visible) {
        if (visible) {
            com.dupeclient.client.gui.overlay.IngameOverlayHost.onModuleOverlayOpening(this);
        }
        manager.getSettings().overlayVisible = visible;
        if (!visible) {
            logScrollOffset = 0;
            dataScrollOffset = 0;
            selectedEntryId = -1;
            searchFocused = false;
            settingsSearchFocused = false;
            logFocused = false;
            dataFocused = false;
            logScrollDragging = false;
            dataScrollDragging = false;
            closeContextMenu();
        }
        manager.save();
    }

    @Override
    public int overlayX() {
        return manager.getSettings().overlayX;
    }

    @Override
    public int overlayY() {
        return manager.getSettings().overlayY;
    }

    @Override
    public void setOverlayPosition(int x, int y) {
        manager.getSettings().overlayX = x;
        manager.getSettings().overlayY = y;
        manager.save();
    }

    @Override
    public int panelWidth() {
        return PANEL_W;
    }

    @Override
    public int panelHeight() {
        int h = TITLE_H + GAP;
        h += BTN_H + GAP + BTN_H + GAP;
        h += TAB_H + GAP + SEARCH_H + GAP;
        h += OPTIONS_ROW_H + GAP;
        if (optionsExpanded) {
            h += OPTIONS_EXPANDED_H + GAP;
        }
        h += MAIN_H + GAP + STATS_H + GAP + 10 + PAD;
        return h;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isActive()) {
            return;
        }
        int px = overlayX();
        int py = overlayY();
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        PacketSnifferSettings s = manager.getSettings();

        context.fill(px, py, px + PANEL_W, py + panelHeight(), 0xE018181B);
        context.fill(px, py, px + PANEL_W, py + TITLE_H, 0xFF27272A);
        context.drawTextWithShadow(tr, Text.literal("Packet Sniffer"), px + PAD, py + 3, UiTokens.TEXT);
        String counts = "C2S " + manager.c2sCount() + " · S2C " + manager.s2cCount();
        context.drawTextWithShadow(tr, Text.literal(counts), px + PANEL_W - PAD - tr.getWidth(counts), py + 3, 0xFF86EFAC);

        int y = py + TITLE_H + GAP;
        y = renderToolbar(context, tr, px, y, s);
        y = renderFilterRow(context, tr, px, y);
        y = renderOptionsRow(context, tr, px, y, s);
        if (optionsExpanded) {
            y = renderExpandedOptions(context, tr, px, y, s);
        }
        y = renderMainSplit(context, tr, px, y);
        y = renderStatsFooter(context, tr, px, y);
        renderStatusLine(context, tr, px, y, s);

        if (contextMenuOpen) {
            renderContextMenu(context, tr);
        }
    }

    private int renderToolbar(DrawContext context, TextRenderer tr, int px, int y, PacketSnifferSettings s) {
        hitBtnW = (PANEL_W - PAD * 2 - GAP * 7) / 8;
        hitPauseX = px + PAD;
        hitClearX = hitPauseX + hitBtnW + GAP;
        hitExportX = hitClearX + hitBtnW + GAP;
        hitFabX = hitExportX + hitBtnW + GAP;
        hitBtnY = y;
        drawMiniBtn(context, tr, hitPauseX, y, hitBtnW, s.paused ? "Resume" : "Pause", true);
        drawMiniBtn(context, tr, hitClearX, y, hitBtnW, "Clear", true);
        drawMiniBtn(context, tr, hitExportX, y, hitBtnW, "Export", true);
        drawMiniBtn(context, tr, hitFabX, y, hitBtnW, "Fabricate", true);

        hitActionW = hitBtnW;
        hitReplayX = hitFabX + hitBtnW + GAP;
        hitEditX = hitReplayX + hitActionW + GAP;
        hitOpenX = hitEditX + hitActionW + GAP;
        hitQueueX = hitOpenX + hitActionW + GAP;
        PacketSnifferEntry selected = selectedEntry();
        boolean canReplay = selected != null && selected.canReplay();
        boolean canEdit = selected != null && selected.canFabricate();
        drawMiniBtn(context, tr, hitReplayX, y, hitActionW, "Replay", canReplay);
        drawMiniBtn(context, tr, hitEditX, y, hitActionW, "Edit", canEdit);
        drawMiniBtn(context, tr, hitOpenX, y, hitActionW, "Open", selected != null);
        drawMiniBtn(context, tr, hitQueueX, y, hitActionW, "Q×5", canReplay);
        hitActionY = y;
        return y + BTN_H + GAP;
    }

    private int renderFilterRow(DrawContext context, TextRenderer tr, int px, int y) {
        int tabW = 52;
        hitTabAllX = px + PAD;
        hitTabC2sX = hitTabAllX + tabW + GAP;
        hitTabS2cX = hitTabC2sX + tabW + GAP;
        hitTabY = y;
        hitTabW = tabW;
        drawTab(context, tr, hitTabAllX, y, tabW, "All", filterTab == FilterTab.ALL);
        drawTab(context, tr, hitTabC2sX, y, tabW, "C2S", filterTab == FilterTab.C2S);
        drawTab(context, tr, hitTabS2cX, y, tabW, "S2C", filterTab == FilterTab.S2C);

        hitSearchX = hitTabS2cX + tabW + GAP + 4;
        hitSearchY = y + 1;
        hitSearchW = PANEL_W - PAD - hitSearchX + px;
        int searchBg = searchFocused ? 0xFF142A24 : 0xFF111827;
        int searchBorder = searchFocused ? 0xFF34D399 : 0xFF374151;
        context.fill(hitSearchX, hitSearchY, hitSearchX + hitSearchW, hitSearchY + SEARCH_H, searchBg);
        context.fill(hitSearchX, hitSearchY, hitSearchX + hitSearchW, hitSearchY + 1, searchBorder);
        String searchShown = search.isEmpty() && !searchFocused ? "Display filter (+inc -exc)…" : search.text();
        context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(searchShown, hitSearchW - 6)), hitSearchX + 3, hitSearchY + 2,
                search.isEmpty() && !searchFocused ? UiTokens.TEXT_DIM : 0xFFE5E7EB);
        return y + TAB_H + GAP;
    }

    private int renderOptionsRow(DrawContext context, TextRenderer tr, int px, int y, PacketSnifferSettings s) {
        int x = px + PAD;
        int chipW = 54;
        hitChipBlockX = x;
        hitChipKeepX = x + chipW + 2;
        hitChipMoveX = x + (chipW + 2) * 2;
        hitChipFileX = x + (chipW + 2) * 3;
        hitChipConsoleX = x + (chipW + 2) * 4;
        hitChipDetailX = x + (chipW + 2) * 5;
        drawChip(context, tr, hitChipBlockX, y, chipW, "Block", s.blockEnabled);
        drawChip(context, tr, hitChipKeepX, y, chipW, "KeepAlive", s.ignoreKeepAlive);
        drawChip(context, tr, hitChipMoveX, y, chipW, "NoMove", s.ignorePlayerMove);
        drawChip(context, tr, hitChipFileX, y, chipW, "LogFile", s.logToFile);
        drawChip(context, tr, hitChipConsoleX, y, chipW, "Console", s.logToConsole);
        drawChip(context, tr, hitChipDetailX, y, chipW, PacketDetailLevel.fromString(s.detailLevel).label, true);

        hitOptionsExpandW = 62;
        hitOptionsExpandX = px + PANEL_W - PAD - hitOptionsExpandW;
        hitOptionsExpandY = y;
        drawMiniBtn(context, tr, hitOptionsExpandX, y, hitOptionsExpandW, optionsExpanded ? "Options ▴" : "Options ▾", true);
        return y + OPTIONS_ROW_H + GAP;
    }

    private int renderExpandedOptions(DrawContext context, TextRenderer tr, int px, int y, PacketSnifferSettings s) {
        hitOptionsBodyY = y;
        hitOptionsBodyH = OPTIONS_EXPANDED_H;
        int innerW = PANEL_W - PAD * 2;
        int x = px + PAD;
        context.fill(x, y, x + innerW, y + OPTIONS_EXPANDED_H, 0xFF0F172A);

        settingsRowHits.clear();
        int rowY = y + 4;
        int half = (innerW - GAP) / 2;
        settingsRowHits.add(new SettingsRowHit(SettingsAction.TOGGLE, x, rowY, half, SETTINGS_ROW_H, "Capture enabled"));
        settingsRowHits.add(new SettingsRowHit(SettingsAction.TOGGLE, x + half + GAP, rowY, half, SETTINGS_ROW_H, "Clear on leave"));
        UiComponents.drawOptionToggle(tr, context, x, rowY, half, "Capture enabled", s.enabled, 1f);
        UiComponents.drawOptionToggle(tr, context, x + half + GAP, rowY, half, "Clear on leave", s.clearOnLeave, 1f);
        rowY += SETTINGS_ROW_H + 2;

        settingsRowHits.add(new SettingsRowHit(SettingsAction.TOGGLE, x, rowY, half, SETTINGS_ROW_H, "Block chat notify"));
        settingsRowHits.add(new SettingsRowHit(SettingsAction.CLICK, x + half + GAP, rowY, half, SETTINGS_ROW_H, "Replay delay ms"));
        UiComponents.drawOptionToggle(tr, context, x, rowY, half, "Block chat notify", s.blockChatNotify, 1f);
        context.drawTextWithShadow(tr, Text.literal("Replay delay"), x + half + GAP + 2, rowY + 3, UiTokens.TEXT_DIM);
        String delay = Integer.toString(s.replayDelayMs) + " ms";
        context.drawTextWithShadow(tr, Text.literal(delay), x + half + GAP + half - tr.getWidth(delay) - 2, rowY + 3, 0xFFE5E7EB);
        rowY += SETTINGS_ROW_H + 2;

        settingsRowHits.add(new SettingsRowHit(SettingsAction.LIST_MODE_C2S, x, rowY, half, TAB_H));
        settingsRowHits.add(new SettingsRowHit(SettingsAction.LIST_MODE_S2C, x + half + GAP, rowY, half, TAB_H));
        UiComponents.drawSegmentTab(tr, context, x, rowY, half, TAB_H, "Log exclude", listMode == SnifferListMode.LOG_EXCLUDE);
        UiComponents.drawSegmentTab(tr, context, x + half + GAP, rowY, half, TAB_H, "Block", listMode == SnifferListMode.BLOCK);
        rowY += TAB_H + 2;

        settingsRowHits.add(new SettingsRowHit(SettingsAction.LIST_SIDE_C2S, x, rowY, half, TAB_H));
        settingsRowHits.add(new SettingsRowHit(SettingsAction.LIST_SIDE_S2C, x + half + GAP, rowY, half, TAB_H));
        UiComponents.drawSegmentTab(tr, context, x, rowY, half, TAB_H, "C2S", listSide == SnifferSide.C2S);
        UiComponents.drawSegmentTab(tr, context, x + half + GAP, rowY, half, TAB_H, "S2C", listSide == SnifferSide.S2C);
        rowY += TAB_H + 2;

        List<String> selected = currentSettingsTargetList(s);
        String summary = selected.isEmpty() ? "no packets" : selected.size() + " selected";
        String prefix = listMode == SnifferListMode.LOG_EXCLUDE ? "Hide" : "Block";
        hitSettingsPickerX = x;
        hitSettingsPickerY = rowY;
        hitSettingsPickerW = innerW - 50;
        hitSettingsPickerClearX = x + innerW - 46;
        hitSettingsPickerClearW = 46;
        settingsRowHits.add(new SettingsRowHit(SettingsAction.PICKER_TOGGLE, hitSettingsPickerX, rowY, hitSettingsPickerW, BTN_H));
        settingsRowHits.add(new SettingsRowHit(SettingsAction.PICKER_CLEAR, hitSettingsPickerClearX, rowY, hitSettingsPickerClearW, BTN_H));
        UiComponents.drawTextField(tr, context, hitSettingsPickerX, rowY, hitSettingsPickerW, BTN_H,
                (settingsPickerOpen ? prefix + " v " : prefix + " > ") + summary, settingsPickerOpen);
        UiComponents.drawPillActionButton(tr, context, hitSettingsPickerClearX, rowY, hitSettingsPickerClearW, BTN_H,
                "Clear", UiComponents.PillActionStyle.SECONDARY_SLATE);
        rowY += BTN_H + 2;

        if (settingsPickerOpen) {
            hitSettingsSearchX = x;
            hitSettingsSearchY = rowY;
            hitSettingsSearchW = innerW;
            String searchShown = settingsSearch.isEmpty() && !settingsSearchFocused ? "Search…" : settingsSearch.text();
            UiComponents.drawTextField(tr, context, hitSettingsSearchX, hitSettingsSearchY, hitSettingsSearchW, SEARCH_H,
                    searchShown, settingsSearchFocused);
            rowY += SEARCH_H + 2;

            ensureSortedNames();
            List<String> included = filterNames(sortedSelectedNames(s), settingsSearch.text());
            List<String> excluded = filterNames(excludedPool(s), settingsSearch.text());
            settingsIncludedScroll = Math.max(0, Math.min(Math.max(0, included.size() - PICKER_LIST_VISIBLE), settingsIncludedScroll));
            settingsExcludedScroll = Math.max(0, Math.min(Math.max(0, excluded.size() - PICKER_LIST_VISIBLE), settingsExcludedScroll));

            hitSettingsExListX = x;
            hitSettingsExListY = rowY;
            hitSettingsExListW = half;
            hitSettingsExListH = PICKER_LIST_H;
            hitSettingsInListX = x + half + GAP;
            hitSettingsInListY = rowY;
            hitSettingsInListW = half;
            hitSettingsInListH = PICKER_LIST_H;
            UiComponents.drawSlotField(context, hitSettingsExListX, hitSettingsExListY, hitSettingsExListW, hitSettingsExListH, 0xD00F172B, 0xFF3A4A5E);
            UiComponents.drawSlotField(context, hitSettingsInListX, hitSettingsInListY, hitSettingsInListW, hitSettingsInListH, 0xD00F172B, 0xFF3A4A5E);
            for (int r = 0; r < PICKER_LIST_VISIBLE; r++) {
                int excIdx = settingsExcludedScroll + r;
                if (excIdx < excluded.size()) {
                    context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(excluded.get(excIdx), hitSettingsExListW - 8)),
                            hitSettingsExListX + 4, hitSettingsExListY + 2 + r * PICKER_LIST_LINE, 0xFFFFB3B3);
                }
                int incIdx = settingsIncludedScroll + r;
                if (incIdx < included.size()) {
                    context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(included.get(incIdx), hitSettingsInListW - 8)),
                            hitSettingsInListX + 4, hitSettingsInListY + 2 + r * PICKER_LIST_LINE, 0xFF56E29A);
                }
            }
        } else {
            hitSettingsSearchW = 0;
            hitSettingsExListW = 0;
            hitSettingsInListW = 0;
        }
        return y + OPTIONS_EXPANDED_H + GAP;
    }

    private int renderMainSplit(DrawContext context, TextRenderer tr, int px, int y) {
        int innerW = PANEL_W - PAD * 2;
        int logW = innerW * LOG_COL_RATIO / 100;
        int dataW = innerW - logW - GAP;
        int x = px + PAD;

        context.drawTextWithShadow(tr, Text.literal("Packet log"), x, y - 2, 0xFF9CA3AF);
        context.drawTextWithShadow(tr, Text.literal("Packet data"), x + logW + GAP, y - 2, 0xFF9CA3AF);
        y += 8;

        List<PacketSnifferEntry> filtered = filteredEntries();
        int maxLogScroll = maxLogScroll(filtered.size());

        hitLogX = x;
        hitLogY = y;
        hitLogW = logW;
        hitLogH = MAIN_H;
        int logInnerW = logW - (maxLogScroll > 0 ? SCROLLBAR_W + 1 : 0);
        context.fill(hitLogX, hitLogY, hitLogX + hitLogW, hitLogY + hitLogH, logFocused ? 0x77111827 : 0x66000000);

        logScrollOffset = Math.max(0, Math.min(maxLogScroll, logScrollOffset));
        int start = logStartIndex(filtered.size());
        for (int i = 0; i < LOG_LINES; i++) {
            int idx = start + i;
            if (idx >= filtered.size()) {
                break;
            }
            PacketSnifferEntry entry = filtered.get(idx);
            boolean selectedRow = entry.id == selectedEntryId;
            if (selectedRow) {
                context.fill(hitLogX + 1, hitLogY + i * LINE_H, hitLogX + logInnerW - 1, hitLogY + (i + 1) * LINE_H, 0x55374151);
            }
            context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(logListLine(entry), logInnerW - 4)),
                    hitLogX + 2, hitLogY + i * LINE_H, lineColor(entry));
        }
        if (maxLogScroll > 0) {
            drawScrollbar(context, hitLogX + hitLogW - SCROLLBAR_W, hitLogY, hitLogH, maxLogScroll, logScrollOffset,
                    LOG_LINES, filtered.size(), true);
        } else {
            hitLogScrollH = 0;
        }

        hitDataX = x + logW + GAP;
        hitDataY = y;
        hitDataW = dataW;
        hitDataH = MAIN_H;
        context.fill(hitDataX, hitDataY, hitDataX + hitDataW, hitDataY + hitDataH, dataFocused ? 0x77111827 : 0x66000000);

        PacketSnifferEntry selected = selectedEntry();
        List<String> dataLines = packetDataLines(selected);
        int maxDataScroll = Math.max(0, dataLines.size() - DATA_LINES);
        dataScrollOffset = Math.max(0, Math.min(maxDataScroll, dataScrollOffset));
        int dataInnerW = dataW - (maxDataScroll > 0 ? SCROLLBAR_W + 1 : 0);
        if (selected == null) {
            context.drawTextWithShadow(tr, Text.literal("Select a packet"), hitDataX + 4, hitDataY + 4, UiTokens.TEXT_DIM);
        } else {
            context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(selected.name, dataInnerW - 4)),
                    hitDataX + 4, hitDataY + 2, selected.isC2s() ? 0xFF86EFAC : 0xFF93C5FD);
            for (int i = 0; i < DATA_LINES; i++) {
                int idx = dataScrollOffset + i;
                if (idx >= dataLines.size()) {
                    break;
                }
                context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(dataLines.get(idx), dataInnerW - 4)),
                        hitDataX + 4, hitDataY + 12 + i * LINE_H, 0xFFE5E7EB);
            }
        }
        if (maxDataScroll > 0) {
            drawScrollbar(context, hitDataX + hitDataW - SCROLLBAR_W, hitDataY, hitDataH, maxDataScroll, dataScrollOffset,
                    DATA_LINES, dataLines.size(), false);
        } else {
            hitDataScrollH = 0;
        }

        return y + MAIN_H + GAP;
    }

    private int renderStatsFooter(DrawContext context, TextRenderer tr, int px, int y) {
        hitStatsX = px + PAD;
        hitStatsY = y;
        hitStatsW = PANEL_W - PAD * 2;
        hitStatsH = STATS_H;
        int gap = 8;
        int colW = (hitStatsW - gap) / 2;
        int s2cX = hitStatsX;
        int c2sX = hitStatsX + colW + gap;

        context.fill(hitStatsX, hitStatsY, hitStatsX + hitStatsW, hitStatsY + hitStatsH, 0xFF0F172A);
        context.fill(hitStatsX, hitStatsY, hitStatsX + hitStatsW, hitStatsY + 1, 0xFF374151);

        context.drawTextWithShadow(tr, Text.literal("Server → Client (" + manager.s2cCount() + ")"), s2cX + 4, y + 2, 0xFF93C5FD);
        context.drawTextWithShadow(tr, Text.literal("Client → Server (" + manager.c2sCount() + ")"), c2sX + 4, y + 2, 0xFF86EFAC);
        context.drawTextWithShadow(tr, Text.literal("Packet"), s2cX + 4, y + 13, 0xFF6B7280);
        context.drawTextWithShadow(tr, Text.literal("Count"), s2cX + colW - 44, y + 13, 0xFF6B7280);
        context.drawTextWithShadow(tr, Text.literal("Packet"), c2sX + 4, y + 13, 0xFF6B7280);
        context.drawTextWithShadow(tr, Text.literal("Count"), c2sX + colW - 44, y + 13, 0xFF6B7280);
        context.fill(s2cX + 2, y + 22, s2cX + colW - 2, y + 23, 0xFF374151);
        context.fill(c2sX + 2, y + 22, c2sX + colW - 2, y + 23, 0xFF374151);

        drawStatsTableColumn(context, tr, s2cX + 4, y + 24, colW - 8, PacketDirection.S2C);
        drawStatsTableColumn(context, tr, c2sX + 4, y + 24, colW - 8, PacketDirection.C2S);
        return y + STATS_H + GAP;
    }

    private void drawStatsTableColumn(DrawContext context, TextRenderer tr, int x, int y, int colW, PacketDirection direction) {
        int total = direction == PacketDirection.C2S ? manager.c2sCount() : manager.s2cCount();
        int color = direction == PacketDirection.C2S ? 0xFF86EFAC : 0xFF93C5FD;
        context.drawTextWithShadow(tr, Text.literal("Total"), x, y, color);
        context.drawTextWithShadow(tr, Text.literal(Integer.toString(total)), x + colW - 40, y, color);
        y += STATS_ROW_H;

        List<Map.Entry<String, Integer>> top = manager.topTypeCounts(direction, STATS_ROWS - 1);
        for (Map.Entry<String, Integer> entry : top) {
            String pct = total > 0 ? String.format(" (%.1f%%)", entry.getValue() * 100.0 / total) : "";
            context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(entry.getKey(), colW - 52)), x, y, color);
            context.drawTextWithShadow(tr, Text.literal(entry.getValue() + pct), x + colW - tr.getWidth(entry.getValue() + pct) - 2, y, color);
            y += STATS_ROW_H;
        }
    }

    private void renderStatusLine(DrawContext context, TextRenderer tr, int px, int y, PacketSnifferSettings s) {
        List<PacketSnifferEntry> filtered = filteredEntries();
        String line = (s.paused ? "Paused" : "Capturing") + " · " + manager.entryCount() + " in buffer";
        if (!search.isEmpty()) {
            line += " · " + filtered.size() + " filtered";
        }
        if (selectedEntryId > 0) {
            line += " · #" + selectedEntryId;
        }
        context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(line, PANEL_W - PAD * 2)), px + PAD, y, UiTokens.TEXT_DIM);
    }

    private void drawScrollbar(
            DrawContext context,
            int trackX,
            int trackY,
            int trackH,
            int maxScroll,
            int scrollOffset,
            int visibleLines,
            int entryCount,
            boolean log) {
        int thumbH = scrollThumbHeight(entryCount, trackH, visibleLines);
        int travel = Math.max(1, trackH - thumbH);
        int thumbY = trackY + (maxScroll == 0 ? 0 : (scrollOffset * travel) / maxScroll);
        if (log) {
            hitLogScrollX = trackX;
            hitLogScrollY = trackY;
            hitLogScrollH = trackH;
            hitLogScrollThumbY = thumbY;
            hitLogScrollThumbH = thumbH;
        } else {
            hitDataScrollX = trackX;
            hitDataScrollY = trackY;
            hitDataScrollH = trackH;
            hitDataScrollThumbY = thumbY;
            hitDataScrollThumbH = thumbH;
        }
        boolean dragging = log ? logScrollDragging : dataScrollDragging;
        context.fill(trackX, trackY, trackX + SCROLLBAR_W, trackY + trackH, 0xFF1F2937);
        context.fill(trackX + 1, thumbY, trackX + SCROLLBAR_W - 1, thumbY + thumbH, dragging ? 0xFF9CA3AF : 0xFF6B7280);
    }

    private static int scrollThumbHeight(int entryCount, int trackH, int visibleCount) {
        return Math.max(12, (int) ((long) trackH * visibleCount / Math.max(1, entryCount)));
    }

    private static String logListLine(PacketSnifferEntry entry) {
        return "#" + entry.id + " " + entry.direction.label + " " + entry.name;
    }

    private List<String> packetDataLines(@Nullable PacketSnifferEntry entry) {
        if (entry == null) {
            return List.of();
        }
        String raw = entry.editableText;
        if (raw.isBlank() && entry.packet != null) {
            raw = PacketDetailFormatter.fullData(entry.packet);
        }
        if (raw.isBlank()) {
            return List.of(entry.direction.label + " " + entry.name, entry.detail.isBlank() ? "(no field data)" : entry.detail);
        }
        List<String> lines = new ArrayList<>();
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private void renderContextMenu(DrawContext context, TextRenderer tr) {
        PacketSnifferEntry entry = contextMenuEntry();
        List<String> labels = contextMenuLabels(entry);
        int menuH = CTX_ITEM_H * labels.size() + 4;
        int mx = Math.min(contextMenuX, hitLogX + hitLogW - CTX_W);
        int my = Math.min(contextMenuY, hitLogY + hitLogH - menuH);
        mx = Math.max(hitLogX, mx);
        my = Math.max(hitLogY, my);

        context.fill(mx, my, mx + CTX_W, my + menuH, 0xF01F2937);
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) {
                context.fill(mx + 4, my + i * CTX_ITEM_H + 1, mx + CTX_W - 4, my + i * CTX_ITEM_H + 2, 0xFF374151);
            }
            context.drawTextWithShadow(tr, Text.literal(labels.get(i)), mx + 6, my + i * CTX_ITEM_H + 4, 0xFFE5E7EB);
        }
        contextMenuX = mx;
        contextMenuY = my;
    }

    private List<String> contextMenuLabels(@Nullable PacketSnifferEntry entry) {
        if (entry == null) {
            return List.of("Exclude from log", "Block packet");
        }
        String excludeLabel = manager.isExcludedFromLog(entry.direction, entry.name) ? "Include in log" : "Exclude from log";
        String blockLabel = entry.isC2s()
                ? (manager.isBlocked(entry.direction, entry.name) ? "Unblock send" : "Block send")
                : (manager.isBlocked(entry.direction, entry.name) ? "Unblock receive" : "Block receive");
        if (entry.isC2s()) {
            return List.of("Repeat packet", "Send to fabricator", "Create macro", "Macro from all C2S", excludeLabel, blockLabel);
        }
        return List.of("Compare to last session", excludeLabel, blockLabel);
    }

    @Nullable
    private PacketDirection directionFilter() {
        return switch (filterTab) {
            case C2S -> PacketDirection.C2S;
            case S2C -> PacketDirection.S2C;
            case ALL -> null;
        };
    }

    private List<PacketSnifferEntry> filteredEntries() {
        return manager.filteredSnapshot(directionFilter(), search.text());
    }

    @Nullable
    private PacketSnifferEntry selectedEntry() {
        return selectedEntryId < 0 ? null : manager.getEntry(selectedEntryId);
    }

    @Nullable
    private PacketSnifferEntry contextMenuEntry() {
        return contextMenuEntryId < 0 ? null : manager.getEntry(contextMenuEntryId);
    }

    private int lineColor(PacketSnifferEntry entry) {
        if (manager.isBlocked(entry.direction, entry.name)) {
            return 0xFFFBBF24;
        }
        if (manager.isExcludedFromLog(entry.direction, entry.name)) {
            return 0xFF6B7280;
        }
        return entry.isC2s() ? 0xFF86EFAC : 0xFF93C5FD;
    }

    private int maxLogScroll(int entryCount) {
        return Math.max(0, entryCount - LOG_LINES);
    }

    private int logStartIndex(int entryCount) {
        return Math.max(0, entryCount - LOG_LINES - logScrollOffset);
    }

    private void scrollLog(int delta, List<PacketSnifferEntry> filtered) {
        logScrollOffset = Math.max(0, Math.min(maxLogScroll(filtered.size()), logScrollOffset + delta));
    }

    private void setScrollFromMouseY(double mouseY, int maxScroll, int trackY, int trackH, int thumbH, int grabY, boolean log) {
        if (maxScroll <= 0) {
            return;
        }
        int travel = Math.max(1, trackH - thumbH);
        int relative = (int) mouseY - trackY - grabY;
        relative = Math.max(0, Math.min(travel, relative));
        int offset = (relative * maxScroll) / travel;
        if (log) {
            logScrollOffset = offset;
        } else {
            dataScrollOffset = offset;
        }
    }

    private void ensureSelectionVisible(List<PacketSnifferEntry> filtered) {
        if (selectedEntryId < 0 || filtered.isEmpty()) {
            return;
        }
        int idx = -1;
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).id == selectedEntryId) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return;
        }
        int maxScroll = maxLogScroll(filtered.size());
        int start = logStartIndex(filtered.size());
        int end = start + LOG_LINES - 1;
        if (idx < start) {
            logScrollOffset = Math.min(maxScroll, Math.max(0, filtered.size() - LOG_LINES - idx));
        } else if (idx > end) {
            logScrollOffset = Math.min(maxScroll, Math.max(0, filtered.size() - idx - 1));
        }
    }

    private void moveSelection(int delta, List<PacketSnifferEntry> filtered) {
        if (filtered.isEmpty()) {
            selectedEntryId = -1;
            return;
        }
        int idx = 0;
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).id == selectedEntryId) {
                idx = i;
                break;
            }
        }
        if (selectedEntryId < 0) {
            idx = delta >= 0 ? 0 : filtered.size() - 1;
        } else {
            idx = Math.max(0, Math.min(filtered.size() - 1, idx + delta));
        }
        selectedEntryId = filtered.get(idx).id;
        dataScrollOffset = 0;
        ensureSelectionVisible(filtered);
    }

    @Nullable
    private PacketSnifferEntry entryAtLogRow(double mouseY, List<PacketSnifferEntry> filtered) {
        int row = (int) ((mouseY - hitLogY) / LINE_H);
        if (row < 0 || row >= LOG_LINES) {
            return null;
        }
        int idx = logStartIndex(filtered.size()) + row;
        if (idx < 0 || idx >= filtered.size()) {
            return null;
        }
        return filtered.get(idx);
    }

    private void openContextMenu(int x, int y, PacketSnifferEntry entry) {
        contextMenuOpen = true;
        contextMenuX = x;
        contextMenuY = y;
        contextMenuEntryId = entry.id;
        selectedEntryId = entry.id;
        dataScrollOffset = 0;
        logFocused = true;
    }

    private void closeContextMenu() {
        contextMenuOpen = false;
        contextMenuEntryId = -1;
    }

    private boolean handleContextMenuClick(double mouseX, double mouseY) {
        if (!contextMenuOpen) {
            return false;
        }
        PacketSnifferEntry entry = contextMenuEntry();
        int menuH = CTX_ITEM_H * contextMenuLabels(entry).size() + 4;
        if (!inRect(mouseX, mouseY, contextMenuX, contextMenuY, CTX_W, menuH)) {
            return false;
        }
        if (entry == null) {
            closeContextMenu();
            return true;
        }
        int row = (int) ((mouseY - contextMenuY) / CTX_ITEM_H);
        closeContextMenu();
        if (entry.isC2s()) {
            switch (row) {
                case 0 -> PacketReplayer.replay(entry);
                case 1 -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    PacketWorkbenchScreen.openFabricationFromEntry(client != null ? client.currentScreen : null, entry);
                }
                case 2 -> {
                    String id = MacroSnifferBridge.createMacroFromSelection(entry.id);
                    if (id.isBlank()) {
                        ClientNotificationHub.warn("Could not build macro from packet");
                    } else {
                        ClientNotificationHub.success("Macro created: " + id);
                    }
                }
                case 3 -> {
                    String id = MacroSnifferBridge.createMacroFromAllVisibleC2s();
                    if (id.isBlank()) {
                        ClientNotificationHub.warn("No C2S packets to convert");
                    } else {
                        ClientNotificationHub.success("Macro created: " + id);
                    }
                }
                case 4 -> manager.toggleExcludeFromLog(entry);
                case 5 -> manager.toggleBlockPacketType(entry);
                default -> {
                }
            }
        } else {
            switch (row) {
                case 0 -> showSessionDiff();
                case 1 -> manager.toggleExcludeFromLog(entry);
                case 2 -> manager.toggleBlockPacketType(entry);
                default -> {
                }
            }
        }
        return true;
    }

    private void showSessionDiff() {
        List<PacketSnifferEntry> prev = manager.previousSessionSnapshot();
        List<PacketSnifferEntry> current = manager.snapshot(null);
        if (prev.isEmpty()) {
            ClientNotificationHub.warn("No previous session to compare");
            return;
        }
        List<PacketSnifferDiff.DiffLine> diff = PacketSnifferDiff.compareSessions(prev, current);
        int shown = 0;
        for (PacketSnifferDiff.DiffLine line : diff) {
            if (shown >= 6) {
                ClientNotificationHub.info("…" + (diff.size() - shown) + " more diff lines");
                break;
            }
            ClientNotificationHub.info(line.kind() + " " + line.text());
            shown++;
        }
    }

    private boolean handleOptionsClick(double mouseX, double mouseY, PacketSnifferSettings s) {
        if (inRect(mouseX, mouseY, hitOptionsExpandX, hitOptionsExpandY, hitOptionsExpandW, OPTIONS_ROW_H)) {
            optionsExpanded = !optionsExpanded;
            return true;
        }
        int chipY = hitOptionsExpandY;
        if (inRect(mouseX, mouseY, hitChipBlockX, chipY, 54, OPTIONS_ROW_H)) {
            s.blockEnabled = !s.blockEnabled;
            manager.save();
            manager.feedback("Packet block " + (s.blockEnabled ? "enabled" : "disabled"));
            return true;
        }
        if (inRect(mouseX, mouseY, hitChipKeepX, chipY, 54, OPTIONS_ROW_H)) {
            s.ignoreKeepAlive = !s.ignoreKeepAlive;
            manager.save();
            return true;
        }
        if (inRect(mouseX, mouseY, hitChipMoveX, chipY, 54, OPTIONS_ROW_H)) {
            s.ignorePlayerMove = !s.ignorePlayerMove;
            manager.save();
            return true;
        }
        if (inRect(mouseX, mouseY, hitChipFileX, chipY, 54, OPTIONS_ROW_H)) {
            manager.onLogToFileToggled(!s.logToFile);
            return true;
        }
        if (inRect(mouseX, mouseY, hitChipConsoleX, chipY, 54, OPTIONS_ROW_H)) {
            s.logToConsole = !s.logToConsole;
            manager.save();
            return true;
        }
        if (inRect(mouseX, mouseY, hitChipDetailX, chipY, 54, OPTIONS_ROW_H)) {
            manager.cycleDetailLevel();
            return true;
        }
        if (!optionsExpanded) {
            return false;
        }
        for (SettingsRowHit hit : settingsRowHits) {
            if (!inRect(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) {
                continue;
            }
            switch (hit.action) {
                case TOGGLE -> handleSettingsToggle(s, hit.label);
                case CLICK -> handleSettingsClickRow(s, hit.label);
                case LIST_MODE_C2S -> listMode = SnifferListMode.LOG_EXCLUDE;
                case LIST_MODE_S2C -> listMode = SnifferListMode.BLOCK;
                case LIST_SIDE_C2S -> listSide = SnifferSide.C2S;
                case LIST_SIDE_S2C -> listSide = SnifferSide.S2C;
                case PICKER_TOGGLE -> settingsPickerOpen = !settingsPickerOpen;
                case PICKER_CLEAR -> {
                    currentSettingsTargetList(s).clear();
                    manager.save();
                    manager.feedback("List cleared");
                }
                default -> {
                }
            }
            return true;
        }
        if (settingsPickerOpen && hitSettingsExListW > 0
                && inRect(mouseX, mouseY, hitSettingsExListX, hitSettingsExListY, hitSettingsExListW, hitSettingsExListH)) {
            return clickSettingsListColumn(mouseX, mouseY, s, false);
        }
        if (settingsPickerOpen && hitSettingsInListW > 0
                && inRect(mouseX, mouseY, hitSettingsInListX, hitSettingsInListY, hitSettingsInListW, hitSettingsInListH)) {
            return clickSettingsListColumn(mouseX, mouseY, s, true);
        }
        if (settingsPickerOpen && hitSettingsSearchW > 0
                && inRect(mouseX, mouseY, hitSettingsSearchX, hitSettingsSearchY, hitSettingsSearchW, SEARCH_H)) {
            settingsSearchFocused = true;
            searchFocused = false;
            return true;
        }
        return inRect(mouseX, mouseY, pxBoundsX(), hitOptionsBodyY, PANEL_W - PAD * 2, hitOptionsBodyH);
    }

    private int pxBoundsX() {
        return overlayX() + PAD;
    }

    private void handleSettingsToggle(PacketSnifferSettings s, String label) {
        if ("Capture enabled".equals(label)) {
            manager.setEnabled(!s.enabled);
        } else if ("Clear on leave".equals(label)) {
            s.clearOnLeave = !s.clearOnLeave;
            manager.save();
        } else if ("Block chat notify".equals(label)) {
            s.blockChatNotify = !s.blockChatNotify;
            manager.save();
        }
    }

    private void handleSettingsClickRow(PacketSnifferSettings s, String label) {
        if (!"Replay delay ms".equals(label)) {
            return;
        }
        int[] steps = {0, 25, 50, 100, 250, 500, 1000};
        int idx = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == s.replayDelayMs) {
                idx = (i + 1) % steps.length;
                break;
            }
        }
        s.replayDelayMs = steps[idx];
        manager.save();
    }

    private boolean clickSettingsListColumn(double mouseX, double mouseY, PacketSnifferSettings s, boolean includedColumn) {
        List<String> target = currentSettingsTargetList(s);
        List<String> included = filterNames(sortedSelectedNames(s), settingsSearch.text());
        List<String> excluded = filterNames(excludedPool(s), settingsSearch.text());
        int row = (int) ((mouseY - (includedColumn ? hitSettingsInListY : hitSettingsExListY) - 2) / PICKER_LIST_LINE);
        if (row < 0 || row >= PICKER_LIST_VISIBLE) {
            return true;
        }
        if (includedColumn) {
            int idx = settingsIncludedScroll + row;
            if (idx >= 0 && idx < included.size()) {
                target.remove(included.get(idx));
                manager.save();
            }
        } else {
            int idx = settingsExcludedScroll + row;
            if (idx >= 0 && idx < excluded.size()) {
                String name = excluded.get(idx);
                if (!target.contains(name)) {
                    target.add(name);
                }
                manager.save();
            }
        }
        return true;
    }

    private List<String> currentSettingsTargetList(PacketSnifferSettings s) {
        s.ensureLists();
        if (listMode == SnifferListMode.LOG_EXCLUDE) {
            return listSide == SnifferSide.C2S ? s.logExcludeC2sNames : s.logExcludeS2cNames;
        }
        return listSide == SnifferSide.C2S ? s.blockC2sNames : s.blockS2cNames;
    }

    private List<String> sortedSelectedNames(PacketSnifferSettings s) {
        List<String> raw = new ArrayList<>(currentSettingsTargetList(s));
        raw.sort(String.CASE_INSENSITIVE_ORDER);
        return raw;
    }

    private List<String> excludedPool(PacketSnifferSettings s) {
        ensureSortedNames();
        List<String> all = listSide == SnifferSide.C2S ? sortedC2sNames : sortedS2cNames;
        Set<String> set = new HashSet<>(currentSettingsTargetList(s));
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

    private static List<String> filterNames(List<String> names, String query) {
        if (query == null || query.isBlank()) {
            return names;
        }
        String q = query.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String n : names) {
            if (n.toLowerCase(Locale.ROOT).contains(q)) {
                out.add(n);
            }
        }
        return out;
    }

    private void drawTab(DrawContext context, TextRenderer tr, int x, int y, int w, String label, boolean active) {
        int bg = active ? 0xFF374151 : 0xFF1F2937;
        context.fill(x, y, x + w, y + TAB_H, bg);
        int tw = tr.getWidth(label);
        context.drawTextWithShadow(tr, Text.literal(label), x + (w - tw) / 2, y + 3, active ? 0xFFE5E7EB : UiTokens.TEXT_DIM);
    }

    private void drawMiniBtn(DrawContext context, TextRenderer tr, int x, int y, int w, String label, boolean enabled) {
        context.fill(x, y, x + w, y + BTN_H, enabled ? 0xFF374151 : 0xFF1F2937);
        int tw = tr.getWidth(label);
        context.drawTextWithShadow(tr, Text.literal(label), x + (w - tw) / 2, y + 3, enabled ? 0xFFE5E7EB : 0xFF6B7280);
    }

    private void drawChip(DrawContext context, TextRenderer tr, int x, int y, int w, String label, boolean on) {
        int bg = on ? 0xFF166534 : 0xFF1F2937;
        context.fill(x, y, x + w, y + OPTIONS_ROW_H, bg);
        context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(label, w - 4)), x + 3, y + 3, on ? 0xFFBBF7D0 : UiTokens.TEXT_DIM);
    }

    private static final int SETTINGS_ROW_H = 14;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int px = overlayX();
        int py = overlayY();
        PacketSnifferSettings s = manager.getSettings();

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (!isActive()) {
                return false;
            }
            closeContextMenu();
            if (inRect(mouseX, mouseY, hitLogX, hitLogY, hitLogW, hitLogH)) {
                PacketSnifferEntry entry = entryAtLogRow(mouseY, filteredEntries());
                if (entry != null) {
                    openContextMenu((int) mouseX, (int) mouseY, entry);
                    searchFocused = false;
                    return true;
                }
            }
            return inRect(mouseX, mouseY, px, py, PANEL_W, panelHeight());
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        if (contextMenuOpen) {
            if (handleContextMenuClick(mouseX, mouseY)) {
                return true;
            }
            closeContextMenu();
        }

        if (beginTitleDrag(mouseX, mouseY, button, px, py, PANEL_W, TITLE_H)) {
            searchFocused = false;
            settingsSearchFocused = false;
            return true;
        }

        if (handleScrollbarClick(mouseX, mouseY)) {
            return true;
        }

        if (handleOptionsClick(mouseX, mouseY, s)) {
            return true;
        }

        if (inRect(mouseX, mouseY, hitSearchX, hitSearchY, hitSearchW, SEARCH_H)) {
            searchFocused = true;
            settingsSearchFocused = false;
            return true;
        }
        searchFocused = false;

        if (inRect(mouseX, mouseY, hitTabAllX, hitTabY, hitTabW, TAB_H)) {
            filterTab = FilterTab.ALL;
            logScrollOffset = 0;
            return true;
        }
        if (inRect(mouseX, mouseY, hitTabC2sX, hitTabY, hitTabW, TAB_H)) {
            filterTab = FilterTab.C2S;
            logScrollOffset = 0;
            return true;
        }
        if (inRect(mouseX, mouseY, hitTabS2cX, hitTabY, hitTabW, TAB_H)) {
            filterTab = FilterTab.S2C;
            logScrollOffset = 0;
            return true;
        }
        if (inRect(mouseX, mouseY, hitPauseX, hitBtnY, hitBtnW, BTN_H)) {
            manager.togglePaused();
            return true;
        }
        if (inRect(mouseX, mouseY, hitClearX, hitBtnY, hitBtnW, BTN_H)) {
            manager.clearEntries();
            selectedEntryId = -1;
            logScrollOffset = 0;
            return true;
        }
        if (inRect(mouseX, mouseY, hitExportX, hitBtnY, hitBtnW, BTN_H)) {
            manager.exportLog();
            return true;
        }
        if (inRect(mouseX, mouseY, hitFabX, hitBtnY, hitBtnW, BTN_H)) {
            MinecraftClient client = MinecraftClient.getInstance();
            PacketWorkbenchScreen.openFabrication(client != null ? client.currentScreen : null);
            return true;
        }
        PacketSnifferEntry selected = selectedEntry();
        if (inRect(mouseX, mouseY, hitReplayX, hitActionY, hitActionW, BTN_H) && selected != null && selected.canReplay()) {
            PacketReplayer.replay(selected);
            return true;
        }
        if (inRect(mouseX, mouseY, hitEditX, hitActionY, hitActionW, BTN_H) && selected != null && selected.canFabricate()) {
            MinecraftClient client = MinecraftClient.getInstance();
            PacketWorkbenchScreen.openFabricationFromEntry(client != null ? client.currentScreen : null, selected);
            return true;
        }
        if (inRect(mouseX, mouseY, hitOpenX, hitActionY, hitActionW, BTN_H) && selected != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            PacketWorkbenchScreen.openCaptured(client != null ? client.currentScreen : null, selected);
            return true;
        }
        if (inRect(mouseX, mouseY, hitQueueX, hitActionY, hitActionW, BTN_H) && selected != null && selected.canReplay()) {
            PacketReplayScheduler.INSTANCE.queue(selected, 5);
            return true;
        }
        if (inRect(mouseX, mouseY, hitLogX, hitLogY, hitLogW, hitLogH)) {
            PacketSnifferEntry entry = entryAtLogRow(mouseY, filteredEntries());
            if (entry != null) {
                selectedEntryId = entry.id;
                dataScrollOffset = 0;
                ensureSelectionVisible(filteredEntries());
            }
            logFocused = true;
            dataFocused = false;
            return true;
        }
        if (inRect(mouseX, mouseY, hitDataX, hitDataY, hitDataW, hitDataH)) {
            dataFocused = true;
            logFocused = false;
            return true;
        }
        logFocused = false;
        dataFocused = false;
        return inRect(mouseX, mouseY, px, py, PANEL_W, panelHeight());
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        if (hitLogScrollH > 0 && inRect(mouseX, mouseY, hitLogScrollX, hitLogScrollY, SCROLLBAR_W, hitLogScrollH)) {
            List<PacketSnifferEntry> filtered = filteredEntries();
            int maxScroll = maxLogScroll(filtered.size());
            if (inRect(mouseX, mouseY, hitLogScrollX, hitLogScrollThumbY, SCROLLBAR_W, hitLogScrollThumbH)) {
                logScrollDragging = true;
                logScrollDragGrabY = (int) mouseY - hitLogScrollThumbY;
            } else {
                setScrollFromMouseY(mouseY - hitLogScrollThumbH / 2.0, maxScroll, hitLogScrollY, hitLogScrollH,
                        hitLogScrollThumbH, hitLogScrollThumbH / 2, true);
                logScrollDragging = true;
                logScrollDragGrabY = hitLogScrollThumbH / 2;
            }
            return true;
        }
        if (hitDataScrollH > 0 && inRect(mouseX, mouseY, hitDataScrollX, hitDataScrollY, SCROLLBAR_W, hitDataScrollH)) {
            PacketSnifferEntry selected = selectedEntry();
            int maxScroll = Math.max(0, packetDataLines(selected).size() - DATA_LINES);
            if (inRect(mouseX, mouseY, hitDataScrollX, hitDataScrollThumbY, SCROLLBAR_W, hitDataScrollThumbH)) {
                dataScrollDragging = true;
                dataScrollDragGrabY = (int) mouseY - hitDataScrollThumbY;
            } else {
                setScrollFromMouseY(mouseY - hitDataScrollThumbH / 2.0, maxScroll, hitDataScrollY, hitDataScrollH,
                        hitDataScrollThumbH, hitDataScrollThumbH / 2, false);
                dataScrollDragging = true;
                dataScrollDragGrabY = hitDataScrollThumbH / 2;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            logScrollDragging = false;
            dataScrollDragging = false;
        }
        endTitleDrag(button);
        return dragging || logScrollDragging || dataScrollDragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (logScrollDragging && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            setScrollFromMouseY(mouseY, maxLogScroll(filteredEntries().size()), hitLogScrollY, hitLogScrollH,
                    hitLogScrollThumbH, logScrollDragGrabY, true);
            return true;
        }
        if (dataScrollDragging && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            PacketSnifferEntry selected = selectedEntry();
            int maxScroll = Math.max(0, packetDataLines(selected).size() - DATA_LINES);
            setScrollFromMouseY(mouseY, maxScroll, hitDataScrollY, hitDataScrollH, hitDataScrollThumbH, dataScrollDragGrabY, false);
            return true;
        }
        return updateTitleDrag(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (settingsSearchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                settingsSearchFocused = false;
                return true;
            }
            return settingsSearch.keyPressed(keyCode);
        }

        List<PacketSnifferEntry> filtered = filteredEntries();
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
            if (search.keyPressed(keyCode)) {
                logScrollOffset = 0;
                ensureSelectionVisible(filtered);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                moveSelection(1, filtered);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                moveSelection(-1, filtered);
                return true;
            }
            return true;
        }

        if (!logFocused && !dataFocused) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (dataFocused) {
                PacketSnifferEntry selected = selectedEntry();
                int max = Math.max(0, packetDataLines(selected).size() - DATA_LINES);
                dataScrollOffset = Math.min(max, dataScrollOffset + 1);
            } else {
                moveSelection(1, filtered);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (dataFocused) {
                dataScrollOffset = Math.max(0, dataScrollOffset - 1);
            } else {
                moveSelection(-1, filtered);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollLog(LOG_LINES, filtered);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scrollLog(-LOG_LINES, filtered);
            return true;
        }
        PacketSnifferEntry selected = selectedEntry();
        if (keyCode == GLFW.GLFW_KEY_ENTER && selected != null && selected.canReplay()) {
            PacketReplayer.replay(selected);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(int codePoint) {
        if (settingsSearchFocused) {
            return settingsSearch.charTyped(codePoint);
        }
        if (!searchFocused) {
            return false;
        }
        if (search.charTyped(codePoint)) {
            logScrollOffset = 0;
            ensureSelectionVisible(filteredEntries());
        }
        return true;
    }

    @Override
    public boolean hasTextFocus() {
        return searchFocused || settingsSearchFocused;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int lines = Math.max(1, (int) Math.round(Math.abs(verticalAmount) * 3.0));
        int delta = verticalAmount > 0 ? -lines : verticalAmount < 0 ? lines : 0;
        if (inRect(mouseX, mouseY, hitDataX, hitDataY, hitDataW, hitDataH)) {
            PacketSnifferEntry selected = selectedEntry();
            int max = Math.max(0, packetDataLines(selected).size() - DATA_LINES);
            dataScrollOffset = Math.max(0, Math.min(max, dataScrollOffset + delta));
            return true;
        }
        if (inRect(mouseX, mouseY, hitLogX, hitLogY, hitLogW, hitLogH)) {
            scrollLog(delta, filteredEntries());
            return true;
        }
        return false;
    }

    private enum FilterTab {
        ALL,
        C2S,
        S2C
    }

    private enum SnifferListMode {
        LOG_EXCLUDE,
        BLOCK
    }

    private enum SnifferSide {
        C2S,
        S2C
    }

    private enum SettingsAction {
        TOGGLE,
        CLICK,
        LIST_MODE_C2S,
        LIST_MODE_S2C,
        LIST_SIDE_C2S,
        LIST_SIDE_S2C,
        PICKER_TOGGLE,
        PICKER_CLEAR
    }

    private static final class SettingsRowHit {
        private final SettingsAction action;
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final @Nullable String label;

        private SettingsRowHit(SettingsAction action, int x, int y, int w, int h) {
            this(action, x, y, w, h, null);
        }

        private SettingsRowHit(SettingsAction action, int x, int y, int w, int h, @Nullable String label) {
            this.action = action;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.label = label;
        }
    }
}
