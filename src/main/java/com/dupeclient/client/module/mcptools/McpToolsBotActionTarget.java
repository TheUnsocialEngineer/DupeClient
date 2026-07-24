package com.dupeclient.client.module.mcptools;

/** Who receives bot control actions from the overlay. */
public enum McpToolsBotActionTarget {
    SELECTED("Selected bots"),
    ALL("All bots");

    public final String label;

    McpToolsBotActionTarget(String label) {
        this.label = label;
    }

    public McpToolsBotActionTarget next() {
        McpToolsBotActionTarget[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static McpToolsBotActionTarget fromName(String raw) {
        if (raw == null) {
            return SELECTED;
        }
        for (McpToolsBotActionTarget t : values()) {
            if (t.name().equalsIgnoreCase(raw.trim())) {
                return t;
            }
        }
        return SELECTED;
    }
}
