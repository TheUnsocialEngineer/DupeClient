package com.dupeclient.client.module.dupedb;

import org.lwjgl.glfw.GLFW;

public class DupedbSettings {
    public boolean overlayVisible = false;
    public int overlayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int overlayX = 24;
    public int overlayY = 64;
    /** OAuth app slug (default {@code dupeclient} when blank). Used by the DupeDB Java SDK. */
    public String oauthAppId = "";
    public DupedbMode mode = DupedbMode.COMMAND;
    public int probeDelayMs = 50;
    public boolean announceNoMatches = true;
    /** Chat lines for DupeDB actions and status (not errors / exploit links). */
    public boolean moduleChatFeedback = true;
    /** After a plugin scan, compute and announce a weighted pay-to-win score. */
    public boolean generateP2wScore = false;
    /** Periodically rescan the current server while connected. */
    public boolean backgroundScanEnabled = false;
    /** Minutes between background scans (minimum 5). */
    public int backgroundScanIntervalMinutes = 30;

    /** @deprecated Legacy field — migrated to {@code dupedb-token.json} on load. */
    @Deprecated
    public String oauthToken = "";
    /** @deprecated Legacy field — migrated to {@code dupedb-token.json} on load. */
    @Deprecated
    public String oauthRefreshToken = "";
    /** @deprecated Legacy field — migrated to {@code dupedb-token.json} on load. */
    @Deprecated
    public long oauthAccessTokenExpiresAtMs = 0L;
}
