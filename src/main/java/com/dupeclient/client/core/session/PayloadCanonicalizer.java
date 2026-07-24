package com.dupeclient.client.core.session;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class PayloadCanonicalizer {
    private PayloadCanonicalizer() {
    }

    static String staffPayload(long issuedAt, long ttlMs, String nonce, List<String> staffUuids) {
        JsonObject root = new JsonObject();
        root.addProperty("issuedAt", issuedAt);
        root.addProperty("ttlMs", ttlMs);
        root.addProperty("nonce", nonce == null ? "" : nonce);

        JsonArray staff = new JsonArray();
        List<String> sorted = new ArrayList<>(staffUuids);
        sorted.sort(Comparator.naturalOrder());
        for (String uuid : sorted) {
            JsonObject row = new JsonObject();
            row.addProperty("minecraftUuid", uuid);
            staff.add(row);
        }
        root.add("staff", staff);
        return root.toString();
    }

    static String mapPayload(Map<String, String> fields) {
        TreeMap<String, String> sorted = new TreeMap<>(fields);
        JsonObject root = new JsonObject();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            root.addProperty(e.getKey(), e.getValue());
        }
        return root.toString();
    }
}
