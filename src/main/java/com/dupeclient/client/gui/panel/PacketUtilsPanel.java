package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.panel.Panel;
import com.dupeclient.client.module.packet.PacketUtils;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import com.dupeclient.client.module.packet.fabricator.FabricatorPresetCodec;
import com.dupeclient.client.module.packet.fabricator.FabricatorPresetStore;
import com.dupeclient.client.module.packet.fabricator.FabricatorTab;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferManager;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferOverlay;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.text.Text;
import net.minecraft.network.packet.Packet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public class PacketUtilsPanel
extends Panel {
    private static final int PD_LIST_H = 72;
    private static final int PD_COL_HEADER = 10;
    private static final int PD_LINE = 9;
    private static final int PD_MAX_VISIBLE = 8;
    private static final float PD_LIST_TEXT_SCALE_MAX = 1.0f;
    private static List<String> sortedC2sNames;
    private static List<String> sortedS2cNames;
    private CaptureMode captureMode = CaptureMode.NONE;
    private SliderMode sliderMode = SliderMode.NONE;
    private boolean packetDelayCollapsed;
    private boolean uiUtilsCollapsed;
    private boolean fabricatorCollapsed;
    private int fabricatorPresetIndex;
    private boolean snifferCollapsed;
    private boolean sniffPickerOpen;
    private SnifferListMode sniffListMode = SnifferListMode.LOG_EXCLUDE;
    private DelaySide sniffSide = DelaySide.C2S;
    private String sniffSearch = "";
    private boolean sniffSearchFocused;
    private int sniffIncludedScroll;
    private int sniffExcludedScroll;
    private boolean sniffListsDirty = true;
    private List<String> cachedSniffIncludedFiltered = List.of();
    private List<String> cachedSniffExcludedFiltered = List.of();
    private float cachedSniffListScale = 1.0f;
    private int sniffIncludedListX;
    private int sniffIncludedListY;
    private int sniffIncludedListW;
    private int sniffIncludedListH;
    private int sniffExcludedListX;
    private int sniffExcludedListY;
    private int sniffExcludedListW;
    private int sniffExcludedListH;
    private int sniffSearchHitX;
    private int sniffSearchHitY;
    private int sniffSearchHitW;
    private int sniffSearchHitH;
    private int snifferClearX;
    private int snifferClearY;
    private int snifferClearW;
    private int snifferExportX;
    private int snifferExportW;
    private int snifferFolderX;
    private int snifferFolderW;
    private int snifferFabX;
    private int snifferFabW;
    private static final int SECTION_TITLE_H = 32;
    private static final int QUEUED_TEXT_H = 10;
    private static final int UI_UTILS_ROWS = 7;

    public PacketUtilsPanel(int x, int y) {
        super("packet_utils", Text.literal("Packet Utils"), x, y, 336, 198);
    }

    private static void ensureSortedNames() {
        String n;
        if (sortedC2sNames != null) {
            return;
        }
        sortedC2sNames = new ArrayList<>();
        for (Class<? extends Packet<?>> clazz : PacketUtils.getC2SPackets()) {
            n = PacketUtils.getName(clazz);
            if (n == null || n.isBlank()) continue;
            sortedC2sNames.add(n);
        }
        Collections.sort(sortedC2sNames);
        sortedS2cNames = new ArrayList<>();
        for (Class<? extends Packet<?>> clazz : PacketUtils.getS2CPackets()) {
            n = PacketUtils.getName(clazz);
            if (n == null || n.isBlank()) continue;
            sortedS2cNames.add(n);
        }
        Collections.sort(sortedS2cNames);
    }

    private int bodyX() {
        return this.x + 16;
    }

    private int bodyW() {
        return this.width - 32;
    }

    private int rowX() {
        return this.bodyX() + 8;
    }

    private int rowW() {
        return this.bodyW() - 16;
    }

    private static int uiUtilsSectionHeight(boolean collapsed) {
        return collapsed ? 32 : 256;
    }

    private int snifferSectionHeight(boolean collapsed) {
        if (collapsed) {
            return 32;
        }
        return 152;
    }

    private int packetDelaySectionHeight() {
        if (this.packetDelayCollapsed) {
            return 32;
        }
        return 166;
    }

    private int fabricatorSectionHeight() {
        if (this.fabricatorCollapsed) {
            return 32;
        }
        return 196;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        PacketUtilsSettings s = PacketUtilsManager.INSTANCE.getSettings();
        PacketUtilsPanel.ensureSortedNames();
        super.render(context, mouseX, mouseY, delta);
        if (this.collapsed) {
            return;
        }
        int tx = this.bodyX();
        int ty = this.y + this.bodyTopOffset() + 8;
        int sw = this.bodyW();
        int rw = this.rowW();
        int rx = this.rowX();
        this.drawToggleRow(context, rx, ty, rw, "Chat feedback", s.moduleChatFeedback, "pkt.chatFb", delta);
        this.drawToggleRow(context, rx, ty += 30, rw, "Disable on leave", s.disableActiveOnLeave, "pkt.leaveOff", delta);
        int delayHeight = this.packetDelaySectionHeight();
        this.drawSectionBox(context, tx, ty += 35, sw, delayHeight, "Packet Delay", this.packetDelayCollapsed);
        if (!this.packetDelayCollapsed) {
            int py = ty + 38;
            this.drawToggleRow(context, rx, py, rw, "Log packets", s.logPacketNamesOnDelay, "pkt.logPkt", delta);
            this.drawToggleRow(context, rx, py += 30, rw, "Blocked chat", s.packetDelayBlockedChatNotify, "pkt.blockChat", delta);
            this.drawBindRow(context, rx, py += 30, rw, "Delay toggle key", s.packetDelayToggleKey, CaptureMode.PACKET_DELAY_TOGGLE);
            this.drawBindRow(context, rx, py += 30, rw, "Delay overlay key", s.packetDelayOverlayToggleKey, CaptureMode.PACKET_DELAY_OVERLAY);
        }
        int uiHeight = PacketUtilsPanel.uiUtilsSectionHeight(this.uiUtilsCollapsed);
        this.drawSectionBox(context, tx, ty += delayHeight + 5, sw, uiHeight, "UI Utils", this.uiUtilsCollapsed);
        if (!this.uiUtilsCollapsed) {
            int rowY = ty + 38;
            this.drawToggleRow(context, rx, rowY, rw, "Show overlay", s.uiUtilsOverlayEnabled, "pkt.uiOverlay", delta);
            this.drawSlider(context, rx, rowY += 30, rw, s.uiUtilsDelayReleaseMs, 0.0, 2000.0, "Delay flush", SliderMode.UIUTILS_RELEASE);
            this.drawBindRow(context, rx, rowY += 30, rw, "Overlay Toggle", s.uiUtilsOverlayToggleKey, CaptureMode.UIUTILS_OVERLAY);
            this.drawBindRow(context, rx, rowY += 30, rw, "Close Without Packet", s.uiUtilsCloseWithoutPacketKey, CaptureMode.UIUTILS_CLOSE);
            this.drawBindRow(context, rx, rowY += 30, rw, "Delay Toggle", s.uiUtilsDelayToggleKey, CaptureMode.UIUTILS_DELAY_TOGGLE);
            this.drawBindRow(context, rx, rowY += 30, rw, "Send Packets Toggle", s.uiUtilsSendPacketsToggleKey, CaptureMode.UIUTILS_SEND_TOGGLE);
            this.drawBindRow(context, rx, rowY += 30, rw, "Flush Queue", s.uiUtilsSendQueuedKey, CaptureMode.UIUTILS_SEND_QUEUED);
        }
        int fabHeight = this.fabricatorSectionHeight();
        this.drawSectionBox(context, tx, ty += uiHeight + 5, sw, fabHeight, "Packet Fabricator", this.fabricatorCollapsed);
        if (!this.fabricatorCollapsed) {
            int fy = ty + 38;
            this.drawToggleRow(context, rx, fy, rw, "Enabled", s.fabricatorEnabled, "pkt.fabEn", delta);
            this.drawToggleRow(context, rx, fy += 30, rw, "Show overlay", s.fabricatorVisible, "pkt.fabVis", delta);
            this.drawBindRow(context, rx, fy += 30, rw, "Overlay toggle key", s.fabricatorToggleKey, CaptureMode.FABRICATOR_TOGGLE);
            this.drawBindRow(context, rx, fy += 30, rw, "Slot IDs toggle key", s.slotIdsToggleKey, CaptureMode.SLOT_IDS_TOGGLE);
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal("Click row: Save preset / Load preset"), rx, fy += 34, -7035976);
        }
        PacketSnifferManager sniffer = PacketSnifferManager.INSTANCE;
        PacketSnifferSettings sniff = sniffer.getSettings();
        int sniffHeight = this.snifferSectionHeight(this.snifferCollapsed);
        this.drawSectionBox(context, tx, ty += fabHeight + 5, sw, sniffHeight, "Packet Sniffer", this.snifferCollapsed);
        if (!this.snifferCollapsed) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int sy = ty + 38;
            String counts = "C2S " + sniffer.c2sCount() + " \u00b7 S2C " + sniffer.s2cCount() + " \u00b7 configure in overlay Settings tab";
            context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(counts, rw)), rx, sy, -7035976);
            this.drawToggleRow(context, rx, sy += 16, rw, "Enabled", sniff.enabled, "sniff.enabled", delta);
            this.drawToggleRow(context, rx, sy += 30, rw, "Show overlay", sniff.overlayVisible, "sniff.overlay", delta);
            this.drawBindRow(context, rx, sy += 30, rw, "Overlay toggle key", sniff.overlayToggleKey, CaptureMode.PACKET_SNIFFER_OVERLAY_TOGGLE);
        }
        this.height = this.bodyTopOffset() + 8 + 60 + 5 + delayHeight + 5 + uiHeight + 5 + fabHeight + 5 + sniffHeight + 8;
        if (this.captureMode != CaptureMode.NONE) {
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(("Press key for " + this.captureMode.label + " (ESC to unbind)")), this.x + 8, this.y + this.height - 12, -14217);
        }
    }

    private void drawTab(DrawContext context, int bx, int by, int tw, String label, boolean active) {
        UiComponents.drawSegmentTab(MinecraftClient.getInstance().textRenderer, context, bx, by, tw, 14, label, active);
    }

    private static void drawScaledPacketRow(DrawContext context, MinecraftClient mc, String name, int x, int y, float scale, int color) {
        if (name == null || name.isEmpty() || scale <= 0.0f) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate((float)x, (float)y);
        matrices.scale(scale, scale);
        context.drawTextWithShadow(tr, Text.literal(name), 0, 0, color);
        matrices.popMatrix();
    }

    private static List<String> filterNames(List<String> all, String query) {
        if (query == null || query.isBlank()) {
            return all;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<>();
        for (String n : all) {
            if (!n.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(n);
        }
        return out;
    }

    private void drawSnifferListModeTabs(DrawContext context, int bx, int by, int w) {
        int half = (w - 6) / 2;
        this.drawTab(context, bx, by, half, "Log exclude", this.sniffListMode == SnifferListMode.LOG_EXCLUDE);
        this.drawTab(context, bx + half + 6, by, half, "Block", this.sniffListMode == SnifferListMode.BLOCK);
    }

    private void drawSnifferSideTabs(DrawContext context, int bx, int by, int w) {
        int half = (w - 6) / 2;
        this.drawTab(context, bx, by, half, "C2S", this.sniffSide == DelaySide.C2S);
        this.drawTab(context, bx + half + 6, by, half, "S2C", this.sniffSide == DelaySide.S2C);
    }

    private void drawSnifferPickerDropdown(DrawContext context, int bx, int by, int w, PacketSnifferSettings sniff) {
        sniff.ensureLists();
        UiComponents.drawSlotField(context, bx, by, w, 12, -938338270, -12957090);
        List<String> selected = this.currentSniffTargetList(sniff);
        String summary = selected.isEmpty() ? "no " + this.sniffSide.name() + " packets" : selected.size() + " " + this.sniffSide.name() + " packet(s)";
        String prefix = this.sniffListMode == SnifferListMode.LOG_EXCLUDE ? "Hide from log" : "Block";
        String text = (this.sniffPickerOpen ? prefix + " v " : prefix + " > ") + summary;
        String trimmed = MinecraftClient.getInstance().textRenderer.trimToWidth(text, Math.max(8, w - 8));
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(trimmed), bx + 4, by + 2, -3483137);
    }

    private void drawSnifferSearchRow(DrawContext context, int bx, int by, int w, int h) {
        int border = this.sniffSearchFocused ? -9789697 : -12957090;
        UiComponents.drawSlotField(context, bx, by, w, h, -804317397, border);
        context.fill(bx, by + h - 1, bx + w, by + h, border);
        String q = this.sniffSearch.isEmpty() ? "Search packets..." : this.sniffSearch;
        int col = this.sniffSearch.isEmpty() ? -8747362 : -2037249;
        String shown = MinecraftClient.getInstance().textRenderer.trimToWidth(q, w - 12);
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(shown), bx + 4, by + 2, col);
    }

    private void markSniffListsDirty() {
        this.sniffListsDirty = true;
    }

    private void refreshSniffListsIfNeeded(PacketSnifferSettings sniff, MinecraftClient mc) {
        if (!this.sniffListsDirty) {
            return;
        }
        this.sniffListsDirty = false;
        this.cachedSniffIncludedFiltered = PacketUtilsPanel.filterNames(this.sortedSniffSelectedNames(sniff), this.sniffSearch);
        this.cachedSniffExcludedFiltered = PacketUtilsPanel.filterNames(this.excludedSniffPool(sniff), this.sniffSearch);
        int maxNamePx = 1;
        for (String n : this.cachedSniffExcludedFiltered) {
            maxNamePx = Math.max(maxNamePx, mc.textRenderer.getWidth(n));
        }
        for (String n : this.cachedSniffIncludedFiltered) {
            maxNamePx = Math.max(maxNamePx, mc.textRenderer.getWidth(n));
        }
        int colAvail = Math.min(this.sniffExcludedListW, this.sniffIncludedListW) - 8 - 3;
        this.cachedSniffListScale = Math.min(1.0f, (float)Math.max(4, colAvail) / (float)maxNamePx);
    }

    private void drawSnifferTwoColumns(DrawContext context, int bx, int by, int innerW, PacketSnifferSettings sniff) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int half = (innerW - 8) / 2;
        context.drawTextWithShadow(mc.textRenderer, Text.literal("Available"), bx, by, -6310168);
        context.drawTextWithShadow(mc.textRenderer, Text.literal("Selected"), bx + half + 8, by, -6310168);
        this.refreshSniffListsIfNeeded(sniff, mc);
        int maxInc = Math.max(0, this.cachedSniffIncludedFiltered.size() - 8);
        this.sniffIncludedScroll = Math.max(0, Math.min(this.sniffIncludedScroll, maxInc));
        int maxExc = Math.max(0, this.cachedSniffExcludedFiltered.size() - 8);
        this.sniffExcludedScroll = Math.max(0, Math.min(this.sniffExcludedScroll, maxExc));
        context.fill(this.sniffExcludedListX, this.sniffExcludedListY, this.sniffExcludedListX + this.sniffExcludedListW, this.sniffExcludedListY + this.sniffExcludedListH, -804317397);
        context.fill(this.sniffIncludedListX, this.sniffIncludedListY, this.sniffIncludedListX + this.sniffIncludedListW, this.sniffIncludedListY + this.sniffIncludedListH, -804317397);
        for (int r = 0; r < 8; ++r) {
            int j = this.sniffExcludedScroll + r;
            int ey = this.sniffExcludedListY + 2 + r * 9;
            if (j < this.cachedSniffExcludedFiltered.size()) {
                PacketUtilsPanel.drawScaledPacketRow(context, mc, this.cachedSniffExcludedFiltered.get(j), this.sniffExcludedListX + 4, ey, this.cachedSniffListScale, -19533);
            }
            int idx = this.sniffIncludedScroll + r;
            int iy = this.sniffIncludedListY + 2 + r * 9;
            if (idx >= this.cachedSniffIncludedFiltered.size()) continue;
            PacketUtilsPanel.drawScaledPacketRow(context, mc, this.cachedSniffIncludedFiltered.get(idx), this.sniffIncludedListX + 4, iy, this.cachedSniffListScale, -11083110);
        }
    }

    private List<String> currentSniffTargetList(PacketSnifferSettings sniff) {
        sniff.ensureLists();
        if (this.sniffListMode == SnifferListMode.LOG_EXCLUDE) {
            return this.sniffSide == DelaySide.C2S ? sniff.logExcludeC2sNames : sniff.logExcludeS2cNames;
        }
        return this.sniffSide == DelaySide.C2S ? sniff.blockC2sNames : sniff.blockS2cNames;
    }

    private List<String> sortedSniffSelectedNames(PacketSnifferSettings sniff) {
        ArrayList<String> raw = new ArrayList<String>(this.currentSniffTargetList(sniff));
        raw.sort(String.CASE_INSENSITIVE_ORDER);
        return raw;
    }

    private List<String> excludedSniffPool(PacketSnifferSettings sniff) {
        List<String> all = this.sniffSide == DelaySide.C2S ? sortedC2sNames : sortedS2cNames;
        HashSet<String> set = new HashSet<String>(this.currentSniffTargetList(sniff));
        ArrayList<String> out = new ArrayList<>();
        for (String n : all) {
            if (set.contains(n)) continue;
            out.add(n);
        }
        return out;
    }

    private boolean clickSnifferListColumns(double mouseX, double mouseY, PacketSnifferSettings sniff) {
        MinecraftClient mc = MinecraftClient.getInstance();
        this.refreshSniffListsIfNeeded(sniff, mc);
        List<String> target = this.currentSniffTargetList(sniff);
        if (this.rect(mouseX, mouseY, this.sniffExcludedListX, this.sniffExcludedListY, this.sniffExcludedListW, this.sniffExcludedListH)) {
            int idx;
            int row = (int)((mouseY - (double)this.sniffExcludedListY - 2.0) / 9.0);
            if (row >= 0 && row < 8 && (idx = this.sniffExcludedScroll + row) >= 0 && idx < this.cachedSniffExcludedFiltered.size()) {
                String name = this.cachedSniffExcludedFiltered.get(idx);
                if (!target.contains(name)) {
                    target.add(name);
                }
                PacketSnifferManager.INSTANCE.save();
                this.markSniffListsDirty();
            }
            return true;
        }
        if (this.rect(mouseX, mouseY, this.sniffIncludedListX, this.sniffIncludedListY, this.sniffIncludedListW, this.sniffIncludedListH)) {
            int idx;
            int row = (int)((mouseY - (double)this.sniffIncludedListY - 2.0) / 9.0);
            if (row >= 0 && row < 8 && (idx = this.sniffIncludedScroll + row) >= 0 && idx < this.cachedSniffIncludedFiltered.size()) {
                String name = this.cachedSniffIncludedFiltered.get(idx);
                while (target.remove(name)) {
                }
                PacketSnifferManager.INSTANCE.save();
                this.markSniffListsDirty();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.collapsed || button != 0) {
            return false;
        }
        PacketUtilsPanel.ensureSortedNames();
        PacketUtilsSettings s = PacketUtilsManager.INSTANCE.getSettings();
        int tx = this.bodyX();
        int ty = this.y + this.bodyTopOffset();
        int sw = this.bodyW();
        int rw = this.rowW();
        int rx = this.rowX();
        if (this.rect(mouseX, mouseY, rx, ty += 8, rw, 30)) {
            s.moduleChatFeedback = !s.moduleChatFeedback;
            this.save();
            PacketUtilsManager.INSTANCE.moduleFeedbackConfigToggle("Packet Utils chat feedback " + (s.moduleChatFeedback ? "on" : "off"));
            return true;
        }
        if (this.rect(mouseX, mouseY, rx, ty += 30, rw, 30)) {
            s.disableActiveOnLeave = !s.disableActiveOnLeave;
            this.save();
            PacketUtilsManager.INSTANCE.moduleFeedback("Disable on leave " + (s.disableActiveOnLeave ? "on" : "off"));
            return true;
        }
        int delayHeight = this.packetDelaySectionHeight();
        if (this.rect(mouseX, mouseY, tx + sw - 14, (ty += 35) + 3, 10, 10)) {
            this.packetDelayCollapsed = !this.packetDelayCollapsed;
            return true;
        }
        if (!this.packetDelayCollapsed) {
            int py = ty + 38;
            if (this.rect(mouseX, mouseY, rx, py, rw, 30)) {
                s.logPacketNamesOnDelay = !s.logPacketNamesOnDelay;
                this.save();
                PacketUtilsManager.INSTANCE.moduleFeedback("Packet delay log packet names " + (s.logPacketNamesOnDelay ? "on" : "off"));
                return true;
            }
            if (this.rect(mouseX, mouseY, rx, py += 30, rw, 30)) {
                s.packetDelayBlockedChatNotify = !s.packetDelayBlockedChatNotify;
                this.save();
                PacketUtilsManager.INSTANCE.moduleFeedback("Packet delay blocked-packet chat notify " + (s.packetDelayBlockedChatNotify ? "on" : "off"));
                return true;
            }
            if (this.clickBindValue(mouseX, mouseY, rx, py += 30, rw)) {
                this.captureMode = CaptureMode.PACKET_DELAY_TOGGLE;
                return true;
            }
            if (this.clickBindValue(mouseX, mouseY, rx, py += 30, rw)) {
                this.captureMode = CaptureMode.PACKET_DELAY_OVERLAY;
                return true;
            }
        }
        int uiHeight = PacketUtilsPanel.uiUtilsSectionHeight(this.uiUtilsCollapsed);
        if (this.rect(mouseX, mouseY, tx + sw - 14, (ty += delayHeight + 5) + 3, 10, 10)) {
            this.uiUtilsCollapsed = !this.uiUtilsCollapsed;
            return true;
        }
        if (!this.uiUtilsCollapsed) {
            int rowY = ty + 38;
            if (this.rect(mouseX, mouseY, rx, rowY, rw, 30)) {
                PacketUtilsManager.INSTANCE.setUiUtilsOverlayEnabled(!s.uiUtilsOverlayEnabled);
                return true;
            }
            if (this.clickSlider(mouseX, mouseY, rx, rowY += 30, rw, s.uiUtilsDelayReleaseMs, 0.0, 2000.0, SliderMode.UIUTILS_RELEASE)) {
                return true;
            }
            if (this.clickBindValue(mouseX, mouseY, rx, rowY += 30, rw)) {
                this.captureMode = CaptureMode.UIUTILS_OVERLAY;
                return true;
            }
            if (this.clickBindValue(mouseX, mouseY, rx, rowY += 30, rw)) {
                this.captureMode = CaptureMode.UIUTILS_CLOSE;
                return true;
            }
            if (this.clickBindValue(mouseX, mouseY, rx, rowY += 30, rw)) {
                this.captureMode = CaptureMode.UIUTILS_DELAY_TOGGLE;
                return true;
            }
            if (this.clickBindValue(mouseX, mouseY, rx, rowY += 30, rw)) {
                this.captureMode = CaptureMode.UIUTILS_SEND_TOGGLE;
                return true;
            }
            if (this.clickBindValue(mouseX, mouseY, rx, rowY += 30, rw)) {
                this.captureMode = CaptureMode.UIUTILS_SEND_QUEUED;
                return true;
            }
        }
        int fabHeight = this.fabricatorSectionHeight();
        if (this.rect(mouseX, mouseY, tx + sw - 14, (ty += uiHeight + 5) + 3, 10, 10)) {
            this.fabricatorCollapsed = !this.fabricatorCollapsed;
            return true;
        }
        if (this.fabricatorCollapsed) {
            return false;
        }
        int fy = ty + 38;
        if (this.rect(mouseX, mouseY, rx, fy, rw, 30)) {
            boolean bl = s.fabricatorEnabled = !s.fabricatorEnabled;
            if (!s.fabricatorEnabled) {
                PacketFabricatorOverlay.INSTANCE.setVisible(false);
            } else {
                s.fabricatorVisible = true;
                PacketFabricatorOverlay.INSTANCE.setActiveTab(FabricatorTab.FABRICATE);
            }
            this.save();
            PacketUtilsManager.INSTANCE.moduleFeedback("Packet fabricator " + (s.fabricatorEnabled ? "enabled" : "disabled"));
            return true;
        }
        if (this.rect(mouseX, mouseY, rx, fy += 30, rw, 30)) {
            PacketFabricatorOverlay.INSTANCE.setActiveTab(FabricatorTab.FABRICATE);
            PacketFabricatorOverlay.INSTANCE.toggleVisible();
            PacketUtilsManager.INSTANCE.moduleFeedback("Fabricator overlay " + (PacketFabricatorOverlay.INSTANCE.isVisible() ? "shown" : "hidden"));
            return true;
        }
        if (this.clickBindValue(mouseX, mouseY, rx, fy += 30, rw)) {
            this.captureMode = CaptureMode.FABRICATOR_TOGGLE;
            return true;
        }
        if (this.clickBindValue(mouseX, mouseY, rx, fy += 30, rw)) {
            this.captureMode = CaptureMode.SLOT_IDS_TOGGLE;
            return true;
        }
        if (this.rect(mouseX, mouseY, rx, fy += 30, rw / 2 - 2, 18)) {
            String name = "preset_" + (++this.fabricatorPresetIndex);
            FabricatorPresetStore.save(name, FabricatorPresetCodec.captureCurrent());
            PacketUtilsManager.INSTANCE.moduleFeedback("Saved fabricator preset " + name);
            return true;
        }
        if (this.rect(mouseX, mouseY, rx + rw / 2 + 2, fy, rw / 2 - 2, 18)) {
            var names = FabricatorPresetStore.names();
            if (names.isEmpty()) {
                PacketUtilsManager.INSTANCE.moduleFeedback("No fabricator presets saved");
            } else {
                String name = names.get(Math.floorMod(this.fabricatorPresetIndex, names.size()));
                String json = FabricatorPresetStore.load(name);
                FabricatorPresetCodec.apply(json);
                PacketUtilsManager.INSTANCE.moduleFeedback("Loaded preset " + name);
            }
            return true;
        }
        int sniffHeight = this.snifferSectionHeight(this.snifferCollapsed);
        if (this.rect(mouseX, mouseY, tx + sw - 14, (ty += fabHeight + 5) + 3, 10, 10)) {
            this.snifferCollapsed = !this.snifferCollapsed;
            return true;
        }
        if (!this.snifferCollapsed) {
            PacketSnifferManager sniffer = PacketSnifferManager.INSTANCE;
            PacketSnifferSettings sniff = sniffer.getSettings();
            int sy = ty + 38 + 10 + 6;
            if (this.rect(mouseX, mouseY, rx, sy, rw, 30)) {
                sniffer.setEnabled(!sniff.enabled);
                return true;
            }
            if (this.rect(mouseX, mouseY, rx, sy += 30, rw, 30)) {
                PacketSnifferOverlay.INSTANCE.setOverlayVisible(!sniff.overlayVisible);
                sniffer.feedback("Sniffer overlay " + (sniff.overlayVisible ? "shown" : "hidden"));
                return true;
            }
            if (this.clickBindValue(mouseX, mouseY, rx, sy += 30, rw)) {
                this.captureMode = CaptureMode.PACKET_SNIFFER_OVERLAY_TOGGLE;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.sliderMode = SliderMode.NONE;
        }
        super.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (super.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        if (this.collapsed || button != 0 || this.sliderMode == SliderMode.NONE) {
            return false;
        }
        PacketUtilsSettings s = PacketUtilsManager.INSTANCE.getSettings();
        int sx = this.x + 12;
        int sw = this.width - 24;
        int barX = sx + 70;
        int barW = sw - 76;
        if (this.sliderMode == SliderMode.UIUTILS_RELEASE) {
            s.uiUtilsDelayReleaseMs = this.sliderValue(mouseX, barX, barW, 0.0, 2000.0);
            this.save();
            return true;
        }
        if (this.sliderMode == SliderMode.SNIFFER_REPLAY_DELAY) {
            PacketSnifferManager.INSTANCE.getSettings().replayDelayMs = (int)this.sliderValue(mouseX, barX, barW, 0.0, 1000.0);
            PacketSnifferManager.INSTANCE.save();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PacketUtilsSettings s = PacketUtilsManager.INSTANCE.getSettings();
        if (this.sniffSearchFocused) {
            if (keyCode == 256) {
                this.sniffSearchFocused = false;
                return true;
            }
            if (keyCode == 259) {
                if (!this.sniffSearch.isEmpty()) {
                    this.sniffSearch = this.sniffSearch.substring(0, this.sniffSearch.length() - 1);
                    this.markSniffListsDirty();
                }
                return true;
            }
            return false;
        }
        if (this.captureMode != CaptureMode.NONE) {
            int key = keyCode == 256 ? -1 : keyCode;
            CaptureMode finished = this.captureMode;
            this.captureMode = CaptureMode.NONE;
            switch (finished) {
                case PACKET_SNIFFER_OVERLAY_TOGGLE ->
                        PacketSnifferManager.INSTANCE.getSettings().overlayToggleKey = key;
                case PACKET_DELAY_TOGGLE -> s.packetDelayToggleKey = key;
                case PACKET_DELAY_OVERLAY -> s.packetDelayOverlayToggleKey = key;
                case UIUTILS_OVERLAY -> s.uiUtilsOverlayToggleKey = key;
                case UIUTILS_CLOSE -> s.uiUtilsCloseWithoutPacketKey = key;
                case UIUTILS_DELAY_TOGGLE -> s.uiUtilsDelayToggleKey = key;
                case UIUTILS_SEND_TOGGLE -> s.uiUtilsSendPacketsToggleKey = key;
                case UIUTILS_SEND_QUEUED -> s.uiUtilsSendQueuedKey = key;
                case FABRICATOR_TOGGLE -> s.fabricatorToggleKey = key;
                case SLOT_IDS_TOGGLE -> s.slotIdsToggleKey = key;
                default -> {
                }
            }
            if (finished != CaptureMode.PACKET_SNIFFER_OVERLAY_TOGGLE) {
                this.save();
            } else {
                PacketSnifferManager.INSTANCE.save();
            }
            String msg = finished.captureLabel() + " hotkey \u2192 " + this.keyName(key);
            if (finished == CaptureMode.PACKET_SNIFFER_OVERLAY_TOGGLE) {
                PacketSnifferManager.INSTANCE.feedback(msg);
            } else {
                PacketUtilsManager.INSTANCE.moduleFeedback(msg);
            }
            return true;
        }
        if (keyCode == 256) {
            this.sliderMode = SliderMode.NONE;
        }
        return false;
    }

    @Override
    public boolean charTyped(int codePoint, int modifiers) {
        if (this.sniffSearchFocused) {
            if (codePoint >= 32 && codePoint < 127 && this.sniffSearch.length() < 96) {
                this.sniffSearch = this.sniffSearch + (char)codePoint;
                this.markSniffListsDirty();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onModuleHidden() {
        this.captureMode = CaptureMode.NONE;
        this.sliderMode = SliderMode.NONE;
        this.sniffSearchFocused = false;
        PacketUtilsManager.INSTANCE.setTextInputFocused(false);
    }

    @Override
    public boolean hasFocusedTextInput() {
        if (!this.isVisible()) {
            return false;
        }
        return this.captureMode != CaptureMode.NONE || this.sniffSearchFocused;
    }

    private void drawSectionBox(DrawContext context, int x, int y, int w, int h, String title, boolean collapsed) {
        UiComponents.drawSectionCard(MinecraftClient.getInstance().textRenderer, context, x, y, w, h, title, collapsed);
    }

    private void drawToggleRow(DrawContext context, int x, int y, int rowW, String label, boolean enabled, String smoothKey, float delta) {
        UiComponents.drawOptionToggle(MinecraftClient.getInstance().textRenderer, context, x, y, rowW, label, enabled, this.smoothToggle(smoothKey, enabled, delta));
    }

    private void drawSlider(DrawContext context, int x, int y, int w, double value, double min, double max, String label, SliderMode mode) {
        double t = (value - min) / (max - min);
        UiComponents.drawValueSlider(MinecraftClient.getInstance().textRenderer, context, x, y, w, t, label, this.format1(value), this.sliderMode == mode);
    }

    private void drawBindRow(DrawContext context, int x, int y, int w, String label, int keyCode, CaptureMode mode) {
        boolean listening = this.captureMode == mode;
        String text = listening ? "Press key..." : this.keyName(keyCode);
        UiComponents.drawPillKeybind(MinecraftClient.getInstance().textRenderer, context, x, y, w, 20, label, text, listening);
    }

    private void drawValueRow(DrawContext context, int x, int y, int w, String label, String value, String smoothKey, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(tr, Text.literal(label), x, y + 6, -6184534);
        int valueW = 72;
        int valueX = x + w - valueW;
        UiComponents.drawPillActionButton(tr, context, valueX, y, valueW, 20, value, UiComponents.PillActionStyle.SECONDARY_SLATE);
    }

    private boolean clickBindValue(double mouseX, double mouseY, int x, int y, int w) {
        int bindW = 98;
        int labelW = w - bindW - 8;
        int bx = x + labelW + 8;
        return this.rect(mouseX, mouseY, bx, y, bindW, 20);
    }

    private boolean clickSlider(double mouseX, double mouseY, int x, int y, int w, double value, double min, double max, SliderMode mode) {
        int barX = x + 76;
        int barW = w - 82 - 36;
        if (!this.rect(mouseX, mouseY, barX, y + 1, barW, 8)) {
            return false;
        }
        this.sliderMode = mode;
        PacketUtilsSettings s = PacketUtilsManager.INSTANCE.getSettings();
        if (mode == SliderMode.UIUTILS_RELEASE) {
            s.uiUtilsDelayReleaseMs = this.sliderValue(mouseX, barX, barW, min, max);
        }
        this.save();
        return true;
    }

    private boolean rect(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= (double)x && mouseX <= (double)(x + w) && mouseY >= (double)y && mouseY <= (double)(y + h);
    }

    private double sliderValue(double mouseX, int x, int w, double min, double max) {
        double t = (mouseX - (double)x) / (double)w;
        t = Math.max(0.0, Math.min(1.0, t));
        return min + (max - min) * t;
    }

    private void save() {
        PacketUtilsManager.INSTANCE.save();
    }

    private String keyName(int keyCode) {
        if (keyCode == -1) {
            return "UNBOUND";
        }
        String glfw = GLFW.glfwGetKeyName((int)keyCode, (int)0);
        if (glfw != null) {
            return glfw.toUpperCase(Locale.ROOT);
        }
        return switch (keyCode) {
            case 345 -> "RCTRL";
            case 341 -> "LCTRL";
            case 340 -> "LSHIFT";
            case 344 -> "RSHIFT";
            case 342 -> "LALT";
            case 346 -> "RALT";
            default -> "KEY_" + keyCode;
        };
    }

    private String format1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static enum CaptureMode {
        NONE(""),
        PACKET_SNIFFER_OVERLAY_TOGGLE("Packet sniffer overlay toggle"),
        PACKET_DELAY_TOGGLE("Packet delay toggle"),
        PACKET_DELAY_OVERLAY("Packet delay overlay toggle"),
        UIUTILS_OVERLAY("UI Utils overlay toggle"),
        UIUTILS_CLOSE("UI Utils close without packet"),
        UIUTILS_DELAY_TOGGLE("UI Utils delay toggle"),
        UIUTILS_SEND_TOGGLE("UI Utils send packets toggle"),
        UIUTILS_SEND_QUEUED("UI Utils flush queue"),
        FABRICATOR_TOGGLE("Fabricator overlay toggle"),
        SLOT_IDS_TOGGLE("Fabricator slot IDs toggle");

        private final String label;

        private CaptureMode(String label) {
            this.label = label;
        }

        String captureLabel() {
            return this.label;
        }
    }

    private static enum SliderMode {
        NONE,
        UIUTILS_RELEASE,
        SNIFFER_REPLAY_DELAY;

    }

    private static enum SnifferListMode {
        LOG_EXCLUDE,
        BLOCK;

    }

    private static enum DelaySide {
        C2S,
        S2C;

    }
}

