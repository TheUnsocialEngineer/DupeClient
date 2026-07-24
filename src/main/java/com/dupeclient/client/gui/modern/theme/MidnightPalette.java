package com.dupeclient.client.gui.modern.theme;

/**
 * Modern dark-mode palette (Tailwind slate / zinc) for hub chrome, cards, and controls.
 */
public final class MidnightPalette {
    private MidnightPalette() {
    }

    /** Fullscreen gradient top */
    public static final int BG_TOP = 0xFF0A0A0B;
    /** Fullscreen gradient bottom */
    public static final int BG_BOTTOM = 0xFF111118;

    /** Elevated card surface */
    public static final int PANEL_FILL = 0xF0181B22;
    /** Card hover / nested surface */
    public static final int PANEL_FILL_RAISED = 0xF022272E;
    /** Subtle border (slate-700) */
    public static final int BORDER_LIGHT = 0x383D4654;
    /** Focus ring */
    public static final int BORDER_FOCUS = 0x663B82F6;

    public static final int TEXT_PRIMARY = 0xFFF4F4F5;
    public static final int TEXT_SECONDARY = 0xFFA1A1AA;
    public static final int TEXT_MUTED = 0xFF71717A;

    /** Emerald accent */
    public static final int GREEN = 0xFF22C55E;
    public static final int GREEN_DIM = 0xFF16A34A;

    /** Primary blue */
    public static final int BLUE_L = 0xFF3B82F6;
    public static final int BLUE_R = 0xFF2563EB;

    /** Active sidebar item */
    public static final int SIDEBAR_ACTIVE_L = 0xFF3F3F46;
    public static final int SIDEBAR_ACTIVE_R = 0xFF27272A;

    /** Inactive sidebar */
    public static final int SIDEBAR_IDLE = 0x4027272A;
    public static final int SIDEBAR_IDLE_BORDER = 0x303F3F46;

    public static final int TOGGLE_OFF = 0xFF3F3F46;

    public static final int PATH_GREEN = 0xFF4ADE80;

    public static final int CHROME_TOP = 0xFF27272A;
    public static final int CHROME_BOT = 0xFF18181B;

    public static int alphaRgb(int a, int rgb) {
        return (Math.min(255, Math.max(0, a)) << 24) | (rgb & 0xFFFFFF);
    }
}
