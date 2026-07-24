package com.dupeclient.client.module.security;

public final class SecuritySettings {
    public enum OpsecBrandMode { VANILLA, FABRIC }
    public enum OpsecWhitelistMode { OFF, AUTO, CUSTOM }

    public boolean moduleChatFeedback = true;
    public boolean showToasts = true;
    public boolean logDetections = true;

    public boolean telemetryBlocking = true;
    public boolean blockLocalPackUrls = true;
    public boolean noChatRestrictions;
    public boolean keyProbeAlerts = true;

    /**
     * When true, {@code key.*} translations and keybind text that look mod-specific can be spoofed while you are in a
     * remote multiplayer world (not title screen, menus, or singleplayer) where fingerprinting matters.
     */
    public boolean keyResolutionProtection = true;
    /** OpSec parity: spoof vanilla keybind probes to default vanilla values instead of current binds. */
    public boolean opsecFakeDefaultKeybinds = true;
    /** OpSec parity: key-resolution behavior mode. VANILLA blocks mod keys broadly; FABRIC allows Fabric/whitelisted keys. */
    public OpsecBrandMode opsecBrandMode = OpsecBrandMode.FABRIC;
    /** OpSec parity: whitelist mode used by key-resolution checks. */
    public OpsecWhitelistMode opsecWhitelistMode = OpsecWhitelistMode.AUTO;
    /** OpSec parity: custom whitelist mod fragments (csv), used when mode is CUSTOM. */
    public String opsecWhitelistedModsCsv = "voicechat,xaero,minimap";

    /**
     * When true, unmarked vanilla "fake default" keybind spoofing is limited to server-tagged text (signs / packets).
     * Mod/segment key probes (e.g. {@code key.*.*} mod IDs) are always spoofed in remote MP whether tagged or not.
     */
    public boolean keyResolutionServerMarkedOnly = true;

    /**
     * When true, the client ignores the sign-editor open packet if front/back sign text would trigger mod
     * key-resolution fingerprinting (OpSec-style hard block instead of only spoofing).
     */
    public boolean keyResolutionBlockSignEditorOnKeyProbe = false;

    // Staff detection / watchlist
    public boolean staffDetectionEnabled = true;
    public boolean staffGlowEnabled = false;
    public boolean staffDetectedAlerts = true;
    public boolean staffOnlineOfflineAlerts = true;
    /** Alert when a staff member's entity is within {@link #staffProximityRadius} blocks. */
    public boolean staffProximityAlerts = true;
    /** Block radius for staff proximity alerts (8–256). */
    public int staffProximityRadius = 64;
    /** Alert when a player was visible nearby and disappears from the world while still on tab. */
    /** Alert when nearby player entities are invisible or have Invisibility effect. */
    public boolean antiInvisibleEntities = true;
    public String staffRolePrefixesCsv = "jr,sr";
    public String staffRoleSuffixesCsv = "owner,moderator,mod,helper,developer,dev";
    /** Extra direct keywords (not generated via prefix+suffix), comma-separated. */
    public String staffRankKeywordsCsv = "admin,staff";

    /**
     * Multi-segment {@code key.*} prefixes that may resolve normally when they match vanilla-style paths
     * (e.g. {@code key.categories.}, {@code key.keyboard.}).
     */
    public String keyResolutionAllowedPrefixesCsv = "key.categories.,key.mouse.,key.keyboard.,key.modifier.,key.fabric.,key.hotbar.";

    /** If the translation key contains any of these (case-insensitive), force spoofing. */
    public String keyResolutionBlockedFragmentsCsv = "meteor,meteor-client,baritone,wurst,litematica,xaero,minimap,iris,sodium,replay,dupeclient,ui_utils,rusher,konas,phobos,inertia,lambda,impact";

    /** Locally replace your username in rendered text (chat, tab, signs, etc.). */
    public boolean nameChangerEnabled = false;
    public String nameChangerDisplayName = "Duper";
    /** Censor the real username instead of replacing with {@link #nameChangerDisplayName}. */
    public boolean nameChangerCensor = false;
    /** Only apply the name changer while in a world (not menus/title). */
    public boolean nameChangerOnlyInGame = false;
    /** Remove block texture rotation variance (position-based rendering seed). */
    public boolean noTextureRotations = false;
    /** Apply stored OpSec profile when joining a server. */
    public boolean profileAutoSwitchPerServer = true;
}
