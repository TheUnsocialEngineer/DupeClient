package com.dupeclient.client.module.macro.graph;

import java.util.ArrayList;
import java.util.List;

/** Serialized fragment for copy/paste in the macro graph editor. */
public final class MacroGraphClipboard {
    public List<MacroGraphNode> nodes = new ArrayList<>();
    public List<MacroGraphEdge> edges = new ArrayList<>();
    public List<MacroGraphGroup> groups = new ArrayList<>();
}
