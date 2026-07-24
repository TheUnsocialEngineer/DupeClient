package com.dupeclient.client.module.acaudit;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.overlay.AbstractDraggableOverlay;
import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class AcAuditOverlay extends AbstractDraggableOverlay implements IngameModuleOverlay {
    public static final AcAuditOverlay INSTANCE = new AcAuditOverlay();

    private static final int PANEL_W = 580;
    private static final int TITLE_H = 14;
    private static final int TAB_H = 14;
    private static final int TAB_COUNT = 3;
    private static final int GAP = 4;
    private static final int PAD = 8;
    private static final int CTRL_ROW = 18;
    private static final int METRIC_ROW = 22;
    private static final int TOGGLE_ROW = UiTokens.ROW_STEP;
    private static final int BTN_H = 14;
    private static final int SMALL_BTN = 22;
    private static final int BODY_H = 210;
    private static final int LOG_LINES = 7;
    private static final int LOG_LINE_H = 10;
    private static final int LABEL_COL = 108;
    private static final int SECTION_HEADER = 14;
    private static final int TAB_STRIP_H = Math.max(TAB_H, AcAuditGitHubCard.height());

    private static final int PANEL_BG = 0xE018181B;
    private static final int TITLE_BG = 0xFF27272A;
    private static final int BODY_BG = 0xFF0F172A;
    private static final int BODY_BORDER = 0xFF374151;
    private static final int LOG_BG = 0xFF0D0D12;

    private final AcAuditManager manager = AcAuditManager.INSTANCE;
    private final List<ToggleHit> toggleHits = new ArrayList<>();
    private final List<ButtonHit> buttonHits = new ArrayList<>();
    private final List<CycleHit> cycleHits = new ArrayList<>();
    private final List<StepperHit> stepperHits = new ArrayList<>();

    private Tab activeTab = Tab.MONITOR;
    private int logScrollOffset;

    private int hitTabMonitorX;
    private int hitTabProbesX;
    private int hitTabSettingsX;
    private int hitTabY;
    private int hitTabW;
    private int hitGithubX;
    private int hitGithubY;
    private int bodyTop;
    private int hitLogX;
    private int hitLogY;
    private int hitLogW;
    private int hitLogH;

    private AcAuditOverlay() {
    }

    @Override
    public String id() {
        return "ac_audit";
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
            com.dupeclient.client.gui.overlay.IngameOverlayHost.onModuleOverlayOpening(this);
        }
        manager.getSettings().overlayVisible = visible;
        if (!visible) {
            logScrollOffset = 0;
        }
        manager.save();
    }

    public void toggleOverlayVisible() {
        setOverlayVisible(!isOverlayVisible());
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
        return TITLE_H + GAP + TAB_STRIP_H + GAP + BODY_H + GAP + 10 + GAP + LOG_LINES * LOG_LINE_H + 6 + PAD;
    }

    @Override
    public boolean hasTextFocus() {
        return false;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!isActive()) {
            return;
        }
        syncTabFromSettings();
        toggleHits.clear();
        buttonHits.clear();
        cycleHits.clear();
        stepperHits.clear();

        AcAuditSettings s = manager.getSettings();
        AcAuditMetrics m = manager.getMetrics();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        Font tr = mc.font;
        int px = overlayX();
        int py = overlayY();
        int ph = panelHeight();

        context.fill(px, py, px + PANEL_W, py + ph, PANEL_BG);
        context.fill(px, py, px + PANEL_W, py + TITLE_H, TITLE_BG);
        context.drawString(tr, Component.literal("AC Audit"), px + PAD, py + 3, UiTokens.ACCENT);

        int stripY = py + TITLE_H + GAP;
        hitGithubX = px + PANEL_W - PAD - AcAuditGitHubCard.width();
        hitGithubY = stripY + (TAB_STRIP_H - AcAuditGitHubCard.height()) / 2;
        int tabAreaW = Math.max(180, hitGithubX - GAP - (px + PAD));
        hitTabW = (tabAreaW - GAP * (TAB_COUNT - 1)) / TAB_COUNT;
        hitTabY = stripY + (TAB_STRIP_H - TAB_H) / 2;
        hitTabMonitorX = px + PAD;
        hitTabProbesX = hitTabMonitorX + hitTabW + GAP;
        hitTabSettingsX = hitTabProbesX + hitTabW + GAP;
        UiComponents.drawSegmentTab(tr, context, hitTabMonitorX, hitTabY, hitTabW, TAB_H, "Monitor", activeTab == Tab.MONITOR);
        UiComponents.drawSegmentTab(tr, context, hitTabProbesX, hitTabY, hitTabW, TAB_H, "Probes", activeTab == Tab.PROBES);
        UiComponents.drawSegmentTab(tr, context, hitTabSettingsX, hitTabY, hitTabW, TAB_H, "Settings", activeTab == Tab.SETTINGS);
        AcAuditGitHubCard.render(context, tr, hitGithubX, hitGithubY, mouseX, mouseY);

        bodyTop = stripY + TAB_STRIP_H + GAP;
        int bodyBottom = bodyTop + BODY_H;
        context.fill(px + PAD, bodyTop, px + PANEL_W - PAD, bodyBottom, BODY_BG);
        context.fill(px + PAD, bodyTop, px + PANEL_W - PAD, bodyTop + 1, BODY_BORDER);

        int rx = px + PAD + 6;
        int inner = PANEL_W - PAD * 2 - 12;
        int y = bodyTop + 8;
        switch (activeTab) {
            case MONITOR -> y = renderMonitor(context, tr, m, rx, y, inner);
            case PROBES -> y = renderProbes(context, tr, s, rx, y, inner);
            case SETTINGS -> y = renderSettings(context, tr, s, rx, y, inner);
        }

        int logTop = bodyBottom + GAP;
        context.drawString(tr, Component.literal("Log"), px + PAD, logTop, UiTokens.TEXT_DIM);
        logTop += 10 + GAP;
        hitLogX = px + 4;
        hitLogY = logTop - 2;
        hitLogW = PANEL_W - 8;
        hitLogH = LOG_LINES * LOG_LINE_H + 4;
        context.fill(hitLogX, hitLogY, hitLogX + hitLogW, hitLogY + hitLogH, LOG_BG);
        renderLog(context, tr, px + PAD, logTop);

        int statusY = py + ph - PAD - 8;
        context.drawString(
                tr,
                Component.literal("Authorized use — own servers / consent"),
                px + PAD,
                statusY,
                UiTokens.TEXT_DIM);
    }

    private int renderMonitor(GuiGraphics context, Font tr, AcAuditMetrics m, int x, int y, int inner) {
        int col = inner / 2;
        drawMetric(tr, context, x, y, "TPS", String.format(Locale.ROOT, "%.1f", m.tps));
        drawMetric(tr, context, x + col, y, "Ping", m.ping >= 0 ? m.ping + "ms" : "—");
        y += METRIC_ROW;
        drawMetric(tr, context, x, y, "Setbacks/s", Integer.toString(m.setbackRate));
        drawMetric(tr, context, x + col, y, "In / Out", m.inRate + " / " + m.outRate);
        y += METRIC_ROW;
        drawMetric(tr, context, x, y, "Moving / Still", m.setbacksMoving + " / " + m.setbacksStill);
        drawMetric(tr, context, x + col, y, "Corrections", Integer.toString(m.correctionCount));
        y += METRIC_ROW;
        if (m.brand != null) {
            context.drawString(tr, Component.literal("Brand"), x, y + 2, UiTokens.TEXT_DIM);
            context.drawString(tr, Component.literal(tr.plainSubstrByWidth(m.brand, inner - 8)), x, y + 12, 0xFFE5E7EB);
            y += METRIC_ROW;
            context.drawString(tr, Component.literal(tr.plainSubstrByWidth(m.platform, inner)), x, y + 4, 0xFF86EFAC);
            y += CTRL_ROW;
        }
        if (m.correctionCount > 0) {
            drawMetric(tr, context, x, y, "Corr RTT",
                    String.format(Locale.ROOT, "%d-%dms avg %d", m.correctionRttMin, m.correctionRttMax, m.correctionRttAvg));
            y += METRIC_ROW;
        }
        if (!m.anticheatPlugins.isEmpty()) {
            context.drawString(tr, Component.literal("AC plugins"), x, y + 2, UiTokens.TEXT_DIM);
            context.drawString(tr, Component.literal(tr.plainSubstrByWidth(String.join(", ", m.anticheatPlugins), inner)), x, y + 12, 0xFFFF6B6B);
            y += METRIC_ROW;
        }
        if (!m.pluginNamespaces.isEmpty()) {
            context.drawString(tr, Component.literal("Namespaces (" + m.discoveredCommandCount + " cmds)"), x, y + 2, UiTokens.TEXT_DIM);
            context.drawString(tr, Component.literal(tr.plainSubstrByWidth(String.join(", ", m.pluginNamespaces), inner)), x, y + 12, UiTokens.TEXT_DIM);
            y += METRIC_ROW;
        }
        if (!m.topPackets.isEmpty()) {
            context.drawString(tr, Component.literal("Top S2C"), x, y + 2, UiTokens.TEXT_DIM);
            y += 12;
            for (String line : m.topPackets) {
                context.drawString(tr, Component.literal("  " + line), x, y, UiTokens.TEXT_DIM);
                y += LOG_LINE_H;
            }
        }
        if (manager.getSettings().slotSyncProbeActive && m.slotSyncProbeTotal > 0) {
            context.drawString(tr, Component.literal(String.format(Locale.ROOT,
                    "Slot probe %d/%d %s", m.slotSyncProbeIndex + 1, m.slotSyncProbeTotal,
                    m.slotSyncProbeLabel != null ? m.slotSyncProbeLabel : "")), x, y + 2, 0xFFFBBF24);
            y += CTRL_ROW;
        }
        return y;
    }

    private int renderProbes(GuiGraphics context, Font tr, AcAuditSettings s, int x, int y, int inner) {
        int colW = (inner - GAP) / 2;
        int left = x;
        int right = x + colW + GAP;
        int ly = y;
        int ry = y;

        drawSectionHeader(tr, context, left, ly, "Slot sync probe");
        drawSectionHeader(tr, context, right, ry, "Manual click");
        ly += SECTION_HEADER;
        ry += SECTION_HEADER;

        int ctrlLeft = colW - 4;
        int ctrlRight = colW - 4;

        drawLabel(tr, context, left, ly, "Field");
        drawCycleButton(context, tr, left + 52, ly, ctrlLeft - 52, enumLabel(s.slotSyncProbeField), () -> {
            s.slotSyncProbeField = cycle(s.slotSyncProbeField);
            manager.save();
        });
        ly += CTRL_ROW;

        drawLabel(tr, context, right, ry, "Sync");
        drawCycleButton(context, tr, right + 52, ry, ctrlRight - 52, s.manualClickSyncMode.name(), () -> {
            s.manualClickSyncMode = cycle(s.manualClickSyncMode);
            manager.save();
        });
        ry += CTRL_ROW;

        drawLabel(tr, context, left, ly, "Delay");
        drawStepper(context, tr, left + 52, ly, ctrlLeft - 52, s.slotSyncProbeDelayTicks, 1, 100, v -> {
            s.slotSyncProbeDelayTicks = v;
            manager.save();
        });
        ly += CTRL_ROW;

        drawLabel(tr, context, right, ry, "Revision");
        drawCycleButton(context, tr, right + 52, ry, ctrlRight - 52, s.manualClickRevMode.name(), () -> {
            s.manualClickRevMode = cycle(s.manualClickRevMode);
            manager.save();
        });
        ry += CTRL_ROW;

        drawToggleRow(context, tr, left, ly, colW, "Loop", s.slotSyncProbeLoop, () -> {
            s.slotSyncProbeLoop = !s.slotSyncProbeLoop;
            manager.save();
        });
        ly += TOGGLE_ROW;

        drawLabel(tr, context, right, ry, "Action");
        drawCycleButton(context, tr, right + 52, ry, ctrlRight - 52, s.manualClickAction.name(), () -> {
            s.manualClickAction = cycle(s.manualClickAction);
            manager.save();
        });
        ry += CTRL_ROW;

        drawActionButton(context, tr, left, ly, colW - 4, BTN_H,
                s.slotSyncProbeActive ? "Stop slot probe" : "Start slot probe", () -> {
                    if (s.slotSyncProbeActive) {
                        manager.stopSlotSyncProbe();
                    } else {
                        manager.startSlotSyncProbe();
                    }
                });
        ly += BTN_H + GAP;

        drawLabel(tr, context, right, ry, "Slot");
        drawStepper(context, tr, right + 52, ry, ctrlRight - 52, s.manualClickSlot, -128, 32767, v -> {
            s.manualClickSlot = v;
            manager.save();
        });
        ry += CTRL_ROW;

        drawActionButton(context, tr, left, ly, colW - 4, BTN_H,
                s.commandFingerprintActive ? "Stop cmd scan" : "Start cmd scan", () -> {
                    if (s.commandFingerprintActive) {
                        manager.stopCommandFingerprint();
                    } else {
                        manager.startCommandFingerprint();
                    }
                });
        ly += BTN_H + GAP;

        drawLabel(tr, context, right, ry, "Button");
        drawStepper(context, tr, right + 52, ry, ctrlRight - 52, s.manualClickButton, -128, 127, v -> {
            s.manualClickButton = v;
            manager.save();
        });
        ry += CTRL_ROW;

        drawLabel(tr, context, right, ry, "Count");
        drawStepper(context, tr, right + 52, ry, ctrlRight - 52, s.manualClickCount, 1, 500, v -> {
            s.manualClickCount = v;
            manager.save();
        });
        ry += CTRL_ROW;

        drawActionButton(context, tr, right, ry, colW - 4, BTN_H, "Fire manual click",
                () -> manager.fireManualClick(Minecraft.getInstance()));
        return Math.max(ly, ry + BTN_H);
    }

    private int renderSettings(GuiGraphics context, Font tr, AcAuditSettings s, int x, int y, int inner) {
        int ctrlW = inner - LABEL_COL;
        drawToggleRow(context, tr, x, y, inner, "Log probe to chat", s.logProbeToChat, () -> {
            s.logProbeToChat = !s.logProbeToChat;
            manager.save();
        });
        y += TOGGLE_ROW;
        drawToggleRow(context, tr, x, y, inner, "Announce platform", s.announcePlatform, () -> {
            s.announcePlatform = !s.announcePlatform;
            manager.save();
        });
        y += TOGGLE_ROW;
        drawToggleRow(context, tr, x, y, inner, "Setback verbose", s.setbackVerbose, () -> {
            s.setbackVerbose = !s.setbackVerbose;
            manager.save();
        });
        y += TOGGLE_ROW;
        drawToggleRow(context, tr, x, y, inner, "Correction RTT log", s.correctionVerbose, () -> {
            s.correctionVerbose = !s.correctionVerbose;
            manager.save();
        });
        y += TOGGLE_ROW;
        drawToggleRow(context, tr, x, y, inner, "Packet cadence", s.packetCadenceEnabled, () -> {
            s.packetCadenceEnabled = !s.packetCadenceEnabled;
            manager.save();
        });
        y += TOGGLE_ROW;

        drawLabel(tr, context, x, y, "Setback report");
        drawStepper(context, tr, x + LABEL_COL, y, ctrlW, s.setbackReportIntervalSec, 1, 30, v -> {
            s.setbackReportIntervalSec = v;
            manager.save();
        });
        y += CTRL_ROW;

        drawLabel(tr, context, x, y, "Cadence top-N");
        drawStepper(context, tr, x + LABEL_COL, y, ctrlW, s.packetCadenceTopN, 1, 20, v -> {
            s.packetCadenceTopN = v;
            manager.save();
        });
        y += CTRL_ROW;

        drawLabel(tr, context, x, y, "Cadence interval");
        drawStepper(context, tr, x + LABEL_COL, y, ctrlW, s.packetCadenceIntervalSec, 1, 60, v -> {
            s.packetCadenceIntervalSec = v;
            manager.save();
        });
        y += CTRL_ROW;

        drawLabel(tr, context, x, y, "Cmd scan delay");
        drawStepper(context, tr, x + LABEL_COL, y, ctrlW, s.commandFingerprintDelayTicks, 1, 100, v -> {
            s.commandFingerprintDelayTicks = v;
            manager.save();
        });
        y += CTRL_ROW;

        drawToggleRow(context, tr, x, y, inner, "Cmd scan sweep A-Z", s.commandFingerprintSweep, () -> {
            s.commandFingerprintSweep = !s.commandFingerprintSweep;
            manager.save();
        });
        return y + TOGGLE_ROW;
    }

    private void renderLog(GuiGraphics context, Font tr, int x, int y) {
        List<String> logs = manager.getLogLines();
        int maxScroll = Math.max(0, logs.size() - LOG_LINES);
        logScrollOffset = Math.max(0, Math.min(maxScroll, logScrollOffset));
        int start = Math.max(0, logs.size() - LOG_LINES - logScrollOffset);
        for (int i = 0; i < LOG_LINES; i++) {
            int idx = start + i;
            String line = idx < logs.size() ? logs.get(idx) : "";
            int color = line.contains("Disconnect") || line.contains("AC plugin") ? 0xFFFF6B6B : UiTokens.TEXT_DIM;
            context.drawString(tr, Component.literal(tr.plainSubstrByWidth(line, PANEL_W - PAD * 2)), x, y, color);
            y += LOG_LINE_H;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isActive() || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        int px = overlayX();
        int py = overlayY();
        if (beginTitleDrag(mouseX, mouseY, button, px, py, PANEL_W, TITLE_H)) {
            return true;
        }
        if (AcAuditGitHubCard.mouseClicked(mouseX, mouseY, hitGithubX, hitGithubY, button)) {
            return true;
        }
        if (clickTab(mouseX, mouseY, hitTabMonitorX, Tab.MONITOR)) {
            return true;
        }
        if (clickTab(mouseX, mouseY, hitTabProbesX, Tab.PROBES)) {
            return true;
        }
        if (clickTab(mouseX, mouseY, hitTabSettingsX, Tab.SETTINGS)) {
            return true;
        }
        for (ToggleHit hit : toggleHits) {
            if (inRect(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) {
                hit.action.run();
                return true;
            }
        }
        for (ButtonHit hit : buttonHits) {
            if (inRect(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) {
                hit.action.run();
                return true;
            }
        }
        for (CycleHit hit : cycleHits) {
            if (inRect(mouseX, mouseY, hit.x, hit.y, hit.w, hit.h)) {
                hit.action.run();
                return true;
            }
        }
        for (StepperHit hit : stepperHits) {
            if (inRect(mouseX, mouseY, hit.minusX(), hit.y(), hit.btnW(), hit.h())) {
                hit.stepConsumer().accept(Math.max(hit.min(), hit.value() - 1));
                return true;
            }
            if (inRect(mouseX, mouseY, hit.plusX(), hit.y(), hit.btnW(), hit.h())) {
                hit.stepConsumer().accept(Math.min(hit.max(), hit.value() + 1));
                return true;
            }
        }
        if (inRect(mouseX, mouseY, hitLogX, hitLogY, hitLogW, hitLogH)) {
            return true;
        }
        return containsPoint(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        endTitleDrag(button);
        return isDragging();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return updateTitleDrag(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!isActive() || !inRect(mouseX, mouseY, hitLogX, hitLogY, hitLogW, hitLogH)) {
            return false;
        }
        logScrollOffset = Math.max(0, logScrollOffset - (int) vertical);
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

    private boolean clickTab(double mx, double my, int tabX, Tab tab) {
        if (!inRect(mx, my, tabX, hitTabY, hitTabW, TAB_H)) {
            return false;
        }
        activeTab = tab;
        persistTab();
        return true;
    }

    private void syncTabFromSettings() {
        activeTab = Tab.fromId(manager.getSettings().overlayTab);
    }

    private void persistTab() {
        manager.getSettings().overlayTab = activeTab.id;
        manager.save();
    }

    private void drawMetric(Font tr, GuiGraphics context, int x, int y, String label, String value) {
        context.drawString(tr, Component.literal(label), x, y + 2, UiTokens.TEXT_DIM);
        context.drawString(tr, Component.literal(value), x, y + 12, 0xFFE5E7EB);
    }

    private static void drawSectionHeader(Font tr, GuiGraphics context, int x, int y, String label) {
        context.drawString(tr, Component.literal(label), x, y + 3, 0xFF86EFAC);
    }

    private static void drawLabel(Font tr, GuiGraphics context, int x, int y, String label) {
        context.drawString(tr, Component.literal(label), x, y + 4, UiTokens.TEXT_DIM);
    }

    private void drawToggleRow(GuiGraphics context, Font tr, int x, int y, int w, String label, boolean on, Runnable action) {
        UiComponents.drawOptionToggle(tr, context, x, y, w, label, on, on ? 1f : 0f);
        toggleHits.add(new ToggleHit(x, y, w, TOGGLE_ROW, action));
    }

    private void drawActionButton(GuiGraphics context, Font tr, int x, int y, int w, int h, String label, Runnable action) {
        drawMiniBtn(context, tr, x, y, w, h, label, true);
        buttonHits.add(new ButtonHit(x, y, w, h, action));
    }

    private void drawCycleButton(GuiGraphics context, Font tr, int x, int y, int w, String value, Runnable action) {
        int bg = 0xFF1F2937;
        int border = 0xFF374151;
        context.fill(x, y, x + w, y + BTN_H, bg);
        context.fill(x, y, x + w, y + 1, border);
        context.drawCenteredString(tr, Component.literal(tr.plainSubstrByWidth(value, w - 6)), x + w / 2, y + 3, 0xFFE5E7EB);
        cycleHits.add(new CycleHit(x, y, w, BTN_H, action));
    }

    private void drawStepper(GuiGraphics context, Font tr, int x, int y, int w, int value, int min, int max, IntConsumer apply) {
        int btnW = SMALL_BTN;
        int minusX = x;
        int plusX = x + w - btnW;
        drawMiniBtn(context, tr, minusX, y, btnW, BTN_H, "-", true);
        drawMiniBtn(context, tr, plusX, y, btnW, BTN_H, "+", true);
        context.drawCenteredString(tr, Component.literal(Integer.toString(value)), x + w / 2, y + 3, 0xFF86EFAC);
        stepperHits.add(new StepperHit(minusX, plusX, y, btnW, BTN_H, value, min, max, apply));
    }

    private static void drawMiniBtn(GuiGraphics context, Font tr, int x, int y, int w, int h, String label, boolean enabled) {
        context.fill(x, y, x + w, y + h, enabled ? 0xFF374151 : 0xFF1F2937);
        int tw = tr.width(label);
        context.drawString(tr, Component.literal(label), x + (w - tw) / 2, y + 3, enabled ? 0xFFE5E7EB : UiTokens.TEXT_DIM);
    }

    private static String enumLabel(AcAuditSettings.SlotSyncField field) {
        return field != null ? field.name() : AcAuditSettings.SlotSyncField.ALL.name();
    }

    private static AcAuditSettings.SlotSyncField cycle(AcAuditSettings.SlotSyncField current) {
        AcAuditSettings.SlotSyncField[] values = AcAuditSettings.SlotSyncField.values();
        return values[((current != null ? current.ordinal() : 0) + 1) % values.length];
    }

    private static AcAuditSettings.ManualSyncMode cycle(AcAuditSettings.ManualSyncMode current) {
        AcAuditSettings.ManualSyncMode[] values = AcAuditSettings.ManualSyncMode.values();
        return values[((current != null ? current.ordinal() : 0) + 1) % values.length];
    }

    private static AcAuditSettings.ManualRevMode cycle(AcAuditSettings.ManualRevMode current) {
        AcAuditSettings.ManualRevMode[] values = AcAuditSettings.ManualRevMode.values();
        return values[((current != null ? current.ordinal() : 0) + 1) % values.length];
    }

    private static AcAuditSettings.ManualClickAction cycle(AcAuditSettings.ManualClickAction current) {
        AcAuditSettings.ManualClickAction[] values = AcAuditSettings.ManualClickAction.values();
        return values[((current != null ? current.ordinal() : 0) + 1) % values.length];
    }

    private enum Tab {
        MONITOR("MONITOR"),
        PROBES("PROBES"),
        SETTINGS("SETTINGS");

        private final String id;

        Tab(String id) {
            this.id = id;
        }

        static Tab fromId(String id) {
            if (id == null || "SLOT".equalsIgnoreCase(id)) {
                return MONITOR;
            }
            for (Tab tab : values()) {
                if (tab.id.equalsIgnoreCase(id)) {
                    return tab;
                }
            }
            return MONITOR;
        }
    }

    @FunctionalInterface
    private interface IntConsumer {
        void accept(int value);
    }

    private record ToggleHit(int x, int y, int w, int h, Runnable action) {
    }

    private record ButtonHit(int x, int y, int w, int h, Runnable action) {
    }

    private record CycleHit(int x, int y, int w, int h, Runnable action) {
    }

    private record StepperHit(int minusX, int plusX, int y, int btnW, int h, int value, int min, int max, IntConsumer stepConsumer) {
    }
}
