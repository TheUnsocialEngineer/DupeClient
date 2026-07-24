package com.dupeclient.client.module.packet.sniffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Search/filter for packet type names with camelCase-aware matching and +/- syntax. */
public final class PacketNameSearch {
    private PacketNameSearch() {
    }

    public static boolean matches(String packetName, String rawQuery) {
        if (packetName == null || packetName.isBlank()) {
            return false;
        }
        if (rawQuery == null || rawQuery.isBlank()) {
            return true;
        }
        PacketSearchFilter.Parsed parsed = PacketSearchFilter.parse(rawQuery);
        String haystack = expand(packetName);
        for (String exclude : parsed.excludes()) {
            if (haystack.contains(exclude)) {
                return false;
            }
        }
        if (parsed.includes().isEmpty()) {
            return true;
        }
        for (String include : parsed.includes()) {
            if (!haystack.contains(include)) {
                return false;
            }
        }
        return true;
    }

    public static List<String> filter(List<String> names, String rawQuery) {
        List<String> out = new ArrayList<>();
        if (names == null) {
            return out;
        }
        for (String name : names) {
            if (name != null && matches(name, rawQuery)) {
                out.add(name);
            }
        }
        return out;
    }

    private static String expand(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length() * 2);
        sb.append(lower).append(' ');
        String[] parts = name.split("(?=[A-Z0-9])|(?<=[a-z])(?=[0-9])");
        for (String part : parts) {
            if (part == null || part.isEmpty()) {
                continue;
            }
            sb.append(part.toLowerCase(Locale.ROOT)).append(' ');
        }
        return sb.toString();
    }
}
