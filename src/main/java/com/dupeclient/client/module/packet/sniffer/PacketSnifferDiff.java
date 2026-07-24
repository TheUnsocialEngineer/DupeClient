package com.dupeclient.client.module.packet.sniffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PacketSnifferDiff {
    private PacketSnifferDiff() {
    }

    public record DiffLine(String kind, String text) {
    }

    public static List<DiffLine> compareSessions(List<PacketSnifferEntry> a, List<PacketSnifferEntry> b) {
        List<DiffLine> out = new ArrayList<>();
        Map<String, Integer> countA = signatureCounts(a);
        Map<String, Integer> countB = signatureCounts(b);
        Set<String> keys = new HashSet<>();
        keys.addAll(countA.keySet());
        keys.addAll(countB.keySet());
        List<String> sorted = new ArrayList<>(keys);
        sorted.sort(String::compareTo);
        for (String key : sorted) {
            int ca = countA.getOrDefault(key, 0);
            int cb = countB.getOrDefault(key, 0);
            if (ca == cb) {
                continue;
            }
            if (ca == 0) {
                out.add(new DiffLine("+", key + " x" + cb));
            } else if (cb == 0) {
                out.add(new DiffLine("-", key + " x" + ca));
            } else {
                out.add(new DiffLine("~", key + " " + ca + "→" + cb));
            }
        }
        if (out.isEmpty()) {
            out.add(new DiffLine("=", "No signature differences"));
        }
        return out;
    }

    public static List<PacketSnifferEntry> filterDirection(List<PacketSnifferEntry> entries, boolean c2s) {
        List<PacketSnifferEntry> out = new ArrayList<>();
        for (PacketSnifferEntry e : entries) {
            if (e.isC2s() == c2s) {
                out.add(e);
            }
        }
        return out;
    }

    private static Map<String, Integer> signatureCounts(List<PacketSnifferEntry> entries) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (PacketSnifferEntry e : entries) {
            String sig = signature(e);
            out.merge(sig, 1, Integer::sum);
        }
        return out;
    }

    private static String signature(PacketSnifferEntry e) {
        String dir = e.isC2s() ? "C2S" : "S2C";
        String name = e.name == null ? "?" : e.name;
        String detail = e.detail == null ? "" : e.detail;
        if (detail.length() > 80) {
            detail = detail.substring(0, 80);
        }
        return dir + " " + name + " | " + detail.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
