package com.dupeclient.client.module.macro.graph;

/** Directed edge: {@code from} node output → {@code to} node input. */
public final class MacroGraphEdge {
    public String from = "";
    public String to = "";
    /**
     * For {@link MacroGraphTypes#REPEAT}: {@code "loop"} (body entry) or {@code "next"} (after the loop).
     * Empty = default single output on other nodes.
     */
    public String fromSlot = "";
}
