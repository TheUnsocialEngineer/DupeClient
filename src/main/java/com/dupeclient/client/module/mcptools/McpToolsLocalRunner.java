package com.dupeclient.client.module.mcptools;

import com.dupeclient.client.DupeClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class McpToolsLocalRunner {
    private static final Object NPM_INSTALL_LOCK = new Object();
    private static volatile boolean npmReady;
    private static final String[] REQUIRED_NPM_PACKAGES = {
            "mineflayer",
            "mineflayer-pathfinder",
            "mineflayer-collectblock",
    };

    private McpToolsLocalRunner() {
    }

    public static boolean isNodeAvailable() {
        try {
            Process p = new ProcessBuilder(nodeCommand(), "--version").redirectErrorStream(true).start();
            boolean ok = p.waitFor(8, TimeUnit.SECONDS) && p.exitValue() == 0;
            if (p.isAlive()) {
                p.destroyForcibly();
            }
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    public static void runAsync(
            McpToolsTool tool,
            McpToolsSettings settings,
            Consumer<String> logLine,
            Consumer<Integer> onComplete) {
        Thread.startVirtualThread(() -> {
            int code = -1;
            try {
                if (!McpToolsBundleSync.isBundleReady()) {
                    logLine.accept("Bundle missing — sync from presence first.");
                    onComplete.accept(-1);
                    return;
                }
                if (!isNodeAvailable()) {
                    logLine.accept("Node.js not found on PATH.");
                    onComplete.accept(-1);
                    return;
                }
                maybeNpmInstall(logLine);
                List<String> cmd = buildCommand(tool, settings);
                logLine.accept("$ " + String.join(" ", cmd));
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(McpToolsBundleSync.bundleRoot().toFile());
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logLine.accept(line);
                    }
                }
                proc.waitFor(120, TimeUnit.SECONDS);
                code = proc.exitValue();
                if (proc.isAlive()) {
                    proc.destroyForcibly();
                    logLine.accept("Process timed out (120s).");
                    code = -1;
                }
            } catch (Exception e) {
                logLine.accept("Run failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                DupeClient.LOGGER.warn("MCPTools local run failed", e);
            }
            onComplete.accept(code);
        });
    }

    public static void ensureDependencies(Consumer<String> logLine) throws IOException, InterruptedException {
        maybeNpmInstall(logLine);
    }

    /** Call after bundle sync so new package.json dependencies get installed. */
    public static void invalidateDependencyCache() {
        npmReady = false;
    }

    public static String nodeCommand() {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "node.exe" : "node";
    }

    private static void maybeNpmInstall(Consumer<String> logLine) throws IOException, InterruptedException {
        Path root = McpToolsBundleSync.bundleRoot();
        if (npmReady && dependenciesSatisfied(root)) {
            return;
        }
        synchronized (NPM_INSTALL_LOCK) {
            if (npmReady && dependenciesSatisfied(root)) {
                return;
            }
            if (!Files.isRegularFile(root.resolve("package.json"))) {
                return;
            }
            if (Files.isDirectory(root.resolve("node_modules")) && !dependenciesSatisfied(root)) {
                logLine.accept("Installing missing MCPTools npm dependencies…");
            } else {
                logLine.accept("Running npm install (first use)…");
            }
            ProcessBuilder pb = new ProcessBuilder(npmCommand(), "install", "--omit=dev", "--no-audit", "--no-fund");
            pb.directory(root.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("added ") && line.contains("packages")) {
                        logLine.accept("npm install complete.");
                        break;
                    }
                }
            }
            p.waitFor(180, TimeUnit.SECONDS);
            if (p.isAlive()) {
                p.destroyForcibly();
                throw new IOException("npm install timed out");
            }
            if (p.exitValue() != 0) {
                throw new IOException("npm install failed (" + p.exitValue() + ")");
            }
            if (!dependenciesSatisfied(root)) {
                throw new IOException("npm install finished but pathfinder packages are still missing");
            }
            npmReady = true;
        }
    }

    private static boolean dependenciesSatisfied(Path root) {
        Path modules = root.resolve("node_modules");
        if (!Files.isDirectory(modules)) {
            return false;
        }
        for (String pkg : REQUIRED_NPM_PACKAGES) {
            if (!Files.isDirectory(modules.resolve(pkg))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> buildCommand(McpToolsTool tool, McpToolsSettings settings) throws IOException {
        McpToolsServerAddress target = McpToolsServerAddress.resolve(settings.lastHost, settings.lastPort);
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add(nodeCommand());
        cmd.add(tool.script);
        cmd.add(target.host());
        cmd.add(String.valueOf(target.port()));
        cmd.add(settings.lastUsername.trim());
        cmd.add(McpToolsMcVersion.resolveForSettings(settings));
        if (tool.needsUpload) {
            Path tmp = McpToolsBundleSync.bundleRoot().resolve(".runtime");
            Files.createDirectories(tmp);
            Path payload = tmp.resolve(tool.id + "_payload.txt");
            Files.writeString(payload, settings.uploadText == null ? "" : settings.uploadText, StandardCharsets.UTF_8);
            cmd.add(payload.toAbsolutePath().toString());
        }
        return cmd;
    }

    private static String npmCommand() {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "npm.cmd" : "npm";
    }
}
