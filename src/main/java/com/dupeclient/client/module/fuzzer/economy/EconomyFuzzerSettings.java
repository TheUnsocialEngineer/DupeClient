package com.dupeclient.client.module.fuzzer.economy;

import org.lwjgl.glfw.GLFW;

public final class EconomyFuzzerSettings {
    public boolean enabled = true;
    public boolean overlayVisible = false;
    public int overlayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int overlayX = 12;
    public int overlayY = 80;

    public String targetPlayer = "";
    public String payCommand = "pay";
    /** {@code auto}, {@code player_amount}, {@code amount_player}, or {@code amount_only}. */
    public String syntaxMode = "auto";
    /** Legacy; migrated to {@link #syntaxMode} on load. */
    public boolean reverseSyntax;
    public long delayMs = 750L;
    public long responseWaitMs = 400L;
    public long responseTimeoutMs = 3500L;

    public boolean moduleChatFeedback = true;
    public boolean disableOnLeave = true;

    /** Active fuzzer tab: economy, sqli, minimessage */
    public String fuzzerTab = "economy";

    public String sqliCommand = "";
    public long sqliDelayMs = 750L;
    /** Include DROP / DELETE / EXEC and other non-enumeration SQLI probes. */
    public boolean sqliDestructivePayloads;

    public String minimessageTarget = "";
    public long minimessageDelayMs = 750L;
    /** MiniMessage send channel: {@code msg} (private /msg) or {@code chat} (public chat). */
    public String minimessageSendMode = "msg";
    /** Slash command used when {@link #minimessageSendMode} is {@code msg} (e.g. msg, tell, w). */
    public String minimessageMsgCommand = "msg";
}
