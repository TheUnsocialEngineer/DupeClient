package com.dupeclient.client.module.fuzzer;

import com.dupeclient.client.module.fuzzer.economy.EconomyCommandDetector;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerManager;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerSettings;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.overlay.AbstractDraggableOverlay;
import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import com.dupeclient.client.gui.overlay.SearchableDropdown;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class FuzzerOverlay extends AbstractDraggableOverlay implements IngameModuleOverlay {
    public static final FuzzerOverlay INSTANCE = new FuzzerOverlay();

    private static final int PANEL_W = 304;
    private static final int GAP = 5;
    private static final int PAD = 8;
    private static final int LABEL_COL = 64;
    private static final int CTRL_H = 16;
    private static final int ROW_STEP = CTRL_H + GAP;
    private static final int TAB_H = 14;
    private static final int LOG_LINES = 5;
    private static final int LOG_LINE_H = 10;
    private int logScrollOffset;
    private int hitLogX;
    private int hitLogY;
    private int hitLogW;
    private int hitLogH;
    private static final int TITLE_H = 12;
    private static final int SMALL_BTN_W = 22;
    private static final int SCAN_BTN_W = 44;

    private final SearchableDropdown targetDropdown = new SearchableDropdown("(none)");
    private final SearchableDropdown payCmdDropdown = new SearchableDropdown("pay");
    private final SearchableDropdown sqliCmdDropdown = new SearchableDropdown("Select command…", 6);
    private final SearchableDropdown miniTargetDropdown = new SearchableDropdown("(broadcast)");

    private Tab activeTab = Tab.ECONOMY;
    private int hitTabEcoX;
    private int hitTabSqliX;
    private int hitTabMiniX;
    private int hitTabY;
    private int hitTabW;
    private int bodyY;
    private int hitSyntaxX;
    private int hitSyntaxY;
    private int hitSyntaxW;
    private int hitDelayMinusX;
    private int hitDelayPlusX;
    private int hitDelayY;
    private int hitDelayBtnW;
    private int hitStartX;
    private int hitStopX;
    private int hitPauseX;
    private int hitClearX;
    private int hitBtnY;
    private int hitBtnW;
    private int hitScanX;
    private int hitScanY;
    private int hitScanW;
    private int hitSqliDestructiveX;
    private int hitSqliDestructiveY;
    private int hitSqliDestructiveW;
    private int hitMiniSendX;
    private int hitMiniSendY;
    private int hitMiniSendW;

    private FuzzerOverlay() {
        payCmdDropdown.setAllowCustomEntry(true);
        sqliCmdDropdown.setAllowCustomEntry(true);
    }

    @Override
    public String id() {
        return "fuzzer";
    }

    @Override
    public boolean isModuleEnabled() {
        return EconomyFuzzerManager.INSTANCE.getSettings().enabled;
    }

    @Override
    public boolean isOverlayVisible() {
        return EconomyFuzzerManager.INSTANCE.getSettings().overlayVisible;
    }

    @Override
    public void setOverlayVisible(boolean visible) {
        if (visible) {
            com.dupeclient.client.gui.overlay.IngameOverlayHost.onModuleOverlayOpening(this);
        }
        EconomyFuzzerSettings s = EconomyFuzzerManager.INSTANCE.getSettings();
        s.overlayVisible = visible;
        if (!visible) {
            closeAllDropdowns();
        }
        EconomyFuzzerManager.INSTANCE.save();
    }

    @Override
    public int overlayX() {
        return EconomyFuzzerManager.INSTANCE.getSettings().overlayX;
    }

    @Override
    public int overlayY() {
        return EconomyFuzzerManager.INSTANCE.getSettings().overlayY;
    }

    @Override
    public void setOverlayPosition(int x, int y) {
        EconomyFuzzerSettings s = EconomyFuzzerManager.INSTANCE.getSettings();
        s.overlayX = x;
        s.overlayY = y;
        EconomyFuzzerManager.INSTANCE.save();
    }

    @Override
    public int panelWidth() {
        return PANEL_W;
    }

    private int dropdownExtra() {
        return switch (activeTab) {
            case ECONOMY -> Math.max(targetDropdown.extraHeight(), payCmdDropdown.extraHeight());
            case SQLI -> sqliCmdDropdown.extraHeight();
            case MINIMESSAGE -> miniTargetDropdown.extraHeight();
        };
    }

    private int tabBodyRows() {
        return switch (activeTab) {
            case ECONOMY -> 5;
            case SQLI -> 5;
            case MINIMESSAGE -> 5;
        };
    }

    private int bodyContentHeight() {
        int rows = tabBodyRows() * ROW_STEP;
        return rows + dropdownExtra() + GAP;
    }

    @Override
    public int panelHeight() {
        return TITLE_H + GAP + TAB_H + GAP + GAP
                + bodyContentHeight() + GAP
                + CTRL_H + GAP + 10 + GAP + LOG_LINES * LOG_LINE_H + 4 + PAD;
    }

    @Override
    public boolean hasTextFocus() {
        return targetDropdown.hasTextFocus() || payCmdDropdown.hasTextFocus()
                || sqliCmdDropdown.hasTextFocus() || miniTargetDropdown.hasTextFocus();
    }

    private void closeAllDropdowns() {
        targetDropdown.close();
        payCmdDropdown.close();
        sqliCmdDropdown.close();
        miniTargetDropdown.close();
    }

    private void syncTabFromSettings() {
        EconomyFuzzerSettings s = EconomyFuzzerManager.INSTANCE.getSettings();
        if (s.fuzzerTab != null) {
            activeTab = Tab.fromId(s.fuzzerTab);
        }
    }

    private void persistTab() {
        EconomyFuzzerSettings s = EconomyFuzzerManager.INSTANCE.getSettings();
        s.fuzzerTab = activeTab.id;
        EconomyFuzzerManager.INSTANCE.save();
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!isActive()) {
            return;
        }
        syncTabFromSettings();
        EconomyFuzzerSettings s = EconomyFuzzerManager.INSTANCE.getSettings();
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        Font tr = client.font;
        int px = s.overlayX;
        int py = s.overlayY;
        int contentRight = px + PANEL_W - PAD;

        context.fill(px, py, px + PANEL_W, py + panelHeight(), 0xE018181B);
        context.fill(px, py, px + PANEL_W, py + TITLE_H, 0xFF27272A);
        context.text(tr, Component.literal("Fuzzer"), px + PAD, py + 2, 0xFFFBBF24);

        int tabY = py + TITLE_H + GAP;
        int tabW = (PANEL_W - PAD * 2 - GAP * 2) / 3;
        hitTabY = tabY;
        hitTabW = tabW;
        hitTabEcoX = px + PAD;
        hitTabSqliX = hitTabEcoX + tabW + GAP;
        hitTabMiniX = hitTabSqliX + tabW + GAP;
        UiComponents.drawSegmentTab(tr, context, hitTabEcoX, tabY, tabW, TAB_H, "Economy", activeTab == Tab.ECONOMY);
        UiComponents.drawSegmentTab(tr, context, hitTabSqliX, tabY, tabW, TAB_H, "SQLI", activeTab == Tab.SQLI);
        UiComponents.drawSegmentTab(tr, context, hitTabMiniX, tabY, tabW, TAB_H, "MiniMsg", activeTab == Tab.MINIMESSAGE);

        bodyY = tabY + TAB_H + GAP + GAP;
        int y = bodyY;
        int controlW = contentRight - (px + LABEL_COL);

        switch (activeTab) {
            case ECONOMY -> y = renderEconomy(context, tr, client, s, px, y, controlW, contentRight, mouseX, mouseY);
            case SQLI -> y = renderSqli(context, tr, client, px, y, controlW, contentRight, mouseX, mouseY);
            case MINIMESSAGE -> y = renderMinimessage(context, tr, client, px, y, controlW, contentRight, mouseX, mouseY);
        }

        y += GAP;
        renderActionButtons(context, tr, px, y, mouseX, mouseY);
        y += CTRL_H + GAP;
        context.text(tr, Component.literal("Log"), px + PAD, y, 0xFF6B7280);
        y += 10 + GAP;
        int logBoxH = LOG_LINES * LOG_LINE_H + 4;
        hitLogX = px + 4;
        hitLogY = y - 2;
        hitLogW = PANEL_W - 8;
        hitLogH = logBoxH + 2;
        context.fill(hitLogX, hitLogY, hitLogX + hitLogW, hitLogY + hitLogH, 0xFF121218);
        List<String> logs = allActiveLogs();
        int maxLogScroll = Math.max(0, logs.size() - LOG_LINES);
        logScrollOffset = Math.max(0, Math.min(maxLogScroll, logScrollOffset));
        int start = Math.max(0, logs.size() - LOG_LINES - logScrollOffset);
        for (int i = 0; i < LOG_LINES; i++) {
            int idx = start + i;
            String line = idx < logs.size() ? logs.get(idx) : "";
            int color = line.contains("SUSPECT") || line.contains("TAG_LEAK") || line.contains("ABNORMAL")
                    || line.contains("ACCEPTED_BAD") ? 0xFFFF6B6B : 0xFF9CA3AF;
            context.text(tr, Component.literal(tr.plainSubstrByWidth(line, PANEL_W - PAD * 2)), px + PAD, y, color);
            y += LOG_LINE_H;
        }

        renderOpenDropdownPopups(context, tr, client, mouseX, mouseY);
    }

    private void renderOpenDropdownPopups(
            GuiGraphicsExtractor context, Font tr, Minecraft client, int mouseX, int mouseY) {
        switch (activeTab) {
            case ECONOMY -> {
                targetDropdown.renderPopupLayer(context, tr,
                        EconomyFuzzerManager.INSTANCE.onlinePlayerNames(client), mouseX, mouseY);
                payCmdDropdown.renderPopupLayer(context, tr,
                        EconomyFuzzerManager.INSTANCE.getPayCommandOptions(client), mouseX, mouseY);
            }
            case SQLI -> sqliCmdDropdown.renderPopupLayer(context, tr,
                    CommandEnumerator.allCommandPaths(client), mouseX, mouseY);
            case MINIMESSAGE -> miniTargetDropdown.renderPopupLayer(context, tr,
                    EconomyFuzzerManager.INSTANCE.onlinePlayerNames(client), mouseX, mouseY);
        }
    }

    private int renderEconomy(
            GuiGraphicsExtractor context, Font tr, Minecraft client, EconomyFuzzerSettings s,
            int px, int y, int controlW, int contentRight, int mouseX, int mouseY) {
        EconomyFuzzerManager mgr = EconomyFuzzerManager.INSTANCE;
        boolean needsTarget = mgr.commandNeedsTarget();
        drawLabel(context, tr, "Target", px + PAD, y);
        targetDropdown.setDisplayValue(needsTarget
                ? (s.targetPlayer == null ? "" : s.targetPlayer.trim())
                : "(not used)");
        targetDropdown.render(context, tr, px + LABEL_COL, y, controlW, CTRL_H, mgr.onlinePlayerNames(client), mouseX, mouseY);
        y += ROW_STEP + targetDropdown.extraHeight();

        drawLabel(context, tr, "Pay cmd", px + PAD, y);
        payCmdDropdown.setDisplayValue(s.payCommand == null || s.payCommand.isBlank() ? "pay" : s.payCommand.trim());
        payCmdDropdown.render(context, tr, px + LABEL_COL, y, controlW, CTRL_H, mgr.getPayCommandOptions(client), mouseX, mouseY);
        y += ROW_STEP + payCmdDropdown.extraHeight();

        drawLabel(context, tr, "Syntax", px + PAD, y);
        hitSyntaxX = px + LABEL_COL;
        hitSyntaxY = y;
        hitSyntaxW = controlW;
        String syntax = EconomyCommandDetector.syntaxModeLabel(s.syntaxMode, s.payCommand);
        drawButton(context, tr, hitSyntaxX, hitSyntaxY, hitSyntaxW, CTRL_H, syntax, mouseX, mouseY, true);
        y += ROW_STEP + GAP;

        drawLabel(context, tr, "Delay ms", px + PAD, y);
        hitDelayBtnW = SMALL_BTN_W;
        hitDelayMinusX = px + LABEL_COL;
        hitDelayPlusX = contentRight - hitDelayBtnW;
        hitDelayY = y;
        drawButton(context, tr, hitDelayMinusX, hitDelayY, hitDelayBtnW, CTRL_H, "-", mouseX, mouseY, true);
        int delayCenterX = hitDelayMinusX + hitDelayBtnW + (hitDelayPlusX - hitDelayMinusX - hitDelayBtnW) / 2;
        context.centeredText(tr, Component.literal(Long.toString(s.delayMs)), delayCenterX, y + 4, 0xFF86EFAC);
        drawButton(context, tr, hitDelayPlusX, hitDelayY, hitDelayBtnW, CTRL_H, "+", mouseX, mouseY, true);
        y += ROW_STEP + GAP;

        drawLabel(context, tr, "Status", px + PAD, y);
        String status = mgr.isRunning()
                ? (mgr.isPaused() ? "Paused" : "Running") + " " + mgr.getFuzzIndex() + "/" + mgr.getFuzzTotal()
                : "Idle";
        int statusW = tr.width(status);
        context.text(tr, Component.literal(status), contentRight - statusW, y + 4,
                mgr.isRunning() ? 0xFF86EFAC : 0xFF9CA3AF);
        return y + ROW_STEP;
    }

    private int renderSqli(
            GuiGraphicsExtractor context, Font tr, Minecraft client,
            int px, int y, int controlW, int contentRight, int mouseX, int mouseY) {
        SqliFuzzerManager mgr = SqliFuzzerManager.INSTANCE;
        CommandArgDiscovery discovery = CommandArgDiscovery.INSTANCE;
        drawLabel(context, tr, "Command", px + PAD, y);
        hitScanX = px + LABEL_COL;
        hitScanY = y;
        hitScanW = SCAN_BTN_W;
        int dropdownX = hitScanX + SCAN_BTN_W + GAP;
        int dropdownW = contentRight - dropdownX;
        boolean discovering = discovery.isDiscovering();
        drawButton(context, tr, hitScanX, hitScanY, hitScanW, CTRL_H,
                discovering ? "…" : "Scan", mouseX, mouseY, !discovering);
        sqliCmdDropdown.setDisplayValue(mgr.getCommand());
        sqliCmdDropdown.render(context, tr, dropdownX, y, dropdownW, CTRL_H,
                CommandEnumerator.allCommandPaths(client), mouseX, mouseY);
        y += ROW_STEP + sqliCmdDropdown.extraHeight();

        drawLabel(context, tr, "Inject", px + PAD, y);
        String inject = mgr.getArgSummary();
        int injectColor = inject.equals("append") ? 0xFF9CA3AF : 0xFF86EFAC;
        context.text(tr, Component.literal(tr.plainSubstrByWidth(inject, controlW)), px + LABEL_COL, y + 4, injectColor);
        y += ROW_STEP;

        EconomyFuzzerSettings sqliSettings = EconomyFuzzerManager.INSTANCE.getSettings();
        drawLabel(context, tr, "Payloads", px + PAD, y);
        hitSqliDestructiveX = px + LABEL_COL;
        hitSqliDestructiveY = y;
        hitSqliDestructiveW = controlW;
        String payloadMode = sqliSettings.sqliDestructivePayloads
                ? "Enum + destructive (" + mgr.payloadCount() + ")"
                : "Enum only (" + mgr.payloadCount() + ")";
        drawButton(context, tr, hitSqliDestructiveX, hitSqliDestructiveY, hitSqliDestructiveW, CTRL_H,
                tr.plainSubstrByWidth(payloadMode, hitSqliDestructiveW - 8), mouseX, mouseY, true);
        y += ROW_STEP;

        drawLabel(context, tr, "Delay ms", px + PAD, y);
        hitDelayBtnW = SMALL_BTN_W;
        hitDelayMinusX = px + LABEL_COL;
        hitDelayPlusX = contentRight - hitDelayBtnW;
        hitDelayY = y;
        drawButton(context, tr, hitDelayMinusX, hitDelayY, hitDelayBtnW, CTRL_H, "-", mouseX, mouseY, true);
        long delay = EconomyFuzzerManager.INSTANCE.getSettings().sqliDelayMs;
        int delayCenterX = hitDelayMinusX + hitDelayBtnW + (hitDelayPlusX - hitDelayMinusX - hitDelayBtnW) / 2;
        context.centeredText(tr, Component.literal(Long.toString(delay)), delayCenterX, y + 4, 0xFF86EFAC);
        drawButton(context, tr, hitDelayPlusX, hitDelayY, hitDelayBtnW, CTRL_H, "+", mouseX, mouseY, true);
        y += ROW_STEP + GAP;

        drawLabel(context, tr, "Status", px + PAD, y);
        String status = sqliStatusLine(mgr, discovery, client);
        context.text(tr, Component.literal(tr.plainSubstrByWidth(status, controlW + LABEL_COL - PAD)),
                contentRight - tr.width(tr.plainSubstrByWidth(status, controlW + LABEL_COL - PAD)), y + 4, 0xFF9CA3AF);
        return y + ROW_STEP;
    }

    private static String sqliStatusLine(SqliFuzzerManager mgr, CommandArgDiscovery discovery, Minecraft client) {
        if (discovery.isDiscovering()) {
            return discovery.status();
        }
        if (mgr.isRunning()) {
            return (mgr.isPaused() ? "Paused" : "Running") + " " + mgr.getIndex() + "/" + mgr.getTotal();
        }
        int paths = discovery.pathCount(client);
        return paths > 0 ? paths + " cmds · Idle" : "Idle · tap Scan";
    }

    private int renderMinimessage(
            GuiGraphicsExtractor context, Font tr, Minecraft client,
            int px, int y, int controlW, int contentRight, int mouseX, int mouseY) {
        MinimessageFuzzerManager mgr = MinimessageFuzzerManager.INSTANCE;
        boolean msgMode = mgr.isMsgMode();
        drawLabel(context, tr, "Target", px + PAD, y);
        miniTargetDropdown.setDisplayValue(mgr.getTarget());
        miniTargetDropdown.render(context, tr, px + LABEL_COL, y, controlW, CTRL_H,
                EconomyFuzzerManager.INSTANCE.onlinePlayerNames(client), mouseX, mouseY);
        y += ROW_STEP + miniTargetDropdown.extraHeight();

        drawLabel(context, tr, "Send via", px + PAD, y);
        hitMiniSendX = px + LABEL_COL;
        hitMiniSendY = y;
        hitMiniSendW = controlW;
        drawButton(context, tr, hitMiniSendX, hitMiniSendY, hitMiniSendW, CTRL_H,
                mgr.sendModeLabel(), mouseX, mouseY, true);
        y += ROW_STEP;

        drawLabel(context, tr, msgMode ? "Target req" : "Channel", px + PAD, y);
        String hint = msgMode
                ? (mgr.getTarget().isBlank() ? "pick player" : mgr.getTarget())
                : "public chat";
        int hintColor = msgMode && mgr.getTarget().isBlank() ? 0xFFFF6B6B : 0xFF9CA3AF;
        context.text(tr, Component.literal(tr.plainSubstrByWidth(hint, controlW)), px + LABEL_COL, y + 4, hintColor);
        y += ROW_STEP;

        drawLabel(context, tr, "Delay ms", px + PAD, y);
        hitDelayBtnW = SMALL_BTN_W;
        hitDelayMinusX = px + LABEL_COL;
        hitDelayPlusX = contentRight - hitDelayBtnW;
        hitDelayY = y;
        drawButton(context, tr, hitDelayMinusX, hitDelayY, hitDelayBtnW, CTRL_H, "-", mouseX, mouseY, true);
        long delay = EconomyFuzzerManager.INSTANCE.getSettings().minimessageDelayMs;
        int delayCenterX = hitDelayMinusX + hitDelayBtnW + (hitDelayPlusX - hitDelayMinusX - hitDelayBtnW) / 2;
        context.centeredText(tr, Component.literal(Long.toString(delay)), delayCenterX, y + 4, 0xFF86EFAC);
        drawButton(context, tr, hitDelayPlusX, hitDelayY, hitDelayBtnW, CTRL_H, "+", mouseX, mouseY, true);
        y += ROW_STEP + GAP;

        drawLabel(context, tr, "Status", px + PAD, y);
        String status = mgr.isRunning()
                ? (mgr.isPaused() ? "Paused" : "Running") + " " + mgr.getIndex() + "/" + mgr.getTotal()
                : "Idle";
        context.text(tr, Component.literal(status), contentRight - tr.width(status), y + 4, 0xFF9CA3AF);
        return y + ROW_STEP;
    }

    private void renderActionButtons(GuiGraphicsExtractor context, Font tr, int px, int y, int mouseX, int mouseY) {
        hitBtnY = y;
        int btnAvail = PANEL_W - PAD * 2;
        hitBtnW = (btnAvail - GAP * 3) / 4;
        hitStartX = px + PAD;
        hitStopX = hitStartX + hitBtnW + GAP;
        hitPauseX = hitStopX + hitBtnW + GAP;
        hitClearX = hitPauseX + hitBtnW + GAP;
        boolean running = isTabRunning();
        boolean paused = isTabPaused();
        drawButton(context, tr, hitStartX, hitBtnY, hitBtnW, CTRL_H, "Start", mouseX, mouseY, !running);
        drawButton(context, tr, hitStopX, hitBtnY, hitBtnW, CTRL_H, "Stop", mouseX, mouseY, running);
        drawButton(context, tr, hitPauseX, hitBtnY, hitBtnW, CTRL_H, paused ? "Resume" : "Pause", mouseX, mouseY, running);
        drawButton(context, tr, hitClearX, hitBtnY, hitBtnW, CTRL_H, "Clear", mouseX, mouseY, true);
    }

    private List<String> allActiveLogs() {
        return switch (activeTab) {
            case ECONOMY -> EconomyFuzzerManager.INSTANCE.getLogs();
            case SQLI -> SqliFuzzerManager.INSTANCE.getLogs();
            case MINIMESSAGE -> MinimessageFuzzerManager.INSTANCE.getLogs();
        };
    }

    private boolean isTabRunning() {
        return switch (activeTab) {
            case ECONOMY -> EconomyFuzzerManager.INSTANCE.isRunning();
            case SQLI -> SqliFuzzerManager.INSTANCE.isRunning();
            case MINIMESSAGE -> MinimessageFuzzerManager.INSTANCE.isRunning();
        };
    }

    private boolean isTabPaused() {
        return switch (activeTab) {
            case ECONOMY -> EconomyFuzzerManager.INSTANCE.isPaused();
            case SQLI -> SqliFuzzerManager.INSTANCE.isPaused();
            case MINIMESSAGE -> MinimessageFuzzerManager.INSTANCE.isPaused();
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isActive()) {
            return false;
        }
        int px = overlayX();
        int py = overlayY();
        boolean onPanel = containsPoint(mouseX, mouseY);
        boolean onDropdown = dropdownHits(mouseX, mouseY);
        if (!onPanel && !onDropdown) {
            return false;
        }
        if (!onPanel && onDropdown) {
            Minecraft client = Minecraft.getInstance();
            return handleTabInput(mouseX, mouseY, button, client);
        }
        if (beginTitleDrag(mouseX, mouseY, button, px, py, PANEL_W, TITLE_H)) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        if (inBtn(mouseX, mouseY, hitTabEcoX, hitTabY, hitTabW, TAB_H)) {
            activeTab = Tab.ECONOMY;
            logScrollOffset = 0;
            closeAllDropdowns();
            persistTab();
            return true;
        }
        if (inBtn(mouseX, mouseY, hitTabSqliX, hitTabY, hitTabW, TAB_H)) {
            activeTab = Tab.SQLI;
            logScrollOffset = 0;
            closeAllDropdowns();
            persistTab();
            return true;
        }
        if (inBtn(mouseX, mouseY, hitTabMiniX, hitTabY, hitTabW, TAB_H)) {
            activeTab = Tab.MINIMESSAGE;
            logScrollOffset = 0;
            closeAllDropdowns();
            persistTab();
            return true;
        }
        if (handleTabInput(mouseX, mouseY, button, client)) {
            return true;
        }
        if (handleButtons(mouseX, mouseY, client)) {
            return true;
        }
        return true;
    }

    private boolean dropdownHits(double mouseX, double mouseY) {
        return switch (activeTab) {
            case ECONOMY -> targetDropdown.hitsInteractive(mouseX, mouseY) || payCmdDropdown.hitsInteractive(mouseX, mouseY);
            case SQLI -> sqliCmdDropdown.hitsInteractive(mouseX, mouseY);
            case MINIMESSAGE -> miniTargetDropdown.hitsInteractive(mouseX, mouseY);
        };
    }

    private boolean handleTabInput(double mouseX, double mouseY, int button, Minecraft client) {
        EconomyFuzzerSettings s = EconomyFuzzerManager.INSTANCE.getSettings();
        return switch (activeTab) {
            case ECONOMY -> {
                if (EconomyFuzzerManager.INSTANCE.commandNeedsTarget()
                        && targetDropdown.mouseClicked(mouseX, mouseY, button,
                        EconomyFuzzerManager.INSTANCE.onlinePlayerNames(client), EconomyFuzzerManager.INSTANCE::setTargetPlayer)) {
                    payCmdDropdown.close();
                    yield true;
                }
                if (payCmdDropdown.mouseClicked(mouseX, mouseY, button,
                        EconomyFuzzerManager.INSTANCE.getPayCommandOptions(client), EconomyFuzzerManager.INSTANCE::setPayCommand)) {
                    targetDropdown.close();
                    yield true;
                }
                if (targetDropdown.isOpen() || payCmdDropdown.isOpen()) {
                    closeAllDropdowns();
                }
                if (inBtn(mouseX, mouseY, hitSyntaxX, hitSyntaxY, hitSyntaxW, CTRL_H)) {
                    EconomyFuzzerManager.INSTANCE.cycleSyntaxMode();
                    yield true;
                }
                if (adjustDelay(mouseX, mouseY, true)) {
                    yield true;
                }
                yield false;
            }
            case SQLI -> {
                if (inBtn(mouseX, mouseY, hitScanX, hitScanY, hitScanW, CTRL_H)) {
                    if (!CommandArgDiscovery.INSTANCE.isDiscovering()) {
                        CommandArgDiscovery.INSTANCE.startDiscovery(client);
                    }
                    yield true;
                }
                if (sqliCmdDropdown.mouseClicked(mouseX, mouseY, button,
                        CommandEnumerator.allCommandPaths(client), SqliFuzzerManager.INSTANCE::setCommand)) {
                    yield true;
                }
                if (sqliCmdDropdown.isOpen()) {
                    sqliCmdDropdown.close();
                }
                if (inBtn(mouseX, mouseY, hitSqliDestructiveX, hitSqliDestructiveY, hitSqliDestructiveW, CTRL_H)) {
                    SqliFuzzerManager.INSTANCE.toggleDestructivePayloads();
                    yield true;
                }
                if (adjustDelay(mouseX, mouseY, false)) {
                    yield true;
                }
                yield false;
            }
            case MINIMESSAGE -> {
                if (miniTargetDropdown.mouseClicked(mouseX, mouseY, button,
                        EconomyFuzzerManager.INSTANCE.onlinePlayerNames(client), MinimessageFuzzerManager.INSTANCE::setTarget)) {
                    yield true;
                }
                if (miniTargetDropdown.isOpen()) {
                    miniTargetDropdown.close();
                }
                if (inBtn(mouseX, mouseY, hitMiniSendX, hitMiniSendY, hitMiniSendW, CTRL_H)) {
                    MinimessageFuzzerManager.INSTANCE.toggleSendMode();
                    yield true;
                }
                if (adjustDelay(mouseX, mouseY, false)) {
                    yield true;
                }
                yield false;
            }
        };
    }

    private boolean adjustDelay(double mouseX, double mouseY, boolean economy) {
        EconomyFuzzerSettings s = EconomyFuzzerManager.INSTANCE.getSettings();
        if (inBtn(mouseX, mouseY, hitDelayMinusX, hitDelayY, hitDelayBtnW, CTRL_H)) {
            if (economy) {
                s.delayMs = Math.max(100L, s.delayMs - 100L);
            } else if (activeTab == Tab.SQLI) {
                s.sqliDelayMs = Math.max(100L, s.sqliDelayMs - 100L);
            } else {
                s.minimessageDelayMs = Math.max(100L, s.minimessageDelayMs - 100L);
            }
            EconomyFuzzerManager.INSTANCE.save();
            return true;
        }
        if (inBtn(mouseX, mouseY, hitDelayPlusX, hitDelayY, hitDelayBtnW, CTRL_H)) {
            if (economy) {
                s.delayMs = Math.min(10_000L, s.delayMs + 100L);
            } else if (activeTab == Tab.SQLI) {
                s.sqliDelayMs = Math.min(10_000L, s.sqliDelayMs + 100L);
            } else {
                s.minimessageDelayMs = Math.min(10_000L, s.minimessageDelayMs + 100L);
            }
            EconomyFuzzerManager.INSTANCE.save();
            return true;
        }
        return false;
    }

    private boolean handleButtons(double mouseX, double mouseY, Minecraft client) {
        if (inBtn(mouseX, mouseY, hitStartX, hitBtnY, hitBtnW, CTRL_H)) {
            switch (activeTab) {
                case ECONOMY -> EconomyFuzzerManager.INSTANCE.start(client);
                case SQLI -> SqliFuzzerManager.INSTANCE.start(client);
                case MINIMESSAGE -> MinimessageFuzzerManager.INSTANCE.start(client);
            }
            return true;
        }
        if (inBtn(mouseX, mouseY, hitStopX, hitBtnY, hitBtnW, CTRL_H)) {
            switch (activeTab) {
                case ECONOMY -> EconomyFuzzerManager.INSTANCE.stop("Stopped.");
                case SQLI -> SqliFuzzerManager.INSTANCE.stop("Stopped.");
                case MINIMESSAGE -> MinimessageFuzzerManager.INSTANCE.stop("Stopped.");
            }
            return true;
        }
        if (inBtn(mouseX, mouseY, hitPauseX, hitBtnY, hitBtnW, CTRL_H)) {
            switch (activeTab) {
                case ECONOMY -> EconomyFuzzerManager.INSTANCE.togglePause();
                case SQLI -> SqliFuzzerManager.INSTANCE.togglePause();
                case MINIMESSAGE -> MinimessageFuzzerManager.INSTANCE.togglePause();
            }
            return true;
        }
        if (inBtn(mouseX, mouseY, hitClearX, hitBtnY, hitBtnW, CTRL_H)) {
            switch (activeTab) {
                case ECONOMY -> EconomyFuzzerManager.INSTANCE.clearLogs();
                case SQLI -> SqliFuzzerManager.INSTANCE.clearLogs();
                case MINIMESSAGE -> MinimessageFuzzerManager.INSTANCE.clearLogs();
            }
            logScrollOffset = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        endTitleDrag(button);
        return dragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return updateTitleDrag(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isActive()) {
            return false;
        }
        if (inBtn(mouseX, mouseY, hitLogX, hitLogY, hitLogW, hitLogH)) {
            int delta = scrollDelta(verticalAmount);
            List<String> logs = allActiveLogs();
            int maxScroll = Math.max(0, logs.size() - LOG_LINES);
            logScrollOffset = Math.max(0, Math.min(maxScroll, logScrollOffset - delta));
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        return switch (activeTab) {
            case ECONOMY -> {
                if (targetDropdown.mouseScrolled(mouseX, mouseY, verticalAmount,
                        EconomyFuzzerManager.INSTANCE.onlinePlayerNames(client))) {
                    yield true;
                }
                yield payCmdDropdown.mouseScrolled(mouseX, mouseY, verticalAmount,
                        EconomyFuzzerManager.INSTANCE.getPayCommandOptions(client));
            }
            case SQLI -> sqliCmdDropdown.mouseScrolled(mouseX, mouseY, verticalAmount,
                    CommandEnumerator.allCommandPaths(client));
            case MINIMESSAGE -> miniTargetDropdown.mouseScrolled(mouseX, mouseY, verticalAmount,
                    EconomyFuzzerManager.INSTANCE.onlinePlayerNames(client));
        };
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (!isActive()) {
            return false;
        }
        if (targetDropdown.keyPressed(keyCode)) {
            return true;
        }
        if (payCmdDropdown.keyPressed(keyCode, EconomyFuzzerManager.INSTANCE::setPayCommand)) {
            return true;
        }
        if (sqliCmdDropdown.keyPressed(keyCode, SqliFuzzerManager.INSTANCE::setCommand)) {
            return true;
        }
        return miniTargetDropdown.keyPressed(keyCode);
    }

    @Override
    public boolean charTyped(int codePoint) {
        return isActive() && (targetDropdown.charTyped(codePoint) || payCmdDropdown.charTyped(codePoint)
                || sqliCmdDropdown.charTyped(codePoint) || miniTargetDropdown.charTyped(codePoint));
    }

    private static void drawLabel(GuiGraphicsExtractor c, Font tr, String label, int x, int y) {
        c.text(tr, Component.literal(label), x, y + 4, 0xFFA1A1AA);
    }

    private static void drawButton(
            GuiGraphicsExtractor c, Font tr, int x, int y, int w, int h, String label, double mx, double my, boolean enabled) {
        boolean hot = enabled && inBtn(mx, my, x, y, w, h);
        int bg = hot ? 0xFF3F3F46 : (enabled ? 0xFF27272A : 0xFF1C1C1F);
        c.fill(x, y, x + w, y + h, bg);
        c.fill(x, y, x + w, y + 1, enabled ? 0xFF52525B : 0xFF3F3F46);
        c.centeredText(tr, Component.literal(label), x + w / 2, y + (h - 8) / 2, enabled ? 0xFFE5E7EB : 0xFF6B7280);
    }

    private static boolean inBtn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int scrollDelta(double verticalAmount) {
        return (int) Math.signum(verticalAmount) * Math.max(1, (int) Math.ceil(Math.abs(verticalAmount)));
    }

    private enum Tab {
        ECONOMY("economy"),
        SQLI("sqli"),
        MINIMESSAGE("minimessage");

        final String id;

        Tab(String id) {
            this.id = id;
        }

        static Tab fromId(String raw) {
            if (raw == null) {
                return ECONOMY;
            }
            for (Tab t : values()) {
                if (t.id.equalsIgnoreCase(raw.trim())) {
                    return t;
                }
            }
            return ECONOMY;
        }
    }
}
