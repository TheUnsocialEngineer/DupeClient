package com.dupeclient.client.gui.modern;

import com.dupeclient.client.gui.modern.theme.MidnightPalette;
import com.dupeclient.client.DupeClient;
import com.dupeclient.client.config.ClientGuiLayoutStorage;
import com.dupeclient.client.gui.panel.Panel;
import com.dupeclient.client.core.session.HubModuleRules;
import com.dupeclient.client.core.session.PresenceRosterSync;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Responsive hub layout: wide = left rail + content; compact = top pill nav + content.
 * Panels keep natural height (no stretch); scroll inside the content viewport.
 */
public final class HubShell {
    private static final String[] NAV_SECTION_LABELS = {"Research", "Network", "Automation", "Interface", "Security"};
    private static final int[] NAV_SECTION_START = {0, 1, 2, 5, 8};

    private int viewportW = 800;
    private int viewportH = 600;
    private boolean firstLayoutTick = true;
    private int contentLeft;
    private int contentTop;
    private int contentW;
    private int contentH;
    private int contentScrollTop;
    private int contentScrollH;
    private int sidebarW;
    private int sidebarRailTop;
    private int sidebarRailBottom;
    private double sidebarScrollY;
    private double sidebarScrollMax;
    private double compactNavScrollY;
    private double compactNavScrollMax;
    private int compactNavAreaTop;
    private int compactNavAreaBottom;
    private HubLayout layoutMetrics = HubLayout.forViewport(800, 600, 0, 11, false);
    private int selectedModuleIndex;
    private int lastSelectedModuleIndex = -1;
    private int navHoverIndex = -1;
    private double[] moduleScrollY;
    private boolean reserveTopForVanillaCloseButton;

    private final int[] navX = new int[24];
    private final int[] navY = new int[24];
    private final int[] navW = new int[24];
    private final int[] navH = new int[24];
    private int navCount;

    public void setReserveTopForVanillaCloseButton(boolean v) {
        this.reserveTopForVanillaCloseButton = v;
    }

    public void syncViewport(int width, int height) {
        if (width > 0 && height > 0) {
            this.viewportW = width;
            this.viewportH = height;
        }
    }

    public static int sidebarWidthForViewport(int vw) {
        return HubLayout.sidebarWidthForViewport(vw);
    }

    public void onScreenOpen() {
        for (Panel panel : DupeClient.getGuiManager().getPanels()) {
            panel.setDraggable(false);
        }
        if (firstLayoutTick) {
            int n = panelCount();
            selectedModuleIndex = Mth.clamp(ClientGuiLayoutStorage.loadSelectedModule(), 0, Math.max(0, n - 1));
            moduleScrollY = ClientGuiLayoutStorage.loadModuleScrollY(n);
            firstLayoutTick = false;
        }
        ensureAllowedSelection();
        ensureScrollBuffers();
        recomputeLayout(DupeClient.getGuiManager().getPanels());
        ensureNavItemVisible(selectedModuleIndex);
    }

    private void ensureAllowedSelection() {
        List<Panel> panels = DupeClient.getGuiManager().getPanels();
        if (panels.isEmpty()) {
            return;
        }
        if (selectedModuleIndex >= 0 && selectedModuleIndex < panels.size()
                && HubModuleRules.panelAllowed(panels.get(selectedModuleIndex).getId())) {
            return;
        }
        setSelectedModuleIndex(HubModuleRules.firstAllowedPanelIndex(panels));
    }

    private void recomputeLayout(List<Panel> panels) {
        int n = panels.size();
        int vw = viewportW;
        int vh = viewportH;
        int topPad = reserveTopForVanillaCloseButton ? UiTokens.APP_BAR_H : 0;
        layoutMetrics = HubLayout.forViewport(vw, vh, topPad, n, reserveTopForVanillaCloseButton);
        boolean compact = layoutMetrics.compactNav;
        sidebarW = compact ? 0 : layoutMetrics.sidebarWidth;

        navCount = n;
        if (compact) {
            int perRow = vw >= 920 ? 5 : (vw >= 720 ? 4 : (vw >= 480 ? 3 : 2));
            int gap = layoutMetrics.pillGap;
            int pillH = layoutMetrics.pillH;
            int navBlockTop = topPad + UiTokens.SP_2;
            int btnW = Math.max(64, (vw - UiTokens.SP_4 * 2 - gap * (perRow - 1)) / perRow);

            for (int i = 0; i < n; i++) {
                int row = i / perRow;
                int col = i % perRow;
                navX[i] = UiTokens.SP_4 + col * (btnW + gap);
                navY[i] = navBlockTop + row * (pillH + gap);
                navW[i] = btnW;
                navH[i] = pillH;
            }

            int navBlockH = layoutMetrics.estimatedSidebarNavHeight;
            compactNavAreaTop = layoutMetrics.navAreaTop;
            compactNavAreaBottom = layoutMetrics.navAreaBottom;
            if (n > 0) {
                int navBottom = navY[n - 1] + navH[n - 1];
                compactNavScrollMax = Math.max(0, navBottom + UiTokens.SP_2 - compactNavAreaBottom);
            } else {
                compactNavScrollMax = 0;
            }
            compactNavScrollY = Mth.clamp(compactNavScrollY, 0.0, compactNavScrollMax);

            contentLeft = UiTokens.SP_3;
            contentW = Math.max(120, vw - 2 * UiTokens.SP_3);
            contentTop = navBlockTop + navBlockH + UiTokens.SP_3;
            contentH = Math.max(HubLayout.MIN_CONTENT_HEIGHT / 2, vh - contentTop - UiTokens.SP_3);
            sidebarRailTop = 0;
            sidebarRailBottom = 0;
            sidebarScrollMax = 0;
            sidebarScrollY = 0;
        } else {
            int ix = UiTokens.SP_2;
            int iy = topPad + UiTokens.SP_3;
            int iw = sidebarW - 2 * UiTokens.SP_2;
            int ih = layoutMetrics.navItemH;
            int railGap = layoutMetrics.navGap;
            int sectionHeaderH = layoutMetrics.sectionHeaderH;
            for (int i = 0; i < n; i++) {
                String section = sectionLabelForIndex(i);
                if (section != null) {
                    iy += sectionHeaderH;
                }
                navX[i] = ix;
                navY[i] = iy;
                navW[i] = iw;
                navH[i] = ih;
                iy += ih + railGap;
            }
            contentLeft = sidebarW + UiTokens.SP_4;
            contentW = Math.max(160, vw - contentLeft - UiTokens.SP_4);
            contentTop = topPad + UiTokens.SP_3;
            contentH = Math.max(HubLayout.MIN_CONTENT_HEIGHT / 2, vh - contentTop - UiTokens.SP_3);
            sidebarRailTop = layoutMetrics.navAreaTop;
            sidebarRailBottom = layoutMetrics.navAreaBottom;
            compactNavScrollMax = 0;
            compactNavScrollY = 0;
            if (n > 0) {
                int navBottom = navY[n - 1] + navH[n - 1];
                sidebarScrollMax = Math.max(0, navBottom + UiTokens.SP_2 - sidebarRailBottom);
            } else {
                sidebarScrollMax = 0;
            }
            sidebarScrollY = Mth.clamp(sidebarScrollY, 0.0, sidebarScrollMax);
        }
        contentScrollTop = contentTop;
        contentScrollH = contentH;
    }

    public int panelCount() {
        return DupeClient.getGuiManager().getPanels().size();
    }

    public int getSelectedModuleIndex() {
        return selectedModuleIndex;
    }

    public void setSelectedModuleIndex(int index) {
        int n = panelCount();
        if (n == 0) {
            return;
        }
        selectedModuleIndex = Mth.clamp(index, 0, n - 1);
        ensureNavItemVisible(selectedModuleIndex);
    }

    private void ensureNavItemVisible(int index) {
        if (index < 0 || index >= navCount) {
            return;
        }
        if (layoutMetrics.compactNav) {
            if (compactNavScrollMax <= 0) {
                return;
            }
            int drawY = navScreenY(index);
            int drawBottom = drawY + navH[index];
            if (drawY < compactNavAreaTop) {
                compactNavScrollY -= compactNavAreaTop - drawY;
            } else if (drawBottom > compactNavAreaBottom) {
                compactNavScrollY += drawBottom - compactNavAreaBottom;
            }
            compactNavScrollY = Mth.clamp(compactNavScrollY, 0.0, compactNavScrollMax);
            return;
        }
        if (sidebarScrollMax <= 0) {
            return;
        }
        int drawY = navScreenY(index);
        int drawBottom = drawY + navH[index];
        if (drawY < sidebarRailTop) {
            sidebarScrollY -= sidebarRailTop - drawY;
        } else if (drawBottom > sidebarRailBottom) {
            sidebarScrollY += drawBottom - sidebarRailBottom;
        }
        sidebarScrollY = Mth.clamp(sidebarScrollY, 0.0, sidebarScrollMax);
    }

    private void notifyModuleHidden(int index) {
        List<Panel> panels = DupeClient.getGuiManager().getPanels();
        if (index >= 0 && index < panels.size()) {
            panels.get(index).onModuleHidden();
        }
    }

    private void ensureScrollBuffers() {
        int n = panelCount();
        if (moduleScrollY == null || moduleScrollY.length != n) {
            moduleScrollY = ClientGuiLayoutStorage.loadModuleScrollY(n);
        }
    }

    public void applyEmbeddedLayout(Font textRenderer) {
        recomputeLayout(DupeClient.getGuiManager().getPanels());
        ensureScrollBuffers();
        List<Panel> panels = DupeClient.getGuiManager().getPanels();
        int n = panels.size();
        for (int i = 0; i < n; i++) {
            Panel p = panels.get(i);
            p.setWidth(contentW);
            boolean on = (i == selectedModuleIndex);
            p.setVisible(on);
            p.setEmbedViewportH(on ? contentScrollH : 0);
            p.setEmbedStretchMinH(0);
            if (on) {
                double sy = (moduleScrollY != null && i < moduleScrollY.length) ? moduleScrollY[i] : 0.0;
                p.setPosition(contentLeft, (int) (contentScrollTop - sy));
            } else {
                p.setPosition(contentLeft, -5000);
            }
        }
        if (lastSelectedModuleIndex >= 0 && lastSelectedModuleIndex != selectedModuleIndex) {
            notifyModuleHidden(lastSelectedModuleIndex);
        }
        lastSelectedModuleIndex = selectedModuleIndex;

        int si = selectedModuleIndex;
        if (si >= 0 && si < n && moduleScrollY != null && si < moduleScrollY.length) {
            Panel active = panels.get(si);
            double maxScroll = Math.max(0.0, (double) active.getLayoutContentHeight() - contentScrollH);
            moduleScrollY[si] = Mth.clamp(moduleScrollY[si], 0.0, maxScroll);
        }
    }

    public void render(GuiGraphicsExtractor context, Font tr, int mouseX, int mouseY, float deltaTicks, int viewW, int viewH) {
        int vw = Math.max(1, viewW);
        int vh = Math.max(1, viewH);
        this.viewportW = vw;
        this.viewportH = vh;
        applyEmbeddedLayout(tr);
        boolean compact = layoutMetrics.compactNav;

        if (reserveTopForVanillaCloseButton) {
            UiDraw.drawTopFullWidthBand(context, vw, UiTokens.APP_BAR_H);
            context.text(tr, Component.literal("DupeClient"), UiTokens.SP_4, 12, UiTokens.SLATE_50);
            context.text(tr, Component.literal("Modules"), UiTokens.SP_4, 24, UiTokens.TEXT_DIM);
            String buildLine = DupeClient.MOD_VERSION + " · " + DupeClient.BUILD_TAG;
            int buildW = tr.width(buildLine);
            context.text(tr, Component.literal(buildLine), vw - buildW - UiTokens.SP_4, 18, UiTokens.TEXT_DIM);
            drawHubStatusLine(context, tr, vw);
        } else {
            String buildLine = DupeClient.MOD_VERSION + " · " + DupeClient.BUILD_TAG;
            int buildW = tr.width(buildLine);
            context.text(tr, Component.literal(buildLine), vw - buildW - UiTokens.SP_4, UiTokens.SP_2, UiTokens.TEXT_DIM);
            drawHubStatusLine(context, tr, vw);
        }

        if (!compact && sidebarW > 0) {
            int railH = vh - (reserveTopForVanillaCloseButton ? UiTokens.APP_BAR_H : 0);
            int y0 = reserveTopForVanillaCloseButton ? UiTokens.APP_BAR_H : 0;
            context.fillGradient(0, y0, sidebarW, y0 + railH, MidnightPalette.alphaRgb(0xE8, 0x09090B), MidnightPalette.alphaRgb(0xE8, 0x111118));
            context.fill(sidebarW - 1, y0, sidebarW, y0 + railH, MidnightPalette.BORDER_LIGHT);
            List<Panel> panels = DupeClient.getGuiManager().getPanels();
            context.enableScissor(0, sidebarRailTop, sidebarW, sidebarRailBottom);
            for (int i = 0; i < navCount && i < panels.size(); i++) {
                int drawY = navScreenY(i);
                String section = sectionLabelForIndex(i);
                if (section != null) {
                    UiComponents.drawNavSectionLabel(tr, context, navX[i], drawY - layoutMetrics.sectionHeaderH, navW[i], section);
                }
                boolean sel = i == selectedModuleIndex;
                boolean hot = i == navHoverIndex;
                boolean locked = !HubModuleRules.panelAllowed(panels.get(i).getId());
                String label = panels.get(i).getTitle().getString() + (locked ? " 🔒" : "");
                UiComponents.drawNavItem(tr, context, navX[i], drawY, navW[i], navH[i], label, sel, hot);
            }
            context.disableScissor();
            if (sidebarScrollMax > 0.5) {
                UiDraw.drawScrollbar(context, sidebarW - UiTokens.SP_2, sidebarRailTop, sidebarRailBottom, sidebarScrollY, sidebarScrollMax);
            }
        } else {
            List<Panel> panels = DupeClient.getGuiManager().getPanels();
            context.enableScissor(0, compactNavAreaTop, vw, compactNavAreaBottom);
            for (int i = 0; i < navCount && i < panels.size(); i++) {
                int drawY = navScreenY(i);
                boolean sel = i == selectedModuleIndex;
                boolean hot = i == navHoverIndex;
                boolean locked = !HubModuleRules.panelAllowed(panels.get(i).getId());
                String label = panels.get(i).getTitle().getString() + (locked ? " 🔒" : "");
                UiComponents.drawNavPill(tr, context, navX[i], drawY, navW[i], navH[i], label, sel, hot);
            }
            context.disableScissor();
            if (compactNavScrollMax > 0.5) {
                UiDraw.drawScrollbar(context, vw - UiTokens.SP_2, compactNavAreaTop, compactNavAreaBottom, compactNavScrollY, compactNavScrollMax);
            }
        }

        int x0 = contentLeft;
        int y0c = contentScrollTop;
        int x1 = x0 + contentW;
        int y1 = y0c + contentScrollH;
        context.enableScissor(x0, y0c, x1, y1);
        UiDraw.fillContentWorkspace(context, x0, y0c, contentW, contentScrollH);
        for (Panel panel : DupeClient.getGuiManager().getPanels()) {
            if (panel.isVisible()) {
                panel.render(context, mouseX, mouseY, deltaTicks);
            }
        }
        context.disableScissor();

        int si = selectedModuleIndex;
        List<Panel> panels = DupeClient.getGuiManager().getPanels();
        if (si >= 0 && si < panels.size() && moduleScrollY != null && si < moduleScrollY.length) {
            double maxScroll = Math.max(0.0, (double) panels.get(si).getLayoutContentHeight() - contentScrollH);
            if (maxScroll > 0.5) {
                UiDraw.drawScrollbar(context, vw - UiTokens.SP_2, y0c, y1, moduleScrollY[si], maxScroll);
            }
        }
    }

    public void updateNavHover(int mouseX, int mouseY) {
        navHoverIndex = -1;
        for (int i = 0; i < navCount; i++) {
            if (navHitTest(mouseX, mouseY, i)) {
                navHoverIndex = i;
                return;
            }
        }
    }

    private int navScreenY(int index) {
        if (layoutMetrics.compactNav) {
            return (int) (navY[index] - compactNavScrollY);
        }
        return (int) (navY[index] - sidebarScrollY);
    }

    private int navDrawY(int index) {
        return navScreenY(index);
    }

    private boolean navHitTest(double mx, double my, int index) {
        int drawY = navDrawY(index);
        if (mx < navX[index] || mx >= navX[index] + navW[index] || my < drawY || my >= drawY + navH[index]) {
            return false;
        }
        if (layoutMetrics.compactNav) {
            return my >= compactNavAreaTop && my < compactNavAreaBottom;
        }
        if (sidebarW > 0) {
            return my >= sidebarRailTop && my < sidebarRailBottom;
        }
        return true;
    }

    public boolean handleNavClick(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        for (int i = 0; i < navCount; i++) {
            if (navHitTest(mx, my, i)) {
                List<Panel> panels = DupeClient.getGuiManager().getPanels();
                if (i < panels.size() && !HubModuleRules.panelAllowed(panels.get(i).getId())) {
                    return true;
                }
                setSelectedModuleIndex(i);
                return true;
            }
        }
        return false;
    }

    public boolean handlePanelClick(double mx, double my, int button) {
        List<Panel> panels = DupeClient.getGuiManager().getPanels();
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel panel = panels.get(i);
            if (panel.isVisible() && panel.containsPoint(mx, my) && panel.mouseClicked(mx, my, button)) {
                setSelectedModuleIndex(i);
                return true;
            }
        }
        return false;
    }

    public void handlePanelRelease(double mx, double my, int button) {
        for (Panel panel : DupeClient.getGuiManager().getPanels()) {
            if (panel.isVisible()) {
                panel.mouseReleased(mx, my, button);
            }
        }
    }

    public boolean handleContentScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        recomputeLayout(DupeClient.getGuiManager().getPanels());
        if (verticalAmount == 0) {
            return false;
        }
        double scrollStep = verticalAmount * Math.max(14.0, layoutMetrics.navItemH * 0.55);
        if (layoutMetrics.compactNav
                && mouseY >= compactNavAreaTop && mouseY < compactNavAreaBottom
                && compactNavScrollMax > 0.5) {
            compactNavScrollY -= scrollStep;
            compactNavScrollY = Mth.clamp(compactNavScrollY, 0.0, compactNavScrollMax);
            return true;
        }
        if (!layoutMetrics.compactNav && sidebarW > 0
                && mouseX >= 0 && mouseX < sidebarW
                && mouseY >= sidebarRailTop && mouseY < sidebarRailBottom
                && sidebarScrollMax > 0.5) {
            sidebarScrollY -= scrollStep;
            sidebarScrollY = Mth.clamp(sidebarScrollY, 0.0, sidebarScrollMax);
            return true;
        }
        if (mouseX >= contentLeft
                && mouseX < contentLeft + contentW
                && mouseY >= contentScrollTop
                && mouseY < contentScrollTop + contentScrollH
                && verticalAmount != 0) {
            applyEmbeddedLayout(net.minecraft.client.Minecraft.getInstance().font);
            int n = panelCount();
            if (n == 0 || moduleScrollY == null) {
                return true;
            }
            int si = selectedModuleIndex;
            if (si < 0 || si >= n || si >= moduleScrollY.length) {
                return true;
            }
            double maxScroll = Math.max(0.0, (double) DupeClient.getGuiManager().getPanels().get(si).getLayoutContentHeight() - contentScrollH);
            moduleScrollY[si] -= scrollStep;
            moduleScrollY[si] = Mth.clamp(moduleScrollY[si], 0.0, maxScroll);
            return true;
        }
        return false;
    }

    public void tick() {
        for (Panel panel : DupeClient.getGuiManager().getPanels()) {
            panel.tick();
        }
    }

    public void onRemoved() {
        if (moduleScrollY != null) {
            ClientGuiLayoutStorage.saveClientGuiLayout(selectedModuleIndex, moduleScrollY);
        } else {
            ClientGuiLayoutStorage.saveClientGuiLayout(selectedModuleIndex, new double[0]);
        }
    }

    private static String sectionLabelForIndex(int index) {
        for (int s = NAV_SECTION_START.length - 1; s >= 0; s--) {
            if (index == NAV_SECTION_START[s]) {
                return NAV_SECTION_LABELS[s];
            }
        }
        return null;
    }

    private static void drawHubStatusLine(GuiGraphicsExtractor context, Font tr, int vw) {
        String status;
        int color;
        if (HubModuleRules.viewerRestricted()) {
            status = PresenceRosterSync.statusLine();
            color = 0xFFFF9A6A;
        } else if (PresenceRosterSync.isRosterPending()) {
            status = PresenceRosterSync.statusLine();
            color = UiTokens.ACCENT;
        } else if (!HubModuleRules.exploitFeaturesAllowed()) {
            status = HubModuleRules.blockReason();
            color = UiTokens.TEXT_DIM;
        } else {
            return;
        }
        if (status.length() > 72) {
            status = status.substring(0, 69) + "…";
        }
        int w = tr.width(status);
        context.text(tr, Component.literal(status), Math.max(UiTokens.SP_4, (vw - w) / 2), UiTokens.SP_2 + 12, color);
    }
}
