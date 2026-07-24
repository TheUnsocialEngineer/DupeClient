package com.dupeclient.client.module.security;

import com.dupeclient.client.DupeClient;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.entity.SignText;

/**
 * Key-resolution protection aligned with OpSec goals: spoof {@code key.*} translations that look mod-specific,
 * prefer JSON fallbacks when present (vanilla behavior for unknown keys). Spoofing only runs while the player is
 * in a <em>remote</em> multiplayer session (world loaded, not integrated singleplayer) — where server-side fingerprinting
 * is a concern — not on the title screen, in menus, or in singleplayer. When {@link SecuritySettings#keyResolutionServerMarkedOnly}
 * is enabled, only server-marked text is spoofed within that session.
 */
public final class SecurityKeyResolution {
    private SecurityKeyResolution() {
    }

    /**
     * True when the local client has joined a non-integrated server world (player spawned). False on title screen,
     * loading, replay, and singleplayer/LAN host.
     */
    private static boolean inRemoteMultiplayerPlay() {
        Minecraft c = Minecraft.getInstance();
        if (c.level == null || c.player == null) {
            return false;
        }
        return !c.hasSingleplayerServer();
    }

    /** True when connected to a remote server world (alerts and sign exploit UI use this). */
    public static boolean inRemoteMultiplayer() {
        return inRemoteMultiplayerPlay();
    }

    public static boolean shouldApplySpoof(TranslatableContents content) {
        SecuritySettings s = SecurityManager.INSTANCE.getSettings();
        if (!s.keyResolutionProtection || !shouldSpoofTranslationKey(content.getKey())) {
            return false;
        }
        if (!inRemoteMultiplayerPlay()) {
            return false;
        }
        // We only reach here for mod/segment `key.*` probes; OpSec always spoofs them on MP. Server marking is
        // best-effort (PacketContext / sign mark) and must not block when marking missed (re-parsed sign text, etc.).
        return true;
    }

    public static boolean shouldApplySpoof(KeybindContents content) {
        SecuritySettings s = SecurityManager.INSTANCE.getSettings();
        String id = content.getName();
        if (!s.keyResolutionProtection || !shouldSpoofKeybindId(id)) {
            return false;
        }
        if (!inRemoteMultiplayerPlay()) {
            return false;
        }
        if (s.keyResolutionServerMarkedOnly) {
            if (content instanceof SecurityFromServerPacket p && p.dupeclient$isFromServerPacket()) {
                return true;
            }
            // Mod/segment key probes: spoof even if PacketContext / sign mark missed the instance.
            if (shouldSpoofTranslationKey(id)) {
                return true;
            }
            // Fake-default for vanilla `key.*` only: require explicit server / sign mark so local UIs are unchanged.
            return false;
        }
        return true;
    }

    public static boolean shouldSpoofTranslationKey(String key) {
        SecuritySettings s = SecurityManager.INSTANCE.getSettings();
        if (!s.keyResolutionProtection || key == null || key.isBlank()) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("key.")) {
            return false;
        }
        if (isWhitelistedByOpsecMode(lower)) {
            return false;
        }
        if (s.opsecBrandMode == SecuritySettings.OpsecBrandMode.FABRIC && lower.startsWith("key.fabric.")) {
            return false;
        }
        for (String frag : parseCsv(s.keyResolutionBlockedFragmentsCsv)) {
            if (lower.contains(frag)) {
                logOnce(lower);
                return true;
            }
        }
        for (String prefix : parseCsv(s.keyResolutionAllowedPrefixesCsv)) {
            if (lower.startsWith(prefix)) {
                return false;
            }
        }
        String rest = lower.substring("key.".length());
        if (!rest.contains(".")) {
            return false;
        }
        logOnce(lower);
        return true;
    }

    public static boolean shouldSpoofKeybindId(String keybindId) {
        SecuritySettings s = SecurityManager.INSTANCE.getSettings();
        if (!s.keyResolutionProtection || keybindId == null || keybindId.isBlank()) {
            return false;
        }
        String lower = keybindId.toLowerCase(Locale.ROOT);
        // Mod / probe keys first: do not use "fake default" from allKeys (Meteor's keys have defaults too).
        if (shouldSpoofTranslationKey(lower)) {
            return true;
        }
        if (s.opsecFakeDefaultKeybinds && SecurityKeybindDefaults.hasDefault(lower)) {
            return true;
        }
        return false;
    }

    public static String replacementForTranslatable(TranslatableContents content) {
        String fb = content.getFallback();
        if (fb != null && !fb.isBlank()) {
            return fb;
        }
        return content.getKey();
    }

    /**
     * @return default display string for fake-default mode, or {@code null} to show the raw key id (e.g. mod probes);
     *         never re-resolve through {@code Text.translatable} for mod keys.
     */
    public static String replacementForKeybind(String keybindId) {
        SecuritySettings s = SecurityManager.INSTANCE.getSettings();
        if (keybindId == null || keybindId.isBlank() || !s.keyResolutionProtection) {
            return null;
        }
        String lower = keybindId.toLowerCase(Locale.ROOT);
        if (shouldSpoofTranslationKey(lower)) {
            return null;
        }
        if (!s.opsecFakeDefaultKeybinds) {
            return null;
        }
        return SecurityKeybindDefaults.getDefault(lower);
    }

    /**
     * Unresolved display for a spoofed keybind visit: fake default, else raw id (OpSec / vanilla-style, no
     * re-resolution to real bindings for mod keys).
     */
    public static String displayStringForSpoofedKeybind(String keybindId) {
        if (keybindId == null || keybindId.isBlank()) {
            return "";
        }
        String d = replacementForKeybind(keybindId);
        return d != null ? d : keybindId;
    }

    /**
     * True if sign text would resolve mod-specific {@code key.*} in a way {@link #shouldSpoofTranslationKey} covers
     * (for blocking sign editor open when that setting is on).
     */
    public static boolean signTextHasKeyResolutionProbe(SignText st) {
        if (st == null) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            Component line = st.getMessage(i, false);
            if (line != null && textTreeHasKeyResolutionProbe(line)) {
                return true;
            }
        }
        return false;
    }

    public static boolean textTreeHasKeyResolutionProbe(Component root) {
        if (root == null) {
            return false;
        }
        ArrayDeque<Component> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Component node = stack.pop();
            ComponentContents content = node.getContents();
            if (content instanceof TranslatableContents t) {
                if (shouldSpoofTranslationKey(t.getKey())) {
                    return true;
                }
                for (Object arg : t.getArgs()) {
                    if (arg instanceof Component argText) {
                        stack.push(argText);
                    }
                }
            } else if (content instanceof KeybindContents k) {
                if (shouldSpoofTranslationKey(k.getName())) {
                    return true;
                }
            }
            for (Component sibling : node.getSiblings()) {
                stack.push(sibling);
            }
        }
        return false;
    }


    private static List<String> parseCsv(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            String t = part.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private static boolean isWhitelistedByOpsecMode(String lowerKey) {
        SecuritySettings s = SecurityManager.INSTANCE.getSettings();
        SecuritySettings.OpsecWhitelistMode mode = s.opsecWhitelistMode == null
                ? SecuritySettings.OpsecWhitelistMode.AUTO : s.opsecWhitelistMode;
        if (mode == SecuritySettings.OpsecWhitelistMode.OFF) {
            return false;
        }
        if (mode == SecuritySettings.OpsecWhitelistMode.AUTO) {
            return lowerKey.contains("fabric")
                    || lowerKey.contains("voicechat")
                    || lowerKey.contains("xaero")
                    || lowerKey.contains("minimap");
        }
        for (String token : parseCsv(s.opsecWhitelistedModsCsv)) {
            if (lowerKey.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static void logOnce(String key) {
        SecuritySettings s = SecurityManager.INSTANCE.getSettings();
        if (!s.logDetections) {
            return;
        }
        DupeClient.LOGGER.debug("[Security] Key resolution spoof for {}", key);
    }
}
