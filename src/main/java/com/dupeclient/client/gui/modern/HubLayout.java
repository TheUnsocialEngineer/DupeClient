package com.dupeclient.client.gui.modern;

import net.minecraft.util.math.MathHelper;

/**
 * Viewport-aware hub metrics so the sidebar / pill nav fits from low GUI-scale windows
 * (split-screen, fractional OS scaling) through 4K fullscreen.
 */
final class HubLayout {
    /** Use top pill nav when width is below this (scaled px). */
    static final int BP_COMPACT_WIDTH = 560;
    /** Prefer pill nav when height is below this even if the window is wide. */
    static final int BP_SHORT_HEIGHT = 540;
    /** Minimum content column height below which we always prefer pill nav. */
    static final int MIN_CONTENT_HEIGHT = 160;

    int navItemH = 38;
    int navGap = 8;
    int sectionHeaderH = 14;
    int pillH = 32;
    int pillGap = 8;
    int sidebarWidth;
    boolean compactNav;
    int estimatedSidebarNavHeight;
    int navAreaTop;
    int navAreaBottom;

    private HubLayout() {
    }

    static HubLayout forViewport(int vw, int vh, int topPad, int navCount, boolean reserveAppBar) {
        HubLayout out = new HubLayout();
        int safeVw = Math.max(1, vw);
        int safeVh = Math.max(1, vh);
        int sections = sectionCount(navCount);

        out.sidebarWidth = sidebarWidthForViewport(safeVw);
        int sidebarMinH = estimateSidebarHeight(topPad, navCount, sections, 38, 8, 14);
        boolean shortViewport = safeVh < BP_SHORT_HEIGHT;
        boolean tightViewport = safeVh < sidebarMinH + MIN_CONTENT_HEIGHT + topPad;

        out.compactNav = safeVw < BP_COMPACT_WIDTH || shortViewport || tightViewport;

        if (out.compactNav) {
            out.applyCompactMetrics(safeVw, safeVh, topPad, navCount);
        } else {
            out.applySidebarMetrics(safeVw, safeVh, topPad, navCount, sections);
        }
        return out;
    }

    private void applySidebarMetrics(int vw, int vh, int topPad, int navCount, int sections) {
        int availH = Math.max(MIN_CONTENT_HEIGHT, vh - topPad - UiTokens.SP_3 * 2);
        int needed = estimateSidebarHeight(topPad, navCount, sections, navItemH, navGap, sectionHeaderH);
        if (needed > availH + UiTokens.SP_2 && navCount > 0) {
            float scale = availH / (float) needed;
            scale = MathHelper.clamp(scale, 0.58f, 1.0f);
            navItemH = Math.max(24, Math.round(38 * scale));
            navGap = Math.max(3, Math.round(8 * scale));
            sectionHeaderH = Math.max(9, Math.round(14 * scale));
        }
        estimatedSidebarNavHeight = estimateSidebarHeight(topPad, navCount, sections, navItemH, navGap, sectionHeaderH);
        navAreaTop = topPad + UiTokens.SP_3;
        navAreaBottom = vh - UiTokens.SP_3;
    }

    private void applyCompactMetrics(int vw, int vh, int topPad, int navCount) {
        int perRow = vw >= 920 ? 5 : (vw >= 720 ? 4 : (vw >= 480 ? 3 : 2));
        int rows = navCount <= 0 ? 0 : (navCount + perRow - 1) / perRow;
        int availNavH = Math.max(48, vh - topPad - MIN_CONTENT_HEIGHT - UiTokens.SP_3 * 3);
        int needed = rows > 0 ? rows * (pillH + pillGap) + pillGap : 0;
        if (needed > availNavH && rows > 0) {
            float scale = availNavH / (float) needed;
            scale = MathHelper.clamp(scale, 0.55f, 1.0f);
            pillH = Math.max(22, Math.round(32 * scale));
            pillGap = Math.max(4, Math.round(8 * scale));
        }
        estimatedSidebarNavHeight = rows > 0 ? rows * (pillH + pillGap) + pillGap : 0;
        navAreaTop = topPad + UiTokens.SP_2;
        navAreaBottom = navAreaTop + estimatedSidebarNavHeight;
    }

    static int sidebarWidthForViewport(int vw) {
        if (vw < BP_COMPACT_WIDTH) {
            return 0;
        }
        return MathHelper.clamp(Math.round(vw * 0.11f), 128, 196);
    }

    private static int sectionCount(int navCount) {
        int sections = 0;
        for (int i = 0; i < navCount; i++) {
            if (sectionLabelForIndex(i) != null) {
                sections++;
            }
        }
        return sections;
    }

    private static int estimateSidebarHeight(int topPad, int navCount, int sections, int itemH, int gap, int headerH) {
        if (navCount <= 0) {
            return topPad + UiTokens.SP_3 * 2;
        }
        return topPad + UiTokens.SP_3 + sections * headerH + navCount * itemH + Math.max(0, navCount - 1) * gap + UiTokens.SP_2;
    }

    private static String sectionLabelForIndex(int index) {
        String[] labels = {"Research", "Network", "Automation", "Interface", "Security"};
        int[] starts = {0, 1, 2, 5, 8};
        for (int s = starts.length - 1; s >= 0; s--) {
            if (index == starts[s]) {
                return labels[s];
            }
        }
        return null;
    }
}
