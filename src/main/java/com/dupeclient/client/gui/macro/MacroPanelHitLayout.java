package com.dupeclient.client.gui.macro;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers screen-space hit boxes during render; {@link #dispatch(double, double)} resolves clicks
 * in registration order so render and mouse handling stay aligned.
 */
public final class MacroPanelHitLayout {
    private final List<Entry> entries = new ArrayList<>();

    public void clear() {
        entries.clear();
    }

    public void add(int x, int y, int w, int h, Runnable action) {
        if (w <= 0 || h <= 0 || action == null) {
            return;
        }
        entries.add(new Entry(x, y, w, h, action));
    }

    public boolean dispatch(double mouseX, double mouseY) {
        for (Entry e : entries) {
            if (mouseX >= e.x && mouseX < e.x + e.w && mouseY >= e.y && mouseY < e.y + e.h) {
                e.action.run();
                return true;
            }
        }
        return false;
    }

    private record Entry(int x, int y, int w, int h, Runnable action) {
    }
}
