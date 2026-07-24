package com.dupeclient.client.gui.modern;

/**
 * Design tokens inspired by Tailwind CSS: slate neutrals, mint primary accent (modal-style UIs),
 * emerald for legacy success cues.
 */
public final class UiTokens {
    private UiTokens() {
    }

    /** Spacing: Tailwind-like scale (4px base). */
    public static final int SP_1 = 4;
    public static final int SP_2 = 8;
    public static final int SP_3 = 12;
    public static final int SP_4 = 16;
    public static final int SP_5 = 20;
    public static final int SP_6 = 24;
    public static final int SP_8 = 32;

    /** Uniform gap between stacked controls. */
    public static final int UI_GAP = 8;
    /** Gap between stacked section cards in a module. */
    public static final int MODULE_STACK_GAP = 5;

    /** Corner radius — CSS rounded-sm / md / lg / xl. */
    public static final int R_SM = 6;
    public static final int R_MD = 8;
    public static final int R_LG = 12;
    public static final int R_XL = 14;

    /** Layout breakpoints (scaled pixels). */
    public static final int BP_COMPACT = 560;
    public static final int BP_COMFORTABLE = 900;

    /** Top app chrome height (Close + branding). */
    public static final int APP_BAR_H = 44;
    public static final int TOP_BAR = APP_BAR_H;

      /** Reserved for optional chrome; hub panels use sidebar title only (0 = no duplicate header). */
    public static final int MODULE_HEADER_H = 0;
    public static final int CONTENT_PAD = SP_4;
    public static final int PANEL_SPACING = SP_4;
    /** Horizontal inset for module body rows. */
    public static final int BODY_INSET = SP_4;

    /** Legacy fixed width hint; hub uses responsive {@link com.dupeclient.client.gui.modern.HubShell} sizing. */
    public static final int SIDEBAR_WIDTH = 220;

    /** Compact top nav zone height. */
    public static final int MODULE_TAB_BLOCK_H = 56;

    // —— Slate (neutral) —— //
    public static final int SLATE_50 = 0xFFF8FAFC;
    public static final int SLATE_200 = 0xFFE2E8F0;
    public static final int SLATE_300 = 0xFFCBD5E1;
    public static final int SLATE_400 = 0xFF94A3B8;
    public static final int SLATE_500 = 0xFF64748B;
    public static final int SLATE_600 = 0xFF475569;
    public static final int SLATE_700 = 0xFF334155;
    public static final int SLATE_800 = 0xFF1E293B;
    public static final int SLATE_850 = 0xFF172033;
    public static final int SLATE_900 = 0xFF0F172A;
    public static final int SLATE_950 = 0xFF020617;

    // —— Blue (selection / primary actions — reference hub) —— //
    public static final int BLUE_400 = 0xFF60A5FA;
    public static final int BLUE_500 = 0xFF3B82F6;
    public static final int BLUE_600 = 0xFF2563EB;

    // —— Indigo (secondary / legacy) —— //
    public static final int INDIGO_300 = 0xFFA5B4FC;
    public static final int INDIGO_400 = 0xFF818CF8;
    public static final int INDIGO_500 = 0xFF6366F1;
    public static final int INDIGO_600 = 0xFF4F46E5;

    // —— Mint (primary accent — Widget Lab–style modals) —— //
    public static final int MINT_200 = 0xFFB8F5D1;
    public static final int MINT_300 = 0xFF86EFAC;
    public static final int MINT_400 = 0xFF4ADE80;
    public static final int MINT_500 = 0xFF22C55E;
    public static final int MINT_600 = 0xFF16A34A;

    // —— Emerald (success) —— //
    public static final int EMERALD_400 = 0xFF34D399;
    public static final int EMERALD_500 = 0xFF10B981;

    /** First content row inside titled cards (below title band). */
    public static final int CARD_CONTENT_TOP = 38;
    /** First row inside a card with no title strip (hub already names the module). */
    public static final int CARD_BODY_TOP = SP_4;
    /** Vertical step between full-height control rows (toggles, keybinds). */
    public static final int ROW_STEP = 30;
    /** Toggle track size for hit-testing alongside {@link com.dupeclient.client.gui.modern.UiComponents#drawOptionToggle}. */
    public static final int TOGGLE_TRACK_W = 40;
    public static final int TOGGLE_TRACK_H = 20;

    /** Semantic aliases (compatible with older panel code). */
    public static final int BG_DEEP = argb(0xE8, SLATE_950);
    public static final int BG_PANEL = argb(0xE8, SLATE_800);
    public static final int ACCENT = MINT_400;
    public static final int ACCENT_MUTED = argb(0xCC, MINT_600);
    public static final int TEXT = SLATE_50;
    public static final int TEXT_DIM = SLATE_400;
    public static final int HILITE_BG = argb(0x33, SLATE_200);
    public static final int SIDEBAR_EDGE = SLATE_700;
    public static final int BORDER_SUBTLE = argb(0x55, SLATE_500);
    public static final int SUCCESS = MINT_400;

    public static int argb(int a, int rgb) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
