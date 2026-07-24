package com.dupeclient.client.module.fuzzer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;

/** Collects slash-command templates from the server Brigadier tree, including argument slots. */
public final class CommandEnumerator {
    private static final int MAX_DEPTH = 6;
    private static final Pattern ARG_SLOT = Pattern.compile("<([^>]+)>");
    private static final String FILLER = "test";

    private CommandEnumerator() {
    }

    public static List<String> allCommandPaths(Minecraft client) {
        Set<String> raw = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        collectPaths(client, raw);
        raw.addAll(CommandArgDiscovery.INSTANCE.pathsForClient(client));
        return pruneIntermediatePaths(new ArrayList<>(raw));
    }

    /** Brigadier-only paths (used as discovery seeds). */
    public static List<String> brigadierPaths(Minecraft client) {
        Set<String> raw = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        collectPaths(client, raw);
        return new ArrayList<>(raw);
    }

    private static void collectPaths(Minecraft client, Set<String> out) {
        if (client == null || client.getConnection() == null) {
            return;
        }
        try {
            CommandDispatcher<?> dispatcher = client.getConnection().getCommands();
            if (dispatcher == null) {
                return;
            }
            RootCommandNode<?> root = dispatcher.getRoot();
            if (root == null) {
                return;
            }
            for (CommandNode<?> child : root.getChildren()) {
                walk(child, "", 0, out);
            }
        } catch (Exception ignored) {
        }
    }

    private static void walk(CommandNode<?> node, String prefix, int depth, Set<String> out) {
        if (node == null || depth > MAX_DEPTH) {
            return;
        }
        String path = appendSegment(prefix, node);
        if (path.isBlank()) {
            return;
        }
        boolean executable = node.getCommand() != null;
        boolean hasArgs = path.contains("<");
        if (executable || hasArgs || node.getChildren().isEmpty()) {
            out.add(path);
        }
        if (depth >= MAX_DEPTH) {
            return;
        }
        for (CommandNode<?> child : node.getChildren()) {
            walk(child, path, depth + 1, out);
        }
    }

    private static String appendSegment(String prefix, CommandNode<?> node) {
        if (node instanceof ArgumentCommandNode<?, ?> arg) {
            String slot = "<" + normalizeArgName(arg.getName()) + ">";
            return prefix.isEmpty() ? slot : prefix + " " + slot;
        }
        String name = node.getName();
        if (name == null || name.isBlank()) {
            return prefix;
        }
        String segment = name.toLowerCase(Locale.ROOT);
        return prefix.isEmpty() ? segment : prefix + " " + segment;
    }

    private static String normalizeArgName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "arg";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /** Drops short prefixes when a longer, more specific path exists (e.g. keep {@code team description <text>}). */
    private static List<String> pruneIntermediatePaths(List<String> paths) {
        List<String> out = new ArrayList<>();
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            boolean dominated = false;
            String lower = path.toLowerCase(Locale.ROOT);
            if (lower.contains("<args>")) {
                String prefix = lower.replace(" <args>", "").replace("<args>", "").trim();
                for (String other : paths) {
                    if (other == null || other.equalsIgnoreCase(path)) {
                        continue;
                    }
                    String otherLower = other.toLowerCase(Locale.ROOT);
                    if (!otherLower.contains("<args>") && otherLower.startsWith(prefix + " ")) {
                        dominated = true;
                        break;
                    }
                }
            }
            if (!dominated) {
                for (String other : paths) {
                    if (other == null || other.equalsIgnoreCase(path)) {
                        continue;
                    }
                    String otherLower = other.toLowerCase(Locale.ROOT);
                    if (otherLower.startsWith(lower + " ") && other.length() > path.length()) {
                        dominated = true;
                        break;
                    }
                }
            }
            if (!dominated) {
                out.add(path);
            }
        }
        return out;
    }

    public static List<String> argSlots(String template) {
        if (template == null || template.isBlank()) {
            return List.of();
        }
        List<String> slots = new ArrayList<>();
        Matcher matcher = ARG_SLOT.matcher(template.trim());
        while (matcher.find()) {
            slots.add(matcher.group(1));
        }
        return slots;
    }

    public static String describeArgs(String template) {
        List<String> slots = argSlots(template);
        if (slots.isEmpty()) {
            return "append";
        }
        return String.join(", ", slots.stream().map(s -> "<" + s + ">").toList());
    }

    /**
     * Builds a runnable command from a template.
     * Replaces {@code <slot>} markers with payload/filler; if no slots exist, appends payload at the end.
     */
    public static String injectPayload(String template, String payload, int slotIndex) {
        String cmd = template == null ? "" : template.trim();
        String pay = payload == null ? "" : payload.trim();
        if (cmd.isBlank()) {
            return pay;
        }
        List<String> slots = argSlots(cmd);
        if (slots.isEmpty()) {
            return cmd + " " + pay;
        }
        int useSlot = slotIndex < 0 ? slots.size() - 1 : Math.min(slotIndex, slots.size() - 1);
        StringBuilder out = new StringBuilder();
        Matcher matcher = ARG_SLOT.matcher(cmd);
        int idx = 0;
        int last = 0;
        while (matcher.find()) {
            out.append(cmd, last, matcher.start());
            out.append(idx == useSlot ? pay : FILLER);
            last = matcher.end();
            idx++;
        }
        out.append(cmd.substring(last));
        return out.toString().trim();
    }

    /** Total fuzz steps = payloads × injectable slots (or payloads only when no slots). */
    public static int fuzzSteps(String template, int payloadCount) {
        int slots = argSlots(template).size();
        if (slots <= 0) {
            return Math.max(0, payloadCount);
        }
        return Math.max(0, payloadCount * slots);
    }

    public static int slotIndexForStep(String template, int stepIndex, int payloadCount) {
        List<String> slots = argSlots(template);
        if (slots.isEmpty() || payloadCount <= 0) {
            return -1;
        }
        return stepIndex / payloadCount;
    }

    public static int payloadIndexForStep(int stepIndex, int payloadCount) {
        if (payloadCount <= 0) {
            return 0;
        }
        return stepIndex % payloadCount;
    }
}
