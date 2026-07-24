package com.dupeclient.client.module.mcptools;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.serverpassword.RegisterFormat;
import com.dupeclient.client.module.serverpassword.RegisterFormatDetector;
import com.dupeclient.client.module.serverpassword.ServerAuthCommands;
import com.dupeclient.client.module.serverpassword.ServerAuthPrompts;
import com.dupeclient.client.module.serverpassword.VaultInputException;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.function.Consumer;

/** Auto login / register for MCPTools bots — only after server prompts, after resource packs settle. */
public final class McpToolsBotAuthHandler {
    public static final String BOT_PASSWORD = "dupeClientBot";
    private static final int AUTH_SEND_DELAY_MS = 50;
    private static final long RESOURCE_PACK_WAIT_MS = 2_000;
    private static final long NO_AUTH_FALLBACK_MS = 4_000;

    private final SecureRandom random = new SecureRandom();
    private final String registerEmail;

    private RegisterFormat registerFormat = RegisterFormat.PASSWORD_REPEAT;
    private String registerCommand = "register";
    private String loginCommand = "login";
    private volatile boolean preferRegisterFirst;
    private volatile boolean registerAttempted;
    private volatile boolean loginAttempted;
    private volatile boolean authenticated;
    private volatile boolean authPromptSeen;
    private volatile boolean spawnAuthScheduled;
    private volatile boolean resourcePackSettled;
    private volatile long spawnedAtMs;

    public McpToolsBotAuthHandler() {
        this.registerEmail = "bot" + (100_000 + random.nextInt(900_000)) + "@dupeclient.net";
    }

    public void configureForUsername(String username) {
        String name = username == null ? "" : username.toLowerCase(Locale.ROOT);
        preferRegisterFirst = name.startsWith("bot");
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void reset() {
        registerFormat = RegisterFormat.PASSWORD_REPEAT;
        registerCommand = "register";
        loginCommand = "login";
        preferRegisterFirst = false;
        registerAttempted = false;
        loginAttempted = false;
        authenticated = false;
        authPromptSeen = false;
        spawnAuthScheduled = false;
        resourcePackSettled = false;
        spawnedAtMs = 0L;
    }

    public void onSpawnedInWorld(McpToolsBotSession session, String botUsername, Consumer<String> logLine) {
        if (spawnAuthScheduled) {
            return;
        }
        spawnAuthScheduled = true;
        spawnedAtMs = System.currentTimeMillis();
        resourcePackSettled = false;

        scheduleDelayed(session, () -> {
            if (!authenticated && !authPromptSeen && !loginAttempted && !registerAttempted) {
                authenticated = true;
                logLine.accept("No auth required — movement enabled.");
            }
        }, NO_AUTH_FALLBACK_MS);
    }

    public void onChatLine(String rawLine, McpToolsBotSession session, String botUsername, Consumer<String> logLine) {
        if (authenticated || session == null || !session.isActive()) {
            return;
        }
        String line = ServerAuthPrompts.normalizeChatLine(rawLine);
        if (line.isEmpty()) {
            return;
        }
        String lower = ServerAuthPrompts.lower(line);

        if (looksLikeResourcePackSettled(lower)) {
            resourcePackSettled = true;
        }

        updateAuthHints(line, lower);

        if (ServerAuthPrompts.looksLikeLoginSuccess(lower)) {
            authenticated = true;
            logLine.accept("Logged in — movement enabled.");
            return;
        }

        if (ServerAuthPrompts.looksLikeRegisterSuccess(lower)) {
            registerAttempted = true;
            logLine.accept("Registered — logging in…");
            scheduleLogin(session, botUsername, logLine, "post-register");
            return;
        }

        if (ServerAuthPrompts.looksLikeRegisterRejected(lower)) {
            registerAttempted = true;
            logLine.accept("Already registered — logging in…");
            scheduleLogin(session, botUsername, logLine, "already registered");
            return;
        }

        if (ServerAuthPrompts.looksLikeRegisterUsageError(lower)) {
            RegisterFormat next = RegisterFormatDetector.nextFallback(registerFormat);
            if (registerAttempted && next != registerFormat) {
                registerAttempted = false;
                registerFormat = next;
                logLine.accept("Register syntax rejected — retrying as "
                        + describeRegisterFormat(registerFormat) + "…");
                scheduleRegister(session, botUsername, logLine);
            } else if (!registerAttempted) {
                registerFormat = next;
                logLine.accept("Register usage hint — format "
                        + describeRegisterFormat(registerFormat));
                maybeRegisterFromPrompt(session, botUsername, logLine, lower);
            }
            return;
        }

        if (loginAttempted && ServerAuthPrompts.looksLikeLoginFailureNotRegistered(lower)) {
            if (!registerAttempted) {
                logLine.accept("Not registered — registering as "
                        + describeRegisterFormat(registerFormat) + "…");
                scheduleRegister(session, botUsername, logLine);
            }
            return;
        }

        if (ServerAuthPrompts.looksLikeLoginFailure(lower)) {
            logLine.accept("Login failed — wrong password or account locked.");
            return;
        }

        if ((loginAttempted || registerAttempted)
                && (lower.contains("unknown command") || lower.contains("command not found"))) {
            authenticated = true;
            logLine.accept("No auth plugin — movement enabled.");
            return;
        }

        if (ServerAuthPrompts.looksLikeRegisterPrompt(lower)) {
            authPromptSeen = true;
            resourcePackSettled = true;
            maybeRegisterFromPrompt(session, botUsername, logLine, lower);
            return;
        }

        if (ServerAuthPrompts.looksLikeLoginPrompt(lower)) {
            authPromptSeen = true;
            resourcePackSettled = true;
            if (!loginAttempted && !registerAttempted) {
                scheduleLogin(session, botUsername, logLine, "auth prompt");
            }
        }
    }

    private void maybeRegisterFromPrompt(
            McpToolsBotSession session,
            String botUsername,
            Consumer<String> logLine,
            String lower) {
        if (registerAttempted) {
            return;
        }
        if (preferRegisterFirst
                || ServerAuthPrompts.looksLikeRegisterOnlyPrompt(lower)
                || (loginAttempted && ServerAuthPrompts.looksLikeLoginFailureNotRegistered(lower))) {
            logLine.accept("Register prompt — /" + registerCommand + " as "
                    + describeRegisterFormat(registerFormat));
            scheduleRegister(session, botUsername, logLine);
        }
    }

    private void updateAuthHints(String line, String lower) {
        if (lower.contains("login") || lower.contains("/log") || lower.contains("/l ")) {
            loginCommand = RegisterFormatDetector.detectLoginCommand(line);
        }
        if (!lower.contains("register") && !lower.contains("/reg")) {
            return;
        }
        if (ServerAuthPrompts.looksLikeRegisterSuccess(lower)) {
            return;
        }
        registerCommand = RegisterFormatDetector.detectRegisterCommand(line);
        registerFormat = RegisterFormatDetector.detectFromPrompt(line);
    }

    private static boolean looksLikeResourcePackSettled(String lower) {
        return lower.contains("resource pack denied")
                || lower.contains("resource pack accepted")
                || lower.contains("required resource pack bypassed")
                || lower.contains("resource pack settled");
    }

    private static String describeRegisterFormat(RegisterFormat format) {
        return switch (format) {
            case PASSWORD_ONLY -> "<password>";
            case PASSWORD_REPEAT -> "<password> <password>";
            case EMAIL_THEN_PASSWORD -> "<email> <password>";
            case PASSWORD_THEN_EMAIL -> "<password> <email>";
        };
    }

    private void scheduleRegister(McpToolsBotSession session, String botUsername, Consumer<String> logLine) {
        if (registerAttempted || authenticated) {
            return;
        }
        registerAttempted = true;
        String identity = ServerAuthCommands.usesRegisterEmail(registerFormat) ? registerEmail : botUsername;
        String command;
        try {
            command = ServerAuthCommands.buildRegister(registerCommand, registerFormat, identity, BOT_PASSWORD);
        } catch (VaultInputException ex) {
            logLine.accept("Register failed: " + ex.getMessage());
            registerAttempted = false;
            return;
        }
        logLine.accept("> " + McpToolsBotLogFilter.maskAuthCommand(command));
        scheduleSend(session, command, logLine);
    }

    private void scheduleLogin(McpToolsBotSession session, String botUsername, Consumer<String> logLine, String reason) {
        if (authenticated) {
            return;
        }
        boolean allowRetry = "post-register".equals(reason) || "already registered".equals(reason);
        if (loginAttempted && !allowRetry) {
            return;
        }
        loginAttempted = true;
        String command;
        try {
            command = ServerAuthCommands.buildLogin(loginCommand, "", BOT_PASSWORD);
        } catch (VaultInputException ex) {
            logLine.accept("Login failed: " + ex.getMessage());
            loginAttempted = false;
            return;
        }
        logLine.accept("> " + McpToolsBotLogFilter.maskAuthCommand(command));
        scheduleSend(session, command, logLine);
    }

    private void scheduleSend(McpToolsBotSession session, String command, Consumer<String> logLine) {
        Thread.startVirtualThread(() -> {
            try {
                waitForResourcePackGate();
                Thread.sleep(AUTH_SEND_DELAY_MS);
                if (session.isActive()) {
                    session.sendChat(command);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                logLine.accept("Auth command failed: "
                        + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
                DupeClient.LOGGER.debug("MCPTools bot auth send failed", ex);
            }
        });
    }

    private void waitForResourcePackGate() throws InterruptedException {
        if (authPromptSeen || resourcePackSettled) {
            return;
        }
        long deadline = spawnedAtMs > 0 ? spawnedAtMs + RESOURCE_PACK_WAIT_MS : System.currentTimeMillis() + 1_500;
        while (!resourcePackSettled && System.currentTimeMillis() < deadline) {
            Thread.sleep(25);
        }
    }

    private static void scheduleDelayed(McpToolsBotSession session, Runnable task, long delayMs) {
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            if (session != null && session.isActive()) {
                task.run();
            }
        });
    }
}
