package com.dupeclient.client.module.mcptools;

import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

/** Parses MCPTools host/port fields (supports {@code host:port} in the host box). */
public record McpToolsServerAddress(String host, int port) {
    private static final int DEFAULT_PORT = 25565;

    public static McpToolsServerAddress resolve(String hostInput, int portInput) {
        String host = hostInput == null ? "" : hostInput.trim();
        int port = portInput > 0 && portInput <= 65535 ? portInput : DEFAULT_PORT;

        if (host.isEmpty()) {
            return new McpToolsServerAddress("127.0.0.1", port);
        }

        if (host.startsWith("[")) {
            int close = host.indexOf(']');
            if (close > 1) {
                String ipv6 = host.substring(1, close);
                if (close + 1 < host.length() && host.charAt(close + 1) == ':') {
                    port = parsePort(host.substring(close + 2), port);
                }
                return new McpToolsServerAddress(ipv6, port);
            }
        }

        int colon = host.lastIndexOf(':');
        if (colon > 0 && colon < host.length() - 1) {
            String tail = host.substring(colon + 1).trim();
            if (tail.chars().allMatch(Character::isDigit)) {
                int parsed = parsePort(tail, -1);
                if (parsed > 0) {
                    port = parsed;
                    host = host.substring(0, colon).trim();
                }
            }
        }

        return new McpToolsServerAddress(host.isEmpty() ? "127.0.0.1" : host, port);
    }

    public static void applyToSettings(McpToolsSettings settings) {
        if (settings == null) {
            return;
        }
        McpToolsServerAddress resolved = resolve(settings.lastHost, settings.lastPort);
        settings.lastHost = resolved.host();
        settings.lastPort = resolved.port();
    }

    /** Host/port for the server the client is connected to, if any. */
    @Nullable
    public static McpToolsServerAddress fromConnectedClient(@Nullable MinecraftClient client) {
        if (client == null || client.player == null) {
            return null;
        }
        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address != null) {
            String raw = client.getCurrentServerEntry().address.trim();
            if (!raw.isEmpty()) {
                return resolve(raw, DEFAULT_PORT);
            }
        }
        if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null
                && client.getNetworkHandler().getConnection().getAddress() != null) {
            String raw = client.getNetworkHandler().getConnection().getAddress().toString();
            if (raw != null) {
                raw = raw.replaceFirst("^/", "").trim();
                if (!raw.isEmpty()) {
                    return resolve(raw, DEFAULT_PORT);
                }
            }
        }
        return null;
    }

    private static int parsePort(String raw, int fallback) {
        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed > 0 && parsed <= 65535) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }
}
