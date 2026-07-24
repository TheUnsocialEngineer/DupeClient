package com.dupeclient.client.ui.mui;

import icyllis.modernui.view.Gravity;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.TextView;

/**
 * Modern UI mVUS chrome for DupeClient: soft rose accent on dark, translucent bar (inspired by
 * ModernUI-MC preference screens).
 */
public final class MuiDupeStyle {
    public static final int TEXT_PRIMARY = 0xFFF5F0ED;
    public static final int TEXT_MUTED = 0xFF8A7F7C;
    public static final int ACCENT = 0xFFFFB8A0;
    public static final int BAR_BG = 0xE810141C;
    public static final int SIDEBAR_BG = 0xD8181C26;

    private MuiDupeStyle() {
    }

    public static void styleTopTitle(TextView title) {
        title.setTextColor(TEXT_PRIMARY);
        title.setTextSize(18f);
    }

    public static void styleNavButton(Button b, boolean selected) {
        b.setTextColor(selected ? 0xFFFFFFFF : TEXT_MUTED);
        b.setTextSize(15f);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
    }

    public static void styleFrameButton(Button close) {
        close.setTextColor(ACCENT);
        close.setTextSize(15f);
    }
}
