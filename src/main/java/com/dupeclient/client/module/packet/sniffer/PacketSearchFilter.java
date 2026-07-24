package com.dupeclient.client.module.packet.sniffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses overlay search queries with optional {@code +include} and {@code -exclude} terms.
 * Terms are matched against packet name, detail, editable data, and direction label.
 */
public final class PacketSearchFilter {
    private PacketSearchFilter() {
    }

    public static boolean matches(PacketSnifferEntry entry, String rawQuery) {
        if (entry == null) {
            return false;
        }
        if (rawQuery == null || rawQuery.isBlank()) {
            return true;
        }
        Parsed parsed = parse(rawQuery);
        String haystack = (
                entry.name + " " + entry.detail + " " + entry.editableText + " " + entry.direction.label
        ).toLowerCase(Locale.ROOT);

        for (String exclude : parsed.excludes) {
            if (containsTerm(haystack, entry.name, exclude)) {
                return false;
            }
        }
        if (parsed.includes.isEmpty()) {
            return true;
        }
        for (String include : parsed.includes) {
            if (!containsTerm(haystack, entry.name, include)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsTerm(String haystack, String packetName, String term) {
        if (term.isEmpty()) {
            return true;
        }
        String lowerName = packetName.toLowerCase(Locale.ROOT);
        return haystack.contains(term) || lowerName.contains(term);
    }

    public static Parsed parse(String rawQuery) {
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return new Parsed(includes, excludes);
        }
        for (String token : rawQuery.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (token.startsWith("-") && token.length() > 1) {
                excludes.add(token.substring(1).toLowerCase(Locale.ROOT));
            } else if (token.startsWith("+") && token.length() > 1) {
                includes.add(token.substring(1).toLowerCase(Locale.ROOT));
            } else {
                includes.add(token.toLowerCase(Locale.ROOT));
            }
        }
        return new Parsed(includes, excludes);
    }

    public record Parsed(List<String> includes, List<String> excludes) {
    }
}
