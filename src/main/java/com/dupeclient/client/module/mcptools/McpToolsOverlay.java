package com.dupeclient.client.module.mcptools;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.overlay.AbstractDraggableOverlay;
import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.gui.overlay.OverlayTextField;
import com.dupeclient.client.gui.overlay.SearchableDropdown;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class McpToolsOverlay extends AbstractDraggableOverlay implements IngameModuleOverlay {
    public static final McpToolsOverlay INSTANCE = new McpToolsOverlay();

    private static final int PANEL_W = 440;
    private static final int TITLE_H = 12;
    private static final int PAD = 8;
    private static final int GAP = 6;
    private static final int INPUT_H = 14;
    private static final int BTN_H = 14;
    private static final int LOG_LINES = 6;
    private static final int LOG_LINE_H = 10;
    private static final int LOG_CHARS = 90;
    private static final int UPLOAD_H = 48;
    private static final int LABEL_W = 58;
    private static final int SPLIT_LABEL_W = 36;
    private static final int USE_CURRENT_BTN_W = 52;
    private static final int ROSTER_ROW_H = 13;
    private static final int ROSTER_COLS = 4;
    private static final int ROSTER_GRID_ROWS = 2;

    private static final int PANEL_BG = 0xE010101B;
    private static final int TITLE_BG = 0xFF27272A;
    private static final int TITLE_FG = 0xFF34D399;
    private static final int LOG_BG = 0xFF12121A;

    private final McpToolsManager manager = McpToolsManager.INSTANCE;
    private final SearchableDropdown versionDropdown = new SearchableDropdown("Select version…", 8);
    private final SearchableDropdown mineBlockDropdown = new SearchableDropdown("Search blocks…", 8);
    private final OverlayTextField hostInput = OverlayTextField.create(253);
    private final OverlayTextField portInput = OverlayTextField.digits(5);
    private final OverlayTextField userInput = OverlayTextField.create(16);
    private final OverlayTextField chatInput = OverlayTextField.create(256);
    private final OverlayTextField spawnCountInput = OverlayTextField.digits(2);
    private final OverlayTextField loginTimeoutInput = OverlayTextField.digits(3);
    private final OverlayTextField joinDelayInput = OverlayTextField.digits(2);
    private final OverlayTextField uploadInput = OverlayTextField.create(8000);
    private final OverlayTextField gotoXInput = OverlayTextField.create(8);
    private final OverlayTextField gotoYInput = OverlayTextField.create(8);
    private final OverlayTextField gotoZInput = OverlayTextField.create(8);
    private final List<BotRowHit> botRowHits = new ArrayList<>();
    private final List<MovementHit> movementHits = new ArrayList<>();
    private final Set<String> heldMovements = new HashSet<>();

    private FocusField focusField = FocusField.NONE;
    private int logScrollOffset;
    private int rosterScrollOffset;

    private int versionFieldX;
    private int versionFieldY;
    private int versionFieldW;
    private int hostRowY;
    private int hostFieldX;
    private int hostFieldW;
    private int hostUseCurrentX;
    private int logBoxX;
    private int logBoxY;
    private int logBoxW;
    private int logBoxH;
    private int chatFieldX;
    private int chatFieldY;
    private int chatFieldW;
    private int chatSendX;
    private int chatSendW;
    private int rosterTargetX;
    private int rosterTargetY;
    private int rosterTargetW;
    private int rosterAddX;
    private int rosterJoinBotsX;
    private int rosterJoinBotsW;
    private int rosterSelectAllX;
    private int rosterSelectNoneX;
    private int rosterDisconnectAllX;
    private int rosterBtnRowY;
    private int rosterBtnRow2Y;
    private int rosterListY;
    private int botRow1Y;
    private int botRow2Y;
    private int botRow3Y;
    private int botPathRowY;
    private int botMineRowY;
    private int gotoFieldX;
    private int gotoFieldY;
    private int gotoFieldW;
    private int gotoGoX;
    private int gotoGoW;
    private int pathComeX;
    private int pathStopX;
    private int pathBtnW;
    private int mineFieldX;
    private int mineFieldY;
    private int mineFieldW;
    private int mineSendX;
    private int mineSendW;
    private int botBtnW;
    private int portFieldX;
    private int portFieldW;
    private int userFieldX;
    private int userFieldW;
    private int spawnFieldX;
    private int spawnFieldW;
    private int joinDelayFieldX;
    private int joinDelayFieldW;
    private int timeoutFieldX;
    private int timeoutFieldW;
    private int portRowY;
    private int userRowY;
    private int spawnRowY;
    private int joinDelayRowY;
    private int timeoutRowY;
    private int stopFocusedX;
    private int stopFocusedY;
    private int stopFocusedW;
    private int rosterGridH;

    private McpToolsOverlay() {
        versionDropdown.setModernChrome(true);
        mineBlockDropdown.setModernChrome(true);
        mineBlockDropdown.setDisplayValue(McpToolsBlockCatalog.dropdownLabelForMineId("diamond_ore"));
        syncFromSettings();
    }

    private void syncFromSettings() {
        McpToolsSettings s = manager.getSettings();
        hostInput.setText(s.lastHost);
        portInput.setText(String.valueOf(s.lastPort));
        userInput.setText(s.lastUsername);
        spawnCountInput.setText(String.valueOf(Math.max(1, s.botSpawnCount)));
        loginTimeoutInput.setText(String.valueOf(Math.max(30, Math.min(600, s.botLoginTimeoutSec))));
        joinDelayInput.setText(String.valueOf(Math.max(1, Math.min(60, s.botJoinDelaySec))));
        uploadInput.setText(s.uploadText == null ? "" : s.uploadText);
        McpToolsMcVersion version = McpToolsMcVersion.fromId(s.lastMcVersion);
        versionDropdown.setDisplayValue(version.dropdownLabel());
    }

    private void persistFields() {
        McpToolsSettings s = manager.getSettings();
        s.lastHost = hostInput.text().trim().isEmpty() ? s.lastHost : hostInput.text().trim();
        try {
            s.lastPort = Integer.parseInt(portInput.text().trim());
        } catch (NumberFormatException ignored) {
        }
        McpToolsServerAddress.applyToSettings(s);
        hostInput.setText(s.lastHost);
        portInput.setText(String.valueOf(s.lastPort));
        s.lastUsername = userInput.text().trim().isEmpty() ? s.lastUsername : userInput.text().trim();
        McpToolsMcVersion version = McpToolsMcVersion.fromDropdownLabel(versionDropdown.displayValue());
        s.lastMcVersion = version.id;
        s.lastVersion = version.protocol;
        s.uploadText = uploadInput.text();
        try {
            s.botSpawnCount = Math.max(1, Math.min(McpToolsBotFleet.MAX_BOTS, Integer.parseInt(spawnCountInput.text().trim())));
        } catch (NumberFormatException ignored) {
            s.botSpawnCount = 1;
        }
        try {
            s.botLoginTimeoutSec = Math.max(30, Math.min(600, Integer.parseInt(loginTimeoutInput.text().trim())));
        } catch (NumberFormatException ignored) {
            s.botLoginTimeoutSec = 120;
        }
        loginTimeoutInput.setText(String.valueOf(s.botLoginTimeoutSec));
        try {
            s.botJoinDelaySec = Math.max(1, Math.min(60, Integer.parseInt(joinDelayInput.text().trim())));
        } catch (NumberFormatException ignored) {
            s.botJoinDelaySec = 2;
        }
        joinDelayInput.setText(String.valueOf(s.botJoinDelaySec));
        manager.saveSettings();
    }

    @Override
    public String id() {
        return "mcp_tools";
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
            versionDropdown.close();
            mineBlockDropdown.close();
            logScrollOffset = 0;
            releaseAllHeldMovements();
        } else {
            syncFromSettings();
            clampOverlayToScreen(Minecraft.getInstance());
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
        McpToolsTool tool = McpToolsTool.fromId(manager.getSettings().selectedToolId);
        int fieldRows = tool.interactiveBot ? 5 : 4;
        int h = TITLE_H + PAD + (INPUT_H + GAP) * fieldRows + GAP;
        h += BTN_H + GAP + BTN_H + GAP;
        if (manager.showBotControls()) {
            h += botSectionHeight();
        }
        if (tool.needsUpload) {
            h += UPLOAD_H + GAP + 10;
        }
        h += 10 + GAP + LOG_LINES * LOG_LINE_H + 6 + PAD;
        return clampPanelHeight(h);
    }

    private int clampPanelHeight(int raw) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return raw;
        }
        return Math.min(raw, mc.getWindow().getGuiScaledHeight() - 12);
    }

    private void clampOverlayToScreen(Minecraft mc) {
        if (mc.getWindow() == null) {
            return;
        }
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int ph = panelHeight();
        int pw = panelWidth();
        int x = Math.max(4, Math.min(overlayX(), sw - pw - 4));
        int y = Math.max(4, Math.min(overlayY(), sh - ph - 4));
        if (x != overlayX() || y != overlayY()) {
            setOverlayPosition(x, y);
        }
    }

    @Override
    public boolean containsPoint(double mouseX, double mouseY) {
        if (super.containsPoint(mouseX, mouseY)) {
            return true;
        }
        return versionDropdown.hitsInteractive(mouseX, mouseY)
                || mineBlockDropdown.hitsInteractive(mouseX, mouseY);
    }

    @Override
    public boolean hasTextFocus() {
        return focusField != FocusField.NONE
                || versionDropdown.hasTextFocus()
                || mineBlockDropdown.hasTextFocus();
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Font tr = mc.font;
        McpToolsSettings s = manager.getSettings();
        McpToolsTool tool = McpToolsTool.fromId(s.selectedToolId);
        int px = overlayX();
        int py = overlayY();
        int ph = panelHeight();

        context.fill(px, py, px + PANEL_W, py + ph, PANEL_BG);
        context.fill(px, py, px + PANEL_W, py + TITLE_H, TITLE_BG);
        context.text(tr, Component.literal("MCPTools"), px + PAD, py + 2, TITLE_FG);

        int rx = px + PAD;
        int inner = PANEL_W - PAD * 2;
        int y = py + TITLE_H + PAD;

        hostRowY = y;
        layoutHostRow(rx, inner, y);
        context.text(tr, Component.literal("Host"), rx, y + 3, UiTokens.TEXT_DIM);
        UiComponents.drawTextField(
                tr, context, hostFieldX, y, hostFieldW, INPUT_H, hostInput.text(), focusField == FocusField.HOST);
        UiComponents.drawPillActionButton(
                tr, context, hostUseCurrentX, y, USE_CURRENT_BTN_W, INPUT_H, "Current",
                UiComponents.PillActionStyle.SECONDARY_SLATE);
        y += INPUT_H + GAP;
        if (tool.interactiveBot) {
            y = drawSplitFields(tr, context, rx, y, inner, "Port", portInput, FocusField.PORT,
                    "User", userInput, FocusField.USER);
            y = drawSplitFields(tr, context, rx, y, inner, "Join #", spawnCountInput, FocusField.SPAWN_COUNT,
                    "Delay", joinDelayInput, FocusField.JOIN_DELAY);
            drawLabeledField(tr, context, rx, y, "Timeout", loginTimeoutInput, FocusField.LOGIN_TIMEOUT, inner);
            timeoutFieldX = rx + LABEL_W;
            timeoutFieldW = inner - LABEL_W;
            timeoutRowY = y;
            y += INPUT_H + GAP;
        } else {
            drawLabeledField(tr, context, rx, y, "Port", portInput, FocusField.PORT, inner);
            y += INPUT_H + GAP;
            drawLabeledField(tr, context, rx, y, "User", userInput, FocusField.USER, inner);
            y += INPUT_H + GAP;
        }

        context.text(tr, Component.literal("Version"), rx, y + 3, UiTokens.TEXT_DIM);
        versionFieldX = rx + LABEL_W;
        versionFieldY = y;
        versionFieldW = inner - LABEL_W;
        versionDropdown.render(context, tr, versionFieldX, versionFieldY, versionFieldW, INPUT_H,
                McpToolsMcVersion.dropdownLabels(), mouseX, mouseY);
        y += INPUT_H + GAP;

        int half = (inner - GAP) / 2;
        UiComponents.drawPillActionButton(
                tr, context, rx, y, half, BTN_H, "Tool: " + tool.label, UiComponents.PillActionStyle.SECONDARY_SLATE);
        String runLabel = runButtonLabel(tool);
        UiComponents.drawPillActionButton(
                tr, context, rx + half + GAP, y, half, BTN_H, runLabel,
                tool.interactiveBot || (!manager.isRunning() && !manager.isBotSessionActive())
                        ? UiComponents.PillActionStyle.PRIMARY_MINT
                        : UiComponents.PillActionStyle.SECONDARY_SLATE);
        y += BTN_H + GAP;

        int syncHalf = (inner - GAP) / 2;
        String syncLabel = manager.isSyncing() ? "Syncing…" : "Sync bundle";
        UiComponents.drawPillActionButton(
                tr, context, rx, y, syncHalf, BTN_H, syncLabel, UiComponents.PillActionStyle.PRIMARY_BLUE);
        UiComponents.drawPillActionButton(
                tr, context, rx + syncHalf + GAP, y, syncHalf, BTN_H, "Clear log", UiComponents.PillActionStyle.SECONDARY_SLATE);
        y += BTN_H + GAP;

        if (manager.showBotControls()) {
            y = renderBotControls(tr, context, rx, y, inner, mouseX, mouseY);
        }

        if (tool.needsUpload) {
            context.text(tr, Component.literal(tool == McpToolsTool.BRUTE_AUTH ? "Wordlist" : "Commands"), rx, y, UiTokens.TEXT_DIM);
            y += 10;
            UiComponents.drawTextField(
                    tr, context, rx, y, inner, UPLOAD_H, uploadInput.text(), focusField == FocusField.UPLOAD);
            y += UPLOAD_H + GAP;
        }

        String status = manager.isBotSessionActive()
                ? buildBotStatusLine()
                : manager.isSyncing()
                ? manager.syncStatus()
                : (manager.bundleVersion().isBlank() ? "Bundle not synced" : "Bundle " + manager.bundleVersion()
                + (s.remoteRunner ? " · remote" : " · local"));
        context.text(tr, Component.literal(status), rx, y, UiTokens.TEXT_DIM);
        y += 10 + GAP;

        context.text(tr, Component.literal("Log"), rx, y, UiTokens.TEXT_DIM);
        y += 10;
        logBoxX = rx - 2;
        logBoxY = y - 2;
        logBoxW = inner + 4;
        logBoxH = LOG_LINES * LOG_LINE_H + 4;
        context.fill(logBoxX, logBoxY, logBoxX + logBoxW, logBoxY + logBoxH, LOG_BG);
        renderLog(context, tr, rx, y);

        versionDropdown.renderPopupLayer(context, tr, McpToolsMcVersion.dropdownLabels(), mouseX, mouseY);
        mineBlockDropdown.renderPopupLayer(context, tr, McpToolsBlockCatalog.dropdownLabels(), mouseX, mouseY);
        clampOverlayToScreen(mc);
    }

    private int drawSplitFields(
            Font tr,
            GuiGraphicsExtractor context,
            int rx,
            int y,
            int inner,
            String label1,
            OverlayTextField field1,
            FocusField focus1,
            String label2,
            OverlayTextField field2,
            FocusField focus2) {
        int half = (inner - GAP) / 2;
        int fieldW = half - SPLIT_LABEL_W;
        context.text(tr, Component.literal(label1), rx, y + 3, UiTokens.TEXT_DIM);
        int leftFieldX = rx + SPLIT_LABEL_W;
        UiComponents.drawTextField(tr, context, leftFieldX, y, fieldW, INPUT_H, field1.text(), focusField == focus1);
        assignSplitFieldCoords(focus1, leftFieldX, y, fieldW);

        int rx2 = rx + half + GAP;
        context.text(tr, Component.literal(label2), rx2, y + 3, UiTokens.TEXT_DIM);
        int rightFieldX = rx2 + SPLIT_LABEL_W;
        UiComponents.drawTextField(tr, context, rightFieldX, y, fieldW, INPUT_H, field2.text(), focusField == focus2);
        assignSplitFieldCoords(focus2, rightFieldX, y, fieldW);
        return y + INPUT_H + GAP;
    }

    private void assignSplitFieldCoords(FocusField field, int x, int y, int w) {
        switch (field) {
            case PORT -> {
                portFieldX = x;
                portFieldW = w;
                portRowY = y;
            }
            case USER -> {
                userFieldX = x;
                userFieldW = w;
                userRowY = y;
            }
            case SPAWN_COUNT -> {
                spawnFieldX = x;
                spawnFieldW = w;
                spawnRowY = y;
            }
            case JOIN_DELAY -> {
                joinDelayFieldX = x;
                joinDelayFieldW = w;
                joinDelayRowY = y;
            }
            default -> {
            }
        }
    }

    private int botGridTotalRows(int botCount) {
        return botCount == 0 ? 1 : (botCount + ROSTER_COLS - 1) / ROSTER_COLS;
    }

    private int botGridVisibleRows(int botCount) {
        return Math.min(ROSTER_GRID_ROWS, botGridTotalRows(botCount));
    }

    private int botSectionHeight() {
        int bots = manager.getBots().size();
        int gridRows = botGridVisibleRows(bots);
        int scrollHint = botGridTotalRows(bots) > ROSTER_GRID_ROWS ? 10 : 0;
        return 10 + BTN_H + GAP + BTN_H + GAP + gridRows * ROSTER_ROW_H + scrollHint + GAP
                + 10 + INPUT_H + GAP + BTN_H + GAP + BTN_H + GAP + BTN_H + GAP
                + 10 + BTN_H + GAP + 10 + INPUT_H + GAP;
    }

    private String buildBotStatusLine() {
        McpToolsBotHandle focused = manager.focusedBot();
        if (focused != null) {
            String auth = focused.auth().isAuthenticated() ? "ready" : "logging in";
            return manager.activeBotCount() + " bot(s) · control: " + focused.username + " (" + auth + ")";
        }
        return manager.activeBotCount() + " bot(s) · target: " + manager.botActionTarget().label.toLowerCase()
                + " · click a bot to focus";
    }

    private int parsedSpawnCount() {
        try {
            return Math.max(1, Math.min(McpToolsBotFleet.MAX_BOTS, Integer.parseInt(spawnCountInput.text().trim())));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String joinBotsLabel() {
        int n = parsedSpawnCount();
        return n <= 1 ? "Join bot" : "Join " + n + " bots";
    }

    private int renderBotControls(Font tr, GuiGraphicsExtractor context, int rx, int y, int inner, int mouseX, int mouseY) {
        botRowHits.clear();
        movementHits.clear();
        context.text(tr, Component.literal("Bots"), rx, y, UiTokens.ACCENT);
        y += 10;

        McpToolsBotActionTarget target = manager.botActionTarget();
        int third = (inner - GAP * 2) / 3;
        rosterBtnRowY = y;
        rosterTargetX = rx;
        rosterTargetY = y;
        rosterTargetW = third;
        UiComponents.drawPillActionButton(
                tr, context, rosterTargetX, y, rosterTargetW, BTN_H,
                "Target: " + (target == McpToolsBotActionTarget.ALL ? "All" : "Sel"),
                UiComponents.PillActionStyle.SECONDARY_SLATE);
        rosterAddX = rx + third + GAP;
        UiComponents.drawPillActionButton(
                tr, context, rosterAddX, y, third, BTN_H, "+ Add", UiComponents.PillActionStyle.SECONDARY_SLATE);
        stopFocusedX = rx + (third + GAP) * 2;
        stopFocusedW = third;
        stopFocusedY = y;
        UiComponents.drawPillActionButton(
                tr, context, stopFocusedX, y, stopFocusedW, BTN_H, "Stop focus",
                manager.focusedBot() != null ? UiComponents.PillActionStyle.PRIMARY_BLUE : UiComponents.PillActionStyle.SECONDARY_SLATE);
        y += BTN_H + GAP;

        rosterBtnRow2Y = y;
        int third2 = (inner - GAP * 2) / 3;
        rosterSelectAllX = rx;
        rosterSelectNoneX = rx + third2 + GAP;
        rosterDisconnectAllX = rx + (third2 + GAP) * 2;
        UiComponents.drawPillActionButton(tr, context, rosterSelectAllX, y, third2, BTN_H, "All", UiComponents.PillActionStyle.SECONDARY_SLATE);
        UiComponents.drawPillActionButton(tr, context, rosterSelectNoneX, y, third2, BTN_H, "None", UiComponents.PillActionStyle.SECONDARY_SLATE);
        UiComponents.drawPillActionButton(tr, context, rosterDisconnectAllX, y, third2, BTN_H, "Disconnect", UiComponents.PillActionStyle.SECONDARY_SLATE);
        y += BTN_H + GAP;

        rosterListY = y;
        List<McpToolsBotHandle> bots = manager.getBots();
        McpToolsBotHandle focused = manager.focusedBot();
        int cellW = (inner - GAP * (ROSTER_COLS - 1)) / ROSTER_COLS;
        if (bots.isEmpty()) {
            context.text(tr, Component.literal("No bots — click Join above"), rx + 2, y + 2, UiTokens.TEXT_DIM);
            y += ROSTER_ROW_H + GAP;
            rosterGridH = ROSTER_ROW_H;
        } else {
            int totalRows = botGridTotalRows(bots.size());
            int maxRowScroll = Math.max(0, totalRows - ROSTER_GRID_ROWS);
            rosterScrollOffset = Math.min(rosterScrollOffset, maxRowScroll);
            int startRow = rosterScrollOffset;
            int visibleRows = botGridVisibleRows(bots.size());
            rosterGridH = visibleRows * ROSTER_ROW_H;
            for (int row = 0; row < visibleRows; row++) {
                int botRow = startRow + row;
                for (int col = 0; col < ROSTER_COLS; col++) {
                    int idx = botRow * ROSTER_COLS + col;
                    int cellX = rx + col * (cellW + GAP);
                    if (idx >= bots.size()) {
                        continue;
                    }
                    renderBotCell(tr, context, cellX, y, cellW, bots.get(idx), focused);
                }
                y += ROSTER_ROW_H;
            }
            if (totalRows > ROSTER_GRID_ROWS) {
                int startIdx = startRow * ROSTER_COLS + 1;
                int endIdx = Math.min(bots.size(), (startRow + visibleRows) * ROSTER_COLS);
                context.text(tr,
                        Component.literal(startIdx + "-" + endIdx + " / " + bots.size() + " · scroll"),
                        rx + 2, y, UiTokens.TEXT_DIM);
                y += 10;
            }
            y += GAP;
        }

        String controlLabel = focused != null
                ? "Control · " + focused.username + (focused.auth().isAuthenticated() ? " · ready" : " · login pending")
                : "Control · " + (target == McpToolsBotActionTarget.ALL ? "all bots" : "selected bots");
        context.text(tr, Component.literal(controlLabel), rx, y, UiTokens.TEXT_DIM);
        y += 10;

        chatSendW = 42;
        chatFieldW = inner - chatSendW - GAP;
        chatFieldX = rx;
        chatFieldY = y;
        chatSendX = rx + chatFieldW + GAP;
        UiComponents.drawTextField(tr, context, chatFieldX, chatFieldY, chatFieldW, INPUT_H, chatInput.text(), focusField == FocusField.CHAT);
        UiComponents.drawPillActionButton(
                tr, context, chatSendX, chatFieldY, chatSendW, INPUT_H, "Send",
                manager.isBotSessionActive() ? UiComponents.PillActionStyle.PRIMARY_MINT : UiComponents.PillActionStyle.SECONDARY_SLATE);
        y += INPUT_H + GAP;

        context.text(tr, Component.literal("Move"), rx, y, UiTokens.TEXT_DIM);
        y += 10;

        botBtnW = (inner - GAP * 3) / 4;
        botRow1Y = y;
        drawHoldMovementBtn(tr, context, rx, y, botBtnW, "W", "forward");
        drawHoldMovementBtn(tr, context, rx + botBtnW + GAP, y, botBtnW, "S", "back");
        drawHoldMovementBtn(tr, context, rx + (botBtnW + GAP) * 2, y, botBtnW, "A", "left");
        drawHoldMovementBtn(tr, context, rx + (botBtnW + GAP) * 3, y, botBtnW, "D", "right");
        y += BTN_H + GAP;

        botRow2Y = y;
        drawMovementBtn(tr, context, rx, y, botBtnW, "Jump", "jump", false);
        drawToggleMovementBtn(tr, context, rx + botBtnW + GAP, y, botBtnW, "Sprint", "sprint");
        drawToggleMovementBtn(tr, context, rx + (botBtnW + GAP) * 2, y, botBtnW, "Sneak", "sneak");
        UiComponents.drawPillActionButton(
                tr, context, rx + (botBtnW + GAP) * 3, y, botBtnW, BTN_H, "Stop",
                manager.isBotSessionActive() ? UiComponents.PillActionStyle.PRIMARY_BLUE : UiComponents.PillActionStyle.SECONDARY_SLATE);
        registerMovementHit("stop", rx + (botBtnW + GAP) * 3, y, botBtnW, BTN_H, false);
        y += BTN_H + GAP;

        botRow3Y = y;
        drawBotDotBtn(tr, context, rx, y, botBtnW, "Players", "players");
        drawBotDotBtn(tr, context, rx + botBtnW + GAP, y, botBtnW, "Plugins", "plugins");
        drawBotDotBtn(tr, context, rx + (botBtnW + GAP) * 2, y, botBtnW, "Server", "serverip");
        drawBotDotBtn(tr, context, rx + (botBtnW + GAP) * 3, y, botBtnW, "Help", "help");
        y += BTN_H + GAP;

        context.text(tr, Component.literal("Pathfinder"), rx, y, UiTokens.TEXT_DIM);
        y += 10;

        int coordW = (inner - GAP * 3 - 36) / 3;
        gotoFieldY = y;
        gotoFieldX = rx;
        gotoFieldW = coordW;
        UiComponents.drawTextField(tr, context, rx, y, coordW, INPUT_H, gotoXInput.text(), focusField == FocusField.GOTO_X);
        UiComponents.drawTextField(tr, context, rx + coordW + GAP, y, coordW, INPUT_H, gotoYInput.text(), focusField == FocusField.GOTO_Y);
        UiComponents.drawTextField(tr, context, rx + (coordW + GAP) * 2, y, coordW, INPUT_H, gotoZInput.text(), focusField == FocusField.GOTO_Z);
        gotoGoW = 36;
        gotoGoX = rx + inner - gotoGoW;
        UiComponents.drawPillActionButton(
                tr, context, gotoGoX, y, gotoGoW, INPUT_H, "Go",
                manager.isBotSessionActive() ? UiComponents.PillActionStyle.PRIMARY_MINT : UiComponents.PillActionStyle.SECONDARY_SLATE);
        y += INPUT_H + GAP;

        botPathRowY = y;
        pathBtnW = (inner - GAP) / 2;
        pathComeX = rx;
        pathStopX = rx + pathBtnW + GAP;
        UiComponents.drawPillActionButton(
                tr, context, pathComeX, y, pathBtnW, BTN_H, "Come to me",
                manager.isBotSessionActive() ? UiComponents.PillActionStyle.PRIMARY_MINT : UiComponents.PillActionStyle.SECONDARY_SLATE);
        UiComponents.drawPillActionButton(
                tr, context, pathStopX, y, pathBtnW, BTN_H, "Stop path",
                manager.isBotSessionActive() ? UiComponents.PillActionStyle.SECONDARY_SLATE : UiComponents.PillActionStyle.SECONDARY_SLATE);
        y += BTN_H + GAP;

        context.text(tr, Component.literal("Mine block"), rx, y, UiTokens.TEXT_DIM);
        y += 10;

        botMineRowY = y;
        mineSendW = 42;
        mineFieldW = inner - mineSendW - GAP;
        mineFieldX = rx;
        mineFieldY = y;
        mineSendX = rx + mineFieldW + GAP;
        mineBlockDropdown.render(
                context, tr, mineFieldX, mineFieldY, mineFieldW, INPUT_H,
                McpToolsBlockCatalog.dropdownLabels(), mouseX, mouseY);
        UiComponents.drawPillActionButton(
                tr, context, mineSendX, mineFieldY, mineSendW, INPUT_H, "Mine",
                manager.isBotSessionActive() ? UiComponents.PillActionStyle.PRIMARY_MINT : UiComponents.PillActionStyle.SECONDARY_SLATE);
        y += INPUT_H + GAP;
        return y;
    }

    private void drawHoldMovementBtn(Font tr, GuiGraphicsExtractor context, int x, int y, int w, String label, String movement) {
        drawMovementBtn(tr, context, x, y, w, label, movement, true);
    }

    private void drawMovementBtn(Font tr, GuiGraphicsExtractor context, int x, int y, int w, String label, String movement, boolean hold) {
        boolean active = heldMovements.contains(movement);
        UiComponents.drawPillActionButton(
                tr, context, x, y, w, BTN_H, label,
                active
                        ? UiComponents.PillActionStyle.PRIMARY_MINT
                        : manager.isBotSessionActive()
                        ? UiComponents.PillActionStyle.SECONDARY_SLATE
                        : UiComponents.PillActionStyle.SECONDARY_SLATE);
        registerMovementHit(movement, x, y, w, BTN_H, hold);
    }

    private void drawToggleMovementBtn(Font tr, GuiGraphicsExtractor context, int x, int y, int w, String label, String movement) {
        drawMovementBtn(tr, context, x, y, w, label, movement, false);
    }

    private void drawBotDotBtn(Font tr, GuiGraphicsExtractor context, int x, int y, int w, String label, String command) {
        UiComponents.drawPillActionButton(
                tr, context, x, y, w, BTN_H, label,
                manager.isBotSessionActive()
                        ? UiComponents.PillActionStyle.SECONDARY_SLATE
                        : UiComponents.PillActionStyle.SECONDARY_SLATE);
        registerMovementHit("cmd:" + command, x, y, w, BTN_H, false);
    }

    private void registerMovementHit(String movement, int x, int y, int w, int h, boolean hold) {
        movementHits.add(new MovementHit(movement, x, y, w, h, hold));
    }

    private void renderBotCell(
            Font tr,
            GuiGraphicsExtractor context,
            int x,
            int y,
            int w,
            McpToolsBotHandle bot,
            McpToolsBotHandle focused) {
        boolean isFocused = focused != null && focused.id.equals(bot.id);
        int bg = isFocused ? 0x5534D399 : bot.selected ? 0x3322C55E : 0xFF1E293B;
        context.fill(x, y, x + w, y + ROSTER_ROW_H - 1, bg);
        if (isFocused) {
            context.fill(x, y + ROSTER_ROW_H - 2, x + w, y + ROSTER_ROW_H - 1, UiTokens.ACCENT);
        }
        int dot = switch (bot.state) {
            case CONNECTED -> UiTokens.EMERALD_500;
            case STOPPED -> 0xFFEF4444;
            case CONNECTING -> 0xFFF59E0B;
        };
        context.fill(x + 3, y + 5, x + 6, y + 8, dot);
        String name = bot.username;
        if (name.length() > 8) {
            name = name.substring(0, 6) + "…";
        }
        context.text(
                tr,
                Component.literal(name),
                x + 9,
                y + 2,
                isFocused ? UiTokens.ACCENT : UiTokens.TEXT);
        int checkX = x + w - 9;
        context.fill(checkX, y + 3, checkX + 7, y + 10, bot.selected ? UiTokens.EMERALD_500 : 0xFF374151);
        botRowHits.add(new BotRowHit(
                bot.id,
                x, y, w - 11, ROSTER_ROW_H - 1,
                checkX, y + 2, 8, 8));
    }

    private String runButtonLabel(McpToolsTool tool) {
        if (tool.interactiveBot) {
            int n = parsedSpawnCount();
            return n <= 1 ? "Join bot" : "Join " + n + " bots";
        }
        return manager.isRunning() ? "Running…" : "Run";
    }

    private void layoutHostRow(int rx, int inner, int y) {
        hostRowY = y;
        hostFieldX = rx + LABEL_W;
        hostFieldW = inner - LABEL_W - GAP - USE_CURRENT_BTN_W;
        hostUseCurrentX = hostFieldX + hostFieldW + GAP;
    }

    private void drawLabeledField(
            Font tr, GuiGraphicsExtractor context, int x, int y, String label, OverlayTextField value, FocusField field, int inner) {
        context.text(tr, Component.literal(label), x, y + 3, UiTokens.TEXT_DIM);
        UiComponents.drawTextField(tr, context, x + LABEL_W, y, inner - LABEL_W, INPUT_H, value.text(), focusField == field);
    }

    private void renderLog(GuiGraphicsExtractor context, Font tr, int x, int y) {
        List<String> lines = manager.getLogs();
        int maxScroll = Math.max(0, lines.size() - LOG_LINES);
        logScrollOffset = Math.min(logScrollOffset, maxScroll);
        int start = Math.max(0, lines.size() - LOG_LINES - logScrollOffset);
        int end = Math.min(lines.size(), start + LOG_LINES);
        int ly = y;
        for (int i = start; i < end; i++) {
            String line = lines.get(i);
            if (line.length() > LOG_CHARS) {
                line = line.substring(0, LOG_CHARS - 1) + "…";
            }
            context.text(tr, Component.literal(line), x, ly, 0xFFCBD5E1);
            ly += LOG_LINE_H;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isActive()) {
            return false;
        }
        int px = overlayX();
        int py = overlayY();
        if (!containsPoint(mouseX, mouseY) && !versionDropdown.hitsInteractive(mouseX, mouseY)
                && !mineBlockDropdown.hitsInteractive(mouseX, mouseY)) {
            return false;
        }
        if (beginTitleDrag(mouseX, mouseY, button, px, py, panelWidth(), TITLE_H)) {
            return true;
        }

        if (versionDropdown.mouseClicked(
                mouseX, mouseY, button, McpToolsMcVersion.dropdownLabels(), label -> {
                    McpToolsMcVersion picked = McpToolsMcVersion.fromDropdownLabel(label);
                    versionDropdown.setDisplayValue(picked.dropdownLabel());
                    manager.getSettings().lastMcVersion = picked.id;
                    manager.getSettings().lastVersion = picked.protocol;
                    manager.saveSettings();
                })) {
            focusField = FocusField.NONE;
            mineBlockDropdown.close();
            return true;
        }

        if (mineBlockDropdown.mouseClicked(
                mouseX, mouseY, button, McpToolsBlockCatalog.dropdownLabels(), label -> {
                    mineBlockDropdown.setDisplayValue(label);
                })) {
            focusField = FocusField.NONE;
            versionDropdown.close();
            return true;
        }

        McpToolsSettings s = manager.getSettings();
        McpToolsTool tool = McpToolsTool.fromId(s.selectedToolId);
        int rx = px + PAD;
        int inner = PANEL_W - PAD * 2;
        int y = py + TITLE_H + PAD;
        layoutHostRow(rx, inner, y);

        if (inRect(mouseX, mouseY, hostUseCurrentX, hostRowY, USE_CURRENT_BTN_W, INPUT_H)) {
            focusField = FocusField.NONE;
            versionDropdown.close();
            if (manager.applyCurrentServerToHost(Minecraft.getInstance())) {
                syncFromSettings();
            }
            return true;
        }
        if (fieldHit(mouseX, mouseY, hostFieldX, hostRowY, hostFieldW, INPUT_H)) {
            focusField = FocusField.HOST;
            versionDropdown.close();
            return true;
        }
        y += INPUT_H + GAP;
        if (tool.interactiveBot) {
            if (fieldHit(mouseX, mouseY, portFieldX, portRowY, portFieldW, INPUT_H)) {
                focusField = FocusField.PORT;
                versionDropdown.close();
                return true;
            }
            if (fieldHit(mouseX, mouseY, userFieldX, userRowY, userFieldW, INPUT_H)) {
                focusField = FocusField.USER;
                versionDropdown.close();
                return true;
            }
            if (fieldHit(mouseX, mouseY, spawnFieldX, spawnRowY, spawnFieldW, INPUT_H)) {
                focusField = FocusField.SPAWN_COUNT;
                versionDropdown.close();
                return true;
            }
            if (fieldHit(mouseX, mouseY, joinDelayFieldX, joinDelayRowY, joinDelayFieldW, INPUT_H)) {
                focusField = FocusField.JOIN_DELAY;
                versionDropdown.close();
                return true;
            }
            if (fieldHit(mouseX, mouseY, timeoutFieldX, timeoutRowY, timeoutFieldW, INPUT_H)) {
                focusField = FocusField.LOGIN_TIMEOUT;
                versionDropdown.close();
                return true;
            }
        } else {
            if (fieldHit(mouseX, mouseY, rx + LABEL_W, y, inner - LABEL_W, INPUT_H)) {
                focusField = FocusField.PORT;
                versionDropdown.close();
                return true;
            }
            y += INPUT_H + GAP;
            if (fieldHit(mouseX, mouseY, rx + LABEL_W, y, inner - LABEL_W, INPUT_H)) {
                focusField = FocusField.USER;
                versionDropdown.close();
                return true;
            }
        }
        y = versionFieldY + INPUT_H + GAP;

        int half = (inner - GAP) / 2;
        if (inRect(mouseX, mouseY, rx, y, half, BTN_H)) {
            focusField = FocusField.NONE;
            versionDropdown.close();
            manager.cycleTool();
            return true;
        }
        if (inRect(mouseX, mouseY, rx + half + GAP, y, half, BTN_H) && (!manager.isRunning() || tool.interactiveBot)) {
            focusField = FocusField.NONE;
            versionDropdown.close();
            persistFields();
            manager.runSelectedTool();
            return true;
        }
        y += BTN_H + GAP;

        int syncHalf = (inner - GAP) / 2;
        if (inRect(mouseX, mouseY, rx, y, syncHalf, BTN_H) && !manager.isSyncing()) {
            focusField = FocusField.NONE;
            manager.syncBundleAsync();
            return true;
        }
        if (inRect(mouseX, mouseY, rx + syncHalf + GAP, y, syncHalf, BTN_H)) {
            focusField = FocusField.NONE;
            manager.clearLogs();
            logScrollOffset = 0;
            return true;
        }
        y += BTN_H + GAP;

        if (manager.showBotControls()) {
            if (handleBotControlsClick(mouseX, mouseY, rx, y, inner)) {
                return true;
            }
            y += botSectionHeight();
        }

        if (tool.needsUpload) {
            y += 10;
            if (fieldHit(mouseX, mouseY, rx, y, inner, UPLOAD_H)) {
                focusField = FocusField.UPLOAD;
                return true;
            }
            y += UPLOAD_H + GAP;
        }

        y += 10 + GAP + 10;
        if (inRect(mouseX, mouseY, logBoxX, logBoxY, logBoxW, logBoxH)) {
            focusField = FocusField.NONE;
            versionDropdown.close();
            return true;
        }

        focusField = FocusField.NONE;
        versionDropdown.close();
        return true;
    }

    private boolean handleBotControlsClick(double mouseX, double mouseY, int rx, int y, int inner) {
        y += 10;
        int third = (inner - GAP * 2) / 3;

        if (inRect(mouseX, mouseY, rosterTargetX, rosterBtnRowY, rosterTargetW, BTN_H)) {
            manager.cycleBotActionTarget();
            return true;
        }
        if (inRect(mouseX, mouseY, rosterAddX, rosterBtnRowY, third, BTN_H)) {
            persistFields();
            manager.addBot();
            return true;
        }
        if (inRect(mouseX, mouseY, stopFocusedX, stopFocusedY, stopFocusedW, BTN_H)) {
            McpToolsBotHandle focused = manager.focusedBot();
            if (focused != null) {
                manager.stopBot(focused.id);
            }
            return true;
        }

        int third2 = (inner - GAP * 2) / 3;
        if (inRect(mouseX, mouseY, rosterSelectAllX, rosterBtnRow2Y, third2, BTN_H)) {
            manager.selectAllBots();
            return true;
        }
        if (inRect(mouseX, mouseY, rosterSelectNoneX, rosterBtnRow2Y, third2, BTN_H)) {
            manager.selectNoBots();
            return true;
        }
        if (inRect(mouseX, mouseY, rosterDisconnectAllX, rosterBtnRow2Y, third2, BTN_H)) {
            manager.stopAllBots("All bots disconnected.");
            return true;
        }

        for (BotRowHit hit : botRowHits) {
            if (inRect(mouseX, mouseY, hit.checkX, hit.checkY, hit.checkW, hit.checkH)) {
                manager.toggleBotSelected(hit.botId);
                return true;
            }
            if (inRect(mouseX, mouseY, hit.cellX, hit.cellY, hit.cellW, hit.cellH)) {
                manager.selectOnlyBot(hit.botId);
                return true;
            }
        }

        y = rosterListY;
        List<McpToolsBotHandle> bots = manager.getBots();
        int rosterRows = botGridVisibleRows(bots.size());
        y += rosterRows * ROSTER_ROW_H + (botGridTotalRows(bots.size()) > ROSTER_GRID_ROWS ? 10 : 0) + GAP;
        y += 10;

        if (fieldHit(mouseX, mouseY, chatFieldX, chatFieldY, chatFieldW, INPUT_H)) {
            focusField = FocusField.CHAT;
            return true;
        }
        if (inRect(mouseX, mouseY, chatSendX, chatFieldY, chatSendW, INPUT_H)) {
            focusField = FocusField.NONE;
            persistFields();
            manager.sendBotChat(chatInput.text());
            chatInput.clear();
            return true;
        }
        y += INPUT_H + GAP + 10;

        for (MovementHit hit : movementHits) {
            if (!inRect(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) {
                continue;
            }
            if (handleMovementPress(hit)) {
                return true;
            }
        }

        int coordW = (inner - GAP * 3 - 36) / 3;
        if (fieldHit(mouseX, mouseY, rx, gotoFieldY, coordW, INPUT_H)) {
            focusField = FocusField.GOTO_X;
            return true;
        }
        if (fieldHit(mouseX, mouseY, rx + coordW + GAP, gotoFieldY, coordW, INPUT_H)) {
            focusField = FocusField.GOTO_Y;
            return true;
        }
        if (fieldHit(mouseX, mouseY, rx + (coordW + GAP) * 2, gotoFieldY, coordW, INPUT_H)) {
            focusField = FocusField.GOTO_Z;
            return true;
        }
        if (inRect(mouseX, mouseY, gotoGoX, gotoFieldY, gotoGoW, INPUT_H)) {
            focusField = FocusField.NONE;
            submitGoto();
            return true;
        }

        if (inRect(mouseX, mouseY, pathComeX, botPathRowY, pathBtnW, BTN_H)) {
            focusField = FocusField.NONE;
            manager.sendBotsToLocalPlayer(Minecraft.getInstance());
            return true;
        }
        if (inRect(mouseX, mouseY, pathStopX, botPathRowY, pathBtnW, BTN_H)) {
            focusField = FocusField.NONE;
            manager.sendBotPathStop();
            return true;
        }

        if (inRect(mouseX, mouseY, mineSendX, mineFieldY, mineSendW, INPUT_H)) {
            focusField = FocusField.NONE;
            submitMineBlock();
            return true;
        }
        return false;
    }

    private void submitMineBlock() {
        manager.sendBotMineBlock(mineBlockDropdown.displayValue());
    }

    private boolean handleMovementPress(MovementHit hit) {
        focusField = FocusField.NONE;
        if (hit.movement.startsWith("cmd:")) {
            manager.sendBotDot(hit.movement.substring(4));
            return true;
        }
        if ("stop".equals(hit.movement)) {
            releaseAllHeldMovements();
            manager.releaseAllBotMovement();
            return true;
        }
        if (hit.hold) {
            if (heldMovements.add(hit.movement)) {
                manager.holdBotMovement(hit.movement);
            }
            return true;
        }
        if ("jump".equals(hit.movement)) {
            manager.pulseBotMovement("jump");
            return true;
        }
        manager.toggleBotMovement(hit.movement);
        return true;
    }

    private void submitGoto() {
        try {
            double x = Double.parseDouble(gotoXInput.text().trim());
            double y = Double.parseDouble(gotoYInput.text().trim());
            double z = Double.parseDouble(gotoZInput.text().trim());
            manager.sendBotGoto(x, y, z);
        } catch (NumberFormatException ex) {
            manager.sendBotDot("goto");
        }
    }

    private void releaseAllHeldMovements() {
        if (heldMovements.isEmpty()) {
            return;
        }
        for (String movement : heldMovements) {
            manager.releaseBotMovement(movement);
        }
        heldMovements.clear();
    }

    private void releaseMovementHoldsOutside(double mouseX, double mouseY) {
        Iterator<String> it = heldMovements.iterator();
        while (it.hasNext()) {
            String movement = it.next();
            if (!isMovementHeldUnder(mouseX, mouseY, movement)) {
                manager.releaseBotMovement(movement);
                it.remove();
            }
        }
    }

    private boolean isMovementHeldUnder(double mouseX, double mouseY, String movement) {
        for (MovementHit hit : movementHits) {
            if (hit.hold && movement.equals(hit.movement)
                    && inRect(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        endTitleDrag(button);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && manager.showBotControls()) {
            releaseAllHeldMovements();
        }
        return dragging || !heldMovements.isEmpty();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return updateTitleDrag(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (versionDropdown.isOpen()) {
            if (versionDropdown.mouseScrolled(mouseX, mouseY, vertical, McpToolsMcVersion.dropdownLabels())
                    || versionDropdown.scrollOpenList(vertical, McpToolsMcVersion.dropdownLabels())) {
                return true;
            }
        }
        if (mineBlockDropdown.isOpen()) {
            if (mineBlockDropdown.mouseScrolled(mouseX, mouseY, vertical, McpToolsBlockCatalog.dropdownLabels())
                    || mineBlockDropdown.scrollOpenList(vertical, McpToolsBlockCatalog.dropdownLabels())) {
                return true;
            }
        }
        int rx = overlayX() + PAD;
        if (manager.showBotControls()
                && inRect(mouseX, mouseY, rx, rosterListY, PANEL_W - PAD * 2, rosterGridH)) {
            int delta = vertical > 0 ? -1 : vertical < 0 ? 1 : 0;
            int maxScroll = Math.max(0, botGridTotalRows(manager.getBots().size()) - ROSTER_GRID_ROWS);
            rosterScrollOffset = Math.max(0, Math.min(maxScroll, rosterScrollOffset + delta));
            return true;
        }
        if (versionDropdown.isOpen() && containsPoint(mouseX, mouseY)) {
            if (versionDropdown.scrollOpenList(vertical, McpToolsMcVersion.dropdownLabels())) {
                return true;
            }
        }
        if (mineBlockDropdown.isOpen() && containsPoint(mouseX, mouseY)) {
            if (mineBlockDropdown.scrollOpenList(vertical, McpToolsBlockCatalog.dropdownLabels())) {
                return true;
            }
        }
        if (!isActive() || !inRect(mouseX, mouseY, logBoxX, logBoxY, logBoxW, logBoxH)) {
            return false;
        }
        int delta = vertical > 0 ? -1 : vertical < 0 ? 1 : 0;
        int maxScroll = Math.max(0, manager.getLogs().size() - LOG_LINES);
        logScrollOffset = Math.max(0, Math.min(maxScroll, logScrollOffset + delta));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (versionDropdown.keyPressed(keyCode)) {
            return true;
        }
        if (mineBlockDropdown.keyPressed(keyCode)) {
            return true;
        }
        OverlayTextField active = activeField();
        if (active == null) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            focusField = FocusField.NONE;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (focusField == FocusField.CHAT) {
                manager.sendBotChat(chatInput.text());
                chatInput.clear();
            } else if (focusField == FocusField.GOTO_X || focusField == FocusField.GOTO_Y || focusField == FocusField.GOTO_Z) {
                submitGoto();
            } else if (focusField == FocusField.SPAWN_COUNT) {
                persistFields();
                manager.joinBotsFromSettings();
            } else {
                persistFields();
            }
            return true;
        }
        return active.keyPressed(keyCode);
    }

    @Override
    public boolean charTyped(int codePoint) {
        if (versionDropdown.charTyped(codePoint)) {
            return true;
        }
        if (mineBlockDropdown.charTyped(codePoint)) {
            return true;
        }
        OverlayTextField active = activeField();
        if (active == null) {
            return false;
        }
        return active.charTyped(codePoint);
    }

    private OverlayTextField activeField() {
        return switch (focusField) {
            case HOST -> hostInput;
            case PORT -> portInput;
            case SPAWN_COUNT -> spawnCountInput;
            case LOGIN_TIMEOUT -> loginTimeoutInput;
            case JOIN_DELAY -> joinDelayInput;
            case USER -> userInput;
            case CHAT -> chatInput;
            case GOTO_X -> gotoXInput;
            case GOTO_Y -> gotoYInput;
            case GOTO_Z -> gotoZInput;
            case UPLOAD -> uploadInput;
            default -> null;
        };
    }

    private static boolean fieldHit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private enum FocusField {
        NONE,
        HOST,
        PORT,
        USER,
        SPAWN_COUNT,
        LOGIN_TIMEOUT,
        JOIN_DELAY,
        CHAT,
        GOTO_X,
        GOTO_Y,
        GOTO_Z,
        UPLOAD
    }

    private record MovementHit(String movement, int x, int y, int w, int h, boolean hold) {
    }

    private record BotRowHit(
            String botId,
            int cellX, int cellY, int cellW, int cellH,
            int checkX, int checkY, int checkW, int checkH) {
    }
}
