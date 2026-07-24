package com.dupeclient.client.multiplayer;

import java.util.UUID;

public record ProxyProfile(
    String id,
    String name,
    String host,
    int port,
    ProxyType type,
    String username,
    String password
) {
    public static ProxyProfile create(String name, String host, int port, ProxyType type, String username, String password) {
        String cleanName = name == null || name.isBlank() ? host + ":" + port : name.trim();
        return new ProxyProfile(
            UUID.randomUUID().toString(),
            cleanName,
            host == null ? "" : host.trim(),
            port,
            type == null ? ProxyType.SOCKS5 : type,
            username == null ? "" : username.trim(),
            password == null ? "" : password
        );
    }

    public String displayLabel() {
        return name + " (" + type.label + " " + host + ":" + port + ")";
    }
}
