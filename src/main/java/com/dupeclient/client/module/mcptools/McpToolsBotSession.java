package com.dupeclient.client.module.mcptools;

import com.dupeclient.client.DupeClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Long-running connect.mjs process with stdin command channel. */
public final class McpToolsBotSession {
    private final Object lock = new Object();
    private Process process;
    private OutputStream stdin;
    private volatile boolean active;

    public boolean isActive() {
        return active;
    }

    public void start(
            String host,
            int port,
            String username,
            String mcVersion,
            int loginTimeoutSec,
            Consumer<String> logLine,
            Runnable onExit) {
        synchronized (lock) {
            if (active) {
                return;
            }
        }
        Thread.startVirtualThread(() -> {
            try {
                if (!McpToolsBundleSync.isBundleReady()) {
                    logLine.accept("Bundle missing — sync from presence first.");
                    onExit.run();
                    return;
                }
                if (!McpToolsLocalRunner.isNodeAvailable()) {
                    logLine.accept("Node.js not found on PATH.");
                    onExit.run();
                    return;
                }
                McpToolsLocalRunner.ensureDependencies(line -> {
                    if (!McpToolsBotLogFilter.isNoise(line)) {
                        logLine.accept(line);
                    }
                });
                List<String> cmd = new ArrayList<>();
                cmd.add(McpToolsLocalRunner.nodeCommand());
                cmd.add("scripts/connect.mjs");
                cmd.add(host);
                cmd.add(String.valueOf(port));
                cmd.add(username);
                cmd.add(mcVersion);
                cmd.add(String.valueOf(Math.max(30, Math.min(600, loginTimeoutSec))));
                cmd.add("4");
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(McpToolsBundleSync.bundleRoot().toFile());
                pb.redirectErrorStream(true);
                pb.redirectInput(ProcessBuilder.Redirect.PIPE);
                Process proc = pb.start();
                synchronized (lock) {
                    process = proc;
                    stdin = proc.getOutputStream();
                    active = true;
                }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains("bot has connected")) {
                            // fleet marks connected externally via log sniff or we could callback
                        }
                        logLine.accept(line);
                    }
                }
                proc.waitFor(30, TimeUnit.SECONDS);
                if (proc.isAlive()) {
                    proc.destroyForcibly();
                }
            } catch (Exception e) {
                logLine.accept("Bot session failed: "
                        + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                DupeClient.LOGGER.warn("MCPTools bot session failed", e);
            } finally {
                synchronized (lock) {
                    active = false;
                    process = null;
                    stdin = null;
                }
                onExit.run();
            }
        });
    }

    public void sendLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        synchronized (lock) {
            if (!active || stdin == null) {
                return;
            }
            try {
                String lineEnding = System.getProperty("os.name", "").toLowerCase().contains("win")
                        ? "\r\n"
                        : "\n";
                stdin.write((line.strip() + lineEnding).getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            } catch (IOException e) {
                DupeClient.LOGGER.warn("MCPTools bot stdin write failed", e);
            }
        }
    }

    public void sendChat(String message) {
        sendLine(message);
    }

    public void sendDotCommand(String command, String... args) {
        StringBuilder sb = new StringBuilder(command.startsWith(".") ? command : "." + command);
        for (String arg : args) {
            if (arg != null && !arg.isBlank()) {
                sb.append(' ').append(arg.trim());
            }
        }
        sendLine(sb.toString());
    }

    public void stop() {
        synchronized (lock) {
            if (!active) {
                return;
            }
            try {
                if (stdin != null) {
                    stdin.write("exit\n".getBytes(StandardCharsets.UTF_8));
                    stdin.flush();
                }
            } catch (IOException ignored) {
            }
            Process p = process;
            if (p != null && p.isAlive()) {
                p.destroy();
                try {
                    if (!p.waitFor(3, TimeUnit.SECONDS)) {
                        p.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    p.destroyForcibly();
                }
            }
            active = false;
            process = null;
            stdin = null;
        }
    }
}
