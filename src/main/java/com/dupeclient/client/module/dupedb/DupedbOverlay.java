package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.core.notify.ClientNotificationHub;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.overlay.AbstractDraggableOverlay;
import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import com.dupeclient.client.module.macro.DupedbMacroBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import org.lwjgl.glfw.GLFW;

public final class DupedbOverlay extends AbstractDraggableOverlay implements IngameModuleOverlay {
    public static final DupedbOverlay INSTANCE = new DupedbOverlay();

    private static final int PANEL_W = 296;
    private static final int TITLE_H = 12;
    private static final int BTN_H = 14;
    private static final int SLIDER_ROW_H = 16;
    private static final int SCAN_PLUGINS_GAP = 5;

    private final DupedbManager manager = DupedbManager.INSTANCE;
    private boolean draggingProbeDelaySlider;

    private DupedbOverlay() {
    }

    @Override
    public String id() {
        return "dupedb";
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
        DupedbSettings s = manager.getSettings();
        s.overlayX = x;
        s.overlayY = y;
        manager.save();
    }

    @Override
    public int panelWidth() {
        return PANEL_W;
    }

    @Override
    public int panelHeight() {
        return computePanelHeight();
    }

    private int computePanelHeight() {
        int h = TITLE_H + UiTokens.UI_GAP + UiTokens.CARD_BODY_TOP;
        h += UiTokens.ROW_STEP * 2; // mode + OAuth status
        h += UiTokens.ROW_STEP; // login / revoke
        h += BTN_H + SCAN_PLUGINS_GAP + 10; // scan button + plugins line
        h += BTN_H + SCAN_PLUGINS_GAP; // generate macro
        if (manager.getLastP2wResult() != null) {
            h += 10;
        }
        h += UiTokens.ROW_STEP; // spacer before toggles
        h += UiTokens.ROW_STEP * 3; // announce + P2W + background scan
        h += SLIDER_ROW_H; // probe delay slider
        h += UiTokens.SP_4; // bottom inset
        return h;
    }

    @Override
    public boolean hasTextFocus() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isActive()) {
            return;
        }
        int px = overlayX();
        int py = overlayY();
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        DupedbSettings s = manager.getSettings();

        int panelH = computePanelHeight();
        context.fill(px, py, px + PANEL_W, py + panelH, 0xE018181B);
        context.fill(px, py, px + PANEL_W, py + TITLE_H, 0xFF27272A);
        context.drawTextWithShadow(tr, Text.literal("DupeDB"), px + 6, py + 2, 0xFF34D399);

        int tx = px + UiTokens.BODY_INSET;
        int ty = py + TITLE_H + UiTokens.UI_GAP;
        int sw = PANEL_W - UiTokens.BODY_INSET * 2;
        int inner = sw - UiTokens.SP_4;
        int rx = tx + UiTokens.SP_2;
        int lineY = ty + UiTokens.CARD_BODY_TOP;

        drawModeRow(tr, context, rx, lineY, inner, s);
        lineY += UiTokens.ROW_STEP;

        boolean auth = manager.isAuthenticated();
        int dotC = auth ? UiTokens.EMERALD_500 : 0xFFEF4444;
        context.fill(rx, lineY + 5, rx + 3, lineY + 8, dotC);
        context.drawTextWithShadow(tr, Text.literal("OAuth Status"), rx + 6, lineY + 2, UiTokens.TEXT);
        String st = auth
                ? "Connected"
                : (manager.isOauthInFlight() ? "Authenticating…" : "Disconnected");
        int stCol = auth ? UiTokens.EMERALD_500 : 0xFFF87171;
        int stw = tr.getWidth(st);
        context.drawTextWithShadow(tr, Text.literal(st), tx + sw - stw - UiTokens.SP_2, lineY + 2, stCol);
        lineY += UiTokens.ROW_STEP;

        if (!auth) {
            UiComponents.drawPillActionButton(
                    tr, context, rx, lineY, inner, BTN_H, "Login", UiComponents.PillActionStyle.PRIMARY_BLUE);
            lineY += UiTokens.ROW_STEP;
        } else {
            UiComponents.drawPillActionButton(
                    tr, context, rx, lineY, inner, BTN_H, "Revoke", UiComponents.PillActionStyle.SECONDARY_SLATE);
            lineY += UiTokens.ROW_STEP;
        }

        UiComponents.drawPillActionButton(
                tr, context, rx, lineY, inner, BTN_H, "Scan Now", UiComponents.PillActionStyle.PRIMARY_MINT);
        lineY += BTN_H + SCAN_PLUGINS_GAP;
        drawTrimmedText(
                context,
                tr,
                "Plugins: " + manager.getDiscoveredPluginCount() + (manager.isScanning() ? " (scanning)" : ""),
                rx,
                lineY,
                inner,
                UiTokens.TEXT_DIM);
        lineY += 10;
        DupedbP2wScorer.Result p2w = manager.getLastP2wResult();
        if (p2w != null) {
            drawTrimmedText(context, tr, "P2W: " + p2w.percent() + "%", rx, lineY, inner, UiTokens.EMERALD_500);
            lineY += 10;
        }
        UiComponents.drawPillActionButton(
                tr, context, rx, lineY, inner, BTN_H, "Generate macro", UiComponents.PillActionStyle.SECONDARY_SLATE);
        lineY += BTN_H + SCAN_PLUGINS_GAP;
        lineY += UiTokens.ROW_STEP;

        UiComponents.drawOptionToggle(
                tr, context, rx, lineY, inner, "Announce no matches", s.announceNoMatches,
                s.announceNoMatches ? 1f : 0f);
        lineY += UiTokens.ROW_STEP;

        UiComponents.drawOptionToggle(
                tr, context, rx, lineY, inner, "Generate P2W score", s.generateP2wScore,
                s.generateP2wScore ? 1f : 0f);
        lineY += UiTokens.ROW_STEP;

        UiComponents.drawOptionToggle(
                tr, context, rx, lineY, inner, "Background rescans", s.backgroundScanEnabled,
                s.backgroundScanEnabled ? 1f : 0f);
        lineY += UiTokens.ROW_STEP;

        UiComponents.drawLabeledValueSlider(
                tr,
                context,
                rx,
                lineY,
                inner,
                s.probeDelayMs,
                10,
                500,
                "Probe delay",
                86,
                40,
                draggingProbeDelaySlider,
                (int) Math.round(s.probeDelayMs) + "ms");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isActive()) {
            return false;
        }
        int px = overlayX();
        int py = overlayY();
        if (!containsPoint(mouseX, mouseY)) {
            return false;
        }
        if (beginTitleDrag(mouseX, mouseY, button, px, py, panelWidth(), TITLE_H)) {
            return true;
        }
        DupedbSettings s = manager.getSettings();
        int tx = px + UiTokens.BODY_INSET;
        int ty = py + TITLE_H + UiTokens.UI_GAP;
        int sw = PANEL_W - UiTokens.BODY_INSET * 2;
        int inner = sw - UiTokens.SP_4;
        int rx = tx + UiTokens.SP_2;
        int lineY = ty + UiTokens.CARD_BODY_TOP;

        int tabsX = rx + 42;
        int half = (inner - 42 - 4) / 2;
        if (inRect(mouseX, mouseY, tabsX, lineY, half, 12)) {
            s.mode = DupedbMode.COMMAND;
            manager.save();
            manager.chatFeedback("DupeDB mode: COMMAND");
            return true;
        }
        if (inRect(mouseX, mouseY, tabsX + half + 4, lineY, half, 12)) {
            s.mode = DupedbMode.AUTO;
            manager.save();
            manager.chatFeedback("DupeDB mode: AUTO");
            return true;
        }
        lineY += UiTokens.ROW_STEP;
        lineY += UiTokens.ROW_STEP;

        if (!manager.isAuthenticated()) {
            if (inRect(mouseX, mouseY, rx, lineY, inner, BTN_H) && !manager.isOauthInFlight()) {
                manager.startLoginFlow();
                return true;
            }
            lineY += UiTokens.ROW_STEP;
        } else {
            if (inRect(mouseX, mouseY, rx, lineY, inner, BTN_H)) {
                manager.clearToken();
                return true;
            }
            lineY += UiTokens.ROW_STEP;
        }

        if (inRect(mouseX, mouseY, rx, lineY, inner, BTN_H) && !manager.isScanning()) {
            manager.startScan(false);
            return true;
        }
        lineY += BTN_H + SCAN_PLUGINS_GAP + 10;
        if (manager.getLastP2wResult() != null) {
            lineY += 10;
        }
        if (inRect(mouseX, mouseY, rx, lineY, inner, BTN_H)) {
            String id = DupedbMacroBridge.createFromScan("dupedb_scan");
            if (id.isBlank()) {
                ClientNotificationHub.warn("Run a scan first");
            } else {
                ClientNotificationHub.success("Macro created: " + id);
            }
            return true;
        }
        lineY += BTN_H + SCAN_PLUGINS_GAP;
        lineY += UiTokens.ROW_STEP;

        if (inRect(mouseX, mouseY, rx, lineY, inner, UiTokens.ROW_STEP)) {
            s.announceNoMatches = !s.announceNoMatches;
            manager.save();
            manager.chatFeedback("Announce no matches: " + (s.announceNoMatches ? "on" : "off"));
            return true;
        }
        lineY += UiTokens.ROW_STEP;

        if (inRect(mouseX, mouseY, rx, lineY, inner, UiTokens.ROW_STEP)) {
            s.generateP2wScore = !s.generateP2wScore;
            manager.save();
            manager.chatFeedbackConfigToggle("Generate P2W score: " + (s.generateP2wScore ? "on" : "off"));
            return true;
        }
        lineY += UiTokens.ROW_STEP;

        if (inRect(mouseX, mouseY, rx, lineY, inner, UiTokens.ROW_STEP)) {
            s.backgroundScanEnabled = !s.backgroundScanEnabled;
            manager.save();
            manager.chatFeedbackConfigToggle("Background rescans: " + (s.backgroundScanEnabled ? "on" : "off"));
            return true;
        }
        lineY += UiTokens.ROW_STEP;

        int barX = rx + 86;
        int barW = inner - 92 - 40;
        if (inRect(mouseX, mouseY, barX, lineY + 1, barW, 8)) {
            draggingProbeDelaySlider = true;
            s.probeDelayMs = (int) Math.round(sliderValue(mouseX, barX, barW, 10, 500));
            manager.save();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        endTitleDrag(button);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingProbeDelaySlider = false;
        }
        return dragging;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (updateTitleDrag(mouseX, mouseY, button)) {
            return true;
        }
        if (!draggingProbeDelaySlider || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        DupedbSettings s = manager.getSettings();
        int rx = overlayX() + UiTokens.BODY_INSET + UiTokens.SP_2;
        int inner = PANEL_W - UiTokens.BODY_INSET * 2 - UiTokens.SP_4;
        int lineY = overlayY() + TITLE_H + UiTokens.UI_GAP + UiTokens.CARD_BODY_TOP;
        lineY += UiTokens.ROW_STEP * 2;
        lineY += manager.isAuthenticated() ? UiTokens.ROW_STEP * 2 : UiTokens.ROW_STEP * 2;
        lineY += BTN_H + SCAN_PLUGINS_GAP + 10;
        if (manager.getLastP2wResult() != null) {
            lineY += 10;
        }
        lineY += UiTokens.ROW_STEP * 4; // spacer + announce + generate + background
        int barX = rx + 86;
        int barW = inner - 92 - 40;
        s.probeDelayMs = (int) Math.round(sliderValue(mouseX, barX, barW, 10, 500));
        manager.save();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        return false;
    }

    @Override
    public boolean charTyped(int codePoint) {
        return false;
    }

    private void drawModeRow(TextRenderer tr, DrawContext context, int sx, int sy, int rowW, DupedbSettings s) {
        boolean autoMode = s.mode == DupedbMode.AUTO;
        context.drawTextWithShadow(tr, Text.literal("Mode"), sx, sy + 2, UiTokens.TEXT_DIM);
        int tabsX = sx + 42;
        int avail = rowW - 42;
        int half = (avail - 4) / 2;
        UiComponents.drawSegmentTab(tr, context, tabsX, sy, half, 12, "Command", !autoMode);
        UiComponents.drawSegmentTab(tr, context, tabsX + half + 4, sy, half, 12, "Auto", autoMode);
    }

    private static void drawTrimmedText(
            DrawContext context, TextRenderer tr, String value, int x, int y, int maxWidth, int color) {
        String trimmed = tr.trimToWidth(value, Math.max(6, maxWidth));
        context.drawTextWithShadow(tr, Text.literal(trimmed), x, y, color);
    }

    private static double sliderValue(double mouseX, int x, int w, double min, double max) {
        double t = (mouseX - x) / w;
        t = Math.max(0.0, Math.min(1.0, t));
        return min + (max - min) * t;
    }
}
