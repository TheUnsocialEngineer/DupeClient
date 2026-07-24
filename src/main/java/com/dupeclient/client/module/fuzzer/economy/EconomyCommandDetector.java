package com.dupeclient.client.module.fuzzer.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Detects economy-related slash commands from Brigadier and static presets. */
public final class EconomyCommandDetector {
    private static final List<String> PAY_PRESETS = List.of(
            "pay",
            "eco pay",
            "money pay",
            "essentials:pay",
            "cmi pay",
            "clan withdraw",
            "clan deposit",
            "clan pay",
            "clan ban withdraw",
            "clan balance",
            "bank withdraw",
            "bank deposit",
            "economy pay",
            "money send",
            "money give",
            "money transfer");

    private static final List<String> BALANCE_PRESETS = List.of(
            "bal",
            "balance",
            "money",
            "eco balance",
            "clan balance",
            "bank balance");

    private static final Set<String> PAY_PATH_KEYWORDS = Set.of(
            "pay", "withdraw", "deposit", "transfer", "send", "give");
    private static final Set<String> BALANCE_PATH_KEYWORDS = Set.of(
            "bal", "balance", "money");

    private EconomyCommandDetector() {
    }

    public static List<String> payCommandOptions(MinecraftClient client) {
        Set<String> found = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        found.addAll(PAY_PRESETS);
        found.addAll(detectPaths(client, PAY_PATH_KEYWORDS, 3));
        return new ArrayList<>(found);
    }

    public static List<String> balanceCommandOptions(MinecraftClient client) {
        Set<String> found = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        found.addAll(BALANCE_PRESETS);
        found.addAll(detectPaths(client, BALANCE_PATH_KEYWORDS, 3));
        return new ArrayList<>(found);
    }

    public static int indexOfIgnoreCase(List<String> options, String value) {
        if (value == null || options == null) {
            return -1;
        }
        String trimmed = value.trim();
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(trimmed)) {
                return i;
            }
        }
        return -1;
    }

    public static String normalizeCommand(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim();
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1).trim();
        }
        return cleaned;
    }

    /**
     * Commands like {@code bank withdraw} / {@code clan deposit} that take only an amount (no player target).
     */
    public static boolean suggestsAmountOnly(String command) {
        String norm = normalizeCommand(command).toLowerCase(Locale.ROOT);
        if (norm.isBlank()) {
            return false;
        }
        if (norm.endsWith(" balance") || norm.equals("balance") || norm.equals("bal") || norm.equals("money")) {
            return false;
        }
        String[] parts = norm.split("\\s+");
        String last = parts[parts.length - 1];
        if (Set.of("withdraw", "deposit", "add", "remove", "take", "put").contains(last)) {
            return true;
        }
        return norm.endsWith(" withdraw") || norm.endsWith(" deposit");
    }

    public enum ResolvedSyntax {
        PLAYER_AMOUNT,
        AMOUNT_PLAYER,
        AMOUNT_ONLY;

        public boolean needsTarget() {
            return this != AMOUNT_ONLY;
        }

        public String displayLabel() {
            return switch (this) {
                case PLAYER_AMOUNT -> "player amount";
                case AMOUNT_PLAYER -> "amount player";
                case AMOUNT_ONLY -> "amount only";
            };
        }
    }

    public static ResolvedSyntax resolveSyntax(String command, String syntaxMode) {
        String mode = syntaxMode == null ? "" : syntaxMode.trim().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "amount_only" -> ResolvedSyntax.AMOUNT_ONLY;
            case "amount_player" -> ResolvedSyntax.AMOUNT_PLAYER;
            case "player_amount" -> ResolvedSyntax.PLAYER_AMOUNT;
            default -> suggestsAmountOnly(command) ? ResolvedSyntax.AMOUNT_ONLY : ResolvedSyntax.PLAYER_AMOUNT;
        };
    }

    public static String syntaxModeLabel(String syntaxMode, String command) {
        String mode = syntaxMode == null ? "" : syntaxMode.trim().toLowerCase(Locale.ROOT);
        if (mode.isBlank() || "auto".equals(mode)) {
            ResolvedSyntax resolved = resolveSyntax(command, "auto");
            return "auto: " + resolved.displayLabel();
        }
        return resolveSyntax(command, mode).displayLabel();
    }

    public static String cycleSyntaxMode(String current) {
        String mode = current == null || current.isBlank() ? "auto" : current.trim().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "auto" -> "player_amount";
            case "player_amount" -> "amount_player";
            case "amount_player" -> "amount_only";
            case "amount_only" -> "auto";
            default -> "auto";
        };
    }

    private static List<String> detectPaths(MinecraftClient client, Set<String> keywords, int maxDepth) {
        Set<String> paths = new LinkedHashSet<>();
        if (client == null || client.getNetworkHandler() == null) {
            return List.of();
        }
        try {
            CommandDispatcher<?> dispatcher = client.getNetworkHandler().getCommandDispatcher();
            if (dispatcher == null) {
                return List.of();
            }
            RootCommandNode<?> root = dispatcher.getRoot();
            if (root == null) {
                return List.of();
            }
            for (CommandNode<?> child : root.getChildren()) {
                walk(child, "", 0, maxDepth, keywords, paths);
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(paths);
    }

    private static void walk(
            CommandNode<?> node,
            String prefix,
            int depth,
            int maxDepth,
            Set<String> keywords,
            Set<String> out) {
        if (node == null || depth > maxDepth) {
            return;
        }
        String name = node.getName();
        if (name == null || name.isBlank()) {
            return;
        }
        String segment = name.toLowerCase(Locale.ROOT);
        String path = prefix.isEmpty() ? segment : prefix + " " + segment;
        if (matchesKeywords(path, segment, keywords)) {
            out.add(path);
        }
        if (depth >= maxDepth) {
            return;
        }
        for (CommandNode<?> child : node.getChildren()) {
            walk(child, path, depth + 1, maxDepth, keywords, out);
        }
    }

    private static boolean matchesKeywords(String path, String segment, Set<String> keywords) {
        for (String kw : keywords) {
            if (segment.equals(kw) || path.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
