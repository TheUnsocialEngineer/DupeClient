package com.dupeclient.client.module.packet.sniffer;
import com.dupeclient.client.module.packet.sniffer.PacketRecordCodec;
import com.dupeclient.client.module.packet.sniffer.PacketRecordCodec;

import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.overlay.IngameModuleOverlayScreen;
import com.dupeclient.client.module.packet.PacketUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Edit captured packets, resend/replay them, or fabricate new C2S packets from field tables.
 */
public final class PacketWorkbenchScreen extends Screen {
    private static final int ROW_H = 14;
    private static final int COL_FIELD_W = 108;
    private static final int COL_TYPE_W = 72;
    private static final int FAB_SEARCH_H = 14;
    private static final int FAB_LIST_ROWS = 10;
    private static final int FAB_ROW_H = 11;
    private static final int STATS_H = 78;
    private static final int STATS_ROWS = 5;
    private static final int STATS_ROW_H = 11;
    private static final int BOTTOM_H = 78;

    private static List<String> sortedC2sNames;

    private final @Nullable Screen parent;
    private final @Nullable PacketSnifferEntry sourceEntry;
    private final List<PacketFieldModel> fields = new ArrayList<>();
    private final StringBuilder repeatInput = new StringBuilder("1");
    private final StringBuilder status = new StringBuilder();
    private final StringBuilder valueEditor = new StringBuilder();
    private final StringBuilder fabricateSearch = new StringBuilder();

    private Mode mode = Mode.EDIT_CAPTURED;
    private int focusedField = -1;
    private int fieldScroll;
    private boolean fabricateSearchFocused;
    private int fabricateListScroll;
    private String selectedFabricateType = "";
    private int hitTableX;
    private int hitTableY;
    private int hitTableW;
    private int hitTableH;
    private int hitRepeatX;
    private int hitRepeatY;
    private int hitRepeatW;
    private int hitFabSearchX;
    private int hitFabSearchY;
    private int hitFabSearchW;
    private int hitFabListX;
    private int hitFabListY;
    private int hitFabListW;
    private int hitFabListH;

    public PacketWorkbenchScreen(@Nullable Screen parent, @Nullable PacketSnifferEntry entry) {
        super(Text.literal(entry == null ? "Packet Fabrication" : "Packet Editor"));
        this.parent = parent;
        this.sourceEntry = entry;
        if (entry != null) {
            loadFields(PacketRecordCodec.fieldsFromEditable(entry.editableText));
            mode = Mode.EDIT_CAPTURED;
        } else {
            mode = Mode.FABRICATE;
        }
    }

    public static void openCaptured(@Nullable Screen parent, PacketSnifferEntry entry) {
        if (entry == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new PacketWorkbenchScreen(normalizeParent(parent), entry));
        }
    }

    public static void openFabricationFromEntry(@Nullable Screen parent, PacketSnifferEntry entry) {
        if (entry == null || !entry.canFabricate()) {
            PacketSnifferManager.INSTANCE.feedback("Only C2S packets can be fabricated");
            return;
        }
        openCaptured(parent, entry);
    }

    public static void openFabrication(@Nullable Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new PacketWorkbenchScreen(normalizeParent(parent), null));
        }
    }

    @Nullable
    private static Screen normalizeParent(@Nullable Screen parent) {
        if (parent instanceof IngameModuleOverlayScreen) {
            return null;
        }
        return parent;
    }

    @Override
    protected void init() {
        ensureC2sNames();
        if (mode == Mode.FABRICATE && selectedFabricateType.isEmpty()) {
            List<String> filtered = filteredFabricateTypes();
            if (!filtered.isEmpty()) {
                selectFabricateType(filtered.get(0), false);
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, 0xC0101018);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        TextRenderer tr = textRenderer;
        int pad = 12;
        int panelW = Math.min(560, width - pad * 2);
        int panelH = height - pad * 2;
        int px = (width - panelW) / 2;
        int py = pad;

        context.fill(px, py, px + panelW, py + panelH, 0xF018181B);
        String title = mode == Mode.FABRICATE ? "Packet Fabrication" : "Packet Editor";
        context.drawTextWithShadow(tr, Text.literal(title), px + pad, py + 8, UiTokens.TEXT);
        PacketSnifferManager sniffer = PacketSnifferManager.INSTANCE;
        String totals = "C2S " + sniffer.c2sCount() + " · S2C " + sniffer.s2cCount();
        context.drawTextWithShadow(
                tr,
                Text.literal(totals),
                px + panelW - pad - tr.getWidth(totals),
                py + 8,
                UiTokens.TEXT_DIM);

        int y = py + 28;
        if (mode == Mode.FABRICATE) {
            y = drawFabricatePicker(context, tr, px, y, panelW, pad, mouseX, mouseY);
        } else if (sourceEntry != null) {
            context.drawTextWithShadow(
                    tr,
                    Text.literal(sourceEntry.direction.label + " · " + sourceEntry.name),
                    px + pad,
                    y,
                    UiTokens.TEXT_DIM);
            y += 14;
        }

        hitTableX = px + pad;
        hitTableY = y;
        hitTableW = panelW - pad * 2;
        hitTableH = panelH - (y - py) - STATS_H - BOTTOM_H;

        context.fill(hitTableX, hitTableY, hitTableX + hitTableW, hitTableY + hitTableH, 0xFF0B0F17);
        drawFieldTable(context, tr, mouseX, mouseY);

        y = hitTableY + hitTableH + 6;
        drawPacketStats(context, tr, hitTableX, y, hitTableW);
        y += STATS_H + 2;
        context.drawTextWithShadow(tr, Text.literal("Repeat"), px + pad, y + 4, UiTokens.TEXT_DIM);
        hitRepeatX = px + pad + 48;
        hitRepeatY = y;
        hitRepeatW = 48;
        context.fill(hitRepeatX, hitRepeatY, hitRepeatX + hitRepeatW, hitRepeatY + 16, repeatFocused() ? 0xFF374151 : 0xFF1F2937);
        context.drawTextWithShadow(tr, Text.literal(repeatInput.toString()), hitRepeatX + 4, hitRepeatY + 4, 0xFFE5E7EB);

        int btnY = y;
        int btnW = 88;
        int btnGap = 6;
        int bx = px + pad + 110;
        boolean canSend = canSendPackets();
        drawBtn(context, tr, bx, btnY, btnW, "Resend", canSend ? 0xFF166534 : 0xFF1F2937);
        bx += btnW + btnGap;
        drawBtn(context, tr, bx, btnY, btnW, "Send Edit", canSend ? 0xFF1D4ED8 : 0xFF1F2937);
        bx += btnW + btnGap;
        drawBtn(context, tr, bx, btnY, btnW, "Queue", canSend ? 0xFF7C3AED : 0xFF1F2937);
        bx += btnW + btnGap;
        drawBtn(context, tr, bx, btnY, 64, "Close", 0xFF374151);

        if (!status.isEmpty()) {
            context.drawTextWithShadow(tr, Text.literal(status.toString()), px + pad, py + panelH - 16, 0xFF9CA3AF);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPacketStats(DrawContext context, TextRenderer tr, int x, int y, int w) {
        PacketSnifferManager sniffer = PacketSnifferManager.INSTANCE;
        int gap = 8;
        int colW = (w - gap) / 2;
        int c2sX = x;
        int s2cX = x + colW + gap;

        context.fill(x, y, x + w, y + STATS_H, 0xFF0F172A);
        context.fill(x, y, x + w, y + 1, 0xFF374151);

        String c2sHeader = "Client → Server (" + sniffer.c2sCount() + ")";
        String s2cHeader = "Server → Client (" + sniffer.s2cCount() + ")";
        context.drawTextWithShadow(tr, Text.literal(c2sHeader), c2sX + 4, y + 3, 0xFF86EFAC);
        context.drawTextWithShadow(tr, Text.literal(s2cHeader), s2cX + 4, y + 3, 0xFF93C5FD);
        context.fill(c2sX + 2, y + 14, c2sX + colW - 2, y + 15, 0xFF374151);
        context.fill(s2cX + 2, y + 14, s2cX + colW - 2, y + 15, 0xFF374151);

        List<java.util.Map.Entry<String, Integer>> c2sTop = sniffer.topTypeCounts(PacketDirection.C2S, STATS_ROWS);
        List<java.util.Map.Entry<String, Integer>> s2cTop = sniffer.topTypeCounts(PacketDirection.S2C, STATS_ROWS);
        for (int row = 0; row < STATS_ROWS; row++) {
            int rowY = y + 16 + row * STATS_ROW_H;
            drawStatsRow(context, tr, c2sX + 4, rowY, colW - 8, PacketDirection.C2S,
                    row < c2sTop.size() ? c2sTop.get(row) : null, 0xFF86EFAC);
            drawStatsRow(context, tr, s2cX + 4, rowY, colW - 8, PacketDirection.S2C,
                    row < s2cTop.size() ? s2cTop.get(row) : null, 0xFF93C5FD);
        }
    }

    private void drawStatsRow(
            DrawContext context,
            TextRenderer tr,
            int x,
            int y,
            int maxW,
            PacketDirection direction,
            @Nullable java.util.Map.Entry<String, Integer> entry,
            int color) {
        if (entry == null) {
            return;
        }
        int total = statsTotal(direction);
        String pct = total > 0 ? String.format(" (%.1f%%)", entry.getValue() * 100.0 / total) : "";
        String line = entry.getKey() + "  " + entry.getValue() + pct;
        context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(line, maxW)), x, y, color);
    }

    private int statsTotal(PacketDirection direction) {
        return direction == PacketDirection.C2S
                ? PacketSnifferManager.INSTANCE.c2sCount()
                : PacketSnifferManager.INSTANCE.s2cCount();
    }

    private String formatTypeCountLabel(String typeName, PacketDirection direction) {
        int count = PacketSnifferManager.INSTANCE.typeCount(direction, typeName);
        if (count <= 0) {
            return typeName;
        }
        int total = statsTotal(direction);
        String pct = total > 0 ? String.format(" (%.1f%%)", count * 100.0 / total) : "";
        return typeName + "  " + count + pct;
    }

    private int drawFabricatePicker(
            DrawContext context,
            TextRenderer tr,
            int px,
            int y,
            int panelW,
            int pad,
            int mouseX,
            int mouseY) {
        List<String> filtered = filteredFabricateTypes();
        int innerW = panelW - pad * 2;

        hitFabSearchX = px + pad;
        hitFabSearchY = y;
        hitFabSearchW = innerW;
        int searchBg = fabricateSearchFocused ? 0xFF142A24 : 0xFF111827;
        int searchBorder = fabricateSearchFocused ? 0xFF34D399 : 0xFF374151;
        context.fill(hitFabSearchX, hitFabSearchY, hitFabSearchX + hitFabSearchW, hitFabSearchY + FAB_SEARCH_H, searchBg);
        context.fill(hitFabSearchX, hitFabSearchY, hitFabSearchX + hitFabSearchW, hitFabSearchY + 1, searchBorder);
        String searchShown = fabricateSearch.isEmpty() && !fabricateSearchFocused
                ? "Search C2S packets (+inc -exc)…"
                : fabricateSearch.toString();
        context.drawTextWithShadow(
                tr,
                Text.literal(tr.trimToWidth(searchShown, hitFabSearchW - 6)),
                hitFabSearchX + 4,
                hitFabSearchY + 3,
                fabricateSearch.isEmpty() && !fabricateSearchFocused ? UiTokens.TEXT_DIM : 0xFFE5E7EB);
        String count = filtered.size() + "/" + sortedC2sNames().size();
        context.drawTextWithShadow(tr, Text.literal(count), hitFabSearchX + hitFabSearchW - tr.getWidth(count) - 4, hitFabSearchY + 3, UiTokens.TEXT_DIM);
        y += FAB_SEARCH_H + 4;

        hitFabListX = px + pad;
        hitFabListY = y;
        hitFabListW = innerW;
        hitFabListH = FAB_LIST_ROWS * FAB_ROW_H;
        context.fill(hitFabListX, hitFabListY, hitFabListX + hitFabListW, hitFabListY + hitFabListH, 0xFF0F172A);

        int maxScroll = Math.max(0, filtered.size() - FAB_LIST_ROWS);
        fabricateListScroll = Math.max(0, Math.min(maxScroll, fabricateListScroll));

        for (int row = 0; row < FAB_LIST_ROWS; row++) {
            int idx = fabricateListScroll + row;
            if (idx >= filtered.size()) {
                break;
            }
            String typeName = filtered.get(idx);
            int ry = hitFabListY + row * FAB_ROW_H;
            boolean selected = typeName.equals(selectedFabricateType);
            boolean hot = mouseX >= hitFabListX && mouseX < hitFabListX + hitFabListW
                    && mouseY >= ry && mouseY < ry + FAB_ROW_H;
            if (selected) {
                context.fill(hitFabListX + 1, ry, hitFabListX + hitFabListW - 1, ry + FAB_ROW_H, 0x55374151);
            } else if (hot) {
                context.fill(hitFabListX + 1, ry, hitFabListX + hitFabListW - 1, ry + FAB_ROW_H, 0x33222C3A);
            }
            String label = formatTypeCountLabel(typeName, PacketDirection.C2S);
            context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(label, hitFabListW - 8)), hitFabListX + 4, ry + 2, selected ? 0xFF86EFAC : 0xFFE5E7EB);
        }

        if (maxScroll > 0) {
            int trackX = hitFabListX + hitFabListW - 5;
            int thumbH = Math.max(10, hitFabListH * FAB_LIST_ROWS / Math.max(1, filtered.size()));
            int travel = hitFabListH - thumbH;
            int thumbY = hitFabListY + (fabricateListScroll * travel) / maxScroll;
            context.fill(trackX, hitFabListY, trackX + 4, hitFabListY + hitFabListH, 0xFF1F2937);
            context.fill(trackX + 1, thumbY, trackX + 3, thumbY + thumbH, 0xFF6B7280);
        }

        return y + hitFabListH + 6;
    }

    private void drawFieldTable(DrawContext context, TextRenderer tr, int mouseX, int mouseY) {
        int headerY = hitTableY + 4;
        context.drawTextWithShadow(tr, Text.literal("Field"), hitTableX + 4, headerY, 0xFF9CA3AF);
        context.drawTextWithShadow(tr, Text.literal("Type"), hitTableX + COL_FIELD_W + 4, headerY, 0xFF9CA3AF);
        context.drawTextWithShadow(tr, Text.literal("Value"), hitTableX + COL_FIELD_W + COL_TYPE_W + 4, headerY, 0xFF9CA3AF);
        context.fill(hitTableX + 2, hitTableY + 16, hitTableX + hitTableW - 2, hitTableY + 17, 0xFF374151);

        int bodyTop = hitTableY + 18;
        int bodyH = hitTableH - 20;
        int visibleRows = Math.max(1, bodyH / ROW_H);
        int maxScroll = Math.max(0, fields.size() - visibleRows);
        fieldScroll = Math.max(0, Math.min(maxScroll, fieldScroll));

        int valueX = hitTableX + COL_FIELD_W + COL_TYPE_W + 4;
        int valueW = hitTableW - COL_FIELD_W - COL_TYPE_W - 8;

        for (int row = 0; row < visibleRows; row++) {
            int idx = fieldScroll + row;
            if (idx >= fields.size()) {
                break;
            }
            PacketFieldModel field = fields.get(idx);
            int ry = bodyTop + row * ROW_H;
            boolean selected = idx == focusedField;
            if (selected) {
                context.fill(hitTableX + 2, ry, hitTableX + hitTableW - 2, ry + ROW_H, 0x55374151);
            }

            int nameColor = field.editable ? 0xFFE5E7EB : 0xFF9CA3AF;
            context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(field.name, COL_FIELD_W - 8)), hitTableX + 4, ry + 3, nameColor);
            context.drawTextWithShadow(
                    tr,
                    Text.literal(tr.trimToWidth(field.typeName, COL_TYPE_W - 8)),
                    hitTableX + COL_FIELD_W + 4,
                    ry + 3,
                    0xFF6B7280);

            String shown = selected && field.editable ? valueEditor.toString() : field.value;
            if (shown.isEmpty() && selected && field.editable) {
                shown = "_";
            }
            int valueColor = field.editable ? 0xFF86EFAC : 0xFF9CA3AF;
            context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(shown, valueW - 4)), valueX, ry + 3, valueColor);

            if (field.editable && field.valueType != null && field.valueType.isEnum()) {
                context.drawTextWithShadow(tr, Text.literal("▸"), hitTableX + hitTableW - 12, ry + 3, 0xFF60A5FA);
            }
        }
    }

    private void drawBtn(DrawContext context, TextRenderer tr, int x, int y, int w, String label, int color) {
        context.fill(x, y, x + w, y + 16, color);
        int tw = tr.getWidth(label);
        context.drawTextWithShadow(tr, Text.literal(label), x + (w - tw) / 2, y + 4, 0xFFE5E7EB);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(click, doubleClick);
        }

        if (mode == Mode.FABRICATE) {
            if (inRect(mouseX, mouseY, hitFabSearchX, hitFabSearchY, hitFabSearchW, FAB_SEARCH_H)) {
                fabricateSearchFocused = true;
                focusedField = -1;
                setRepeatFocused(false);
                return true;
            }
            fabricateSearchFocused = false;
            if (inRect(mouseX, mouseY, hitFabListX, hitFabListY, hitFabListW, hitFabListH)) {
                List<String> filtered = filteredFabricateTypes();
                int row = (int) ((mouseY - hitFabListY) / FAB_ROW_H);
                if (row >= 0 && row < FAB_LIST_ROWS) {
                    int idx = fabricateListScroll + row;
                    if (idx >= 0 && idx < filtered.size()) {
                        selectFabricateType(filtered.get(idx), true);
                    }
                }
                return true;
            }
        }

        if (clickFieldRow(mouseX, mouseY)) {
            fabricateSearchFocused = false;
            return true;
        }
        if (inRect(mouseX, mouseY, hitRepeatX, hitRepeatY, hitRepeatW, 16)) {
            commitFocusedField();
            focusedField = -1;
            fabricateSearchFocused = false;
            setRepeatFocused(true);
            return true;
        }
        commitFocusedField();
        focusedField = -1;
        setRepeatFocused(false);

        int pad = 12;
        int panelW = Math.min(560, width - pad * 2);
        int px = (width - panelW) / 2;
        int btnY = hitTableY + hitTableH + 8;
        int btnW = 88;
        int btnGap = 6;
        int bx = px + pad + 110;
        if (canSendPackets() && inRect(mouseX, mouseY, bx, btnY, btnW, 16)) {
            resendOriginal();
            return true;
        }
        bx += btnW + btnGap;
        if (canSendPackets() && inRect(mouseX, mouseY, bx, btnY, btnW, 16)) {
            sendEdited(false);
            return true;
        }
        bx += btnW + btnGap;
        if (canSendPackets() && inRect(mouseX, mouseY, bx, btnY, btnW, 16)) {
            sendEdited(true);
            return true;
        }
        bx += btnW + btnGap;
        if (inRect(mouseX, mouseY, bx, btnY, 64, 16)) {
            closeScreen();
            return true;
        }
        return super.mouseClicked(click, doubleClick);
    }

    private boolean clickFieldRow(double mouseX, double mouseY) {
        if (!inRect(mouseX, mouseY, hitTableX, hitTableY, hitTableW, hitTableH)) {
            return false;
        }
        int bodyTop = hitTableY + 18;
        int bodyH = hitTableH - 20;
        int visibleRows = Math.max(1, bodyH / ROW_H);
        int row = (int) ((mouseY - bodyTop) / ROW_H);
        if (row < 0 || row >= visibleRows) {
            return true;
        }
        int idx = fieldScroll + row;
        if (idx < 0 || idx >= fields.size()) {
            return true;
        }
        PacketFieldModel field = fields.get(idx);
        commitFocusedField();
        focusedField = idx;
        setRepeatFocused(false);
        if (field.editable) {
            valueEditor.setLength(0);
            valueEditor.append(field.value);
            if (field.valueType != null && field.valueType.isEnum()) {
                String cycled = PacketRecordCodec.cycleEnumValue(field.valueType, field.value);
                fields.set(idx, field.withValue(cycled));
                valueEditor.setLength(0);
                valueEditor.append(cycled);
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mode == Mode.FABRICATE && inRect(mouseX, mouseY, hitFabListX, hitFabListY, hitFabListW, hitFabListH)) {
            List<String> filtered = filteredFabricateTypes();
            int maxScroll = Math.max(0, filtered.size() - FAB_LIST_ROWS);
            int lines = Math.max(1, (int) Math.round(Math.abs(vertical) * 2.0));
            int delta = vertical > 0 ? -lines : vertical < 0 ? lines : 0;
            fabricateListScroll = Math.max(0, Math.min(maxScroll, fabricateListScroll - delta));
            return true;
        }
        if (inRect(mouseX, mouseY, hitTableX, hitTableY, hitTableW, hitTableH)) {
            int bodyH = hitTableH - 20;
            int visibleRows = Math.max(1, bodyH / ROW_H);
            int maxScroll = Math.max(0, fields.size() - visibleRows);
            int lines = Math.max(1, (int) Math.round(Math.abs(vertical) * 2.0));
            int delta = vertical > 0 ? -lines : vertical < 0 ? lines : 0;
            fieldScroll = Math.max(0, Math.min(maxScroll, fieldScroll - delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();

        if (fabricateSearchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                fabricateSearchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !fabricateSearch.isEmpty()) {
                fabricateSearch.setLength(fabricateSearch.length() - 1);
                fabricateListScroll = 0;
                ensureFabricateSelection();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                moveFabricateSelection(1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                moveFabricateSelection(-1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                List<String> filtered = filteredFabricateTypes();
                if (!filtered.isEmpty()) {
                    selectFabricateType(filtered.get(Math.min(fabricateListScroll, filtered.size() - 1)), true);
                }
                return true;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (focusedField >= 0) {
                focusedField = -1;
                valueEditor.setLength(0);
                return true;
            }
            closeScreen();
            return true;
        }
        if (repeatFocused()) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !repeatInput.isEmpty()) {
                repeatInput.setLength(repeatInput.length() - 1);
                return true;
            }
            return true;
        }
        if (focusedField < 0 || focusedField >= fields.size()) {
            return super.keyPressed(input);
        }
        PacketFieldModel field = fields.get(focusedField);
        if (!field.editable) {
            return super.keyPressed(input);
        }
        if (field.valueType != null && field.valueType.isEnum()) {
            if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_SPACE) {
                String cycled = PacketRecordCodec.cycleEnumValue(field.valueType, valueEditor.toString());
                fields.set(focusedField, field.withValue(cycled));
                valueEditor.setLength(0);
                valueEditor.append(cycled);
                return true;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!valueEditor.isEmpty()) {
                valueEditor.setLength(valueEditor.length() - 1);
                fields.set(focusedField, field.withValue(valueEditor.toString()));
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            commitFocusedField();
            int next = focusedField + 1;
            while (next < fields.size() && !fields.get(next).editable) {
                next++;
            }
            if (next < fields.size()) {
                focusedField = next;
                valueEditor.setLength(0);
                valueEditor.append(fields.get(next).value);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_V && (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null) {
                valueEditor.append(clip.replace("\r", "").replace("\n", " "));
                fields.set(focusedField, field.withValue(valueEditor.toString()));
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        if (fabricateSearchFocused) {
            if (!Character.isISOControl(chr) && fabricateSearch.length() < 96) {
                fabricateSearch.append(chr);
                fabricateListScroll = 0;
                ensureFabricateSelection();
            }
            return true;
        }
        if (repeatFocused()) {
            if (Character.isDigit(chr) && repeatInput.length() < 4) {
                repeatInput.append(chr);
            }
            return true;
        }
        if (focusedField < 0 || focusedField >= fields.size()) {
            return super.charTyped(input);
        }
        PacketFieldModel field = fields.get(focusedField);
        if (!field.editable || field.valueType != null && field.valueType.isEnum()) {
            return true;
        }
        if (!Character.isISOControl(chr)) {
            valueEditor.append(chr);
            fields.set(focusedField, field.withValue(valueEditor.toString()));
            return true;
        }
        return super.charTyped(input);
    }

    private void commitFocusedField() {
        if (focusedField < 0 || focusedField >= fields.size()) {
            return;
        }
        PacketFieldModel field = fields.get(focusedField);
        if (field.editable) {
            fields.set(focusedField, field.withValue(valueEditor.toString()));
        }
        valueEditor.setLength(0);
    }

    private void loadFields(List<PacketFieldModel> rows) {
        fields.clear();
        fields.addAll(rows);
        focusedField = -1;
        fieldScroll = 0;
        valueEditor.setLength(0);
    }

    private List<String> filteredFabricateTypes() {
        return PacketNameSearch.filter(sortedC2sNames(), fabricateSearch.toString());
    }

    private void selectFabricateType(String type, boolean feedback) {
        selectedFabricateType = type;
        loadFields(PacketRecordCodec.describeType(type));
        if (feedback) {
            status.setLength(0);
            status.append("Loaded fields for ").append(type);
        }
    }

    private void ensureFabricateSelection() {
        List<String> filtered = filteredFabricateTypes();
        if (filtered.isEmpty()) {
            selectedFabricateType = "";
            fields.clear();
            return;
        }
        if (!filtered.contains(selectedFabricateType)) {
            selectFabricateType(filtered.get(0), false);
        }
    }

    private void moveFabricateSelection(int delta) {
        List<String> filtered = filteredFabricateTypes();
        if (filtered.isEmpty()) {
            return;
        }
        int idx = filtered.indexOf(selectedFabricateType);
        if (idx < 0) {
            idx = 0;
        } else {
            idx = Math.max(0, Math.min(filtered.size() - 1, idx + delta));
        }
        selectFabricateType(filtered.get(idx), false);
        int maxScroll = Math.max(0, filtered.size() - FAB_LIST_ROWS);
        if (idx < fabricateListScroll) {
            fabricateListScroll = idx;
        } else if (idx >= fabricateListScroll + FAB_LIST_ROWS) {
            fabricateListScroll = Math.min(maxScroll, idx - FAB_LIST_ROWS + 1);
        }
    }

    private boolean repeatFocused;
    private boolean repeatFocused() {
        return repeatFocused;
    }

    private void setRepeatFocused(boolean focused) {
        repeatFocused = focused;
    }

    private boolean canSendPackets() {
        return mode == Mode.FABRICATE || sourceEntry == null || sourceEntry.canSend();
    }

    private void resendOriginal() {
        if (sourceEntry == null || sourceEntry.packet == null) {
            status.setLength(0);
            status.append("No captured packet to resend");
            return;
        }
        if (PacketReplayer.replay(sourceEntry)) {
            status.setLength(0);
            status.append("Resent original packet");
        }
    }

    private void sendEdited(boolean queue) {
        commitFocusedField();
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            Packet<?> built = PacketRecordCodec.fromEditable(PacketRecordCodec.buildEditable(fields));
            if (built instanceof ClickSlotC2SPacket click) {
                built = PacketRecordCodec.refreshClickSlot(click, client);
            }
            int times = parseRepeat();
            if (queue) {
                PacketReplayScheduler.INSTANCE.queuePacket(PacketDirection.C2S, built, times);
            } else {
                for (int i = 0; i < times; i++) {
                    PacketReplayer.sendC2s(client, built);
                }
            }
            status.setLength(0);
            status.append(queue ? "Queued " + times + " packet(s)" : "Sent " + times + " packet(s)");
        } catch (PacketRecordCodec.PacketBuildException e) {
            status.setLength(0);
            status.append("Build error: ").append(e.getMessage());
        }
    }

    private int parseRepeat() {
        try {
            return Math.max(1, Math.min(500, Integer.parseInt(repeatInput.toString().trim())));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void closeScreen() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private static void ensureC2sNames() {
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
    }

    private List<String> sortedC2sNames() {
        ensureC2sNames();
        return sortedC2sNames;
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private enum Mode {
        EDIT_CAPTURED,
        FABRICATE
    }
}
