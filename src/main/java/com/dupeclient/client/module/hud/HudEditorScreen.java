package com.dupeclient.client.module.hud;

import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.module.hud.HudElementDefinition;
import com.dupeclient.client.module.hud.HudElementState;
import com.dupeclient.client.module.hud.HudManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class HudEditorScreen
extends Screen {
    private static final int MENU_ROW = 14;
    private final Screen parent;
    private final List<ContextMenuEntry> contextMenuEntries = new ArrayList<ContextMenuEntry>();
    private boolean contextMenuOpen;
    private int contextMenuX;
    private int contextMenuY;
    private int contextMenuW;
    private int contextMenuH;
    @Nullable
    private HudElementState dragging;
    private int dragDx;
    private int dragDy;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    protected void init() {
        this.clearWidgets();
    }

    private void closeContextMenu() {
        this.contextMenuOpen = false;
        this.contextMenuEntries.clear();
    }

    private void openElementsMenu(int anchorX, int anchorY) {
        this.contextMenuEntries.clear();
        for (HudElementDefinition d : HudManager.INSTANCE.definitions()) {
            String id = d.id();
            boolean on = HudManager.INSTANCE.hasElement(id);
            String label = (on ? "[x] " : "[ ] ") + d.displayName();
            this.contextMenuEntries.add(new ContextMenuEntry(label, () -> {
                if (HudManager.INSTANCE.hasElement(id)) {
                    HudManager.INSTANCE.removeElement(id);
                } else {
                    HudManager.INSTANCE.addElement(id);
                }
            }));
        }
        if (this.contextMenuEntries.isEmpty()) {
            return;
        }
        int padX = 10;
        int w = 100;
        for (ContextMenuEntry e : this.contextMenuEntries) {
            w = Math.max(w, this.font.width(e.label()) + padX * 2);
        }
        int h = 4 + this.contextMenuEntries.size() * 14;
        int x = Math.max(4, Math.min(anchorX, this.width - w - 4));
        int y = Math.max(4, Math.min(anchorY, this.height - h - 4));
        this.contextMenuX = x;
        this.contextMenuY = y;
        this.contextMenuW = w;
        this.contextMenuH = h;
        this.contextMenuOpen = true;
    }

    private boolean contextMenuContains(double mx, double my) {
        return this.contextMenuOpen && mx >= (double)this.contextMenuX && mx < (double)(this.contextMenuX + this.contextMenuW) && my >= (double)this.contextMenuY && my < (double)(this.contextMenuY + this.contextMenuH);
    }

    private void renderContextMenu(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (!this.contextMenuOpen || this.contextMenuEntries.isEmpty()) {
            return;
        }
        context.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + this.contextMenuW, this.contextMenuY + this.contextMenuH, UiTokens.argb(240, -15788246));
        context.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + this.contextMenuW, this.contextMenuY + 1, UiTokens.argb(204, -11870592));
        context.fill(this.contextMenuX, this.contextMenuY + this.contextMenuH - 1, this.contextMenuX + this.contextMenuW, this.contextMenuY + this.contextMenuH, UiTokens.argb(136, -15293622));
        context.fill(this.contextMenuX, this.contextMenuY, this.contextMenuX + 1, this.contextMenuY + this.contextMenuH, UiTokens.argb(136, -14498466));
        context.fill(this.contextMenuX + this.contextMenuW - 1, this.contextMenuY, this.contextMenuX + this.contextMenuW, this.contextMenuY + this.contextMenuH, UiTokens.argb(85, -16644585));
        int rowY = this.contextMenuY + 2;
        for (int i = 0; i < this.contextMenuEntries.size(); ++i) {
            ContextMenuEntry e = this.contextMenuEntries.get(i);
            boolean hot = mouseX >= this.contextMenuX && mouseX < this.contextMenuX + this.contextMenuW && mouseY >= rowY && mouseY < rowY + 14;
            boolean bl = hot;
            if (hot) {
                context.fill(this.contextMenuX + 1, rowY, this.contextMenuX + this.contextMenuW - 1, rowY + 14, UiTokens.argb(170, -15293622));
            }
            context.text(this.font, Component.literal(e.label()), this.contextMenuX + 8, rowY + 3, -460036);
            rowY += 14;
        }
    }

    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        if (IngameOverlayHost.onScreenOverlayMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        double mx = click.x();
        double my = click.y();
        if (this.contextMenuOpen) {
            if (this.contextMenuContains(mx, my)) {
                if (click.button() == 0) {
                    int row = ((int)my - this.contextMenuY - 2) / 14;
                    if (row >= 0 && row < this.contextMenuEntries.size()) {
                        this.contextMenuEntries.get(row).action().run();
                    }
                    this.closeContextMenu();
                    return true;
                }
                if (click.button() == 1) {
                    this.closeContextMenu();
                    return true;
                }
            } else {
                this.closeContextMenu();
                if (click.button() == 1) {
                    return true;
                }
            }
        }
        if (click.button() == 1) {
            this.openElementsMenu((int)mx, (int)my);
            return true;
        }
        if (click.button() == 0) {
            for (HudElementState st : HudManager.INSTANCE.elements()) {
                int[] m = HudManager.INSTANCE.measureElement(st, this.minecraft);
                int x = this.anchoredX(st, m[0]);
                int y = this.anchoredY(st, m[1]);
                if (!(mx >= (double)x && mx <= (double)(x + m[0]) && my >= (double)y && my <= (double)(y + m[1]))) continue;
                this.dragging = st;
                this.dragDx = (int)mx - x;
                this.dragDy = (int)my - y;
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent click) {
        if (IngameOverlayHost.onScreenOverlayMouseReleased(click.x(), click.y(), click.button())) {
            return true;
        }
        this.dragging = null;
        return super.mouseReleased(click);
    }

    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (IngameOverlayHost.onScreenOverlayMouseDragged(click.x(), click.y(), click.button())) {
            return true;
        }
        if (this.dragging != null && this.minecraft != null) {
            int[] m = HudManager.INSTANCE.measureElement(this.dragging, this.minecraft);
            int nx = (int)click.x() - this.dragDx;
            int ny = (int)click.y() - this.dragDy;
            nx = HudEditorScreen.applySnap(nx, this.width - m[0], HudManager.INSTANCE.settings().snappingRange);
            ny = HudEditorScreen.applySnap(ny, this.height - m[1], HudManager.INSTANCE.settings().snappingRange);
            this.dragging.x = nx;
            this.dragging.y = ny;
            HudManager.INSTANCE.save();
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        UiDraw.fillMidnightBackground(context, this.width, this.height);
        int hintY = 8;
        context.centeredText(this.font, Component.literal("Right-click: add/remove HUD elements \u00b7 Esc: done \u00b7 drag labels to move"), this.width / 2, hintY, -7934036);
        context.centeredText(this.font, Component.literal("Negative x/y anchor right/bottom."), this.width / 2, hintY + 12, -7035976);
        this.renderElementsOverlay(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
        this.renderContextMenu(context, mouseX, mouseY);
    }

    private void renderElementsOverlay(GuiGraphicsExtractor context) {
        Minecraft mc = this.minecraft;
        if (mc == null) {
            return;
        }
        for (HudElementState st : HudManager.INSTANCE.elements()) {
            if (!st.active) continue;
            int[] m = HudManager.INSTANCE.measureElement(st, mc);
            int x = this.anchoredX(st, m[0]);
            int y = this.anchoredY(st, m[1]);
            context.fill(x - 1, y - 1, x + m[0] + 1, y + m[1] + 1, UiTokens.argb(102, -15293622));
            context.fill(x, y, x + m[0], y + m[1], UiTokens.argb(68, -15788246));
            HudElementDefinition def = null;
            for (HudElementDefinition d : HudManager.INSTANCE.definitions()) {
                if (!d.id().equals(st.id)) continue;
                def = d;
                break;
            }
            if (def == null) continue;
            context.text(this.font, def.textProvider().text(mc, HudManager.INSTANCE), x, y, -460036);
        }
    }

    private int anchoredX(HudElementState st, int widthPx) {
        return st.x >= 0 ? st.x : this.width + st.x - widthPx;
    }

    private int anchoredY(HudElementState st, int hPx) {
        return st.y >= 0 ? st.y : this.height + st.y - hPx;
    }

    private static int applySnap(int v, int max, int snapRange) {
        int out = Math.max(0, Math.min(max, v));
        if (out < snapRange) {
            return 0;
        }
        if (max - out < snapRange) {
            return max;
        }
        return out;
    }

    public void onClose() {
        this.closeContextMenu();
        HudManager.INSTANCE.save();
        this.minecraft.setScreen(this.parent);
    }

    private record ContextMenuEntry(String label, Runnable action) {
    }
}

