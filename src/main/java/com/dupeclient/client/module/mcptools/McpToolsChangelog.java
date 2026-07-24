package com.dupeclient.client.module.mcptools;

import com.dupeclient.client.config.DupeClientConfigDir;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class McpToolsChangelog {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SEEN_FILE = DupeClientConfigDir.root().resolve("mcptools_changelog_seen.json");

    private McpToolsChangelog() {
    }

    public record Notice(String version, String body) {
    }

    public static Notice pendingNotice(String remoteVersion) {
        if (remoteVersion == null || remoteVersion.isBlank()) {
            return null;
        }
        String seen = loadSeenVersion();
        if (remoteVersion.equals(seen)) {
            return null;
        }
        return new Notice(remoteVersion, "MCPTools bundle updated to v" + remoteVersion + ". Review bot manifest before deploying.");
    }

    public static void markSeen(String version) {
        if (version == null || version.isBlank()) {
            return;
        }
        try {
            Files.createDirectories(SEEN_FILE.getParent());
            Files.writeString(SEEN_FILE, GSON.toJson(new Seen(version)));
        } catch (IOException ignored) {
        }
    }

    private static String loadSeenVersion() {
        if (!Files.exists(SEEN_FILE)) {
            return "";
        }
        try {
            Seen seen = GSON.fromJson(Files.readString(SEEN_FILE), Seen.class);
            return seen != null && seen.version != null ? seen.version : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static final class Seen {
        String version;

        Seen(String version) {
            this.version = version;
        }
    }
}
