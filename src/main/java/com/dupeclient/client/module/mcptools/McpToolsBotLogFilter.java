package com.dupeclient.client.module.mcptools;

import com.dupeclient.client.module.serverpassword.ServerAuthPrompts;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Filters verbose connect.mjs / server chat noise from MCPTools bot logs. */
public final class McpToolsBotLogFilter {
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");

    private McpToolsBotLogFilter() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return ANSI.matcher(ServerAuthPrompts.normalizeChatLine(raw)).replaceAll("").trim();
    }

    /** Session infrastructure messages (errors only — no join command spam). */
    public static boolean isInfrastructureError(String raw) {
        String line = normalize(raw);
        if (line.isEmpty()) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.startsWith("bot session failed")
                || lower.contains("bundle missing")
                || lower.contains("node.js not found")
                || lower.contains("bot limit reached")
                || lower.startsWith("bot limit reached");
    }

    public static boolean isNoise(String raw) {
        String line = normalize(raw);
        if (line.isEmpty()) {
            return true;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        return line.startsWith("$ ")
                || lower.startsWith("running npm install")
                || lower.equals("npm install complete.")
                || lower.startsWith("starting bot [");
    }

    public static Optional<String> formatSessionLine(String raw) {
        String line = normalize(raw);
        if (line.isEmpty()) {
            return Optional.empty();
        }
        String lower = line.toLowerCase(Locale.ROOT);

        if (lower.contains("bot has connected")) {
            return Optional.of("Connected.");
        }
        if (lower.contains("kicked from the server")) {
            String reason = extractKickReason(line);
            if (reason.isBlank() || reason.equalsIgnoreCase("The bot was forced to disconnect.")) {
                return Optional.of("Disconnected.");
            }
            return Optional.of("Kicked: " + reason);
        }
        if (lower.contains("forced to disconnect")) {
            return Optional.of("Disconnected.");
        }
        if (ServerAuthPrompts.looksLikeLoginSuccess(lower)) {
            return Optional.of("Logged in.");
        }
        if (ServerAuthPrompts.looksLikeRegisterSuccess(lower)) {
            return Optional.of("Registered.");
        }
        if (lower.contains("spawned in world")) {
            return Optional.of("Spawned in world.");
        }
        if (lower.contains("session ended:")) {
            return Optional.of("Session ended.");
        }
        if (lower.startsWith("error:")) {
            return Optional.of(line.substring(line.toLowerCase(Locale.ROOT).indexOf("error:") + 6).trim());
        }
        if (lower.contains("client timed out after")) {
            return Optional.of("Login timed out.");
        }
        if (lower.contains("resource pack denied")) {
            return Optional.of("Resource pack denied.");
        }
        if (lower.contains("resource pack accepted")) {
            return Optional.of("Resource pack accepted.");
        }
        if (lower.contains("resource pack settled")) {
            return Optional.empty();
        }
        if (lower.contains("required resource pack bypassed")) {
            return Optional.of("Resource pack accepted.");
        }

        if (line.contains("[#]") || line.contains("[# ]")) {
            if (isScriptCommandOutput(lower)) {
                return Optional.of(compactScriptLine(line));
            }
            return Optional.empty();
        }

        if (ServerAuthPrompts.looksLikeLoginPrompt(lower)
                || ServerAuthPrompts.looksLikeRegisterPrompt(lower)
                || ServerAuthPrompts.looksLikeAlreadyRegistered(lower)) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    public static String maskAuthCommand(String command) {
        if (command == null || command.isBlank()) {
            return command;
        }
        String trimmed = command.strip();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/login ") || lower.startsWith("login ")
                || lower.startsWith("/log ") || lower.startsWith("/l ")) {
            int slash = trimmed.indexOf(' ');
            String prefix = slash > 0 ? trimmed.substring(0, slash) : trimmed;
            return prefix + " ***";
        }
        if (lower.startsWith("/register ") || lower.startsWith("register ")
                || lower.startsWith("/reg ")) {
            int slash = trimmed.indexOf(' ');
            String prefix = slash > 0 ? trimmed.substring(0, slash) : trimmed;
            return prefix + " ***";
        }
        return trimmed;
    }

    private static boolean isScriptCommandOutput(String lower) {
        return lower.contains("server ip:")
                || lower.contains("list of connected players")
                || lower.contains("no players in tab list")
                || lower.contains("fetching tab list")
                || lower.contains("players (")
                || lower.contains("pathfinding to")
                || lower.contains("path stopped")
                || lower.contains("mining ")
                || lower.contains("pathfinder load error")
                || lower.contains("pathfinder failed to initialize")
                || lower.contains("plugins (")
                || lower.contains("plugins:")
                || lower.contains("moving the bot")
                || lower.contains("stopping the movement")
                || lower.contains("spawned in world")
                || lower.contains("trying to get the server plugins")
                || lower.contains("there are no plugins")
                || lower.contains("invalid movement")
                || lower.contains("you must specify")
                || lower.contains("list of available commands")
                || lower.contains("the bot is not connected")
                || lower.contains("resource pack denied")
                || lower.contains("required resource pack bypassed");
    }

    private static String compactScriptLine(String line) {
        int idx = line.indexOf("[#");
        if (idx >= 0) {
            int end = line.indexOf(']', idx);
            if (end >= 0 && end + 1 < line.length()) {
                String tail = line.substring(end + 1).strip();
                if (!tail.isEmpty()) {
                    return tail;
                }
            }
        }
        return line.strip();
    }

    private static String extractAfterReason(String line) {
        return extractKickReason(line);
    }

    private static String extractKickReason(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("reason:");
        String reason = idx >= 0 && idx + 7 < line.length()
                ? line.substring(idx + 7).trim()
                : line.trim();
        reason = collapseNbtKickReason(reason);
        reason = stripLegacyColorNames(reason);
        if (reason.length() > 200) {
            return reason.substring(0, 197) + "…";
        }
        return reason;
    }

    private static String stripLegacyColorNames(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        return text.replaceAll(
                "(?i)\\b(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|grey|dark_gray|dark_grey|blue|green|aqua|red|light_purple|yellow|white|reset|bold|italic|underlined|strikethrough|obfuscated)\\b",
                " ").replaceAll("\\s+", " ").trim();
    }

    /** Fallback when JS could not decode NBT chat — pull string values from the blob. */
    private static String collapseNbtKickReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return reason == null ? "" : reason;
        }
        if (!reason.contains("\"type\":\"compound\"") && !reason.contains("\"type\": \"compound\"")) {
            return reason;
        }
        StringBuilder out = new StringBuilder();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"value\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                .matcher(reason);
        while (matcher.find()) {
            String chunk = matcher.group(1)
                    .replace("\\n", " ")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .trim();
            if (!chunk.isBlank() && out.indexOf(chunk) < 0) {
                if (out.length() > 0) {
                    out.append(' ');
                }
                out.append(chunk);
            }
        }
        return out.length() > 0 ? out.toString() : "Disconnected by server.";
    }
}
