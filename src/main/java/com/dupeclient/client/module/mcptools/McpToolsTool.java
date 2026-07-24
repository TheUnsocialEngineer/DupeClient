package com.dupeclient.client.module.mcptools;

public enum McpToolsTool {
    SERVER_RESPONSE("server_response", "Connect test", "scripts/server_response.mjs", false, false),
    CONNECT("connect", "Bot session", "scripts/connect.mjs", false, true),
    SENDCMD("sendcmd", "Send commands", "scripts/sendcmd.mjs", true, false),
    BRUTE_AUTH("brute_auth", "Auth brute", "scripts/brute_auth.mjs", true, false);

    public final String id;
    public final String label;
    public final String script;
    public final boolean needsUpload;
    public final boolean interactiveBot;

    McpToolsTool(String id, String label, String script, boolean needsUpload, boolean interactiveBot) {
        this.id = id;
        this.label = label;
        this.script = script;
        this.needsUpload = needsUpload;
        this.interactiveBot = interactiveBot;
    }

    public static McpToolsTool fromId(String id) {
        if (id == null) {
            return SERVER_RESPONSE;
        }
        for (McpToolsTool t : values()) {
            if (t.id.equalsIgnoreCase(id.trim())) {
                return t;
            }
        }
        return SERVER_RESPONSE;
    }

    public McpToolsTool next() {
        McpToolsTool[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
