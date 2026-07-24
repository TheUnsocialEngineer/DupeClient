package com.dupeclient.client.module.macro.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual grouping in the macro graph editor (saved with the macro). Members are {@link MacroGraphNode#id} values.
 */
public final class MacroGraphGroup {
    public String id = "";
    public String label = "Group";
    /** ARGB border color (e.g. {@code 0xFF4A6ED0}). */
    public int borderArgb = 0xFF4A6ED0;
    /** ARGB fill color, typically with low alpha (e.g. {@code 0x40204060}). */
    public int fillArgb = 0x40204060;
    public boolean collapsed;
    public List<String> memberNodeIds = new ArrayList<>();
}
