package com.dupeclient.client.module.macro;

/**
 * Portable wrapper for sharing macros between clients. Raw {@link MacroDefinition} JSON is also accepted on import.
 */
public final class MacroBundle {
    public static final int WRAPPER_VERSION = 1;

    public int dupeclientMacro = WRAPPER_VERSION;
    public String exportedAt = "";
    public String dupeclientVersion = "";
    public String description = "";
    public MacroDefinition definition;

    public MacroBundle() {
    }

    public MacroBundle(MacroDefinition definition) {
        this.definition = definition;
    }
}
