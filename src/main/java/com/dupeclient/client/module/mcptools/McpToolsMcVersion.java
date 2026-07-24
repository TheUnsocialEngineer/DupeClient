package com.dupeclient.client.module.mcptools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Minecraft release labels mapped to mineflayer protocol version strings. */
public enum McpToolsMcVersion {
    V1_20_1("1.20.1", "763"),
    V1_20_2("1.20.2", "764"),
    V1_20_4("1.20.4", "765"),
    V1_20_5("1.20.5", "766"),
    V1_20_6("1.20.6", "766"),
    V1_21("1.21", "767"),
    V1_21_1("1.21.1", "768"),
    V1_21_2("1.21.2", "768"),
    V1_21_3("1.21.3", "768"),
    V1_21_4("1.21.4", "769"),
    V1_21_5("1.21.5", "770"),
    V1_21_6("1.21.6", "771"),
    V1_21_7("1.21.7", "772"),
    V1_21_8("1.21.8", "773"),
    V1_21_9("1.21.9", "774"),
    V1_21_10("1.21.10", "775"),
    V1_21_11("1.21.11", "776");

    public static final McpToolsMcVersion DEFAULT = V1_21_11;

    public final String id;
    public final String protocol;

    McpToolsMcVersion(String id, String protocol) {
        this.id = id;
        this.protocol = protocol;
    }

    public String dropdownLabel() {
        return id + "  ·  proto " + protocol;
    }

    public String mineflayerVersion() {
        return id;
    }

    public static McpToolsMcVersion fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String norm = raw.trim();
        for (McpToolsMcVersion v : values()) {
            if (v.id.equalsIgnoreCase(norm) || v.protocol.equals(norm)) {
                return v;
            }
        }
        return DEFAULT;
    }

    public static McpToolsMcVersion fromDropdownLabel(String label) {
        if (label == null || label.isBlank()) {
            return DEFAULT;
        }
        String trimmed = label.trim();
        for (McpToolsMcVersion v : values()) {
            if (v.dropdownLabel().equalsIgnoreCase(trimmed)) {
                return v;
            }
        }
        int dot = trimmed.indexOf(' ');
        if (dot > 0) {
            return fromId(trimmed.substring(0, dot).trim());
        }
        return fromId(trimmed);
    }

    public static McpToolsMcVersion migrateLegacy(String legacyVersion) {
        if (legacyVersion == null || legacyVersion.isBlank()) {
            return DEFAULT;
        }
        String norm = legacyVersion.trim();
        if (norm.chars().allMatch(Character::isDigit)) {
            for (McpToolsMcVersion v : values()) {
                if (v.protocol.equals(norm)) {
                    return v;
                }
            }
        }
        return fromId(norm);
    }

    public static List<String> dropdownLabels() {
        List<String> out = new ArrayList<>(values().length);
        for (McpToolsMcVersion v : values()) {
            out.add(v.dropdownLabel());
        }
        return out;
    }

    public McpToolsMcVersion next() {
        McpToolsMcVersion[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static String resolveForSettings(McpToolsSettings settings) {
        if (settings == null) {
            return DEFAULT.mineflayerVersion();
        }
        if (settings.lastMcVersion != null && !settings.lastMcVersion.isBlank()) {
            return fromId(settings.lastMcVersion).mineflayerVersion();
        }
        return migrateLegacy(settings.lastVersion).mineflayerVersion();
    }
}
