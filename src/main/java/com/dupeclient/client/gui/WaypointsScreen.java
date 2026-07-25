package com.dupeclient.client.gui;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.modern.theme.MidnightPalette;
import com.dupeclient.client.gui.modern.theme.MidnightShapes;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.module.cape.DupeClientPresenceConfigManager;
import com.dupeclient.client.module.cape.DupeClientPresenceSettings;
import com.dupeclient.client.gui.GuiContextMenu;
import com.dupeclient.client.module.macro.MacroBaritoneSupport;
import com.dupeclient.client.module.waypoint.DupeClientWaypoint;
import com.dupeclient.client.module.waypoint.DupeClientWaypointManager;
import com.dupeclient.client.module.waypoint.SharedDupeClientWaypoint;
import com.dupeclient.client.module.waypoint.WaypointShareAudience;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class WaypointsScreen extends Screen {
    private static final int CARD_H = 46;
    private static final int CARD_GAP = 6;
    private static final int ROW_STRIDE = CARD_H + CARD_GAP;
    private static final int PANEL_TOP = 24;

    private final Screen parent;
    private int scroll;
    private String status = "";
    private int panelX;
    private int panelW;
    private int innerX;
    private int innerW;
    private int listTop;
    private int listBottom;
    private final GuiContextMenu contextMenu = new GuiContextMenu();

    public WaypointsScreen(Screen parent) {
        super(Component.literal("Waypoints"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        panelW = Math.min(520, width - 40);
        panelX = (width - panelW) / 2;
        innerX = panelX + 16;
        innerW = panelW - 32;

        int y = PANEL_TOP + 52;
        addRenderableWidget(new StylishButtonWidget(innerX, y, 150, 20, Component.literal("Create here"), () -> {
            if (minecraft == null || minecraft.player == null) {
                status = "Join a world first.";
                return;
            }
            DupeClientWaypoint wp = DupeClientWaypoint.create(
                "Waypoint",
                (int) Math.floor(minecraft.player.getX()),
                (int) Math.floor(minecraft.player.getY()),
                (int) Math.floor(minecraft.player.getZ()),
                DupeClientWaypointManager.currentDimensionKey(minecraft),
                0xFF4ADE80,
                null,
                DupeClientWaypointManager.INSTANCE.defaultShareAudience()
            );
            minecraft.gui.setScreen(new WaypointEditScreen(this, wp, true));
        }));
        addRenderableWidget(new StylishButtonWidget(innerX + 156, y, (innerW - 156) / 2 - 3, 20,
            Component.literal("New default: " + DupeClientWaypointManager.INSTANCE.defaultShareAudience().label()), () -> {
            WaypointShareAudience next = DupeClientWaypointManager.INSTANCE.defaultShareAudience().next();
            DupeClientWaypointManager.INSTANCE.setDefaultShareAudience(next);
            status = "New waypoints default to " + next.label().toLowerCase();
            init();
        }));
        addRenderableWidget(new StylishButtonWidget(innerX + 156 + (innerW - 156) / 2 + 3, y, (innerW - 156) / 2 - 3, 20,
            Component.literal("Apply all: " + DupeClientWaypointManager.INSTANCE.defaultShareAudience().label()), () -> {
            WaypointShareAudience target = DupeClientWaypointManager.INSTANCE.defaultShareAudience();
            DupeClientWaypointManager.INSTANCE.applyShareAudienceToAll(target);
            status = "Applied " + target.label().toLowerCase() + " to all waypoints";
            init();
        }));

        y += 26;
        DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
        addRenderableWidget(toggleButton(innerX, y, innerW / 2 - 4, "Share waypoints", Boolean.TRUE.equals(s.shareWaypoints), () -> {
            s.shareWaypoints = !Boolean.TRUE.equals(s.shareWaypoints);
            DupeClientPresenceConfigManager.save(s);
            DupeClientWaypointManager.INSTANCE.markSyncDirty();
            init();
        }));
        addRenderableWidget(toggleButton(innerX + innerW / 2 + 4, y, innerW / 2 - 4, "Show in world", Boolean.TRUE.equals(s.showSharedWaypointsInWorld), () -> {
            s.showSharedWaypointsInWorld = !Boolean.TRUE.equals(s.showSharedWaypointsInWorld);
            DupeClientPresenceConfigManager.save(s);
            init();
        }));

        listTop = y + 34;
        listBottom = height - 56;
        List<Row> rows = buildRows();
        int visibleRows = Math.max(1, (listBottom - listTop + CARD_GAP) / ROW_STRIDE);
        scroll = Math.min(scroll, Math.max(0, rows.size() - visibleRows));

        for (int i = 0; i < visibleRows; i++) {
            int idx = scroll + i;
            if (idx >= rows.size()) {
                break;
            }
            Row row = rows.get(idx);
            int rowY = listTop + i * ROW_STRIDE;
            int editW = 52;
            int delW = 52;
            int gap = 6;
            int editX = innerX + innerW - editW - delW - gap;
            int delX = innerX + innerW - delW;
            if (row.editable()) {
                addRenderableWidget(new StylishButtonWidget(editX, rowY + (CARD_H - 20) / 2, editW, 20, Component.literal("Edit"), () -> {
                    if (row.local() != null) {
                        minecraft.gui.setScreen(new WaypointEditScreen(this, row.local(), false));
                    }
                }));
                addRenderableWidget(new StylishButtonWidget(delX, rowY + (CARD_H - 20) / 2, delW, 20, Component.literal("Delete"), () -> {
                    if (row.local() != null) {
                        DupeClientWaypointManager.INSTANCE.delete(row.local().id());
                        status = "Deleted " + row.local().name();
                        init();
                    }
                }));
            }
        }

        addRenderableWidget(new StylishButtonWidget(width / 2 - 100, height - 28, 200, 20, CommonComponents.GUI_BACK, () -> {
            if (minecraft != null) {
                minecraft.gui.setScreen(parent);
            }
        }));
    }

    private StylishButtonWidget toggleButton(int x, int y, int w, String label, boolean on, Runnable action) {
        String text = label + ": " + (on ? "ON" : "OFF");
        return new StylishButtonWidget(x, y, w, 20, Component.literal(text), action);
    }

    private List<Row> buildRows() {
        ArrayList<Row> rows = new ArrayList<>();
        for (DupeClientWaypoint wp : DupeClientWaypointManager.INSTANCE.localWaypoints()) {
            rows.add(new Row(wp, null, true));
        }
        for (SharedDupeClientWaypoint shared : DupeClientWaypointManager.INSTANCE.sharedWaypoints()) {
            if (shared.ownedBySelf()) {
                continue;
            }
            rows.add(new Row(null, shared, false));
        }
        return rows;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            int visibleRows = Math.max(1, (listBottom - listTop + CARD_GAP) / ROW_STRIDE);
            int maxScroll = Math.max(0, buildRows().size() - visibleRows);
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) verticalAmount));
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        if (contextMenu.isOpen()) {
            if (contextMenu.handleClick(click.x(), click.y(), click.button())) {
                return true;
            }
            if (click.button() == 0) {
                contextMenu.close();
                return true;
            }
        }
        if (click.button() == 1) {
            int idx = rowIndexAt(click.x(), click.y());
            if (idx >= 0) {
                openRowContextMenu(idx, (int) click.x(), (int) click.y());
                return true;
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    private int rowIndexAt(double mouseX, double mouseY) {
        if (mouseX < innerX || mouseX >= innerX + innerW || mouseY < listTop || mouseY >= listBottom) {
            return -1;
        }
        int row = (int) ((mouseY - listTop) / ROW_STRIDE);
        int idx = scroll + row;
        List<Row> rows = buildRows();
        return idx >= 0 && idx < rows.size() ? idx : -1;
    }

    private void openRowContextMenu(int idx, int anchorX, int anchorY) {
        List<Row> rows = buildRows();
        if (idx < 0 || idx >= rows.size()) {
            return;
        }
        Row row = rows.get(idx);
        DupeClientWaypoint wp = row.local() != null ? row.local() : row.shared().waypoint();
        ArrayList<GuiContextMenu.Entry> items = new ArrayList<>();
        items.add(new GuiContextMenu.Entry("Path here", () -> {
            if (minecraft != null) {
                boolean ok = MacroBaritoneSupport.startPathToBlock(minecraft, new BlockPos(wp.x(), wp.y(), wp.z()));
                status = ok ? "Pathing to " + wp.name() : "Baritone not available";
            }
        }));
        if (row.editable()) {
            items.add(new GuiContextMenu.Entry("Edit", () -> {
                if (minecraft != null && row.local() != null) {
                    minecraft.gui.setScreen(new WaypointEditScreen(this, row.local(), false));
                }
            }));
            items.add(new GuiContextMenu.Entry("Delete", () -> {
                if (row.local() != null) {
                    DupeClientWaypointManager.INSTANCE.delete(row.local().id());
                    status = "Deleted " + row.local().name();
                    init();
                }
            }));
        }
        contextMenu.open(anchorX, anchorY, width, height, listTop, items, font);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        UiDraw.fillMidnightBackground(context, width, height);
        int panelH = height - 48;
        UiDraw.cardElevated(context, panelX, PANEL_TOP, panelW, panelH, UiTokens.R_XL);
        context.centeredText(font, title, width / 2, PANEL_TOP + 10, MidnightPalette.TEXT_PRIMARY);
        context.text(font, Component.literal("Your waypoints sync when sharing is enabled."), innerX, PANEL_TOP + 28, MidnightPalette.TEXT_MUTED);
        context.text(font, Component.literal("Per-waypoint share: Everyone / Friends / Only me"), innerX, PANEL_TOP + 40, MidnightPalette.TEXT_MUTED);

        List<Row> rows = buildRows();
        int visibleRows = Math.max(1, (listBottom - listTop + CARD_GAP) / ROW_STRIDE);
        if (rows.isEmpty()) {
            context.text(font, Component.literal("No waypoints yet — create one in-world."), innerX, listTop + 8, MidnightPalette.TEXT_MUTED);
        } else {
            for (int i = 0; i < visibleRows; i++) {
                int idx = scroll + i;
                if (idx >= rows.size()) {
                    break;
                }
                Row row = rows.get(idx);
                int rowY = listTop + i * ROW_STRIDE;
                boolean hovered = mouseX >= innerX && mouseX < innerX + innerW && mouseY >= rowY && mouseY < rowY + CARD_H;
                drawWaypointCard(context, innerX, rowY, innerW, row, hovered);
            }
        }

        if (!status.isEmpty()) {
            context.centeredText(font, Component.literal(status), width / 2, height - 44, UiTokens.EMERALD_500);
        }
        contextMenu.render(context, font, mouseX, mouseY);
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
    }

    private void drawWaypointCard(GuiGraphicsExtractor context, int x, int y, int w, Row row, boolean hovered) {
        int rr = MidnightShapes.controlRadius(CARD_H);
        int fill = hovered ? MidnightPalette.PANEL_FILL_RAISED : MidnightPalette.PANEL_FILL;
        MidnightShapes.fillRoundedFrame(context, x, y, w, CARD_H, rr, fill, hovered ? MidnightPalette.BORDER_FOCUS : MidnightPalette.BORDER_LIGHT);

        int color = row.colorArgb();
        int swatch = 0xFF000000 | (color & 0x00FFFFFF);
        MidnightShapes.fillRoundedRect(context, x + UiTokens.SP_3, y + (CARD_H - 20) / 2, 6, 20, 3, swatch);

        int textX = x + UiTokens.SP_3 + 12;
        int btnReserve = row.editable() ? 116 : 0;
        int textMax = Math.max(48, w - (textX - x) - btnReserve - UiTokens.SP_3);
        String line1 = font.plainSubstrByWidth(row.title(), textMax);
        String line2 = font.plainSubstrByWidth(row.subtitle(), textMax);
        context.text(font, Component.literal(line1), textX, y + UiTokens.SP_2 + 2, MidnightPalette.TEXT_PRIMARY);
        context.text(font, Component.literal(line2), textX, y + UiTokens.SP_2 + 14, MidnightPalette.TEXT_SECONDARY);

        if (!row.editable()) {
            int tagW = 52;
            int tagH = 18;
            int tagX = x + w - tagW - UiTokens.SP_2;
            int tagY = y + (CARD_H - tagH) / 2;
            UiComponents.drawPillActionButton(font, context, tagX, tagY, tagW, tagH, "Shared", UiComponents.PillActionStyle.SECONDARY_SLATE);
        }
    }

    private record Row(DupeClientWaypoint local, SharedDupeClientWaypoint shared, boolean editable) {
        String title() {
            if (local != null) {
                return local.name() + "  (" + local.shareAudience().label() + ")";
            }
            DupeClientWaypoint wp = shared.waypoint();
            String owner = shared.ownerName() == null || shared.ownerName().isBlank() ? "Friend" : shared.ownerName();
            return wp.name() + "  — " + owner;
        }

        String subtitle() {
            DupeClientWaypoint wp = local != null ? local : shared.waypoint();
            return wp.x() + ", " + wp.y() + ", " + wp.z() + "  ·  " + wp.shape().label();
        }

        int colorArgb() {
            return local != null ? local.colorArgb() : shared.waypoint().colorArgb();
        }
    }
}
