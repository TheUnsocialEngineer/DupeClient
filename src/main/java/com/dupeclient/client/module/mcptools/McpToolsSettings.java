package com.dupeclient.client.module.mcptools;

import org.lwjgl.glfw.GLFW;

public final class McpToolsSettings {
    public boolean enabled = true;
    public boolean overlayVisible = false;
    public int overlayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int overlayX = 24;
    public int overlayY = 96;
    public boolean moduleChatFeedback = true;

    /** Run Node tools on the presence server (remote). When false, runs locally after bundle sync. */
    public boolean remoteRunner = false;

    public String lastHost = "127.0.0.1";
    public int lastPort = 25565;
    public String lastUsername = "MCPToolBot";
    /** Selected Minecraft release (e.g. 1.21.11). */
    public String lastMcVersion = McpToolsMcVersion.DEFAULT.id;
    /** @deprecated Legacy protocol field — migrated to {@link #lastMcVersion}. */
    @Deprecated
    public String lastVersion = McpToolsMcVersion.DEFAULT.protocol;
    public String selectedToolId = McpToolsTool.SERVER_RESPONSE.id;

    /** Optional multiline payload for sendcmd / brute_auth. */
    public String uploadText = "";

    /** Who receives bot actions: SELECTED or ALL. */
    public String botActionTarget = McpToolsBotActionTarget.SELECTED.name();

    /** Default count when joining bots (overlay Join # field and batch connect). */
    public int botSpawnCount = 1;

    /** Keep-alive / login timeout for bot sessions (seconds, 30–600). */
    public int botLoginTimeoutSec = 120;

    /** Delay between batch bot joins to avoid connection throttling (seconds, 1–60). */
    public int botJoinDelaySec = 2;
}
