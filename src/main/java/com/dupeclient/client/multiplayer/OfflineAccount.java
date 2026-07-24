package com.dupeclient.client.multiplayer;

import java.util.UUID;

public record OfflineAccount(String username, UUID uuid) {
    public static OfflineAccount ofUsername(String username) {
        String clean = username == null ? "" : username.trim();
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + clean).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new OfflineAccount(clean, offlineUuid);
    }
}
