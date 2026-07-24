package com.dupeclient.client.module.mcptools;

import com.dupeclient.client.config.DupeClientConfigDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

public final class McpToolsBundleSync {
    private McpToolsBundleSync() {
    }

    public static Path bundleRoot() {
        return DupeClientConfigDir.root().resolve("mcptools").resolve("bundle");
    }

    public static boolean isBundleReady() {
        return Files.isRegularFile(bundleRoot().resolve("scripts").resolve("bot.mjs"));
    }

    public static String syncFromPresence(McpToolsManager.ProgressSink sink) throws IOException {
        McpToolsPresenceApi.Manifest manifest = McpToolsPresenceApi.fetchManifest();
        Path root = bundleRoot();
        Files.createDirectories(root);
        int done = 0;
        int total = manifest.files().size();
        for (McpToolsPresenceApi.ManifestFile file : manifest.files()) {
            done++;
            if (sink != null) {
                sink.onProgress("Sync " + done + "/" + total + ": " + file.path());
            }
            Path target = root.resolve(file.path().replace('/', java.io.File.separatorChar));
            Files.createDirectories(target.getParent());
            if (Files.isRegularFile(target)) {
                String existing = sha256(Files.readAllBytes(target));
                if (existing.equalsIgnoreCase(file.sha256())) {
                    continue;
                }
            }
            byte[] data = McpToolsPresenceApi.fetchFile(file.path());
            String hash = sha256(data);
            if (!hash.equalsIgnoreCase(file.sha256())) {
                throw new IOException("Checksum mismatch for " + file.path());
            }
            Files.write(target, data);
        }
        ensurePackageJson(root);
        return manifest.bundleVersion();
    }

    private static void ensurePackageJson(Path root) throws IOException {
        Path pkg = root.resolve("package.json");
        if (!Files.isRegularFile(pkg)) {
            Files.writeString(pkg, """
                    {
                      "name": "mcptool",
                      "version": "1.1.0",
                      "type": "module",
                      "dependencies": {
                        "minecraft-colors": "^1.0.1",
                        "minecraft-protocol": "^1.47.0",
                        "mineflayer": "^4.20.1",
                        "mineflayer-collectblock": "^1.4.1",
                        "mineflayer-pathfinder": "^2.4.5"
                      }
                    }
                    """, StandardCharsets.UTF_8);
        }
    }

    private static String sha256(byte[] data) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
