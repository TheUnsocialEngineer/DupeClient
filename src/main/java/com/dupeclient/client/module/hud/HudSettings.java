package com.dupeclient.client.module.hud;

import java.util.ArrayList;
import java.util.List;

public final class HudSettings {
    public boolean customFont = true;
    public boolean hideInMenus = false;
    public double textScale = 1.0;
    /** Cycled through all rendered HUD lines. */
    public List<Integer> textColors = new ArrayList<>(List.of(
            0xFFFFFFFF, // white
            0xFFAFB0AF, // gray
            0xFF19E119, // green
            0xFFE11919  // red
    ));

    public int border = 4;
    public int snappingRange = 10;

    /** GLFW key code, -1 means none. */
    public int bindKey = -1;
    public int bindMods = 0;

    /** Hotkey to open the HUD editor, GLFW key code, -1 means none. */
    public int editorOpenKey = -1;
}
