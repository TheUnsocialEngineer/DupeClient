package com.dupeclient.client.module.security;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SecurityProfileStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = DupeClientConfigDir.root().resolve("security_profiles.json");
    private static final Type MAP_TYPE = new TypeToken<Map<String, SecuritySettings>>() {}.getType();

    private SecurityProfileStore() {
    }

    public static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String s = host.trim().toLowerCase(Locale.ROOT);
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        return s;
    }

    public static Map<String, SecuritySettings> loadAll() {
        if (!Files.exists(FILE)) {
            return new HashMap<>();
        }
        try {
            String raw = Files.readString(FILE);
            Map<String, SecuritySettings> map = GSON.fromJson(raw, MAP_TYPE);
            return map != null ? map : new HashMap<>();
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    public static void saveAll(Map<String, SecuritySettings> map) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(map));
        } catch (IOException ignored) {
        }
    }

    public static SecuritySettings profileForHost(String host) {
        return loadAll().get(normalizeHost(host));
    }

    public static void saveProfileForHost(String host, SecuritySettings settings) {
        Map<String, SecuritySettings> all = loadAll();
        String key = normalizeHost(host);
        if (key.isBlank() || settings == null) {
            return;
        }
        all.put(key, cloneSubset(settings));
        saveAll(all);
    }

    public static void applyProfileTo(SecuritySettings target, SecuritySettings profile) {
        if (target == null || profile == null) {
            return;
        }
        target.keyResolutionProtection = profile.keyResolutionProtection;
        target.opsecFakeDefaultKeybinds = profile.opsecFakeDefaultKeybinds;
        target.opsecBrandMode = profile.opsecBrandMode;
        target.opsecWhitelistMode = profile.opsecWhitelistMode;
        target.opsecWhitelistedModsCsv = profile.opsecWhitelistedModsCsv;
        target.keyResolutionServerMarkedOnly = profile.keyResolutionServerMarkedOnly;
        target.keyResolutionBlockSignEditorOnKeyProbe = profile.keyResolutionBlockSignEditorOnKeyProbe;
        target.staffDetectionEnabled = profile.staffDetectionEnabled;
        target.staffGlowEnabled = profile.staffGlowEnabled;
        target.staffDetectedAlerts = profile.staffDetectedAlerts;
        target.staffOnlineOfflineAlerts = profile.staffOnlineOfflineAlerts;
        target.staffProximityAlerts = profile.staffProximityAlerts;
        target.staffProximityRadius = profile.staffProximityRadius;
        target.nameChangerEnabled = profile.nameChangerEnabled;
        target.nameChangerOnlyInGame = profile.nameChangerOnlyInGame;
        target.nameChangerCensor = profile.nameChangerCensor;
        target.nameChangerDisplayName = profile.nameChangerDisplayName;
    }

    private static SecuritySettings cloneSubset(SecuritySettings src) {
        SecuritySettings out = new SecuritySettings();
        applyProfileTo(out, src);
        return out;
    }
}
