package com.dupeclient.client.module.macro.graph;

import java.util.Locale;

/** Node type ids for graph control flow (not emitted as {@link com.dupeclient.client.module.macro.MacroStep}s). */
public final class MacroGraphTypes {
    public static final String START = "GRAPH_START";
    public static final String END = "GRAPH_END";
    /**
     * Repeat: mint output {@code loop} (labeled Repeat in the editor) is the body; orange {@code next} (Continue)
     * is the merge/continuation. With Start/End bookends, {@code next} can be inferred at End for a single chain.
     */
    public static final String REPEAT = "REPEAT";

    private MacroGraphTypes() {
    }

    public static boolean isControlNode(String type) {
        if (type == null) {
            return false;
        }
        String t = type.trim();
        return START.equals(t) || END.equals(t);
    }

    public static boolean isRepeatNode(String type) {
        return type != null && REPEAT.equals(type.trim());
    }

    /**
     * Walk heading before holding forward: {@code N}/{@code E}/{@code S}/{@code W} for world cardinals,
     * or {@code PLAYER} to keep the client's current look direction. Unknown values default to {@code S}.
     */
    public static String normalizeWalkFacing(String raw) {
        if (raw == null || raw.isBlank()) {
            return "S";
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        if (t.equals("PLAYER") || t.equals("VIEW") || t.equals("CURRENT")) {
            return "PLAYER";
        }
        char c = t.charAt(0);
        return switch (c) {
            case 'N', 'E', 'W' -> String.valueOf(c);
            default -> "S";
        };
    }
}
