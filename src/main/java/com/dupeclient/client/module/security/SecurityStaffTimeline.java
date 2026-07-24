package com.dupeclient.client.module.security;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class SecurityStaffTimeline {
    private static final int MAX = 48;
    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private SecurityStaffTimeline() {
    }

    public record Entry(long atMs, String message) {
        public String line() {
            return FMT.format(Instant.ofEpochMilli(atMs)) + " — " + message;
        }
    }

    public static void record(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        synchronized (ENTRIES) {
            ENTRIES.addFirst(new Entry(System.currentTimeMillis(), message));
            while (ENTRIES.size() > MAX) {
                ENTRIES.removeLast();
            }
        }
    }

    public static List<Entry> snapshot() {
        synchronized (ENTRIES) {
            return new ArrayList<>(ENTRIES);
        }
    }

    public static void clear() {
        synchronized (ENTRIES) {
            ENTRIES.clear();
        }
    }
}
