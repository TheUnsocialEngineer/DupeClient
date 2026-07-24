package com.dupeclient.client.module.payall;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.overlay.AbstractDraggableOverlay;
import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.gui.overlay.OverlayTextField;
import com.dupeclient.client.gui.overlay.SearchableDropdown;
import com.dupeclient.client.module.fuzzer.economy.EconomyCommandDetector;
import com.dupeclient.client.module.security.SecurityManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * In-game draggable PayAll control panel (amount mode, targets, exclude staff, commands, log).
 */
public final class PayAllOverlay extends AbstractDraggableOverlay implements IngameModuleOverlay {
    public static final PayAllOverlay INSTANCE = new PayAllOverlay();

    private static final int PANEL_W = 332;
    private static final int TITLE_H = 12;
    private static final int PAD = 8;
    private static final int GAP = 6;
    private static final int FIELD_W = 120;
    private static final int CYCLE_W = 68;
    private static final int INPUT_H = 14;
    private static final int TOGGLE_ROW_H = 30;
    private static final int LOG_BOX_H = 50;
    private static final int LOG_VISIBLE = 5;
    private static final int LOG_LINE_H = 10;
    private static final int PICKER_H = 172;
    private static final int LIST_ROW = 11;
    private static final int LIST_VISIBLE = 6;

    private static final int PANEL_BG = 0xE010101B;
    private static final int TITLE_BG = 0xFF27272A;
    private static final int TITLE_FG = 0xFF34D399;
    private static final int TEXT_PRIMARY = 0xFFF8FAFC;
    private static final int TEXT_MUTED = 0xFF94A3B8;
    private static final int TEXT_ONLINE = 0xFF10B981;
    private static final int TEXT_ERROR = 0xFFFF8B8B;
    private static final int TEXT_EXCLUDED = 0xFFFF9A9A;
    private static final int LOG_BG = 0xFF12121A;
    private static final int LIST_FILL = 0xD018182B;
    private static final int LIST_BORDER = 0xFF3A4A5E;

    private final PayAllManager manager = PayAllManager.INSTANCE;
    private final OverlayTextField amountInput = OverlayTextField.create(32);
    private final OverlayTextField commandInput = OverlayTextField.create(128);
    private final OverlayTextField balanceCommandInput = OverlayTextField.create(128);
    private final OverlayTextField targetsSearch = OverlayTextField.create(64);
    private final SearchableDropdown manualDropdown = new SearchableDropdown("Add manual…");
    private final SearchableDropdown excludeDropdown = new SearchableDropdown("Add exclusion…");

    private FocusField focusField = FocusField.NONE;
    private SliderMode sliderMode = SliderMode.NONE;
    private boolean targetsPickerOpen;
    private int includedScroll;
    private int excludedScroll;
    private int logScrollOffset;

    private int logBoxX;
    private int logBoxY;
    private int logBoxW;
    private int logBoxH;
    private int pickSearchX;
    private int pickSearchY;
    private int pickSearchW;
    private int pickSearchH;
    private int inListX;
    private int inListY;
    private int inListW;
    private int inListH;
    private int exListX;
    private int exListY;
    private int exListW;
    private int exListH;

    private PayAllOverlay() {
        amountInput.setText("1000");
        commandInput.setText(manager.getPayCommand());
        balanceCommandInput.setText(manager.getBalanceCommand());
        configurePlayerDropdown(manualDropdown);
        configurePlayerDropdown(excludeDropdown);
    }

    private static void configurePlayerDropdown(SearchableDropdown dropdown) {
        dropdown.setAllowCustomEntry(true);
        dropdown.setModernChrome(true);
        dropdown.setShowPlayerAvatars(true);
    }

    @Override
    public boolean containsPoint(double mouseX, double mouseY) {
        if (mouseX >= overlayX()
                && mouseX < overlayX() + panelWidth()
                && mouseY >= overlayY()
                && mouseY < overlayY() + panelHeight()) {
            return true;
        }
        return manualDropdown.hitsInteractive(mouseX, mouseY) || excludeDropdown.hitsInteractive(mouseX, mouseY);
    }

    @Override
    public String id() {
        return "payall";
    }

    @Override
    public boolean isModuleEnabled() {
        return manager.getSettings().enabled;
    }

    @Override
    public boolean isOverlayVisible() {
        return manager.getSettings().overlayVisible;
    }

    @Override
    public void setOverlayVisible(boolean visible) {
        if (visible) {
            IngameOverlayHost.onModuleOverlayOpening(this);
        }
        manager.getSettings().overlayVisible = visible;
        if (!visible) {
            focusField = FocusField.NONE;
            targetsPickerOpen = false;
            manualDropdown.close();
            excludeDropdown.close();
        }
        manager.saveSettings();
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
        manager.saveSettings();
    }

    @Override
    public int panelWidth() {
        return PANEL_W;
    }

    @Override
    public int panelHeight() {
        return layout().panelH;
    }

    @Override
    public boolean hasTextFocus() {
        return focusField != FocusField.NONE || manualDropdown.hasTextFocus() || excludeDropdown.hasTextFocus();
    }

    private int playerDropdownExtra() {
        return Math.max(manualDropdown.extraHeight(), excludeDropdown.extraHeight());
    }

    private int ox() {
        return overlayX();
    }

    private int oy() {
        return overlayY();
    }

    private int bodyTop() {
        return oy() + TITLE_H + PAD;
    }

    private boolean hasErrorLine() {
        String err = manager.getLastError();
        return err != null && !err.isBlank();
    }

    private PayAllLayout layout() {
        boolean hasErr = hasErrorLine();
        int dropdownExtra = playerDropdownExtra();
        int ox = ox();
        int oy = oy();
        int tx = ox + PAD;
        int innerX = tx + PAD;
        int sw = PANEL_W - 16;
        int rowW = sw - 16;
        int halfW = (rowW - GAP) / 2;
        int actionBtnW = (rowW - 12) / 3;
        int cardTop = bodyTop() + GAP;
        int y = cardTop + 38;
        int chatY = y;
        y += 36;
        int amountLabelY = y;
        y += 14;
        int amountRowY = y;
        y += 20;
        int delayY = y;
        y += 22;
        int actionsY = y;
        y += 20;
        int progressY = y;
        y += 14;
        int targetsY = y;
        y += 14;
        int summaryY = y;
        y += 14;
        int errorY = y;
        if (hasErr) {
            y += 14;
        }
        int pickerY = y + GAP;
        if (targetsPickerOpen) {
            y += PICKER_H + GAP;
        }
        int payLabelY = y;
        y += 14;
        int payRowY = y;
        y += 36;
        int balLabelY = y;
        y += 14;
        int balRowY = y;
        y += 20;
        int manualLabelY = y;
        y += 14;
        int addRowY = y;
        y += INPUT_H + dropdownExtra + GAP;
        int staffY = y;
        y += 36;
        int clearY = y;
        y += 20;
        int logLabelY = y;
        y += 14;
        int logBoxY = y;
        y += LOG_BOX_H + 10;
        int cardH = y - cardTop;
        int panelH = y - oy + PAD;
        return new PayAllLayout(
                tx, innerX, rowW, halfW, actionBtnW, sw, cardTop, cardH, chatY,
                amountLabelY, amountRowY, delayY, actionsY, progressY, targetsY, summaryY, errorY, pickerY,
                payLabelY, payRowY, balLabelY, balRowY, manualLabelY, addRowY, staffY, clearY, logLabelY, logBoxY, panelH);
    }

    private static int centeredInRow(int rowY, int rowH, int itemH) {
        return rowY + Math.max(0, (rowH - itemH) / 2);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!isActive()) {
            return;
        }
        PayAllLayout l = layout();
        int px = ox();
        int py = oy();
        context.fill(px, py, px + PANEL_W, py + l.panelH, PANEL_BG);
        context.fill(px, py, px + PANEL_W, py + TITLE_H, TITLE_BG);
        Font tr = Minecraft.getInstance().font;
        context.drawString(tr, Component.literal("PayAll"), px + 6, py + 2, TITLE_FG);

        Minecraft mc = Minecraft.getInstance();
        UiComponents.drawInfoCard(tr, context, l.tx, l.cardTop, l.cardW, l.cardH, "PayAll");
        UiComponents.drawOptionToggle(
                tr, context, l.innerX, l.chatY, l.rowW, "Chat feedback",
                manager.isModuleChatFeedback(), manager.isModuleChatFeedback() ? 1f : 0f);

        UiComponents.drawAccentLabel(tr, context, l.innerX, l.amountLabelY, "Amount");
        int amountFieldY = centeredInRow(l.amountRowY, INPUT_H, INPUT_H);
        int modeBtnX = l.innerX + FIELD_W + GAP;
        int modeBtnW = l.rowW - FIELD_W - GAP;
        UiComponents.drawTextField(tr, context, l.innerX, amountFieldY, FIELD_W, INPUT_H, amountInput.text(), focusField == FocusField.AMOUNT);
        UiComponents.drawPillActionButton(
                tr, context, modeBtnX, amountFieldY, modeBtnW, INPUT_H,
                manager.getAmountMode().display(), UiComponents.PillActionStyle.PRIMARY_MINT);

        double tDelay = Mth.clamp(manager.getDelayMs() / 5000.0, 0.0, 1.0);
        UiComponents.drawValueSlider(
                tr, context, l.innerX, l.delayY, l.rowW, tDelay, "Delay",
                manager.getDelayMs() + "ms", sliderMode == SliderMode.DELAY);

        int actionY = centeredInRow(l.actionsY, INPUT_H, INPUT_H);
        int action2X = l.innerX + l.actionBtnW + GAP;
        int action3X = action2X + l.actionBtnW + GAP;
        UiComponents.drawPillActionButton(
                tr, context, l.innerX, actionY, l.actionBtnW, INPUT_H,
                manager.isPaying() ? "Running" : "Start", UiComponents.PillActionStyle.PRIMARY_MINT);
        UiComponents.drawPillActionButton(
                tr, context, action2X, actionY, l.actionBtnW, INPUT_H,
                manager.isPaused() ? "Resume" : "Pause", UiComponents.PillActionStyle.SECONDARY_SLATE);
        UiComponents.drawPillActionButton(
                tr, context, action3X, actionY, l.actionBtnW, INPUT_H,
                "Cancel", UiComponents.PillActionStyle.SECONDARY_SLATE);

        drawTrimmedText(context, "Progress " + format1(manager.getProgress() * 100.0) + "%", l.innerX, l.progressY, l.rowW, TEXT_PRIMARY);
        context.drawString(tr, Component.literal("Online targets: " + manager.getOnlineCount(mc)), l.innerX, l.targetsY, TEXT_ONLINE);

        int staffOnline = SecurityManager.INSTANCE.countOnlineStaff(mc);
        String summary = "Manual " + manager.getManualCount()
                + " | Excluded " + manager.getExcludedCount()
                + (manager.isExcludeStaff() ? " | Staff " + staffOnline : "");
        drawTrimmedText(context, summary, l.innerX, l.summaryY, l.rowW, TEXT_MUTED);
        if (hasErrorLine()) {
            drawTrimmedText(context, manager.getLastError(), l.innerX, l.errorY, l.rowW, TEXT_ERROR);
        }
        if (targetsPickerOpen) {
            drawTargetsPicker(context, l.tx, l.pickerY, l.cardW, mc);
        }

        UiComponents.drawAccentLabel(tr, context, l.innerX, l.payLabelY, "Pay command");
        int payFieldY = centeredInRow(l.payRowY, TOGGLE_ROW_H, INPUT_H);
        int payCycleX = l.innerX + FIELD_W + GAP;
        int reverseX = payCycleX + CYCLE_W + GAP;
        int reverseW = l.rowW - FIELD_W - GAP - CYCLE_W - GAP;
        UiComponents.drawTextField(tr, context, l.innerX, payFieldY, FIELD_W, INPUT_H, commandInput.text(), focusField == FocusField.COMMAND);
        UiComponents.drawPillActionButton(tr, context, payCycleX, payFieldY, CYCLE_W, INPUT_H, "Cycle", UiComponents.PillActionStyle.PRIMARY_MINT);
        UiComponents.drawOptionToggle(
                tr, context, reverseX, l.payRowY, reverseW, "Reverse",
                manager.isReverseSyntax(), manager.isReverseSyntax() ? 1f : 0f);

        UiComponents.drawAccentLabel(tr, context, l.innerX, l.balLabelY, "Bal command");
        int balCycleX = l.innerX + FIELD_W + GAP;
        UiComponents.drawTextField(
                tr, context, l.innerX, l.balRowY, FIELD_W, INPUT_H,
                balanceCommandInput.text(), focusField == FocusField.BALANCE_COMMAND);
        UiComponents.drawPillActionButton(tr, context, balCycleX, l.balRowY, CYCLE_W, INPUT_H, "Cycle", UiComponents.PillActionStyle.PRIMARY_MINT);

        int excludeLabelX = l.innerX + l.halfW + GAP;
        UiComponents.drawAccentLabel(tr, context, l.innerX, l.manualLabelY, "Manual");
        UiComponents.drawAccentLabel(tr, context, excludeLabelX, l.manualLabelY, "Exclude");
        List<String> tabPlayers = manager.getOnlineTabPlayerNames(mc);
        manualDropdown.render(context, tr, l.innerX, l.addRowY, l.halfW, INPUT_H, tabPlayers, mouseX, mouseY);
        excludeDropdown.render(context, tr, excludeLabelX, l.addRowY, l.halfW, INPUT_H, tabPlayers, mouseX, mouseY);

        UiComponents.drawOptionToggle(
                tr, context, l.innerX, l.staffY, l.rowW, "Exclude staff",
                manager.isExcludeStaff(), manager.isExcludeStaff() ? 1f : 0f);
        UiComponents.drawPillActionButton(
                tr, context, l.innerX, l.clearY, l.halfW, INPUT_H, "Clear Manual", UiComponents.PillActionStyle.SECONDARY_SLATE);
        UiComponents.drawPillActionButton(
                tr, context, excludeLabelX, l.clearY, l.halfW, INPUT_H, "Clear Excluded", UiComponents.PillActionStyle.SECONDARY_SLATE);

        UiComponents.drawAccentLabel(tr, context, l.innerX, l.logLabelY, "Log");
        logBoxX = l.innerX;
        logBoxY = l.logBoxY;
        logBoxW = l.rowW;
        logBoxH = LOG_BOX_H;
        context.fill(logBoxX, logBoxY, logBoxX + logBoxW, logBoxY + logBoxH, LOG_BG);
        List<String> logs = manager.getLogs();
        int maxLogScroll = Math.max(0, logs.size() - LOG_VISIBLE);
        logScrollOffset = Math.max(0, Math.min(maxLogScroll, logScrollOffset));
        int start = Math.max(0, logs.size() - LOG_VISIBLE - logScrollOffset);
        for (int i = 0; i < LOG_VISIBLE; i++) {
            int idx = start + i;
            if (idx >= logs.size()) {
                continue;
            }
            drawTrimmedText(context, logs.get(idx), l.innerX + 4, l.logBoxY + 4 + i * LOG_LINE_H, l.rowW - 8, TEXT_PRIMARY);
        }

        manualDropdown.renderPopupLayer(context, tr, tabPlayers, mouseX, mouseY);
        excludeDropdown.renderPopupLayer(context, tr, tabPlayers, mouseX, mouseY);
    }

    private void drawTargetsPicker(GuiGraphics context, int sx, int sy, int sw, Minecraft mc) {
        Font tr = mc.font;
        UiComponents.drawInfoCard(tr, context, sx, sy, sw, PICKER_H, "Targets");
        int ix = sx + 6;
        int iy = sy + 38;
        int innerW = sw - 12;
        int half = (innerW - 8) / 2;
        pickSearchX = ix;
        pickSearchY = iy;
        pickSearchW = innerW;
        pickSearchH = 12;
        String searchShown = targetsSearch.isEmpty() && focusField != FocusField.TARGETS_SEARCH
                ? "Search…"
                : targetsSearch.text();
        UiComponents.drawTextField(tr, context, pickSearchX, pickSearchY, pickSearchW, pickSearchH, searchShown, focusField == FocusField.TARGETS_SEARCH);
        UiComponents.drawAccentLabel(tr, context, ix, iy + 18, "Included");
        UiComponents.drawAccentLabel(tr, context, ix + half + 8, iy + 18, "Excluded");
        UiComponents.drawPillActionButton(tr, context, sx + sw - 48, sy + 8, 42, 12, "Done", UiComponents.PillActionStyle.SECONDARY_SLATE);

        int listY = iy + 32;
        inListX = ix;
        inListY = listY;
        inListW = half;
        inListH = LIST_VISIBLE * LIST_ROW;
        exListX = ix + half + 8;
        exListY = listY;
        exListW = half;
        exListH = inListH;

        List<String> inc = filterBySearch(manager.getIncludedTargetNames(mc), targetsSearch.text());
        List<String> exc = filterBySearch(manager.getExcludedNamesSorted(), targetsSearch.text());
        includedScroll = Math.max(0, Math.min(includedScroll, Math.max(0, inc.size() - LIST_VISIBLE)));
        excludedScroll = Math.max(0, Math.min(excludedScroll, Math.max(0, exc.size() - LIST_VISIBLE)));

        UiComponents.drawSlotField(context, inListX, inListY, inListW, inListH, LIST_FILL, LIST_BORDER);
        UiComponents.drawSlotField(context, exListX, exListY, exListW, exListH, LIST_FILL, LIST_BORDER);
        for (int r = 0; r < LIST_VISIBLE; r++) {
            int idx = includedScroll + r;
            if (idx < inc.size()) {
                String row = tr.plainSubstrByWidth(inc.get(idx), inListW - 8);
                context.drawString(tr, Component.literal(row), inListX + 4, inListY + 2 + r * LIST_ROW, TEXT_PRIMARY);
            }
            int j = excludedScroll + r;
            if (j < exc.size()) {
                String row = tr.plainSubstrByWidth(exc.get(j), exListW - 8);
                context.drawString(tr, Component.literal(row), exListX + 4, exListY + 2 + r * LIST_ROW, TEXT_EXCLUDED);
            }
        }
        context.drawString(tr, Component.literal("Click name → exclude / include"), ix, listY + inListH + 2, TEXT_MUTED);
    }

    private static List<String> filterBySearch(List<String> names, String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>(names);
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<>();
        for (String n : names) {
            if (n != null && n.toLowerCase(Locale.ROOT).contains(q)) {
                out.add(n);
            }
        }
        return out;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isActive()) {
            return false;
        }
        int px = ox();
        int py = oy();
        boolean onPanel = containsPoint(mouseX, mouseY);
        if (!onPanel) {
            if (focusField != FocusField.NONE) {
                focusField = FocusField.NONE;
            }
            if (manualDropdown.isOpen() || excludeDropdown.isOpen()) {
                manualDropdown.close();
                excludeDropdown.close();
            }
            return false;
        }
        if (beginTitleDrag(mouseX, mouseY, button, px, py, PANEL_W, TITLE_H)) {
            return true;
        }

        Minecraft mc = Minecraft.getInstance();
        List<String> tabPlayers = manager.getOnlineTabPlayerNames(mc);
        // Player pickers first so open lists win over controls beneath them.
        if (manualDropdown.mouseClicked(mouseX, mouseY, button, tabPlayers, manager::addManualPlayer)) {
            excludeDropdown.close();
            focusField = FocusField.NONE;
            return true;
        }
        if (excludeDropdown.mouseClicked(mouseX, mouseY, button, tabPlayers, manager::addExcludedPlayer)) {
            manualDropdown.close();
            focusField = FocusField.NONE;
            return true;
        }

        PayAllLayout l = layout();
        int amountFieldY = centeredInRow(l.amountRowY, INPUT_H, INPUT_H);
        int modeBtnX = l.innerX + FIELD_W + GAP;
        int modeBtnW = l.rowW - FIELD_W - GAP;
        int actionY = centeredInRow(l.actionsY, INPUT_H, INPUT_H);
        int action2X = l.innerX + l.actionBtnW + GAP;
        int action3X = action2X + l.actionBtnW + GAP;
        int payFieldY = centeredInRow(l.payRowY, TOGGLE_ROW_H, INPUT_H);
        int payCycleX = l.innerX + FIELD_W + GAP;
        int reverseX = payCycleX + CYCLE_W + GAP;
        int reverseW = l.rowW - FIELD_W - GAP - CYCLE_W - GAP;
        int balCycleX = l.innerX + FIELD_W + GAP;
        int excludeColX = l.innerX + l.halfW + GAP;

        if (rect(mouseX, mouseY, l.innerX, l.chatY, l.rowW, TOGGLE_ROW_H)) {
            boolean next = !manager.isModuleChatFeedback();
            manager.setModuleChatFeedback(next);
            manager.moduleFeedbackConfigToggle("PayAll chat feedback " + (next ? "on" : "off"));
            return true;
        }
        if (rect(mouseX, mouseY, l.innerX, amountFieldY, FIELD_W, INPUT_H)) {
            focusField = FocusField.AMOUNT;
            return true;
        }
        if (rect(mouseX, mouseY, modeBtnX, amountFieldY, modeBtnW, INPUT_H)) {
            manager.cycleAmountMode();
            return true;
        }
        if (clickSlider(mouseX, mouseY, l.innerX, l.delayY, l.rowW)) {
            return true;
        }
        if (rect(mouseX, mouseY, l.innerX, actionY, l.actionBtnW, INPUT_H)) {
            manager.startPaying(mc, amountInput.text().trim());
            return true;
        }
        if (rect(mouseX, mouseY, action2X, actionY, l.actionBtnW, INPUT_H)) {
            manager.togglePause();
            return true;
        }
        if (rect(mouseX, mouseY, action3X, actionY, l.actionBtnW, INPUT_H)) {
            manager.cancel();
            return true;
        }
        if (rect(mouseX, mouseY, l.innerX, l.targetsY, l.rowW, INPUT_H)) {
            targetsPickerOpen = !targetsPickerOpen;
            if (!targetsPickerOpen) {
                focusField = FocusField.NONE;
            }
            manager.moduleFeedback("Targets picker " + (targetsPickerOpen ? "opened" : "closed") + ".");
            return true;
        }
        if (targetsPickerOpen) {
            if (rect(mouseX, mouseY, l.tx + l.cardW - 48, l.pickerY + 8, 42, 14)) {
                targetsPickerOpen = false;
                focusField = FocusField.NONE;
                return true;
            }
            if (rect(mouseX, mouseY, pickSearchX, pickSearchY, pickSearchW, pickSearchH)) {
                focusField = FocusField.TARGETS_SEARCH;
                return true;
            }
            if (rect(mouseX, mouseY, inListX, inListY, inListW, inListH)) {
                focusField = FocusField.NONE;
                int row = (int) ((mouseY - inListY - 2.0) / LIST_ROW);
                if (row >= 0 && row < LIST_VISIBLE) {
                    List<String> inc = filterBySearch(manager.getIncludedTargetNames(mc), targetsSearch.text());
                    int idx = includedScroll + row;
                    if (idx >= 0 && idx < inc.size()) {
                        manager.addExcludedPlayer(inc.get(idx));
                    }
                }
                return true;
            }
            if (rect(mouseX, mouseY, exListX, exListY, exListW, exListH)) {
                focusField = FocusField.NONE;
                int row = (int) ((mouseY - exListY - 2.0) / LIST_ROW);
                if (row >= 0 && row < LIST_VISIBLE) {
                    List<String> exc = filterBySearch(manager.getExcludedNamesSorted(), targetsSearch.text());
                    int idx = excludedScroll + row;
                    if (idx >= 0 && idx < exc.size()) {
                        manager.removeExcludedPlayer(exc.get(idx));
                    }
                }
                return true;
            }
        }

        if (rect(mouseX, mouseY, l.innerX, payFieldY, FIELD_W, INPUT_H)) {
            focusField = FocusField.COMMAND;
            return true;
        }
        if (rect(mouseX, mouseY, payCycleX, payFieldY, CYCLE_W, INPUT_H)) {
            cyclePayCommand(mc);
            return true;
        }
        if (rect(mouseX, mouseY, reverseX, l.payRowY, reverseW, TOGGLE_ROW_H)) {
            manager.setReverseSyntax(!manager.isReverseSyntax());
            return true;
        }
        if (rect(mouseX, mouseY, l.innerX, l.balRowY, FIELD_W, INPUT_H)) {
            focusField = FocusField.BALANCE_COMMAND;
            return true;
        }
        if (rect(mouseX, mouseY, balCycleX, l.balRowY, CYCLE_W, INPUT_H)) {
            cycleBalanceCommand(mc);
            return true;
        }
        if (rect(mouseX, mouseY, l.innerX, l.staffY, l.rowW, TOGGLE_ROW_H)) {
            manager.setExcludeStaff(!manager.isExcludeStaff());
            return true;
        }
        if (rect(mouseX, mouseY, l.innerX, l.clearY, l.halfW, INPUT_H)) {
            manager.clearManualPlayers();
            return true;
        }
        if (rect(mouseX, mouseY, excludeColX, l.clearY, l.halfW, INPUT_H)) {
            manager.clearExcludedPlayers();
            return true;
        }
        focusField = FocusField.NONE;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = dragging;
        boolean wasSliding = sliderMode != SliderMode.NONE;
        endTitleDrag(button);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            sliderMode = SliderMode.NONE;
        }
        return wasDragging || wasSliding;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (updateTitleDrag(mouseX, mouseY, button)) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isActive() || sliderMode != SliderMode.DELAY) {
            return false;
        }
        PayAllLayout l = layout();
        int barX = l.innerX + 76;
        int barW = l.rowW - 82 - 36;
        manager.setDelayMs((long) sliderValue(mouseX, barX, barW, 0.0, 5000.0));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isActive()) {
            return false;
        }
        double amount = verticalAmount != 0.0 ? verticalAmount : horizontalAmount;
        if (amount == 0.0) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        List<String> tabPlayers = manager.getOnlineTabPlayerNames(mc);
        boolean overPanel = containsPoint(mouseX, mouseY);

        if (manualDropdown.isOpen()
                && (overPanel || manualDropdown.hitsInteractive(mouseX, mouseY))) {
            return manualDropdown.scrollOpenList(amount, tabPlayers);
        }
        if (excludeDropdown.isOpen()
                && (overPanel || excludeDropdown.hitsInteractive(mouseX, mouseY))) {
            return excludeDropdown.scrollOpenList(amount, tabPlayers);
        }
        if (!overPanel) {
            return false;
        }
        if (rect(mouseX, mouseY, logBoxX, logBoxY, logBoxW, logBoxH)) {
            int delta = scrollDelta(amount);
            int maxScroll = Math.max(0, manager.getLogs().size() - LOG_VISIBLE);
            logScrollOffset = Math.max(0, Math.min(maxScroll, logScrollOffset - delta));
            return true;
        }
        if (!targetsPickerOpen) {
            return false;
        }
        int delta = scrollDelta(amount);
        if (rect(mouseX, mouseY, inListX, inListY, inListW, inListH)) {
            List<String> inc = filterBySearch(manager.getIncludedTargetNames(mc), targetsSearch.text());
            int maxScroll = Math.max(0, inc.size() - LIST_VISIBLE);
            includedScroll = Math.max(0, Math.min(maxScroll, includedScroll - delta));
            return true;
        }
        if (rect(mouseX, mouseY, exListX, exListY, exListW, exListH)) {
            List<String> exc = filterBySearch(manager.getExcludedNamesSorted(), targetsSearch.text());
            int maxScroll = Math.max(0, exc.size() - LIST_VISIBLE);
            excludedScroll = Math.max(0, Math.min(maxScroll, excludedScroll - delta));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (manualDropdown.keyPressed(keyCode, manager::addManualPlayer)) {
            return true;
        }
        if (excludeDropdown.keyPressed(keyCode, manager::addExcludedPlayer)) {
            return true;
        }
        if (focusField == FocusField.TARGETS_SEARCH) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                focusField = FocusField.NONE;
                return true;
            }
            return targetsSearch.keyPressed(keyCode);
        }

        OverlayTextField active = switch (focusField) {
            case AMOUNT -> amountInput;
            case COMMAND -> commandInput;
            case BALANCE_COMMAND -> balanceCommandInput;
            default -> null;
        };
        if (active == null) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            focusField = FocusField.NONE;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (focusField == FocusField.COMMAND) {
                manager.setPayCommand(commandInput.text().trim());
                commandInput.setText(manager.getPayCommand());
            } else if (focusField == FocusField.BALANCE_COMMAND) {
                manager.setBalanceCommand(balanceCommandInput.text().trim());
                balanceCommandInput.setText(manager.getBalanceCommand());
            }
            return true;
        }
        return active.keyPressed(keyCode);
    }

    @Override
    public boolean charTyped(int codePoint) {
        if (manualDropdown.charTyped(codePoint)) {
            return true;
        }
        if (excludeDropdown.charTyped(codePoint)) {
            return true;
        }
        if (focusField == FocusField.TARGETS_SEARCH) {
            return targetsSearch.charTyped(codePoint);
        }
        OverlayTextField active = switch (focusField) {
            case AMOUNT -> amountInput;
            case COMMAND -> commandInput;
            case BALANCE_COMMAND -> balanceCommandInput;
            default -> null;
        };
        if (active == null) {
            return false;
        }
        if (active.charTyped(codePoint)) {
            if (focusField == FocusField.COMMAND) {
                manager.setPayCommand(commandInput.text().trim());
            } else if (focusField == FocusField.BALANCE_COMMAND) {
                manager.setBalanceCommand(balanceCommandInput.text().trim());
            }
            return true;
        }
        return false;
    }

    private boolean clickSlider(double mouseX, double mouseY, int sx, int sy, int sw) {
        int barX = sx + 76;
        int barW = sw - 82 - 36;
        if (!rect(mouseX, mouseY, barX, sy + 1, barW, 8)) {
            return false;
        }
        sliderMode = SliderMode.DELAY;
        manager.setDelayMs((long) sliderValue(mouseX, barX, barW, 0.0, 5000.0));
        return true;
    }

    private boolean rect(double mouseX, double mouseY, int sx, int sy, int sw, int sh) {
        return mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + sh;
    }

    private static int scrollDelta(double verticalAmount) {
        return (int) Math.signum(verticalAmount) * Math.max(1, (int) Math.ceil(Math.abs(verticalAmount)));
    }

    private double sliderValue(double mouseX, int x, int w, double min, double max) {
        double t = (mouseX - x) / (double) w;
        t = Mth.clamp(t, 0.0, 1.0);
        return min + (max - min) * t;
    }

    private String format1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private void drawTrimmedText(GuiGraphics context, String value, int x, int y, int maxWidth, int color) {
        Font tr = Minecraft.getInstance().font;
        String trimmed = tr.plainSubstrByWidth(value, Math.max(6, maxWidth));
        context.drawString(tr, Component.literal(trimmed), x, y, color);
    }

    private void cyclePayCommand(Minecraft mc) {
        List<String> options = manager.getPayCommandOptions(mc);
        if (options.isEmpty()) {
            return;
        }
        int idx = EconomyCommandDetector.indexOfIgnoreCase(options, manager.getPayCommand());
        int next = (idx + 1 + options.size()) % options.size();
        manager.setPayCommand(options.get(next));
        commandInput.setText(manager.getPayCommand());
    }

    private void cycleBalanceCommand(Minecraft mc) {
        List<String> options = manager.getBalanceCommandOptions(mc);
        if (options.isEmpty()) {
            return;
        }
        int idx = EconomyCommandDetector.indexOfIgnoreCase(options, manager.getBalanceCommand());
        int next = (idx + 1 + options.size()) % options.size();
        manager.setBalanceCommand(options.get(next));
        balanceCommandInput.setText(manager.getBalanceCommand());
    }

    private enum FocusField {
        NONE,
        AMOUNT,
        COMMAND,
        BALANCE_COMMAND,
        TARGETS_SEARCH
    }

    private enum SliderMode {
        NONE,
        DELAY
    }

    private record PayAllLayout(
            int tx,
            int innerX,
            int rowW,
            int halfW,
            int actionBtnW,
            int cardW,
            int cardTop,
            int cardH,
            int chatY,
            int amountLabelY,
            int amountRowY,
            int delayY,
            int actionsY,
            int progressY,
            int targetsY,
            int summaryY,
            int errorY,
            int pickerY,
            int payLabelY,
            int payRowY,
            int balLabelY,
            int balRowY,
            int manualLabelY,
            int addRowY,
            int staffY,
            int clearY,
            int logLabelY,
            int logBoxY,
            int panelH) {
    }
}
