package com.dupeclient.client.module.acaudit;

import java.util.Locale;
import java.util.Set;

public final class AcAuditPluginClassifier {
    private static final Set<String> ANTICHEAT_NAMES = Set.of(
            "nocheatplus", "negativity", "warden", "horizon", "illegalstack", "coreprotect",
            "exploitsx", "vulcan", "abc", "spartan", "kauri", "anticheatreloaded", "wraith",
            "antixrayheuristics", "grimac", "grim", "themis", "foxaddition", "guardianac",
            "ggintegrity", "lightanticheat", "anarchyexploitfixes", "matrix", "karhu", "verus",
            "aac", "intave", "polar", "anticheat");

    private AcAuditPluginClassifier() {
    }

    public static String classifyPlatform(String brand) {
        if (brand == null || brand.isBlank()) {
            return "unknown";
        }
        String s = brand.toLowerCase(Locale.ROOT);
        if (s.contains("folia")) {
            return "Folia (regionized threading)";
        }
        if (s.contains("purpur")) {
            return "Purpur (Paper fork)";
        }
        if (s.contains("paper")) {
            return "Paper (packet-limiter + protections)";
        }
        if (s.contains("spigot")) {
            return "Spigot (Bukkit, fewer built-in fixes)";
        }
        if (s.contains("bukkit")) {
            return "CraftBukkit";
        }
        if (s.contains("fabric")) {
            return "Fabric (modded)";
        }
        if (s.contains("forge") || s.contains("neoforge")) {
            return "Forge/NeoForge (modded)";
        }
        if (s.equals("vanilla")) {
            return "vanilla";
        }
        return "unrecognized brand";
    }

    public static boolean isAnticheatNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return false;
        }
        String key = namespace.toLowerCase(Locale.ROOT);
        if (ANTICHEAT_NAMES.contains(key)) {
            return true;
        }
        return key.contains("cheat") || key.contains("anticheat") || key.contains("ac");
    }

    public static boolean looksLikePacketLimiterKick(String reason) {
        if (reason == null) {
            return false;
        }
        String r = reason.toLowerCase(Locale.ROOT);
        return r.contains("packet") || r.contains("spam") || r.contains("too many") || r.contains("limit");
    }
}
