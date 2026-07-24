package com.dupeclient.client.module.fuzzer;

import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerManager;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerSettings;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

/**
 * Discovers injectable command templates via paced tab-complete (DupeDB-style), help output, and usage text.
 * Sends one completion request at a time and waits for the server reply before the next probe.
 */
public final class CommandArgDiscovery {
    public static final CommandArgDiscovery INSTANCE = new CommandArgDiscovery();

    private static final int COMPLETION_ID_BASE = 60_000;
    private static final int MAX_TAB_DEPTH = 3;
    private static final int MAX_TAB_PROBES = 40;
    private static final int MAX_ROOT_SEEDS = 10;
    private static final int MAX_HELP_ROOTS = 3;
    private static final int MAX_CHILDREN_PER_RESPONSE = 6;
    private static final long MIN_PROBE_DELAY_MS = 350L;
    private static final long MIN_HELP_DELAY_MS = 2_500L;
    private static final long RESPONSE_TIMEOUT_MS = 5_000L;
    private static final long SCAN_IDLE_MS = 800L;

    private static final Pattern HELP_USAGE = Pattern.compile(
            "^(?:/)?([a-z][a-z0-9_:.-]*(?:\\s+[a-z][a-z0-9_.-]+)+)\\s+"
                    + "((?:<[^>]+>|\\[[^\\]]+\\])(?:\\s+(?:<[^>]+>|\\[[^\\]]+\\]))*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern USAGE_INLINE = Pattern.compile(
            "(?i)(?:usage|correct usage|invalid (?:syntax|command|args?)|syntax)\\s*:?\\s*/?(.+)");

    private final Map<String, Set<String>> pathsByContext = new HashMap<>();
    private final Map<Integer, TabProbe> pendingTab = new HashMap<>();
    private final Deque<TabProbe> tabQueue = new ArrayDeque<>();
    private final Deque<String> helpQueue = new ArrayDeque<>();
    private final Set<String> queuedQueries = new HashSet<>();

    private volatile boolean discovering;
    private volatile String activeContext = "";
    private volatile int nextCompletionId = COMPLETION_ID_BASE;
    private volatile int tabProbesSent;
    private volatile long nextProbeAtMs;
    private volatile long nextHelpAtMs;
    private volatile long lastSentAtMs;
    private volatile long lastResponseAtMs;
    private volatile long discoverStartedAtMs;
    private volatile String status = "Idle";

    private CommandArgDiscovery() {
    }

    public boolean isDiscovering() {
        return discovering;
    }

    public String status() {
        return status;
    }

    public int pathCount(Minecraft client) {
        return pathsForContext(client).size();
    }

    public boolean hasCached(Minecraft client) {
        return !pathsForContext(client).isEmpty();
    }

    public List<String> pathsForClient(Minecraft client) {
        return new ArrayList<>(pathsForContext(client));
    }

    private Set<String> pathsForContext(Minecraft client) {
        String ctx = contextKey(client);
        if (ctx.isBlank()) {
            return Set.of();
        }
        return pathsByContext.getOrDefault(ctx, Set.of());
    }

    public void clearContext(Minecraft client) {
        String ctx = contextKey(client);
        if (!ctx.isBlank()) {
            pathsByContext.remove(ctx);
        }
    }

    public void startDiscovery(Minecraft client) {
        if (client == null || client.player == null || client.getConnection() == null) {
            status = "Join a server first";
            return;
        }
        if (discovering) {
            return;
        }
        activeContext = contextKey(client);
        if (activeContext.isBlank()) {
            status = "No server context";
            return;
        }

        long now = System.currentTimeMillis();
        pathsByContext.computeIfAbsent(activeContext, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        pendingTab.clear();
        tabQueue.clear();
        helpQueue.clear();
        queuedQueries.clear();
        tabProbesSent = 0;
        nextCompletionId = COMPLETION_ID_BASE;
        discovering = true;
        discoverStartedAtMs = now;
        nextProbeAtMs = now + probeDelayMs();
        nextHelpAtMs = now + helpDelayMs();
        lastSentAtMs = 0L;
        lastResponseAtMs = now;
        status = "Seeding…";

        seedBrigadierPaths(client);
        seedTabProbes(client);
        seedHelpProbes(client);
        status = "Queued " + tabQueue.size() + " tab · " + helpQueue.size() + " help";
    }

    public void stopDiscovery(String reason) {
        discovering = false;
        pendingTab.clear();
        tabQueue.clear();
        helpQueue.clear();
        queuedQueries.clear();
        status = reason == null ? "Idle" : reason;
    }

    public void tick(Minecraft client) {
        if (!discovering || client == null || client.player == null || client.getConnection() == null) {
            return;
        }
        String ctx = contextKey(client);
        if (!ctx.equals(activeContext)) {
            stopDiscovery("Context changed");
            return;
        }

        long now = System.currentTimeMillis();
        expireStalePending(now);

        if (!pendingTab.isEmpty()) {
            return;
        }

        if (!helpQueue.isEmpty() && tabQueue.isEmpty() && now >= nextHelpAtMs) {
            String helpCmd = helpQueue.pollFirst();
            if (helpCmd != null) {
                client.player.connection.sendCommand(helpCmd);
                lastResponseAtMs = now;
                nextHelpAtMs = now + helpDelayMs();
                status = "Help · " + pathsForContext(client).size() + " paths";
            }
            return;
        }

        if (now < nextProbeAtMs) {
            maybeFinish(now, client);
            return;
        }

        if (!tabQueue.isEmpty() && tabProbesSent < MAX_TAB_PROBES) {
            TabProbe probe = tabQueue.pollFirst();
            if (probe != null) {
                sendTabProbe(client, probe);
                tabProbesSent++;
                lastSentAtMs = now;
                nextProbeAtMs = now + probeDelayMs();
                status = "Tab " + tabProbesSent + "/" + MAX_TAB_PROBES
                        + " · " + pathsForContext(client).size() + " paths";
            }
            return;
        }

        maybeFinish(now, client);
    }

    private void maybeFinish(long now, Minecraft client) {
        if (!tabQueue.isEmpty() || !pendingTab.isEmpty() || !helpQueue.isEmpty()) {
            return;
        }
        if (lastResponseAtMs > 0L && now - lastResponseAtMs >= SCAN_IDLE_MS) {
            finishDiscovery();
            return;
        }
        long hardTimeout = Math.max(30_000L, MAX_TAB_PROBES * probeDelayMs() + helpQueue.size() * helpDelayMs() + 5_000L);
        if (discoverStartedAtMs > 0L && now - discoverStartedAtMs > hardTimeout) {
            finishDiscovery();
        }
    }

    private void expireStalePending(long now) {
        if (pendingTab.isEmpty() || lastSentAtMs <= 0L) {
            return;
        }
        if (now - lastSentAtMs > RESPONSE_TIMEOUT_MS) {
            pendingTab.clear();
            lastResponseAtMs = now;
            nextProbeAtMs = now + probeDelayMs();
        }
    }

    public void onCommandSuggestions(ClientboundCommandSuggestionsPacket packet) {
        if (!discovering || packet == null) {
            return;
        }
        Integer id = extractCompletionId(packet);
        if (id == null) {
            return;
        }
        TabProbe probe = pendingTab.remove(id);
        if (probe == null) {
            return;
        }

        long now = System.currentTimeMillis();
        lastResponseAtMs = now;
        nextProbeAtMs = now + probeDelayMs();

        List<String> suggestions = new ArrayList<>();
        for (Suggestion suggestion : packet.toSuggestions().getList()) {
            if (suggestion == null || suggestion.getText() == null) {
                continue;
            }
            String text = suggestion.getText().trim();
            if (!text.isBlank()) {
                suggestions.add(text);
            }
        }

        String base = probe.pathPrefix();
        if (suggestions.isEmpty()) {
            addPath(ensureInjectSlot(base));
            return;
        }

        boolean allSubcommands = suggestions.stream().allMatch(CommandArgDiscovery::looksLikeSubcommand);
        if (allSubcommands && probe.depth() < MAX_TAB_DEPTH) {
            int enqueued = 0;
            for (String sub : suggestions) {
                if (enqueued >= MAX_CHILDREN_PER_RESPONSE) {
                    break;
                }
                String child = base.isBlank() ? sub.toLowerCase(Locale.ROOT) : base + " " + sub.toLowerCase(Locale.ROOT);
                addPath(child);
                enqueueTab(child + " ", child, probe.depth() + 1);
                enqueued++;
            }
        } else {
            addPath(ensureInjectSlot(base));
        }
    }

    public void onChatLine(String raw) {
        if (!discovering || raw == null || raw.isBlank()) {
            return;
        }
        lastResponseAtMs = System.currentTimeMillis();
        String line = stripFormatting(raw).trim();
        if (line.isEmpty()) {
            return;
        }
        line = line.replaceFirst("^\\[[^\\]]+\\]\\s*", "");
        parseHelpOrUsage(line);
        Matcher usage = USAGE_INLINE.matcher(line);
        if (usage.find()) {
            ingestUsageFragment(usage.group(1));
        }
    }

    private void finishDiscovery() {
        int count = pathsByContext.getOrDefault(activeContext, Set.of()).size();
        discovering = false;
        pendingTab.clear();
        tabQueue.clear();
        helpQueue.clear();
        queuedQueries.clear();
        status = "Done (" + count + " paths)";
    }

    private void seedBrigadierPaths(Minecraft client) {
        for (String path : CommandEnumerator.brigadierPaths(client)) {
            addPath(path);
        }
    }

    private void seedTabProbes(Minecraft client) {
        EconomyFuzzerSettings settings = EconomyFuzzerManager.INSTANCE.getSettings();
        String selected = settings.sqliCommand == null ? "" : settings.sqliCommand.trim();
        if (!selected.isBlank()) {
            String norm = normalizePath(selected);
            addPath(norm);
            enqueueTab(norm + " ", norm, 0);
            int space = norm.indexOf(' ');
            if (space > 0) {
                String prefix = norm.substring(0, space);
                enqueueTab(prefix + " ", prefix, 0);
            }
            return;
        }

        int count = 0;
        for (String root : rootLiterals(client)) {
            if (count++ >= MAX_ROOT_SEEDS) {
                break;
            }
            addPath(root);
            enqueueTab(root + " ", root, 0);
        }
    }

    private void seedHelpProbes(Minecraft client) {
        EconomyFuzzerSettings settings = EconomyFuzzerManager.INSTANCE.getSettings();
        String selected = settings.sqliCommand == null ? "" : settings.sqliCommand.trim();
        if (!selected.isBlank()) {
            String root = normalizePath(selected).split("\\s+")[0];
            if (!root.isBlank()) {
                helpQueue.addLast(root + " help");
            }
            return;
        }

        int count = 0;
        for (String root : rootLiterals(client)) {
            if (count++ >= MAX_HELP_ROOTS) {
                break;
            }
            helpQueue.addLast(root + " help");
        }
    }

    private void sendTabProbe(Minecraft client, TabProbe probe) {
        int id = nextCompletionId++;
        pendingTab.put(id, probe);
        client.getConnection().send(new ServerboundCommandSuggestionPacket(id, probe.query()));
    }

    private void enqueueTab(String query, String pathPrefix, int depth) {
        if (depth > MAX_TAB_DEPTH || tabProbesSent + tabQueue.size() >= MAX_TAB_PROBES) {
            addPath(ensureInjectSlot(pathPrefix));
            return;
        }
        String q = query.startsWith("/") ? query.substring(1) : query;
        if (!queuedQueries.add(q.toLowerCase(Locale.ROOT))) {
            return;
        }
        tabQueue.addLast(new TabProbe(q, pathPrefix, depth));
    }

    private long probeDelayMs() {
        long configured = EconomyFuzzerManager.INSTANCE.getSettings().sqliDelayMs;
        return Math.max(MIN_PROBE_DELAY_MS, configured);
    }

    private long helpDelayMs() {
        return Math.max(MIN_HELP_DELAY_MS, probeDelayMs() * 3L);
    }

    private void parseHelpOrUsage(String line) {
        Matcher help = HELP_USAGE.matcher(line);
        if (help.find()) {
            ingestUsageFragment(help.group(1) + " " + help.group(2));
            return;
        }
        if (line.startsWith("/") || line.matches("(?i)[a-z][a-z0-9_:.-]*\\s+.+")) {
            int dash = line.indexOf(" - ");
            String head = dash >= 0 ? line.substring(0, dash).trim() : line;
            if (head.contains("<") || head.contains("[")) {
                ingestUsageFragment(head.startsWith("/") ? head.substring(1) : head);
            }
        }
    }

    private void ingestUsageFragment(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return;
        }
        String cleaned = fragment.trim();
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1).trim();
        }
        int cut = indexOfUsageNoise(cleaned);
        if (cut > 0) {
            cleaned = cleaned.substring(0, cut).trim();
        }
        String template = argsToTemplate(cleaned);
        if (!template.isBlank()) {
            addPath(template);
        }
    }

    private static int indexOfUsageNoise(String s) {
        int dash = s.indexOf(" - ");
        if (dash > 0) {
            return dash;
        }
        int em = s.indexOf(" — ");
        if (em > 0) {
            return em;
        }
        return -1;
    }

    private static String argsToTemplate(String usage) {
        if (usage == null || usage.isBlank()) {
            return "";
        }
        String u = usage.trim();
        if (u.startsWith("/")) {
            u = u.substring(1).trim();
        }
        int argStart = -1;
        for (int i = 0; i < u.length(); i++) {
            char c = u.charAt(i);
            if (c == '<' || c == '[') {
                argStart = i;
                break;
            }
        }
        if (argStart < 0) {
            return normalizePath(u);
        }
        String base = u.substring(0, argStart).trim();
        String argPart = u.substring(argStart);
        StringBuilder slots = new StringBuilder();
        Matcher m = Pattern.compile("<[^>]+>|\\[[^\\]]+\\]").matcher(argPart);
        while (m.find()) {
            String token = m.group();
            String name = token.substring(1, token.length() - 1).trim();
            if (!name.isBlank()) {
                slots.append(" <").append(sanitizeSlotName(name)).append(">");
            }
        }
        if (slots.isEmpty() || base.isBlank()) {
            return normalizePath(u);
        }
        return normalizePath(base + slots);
    }

    private static String sanitizeSlotName(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_").replaceAll("_+", "_");
    }

    private void addPath(String path) {
        if (path == null || path.isBlank() || activeContext.isBlank()) {
            return;
        }
        String normalized = normalizePath(path);
        if (normalized.isBlank()) {
            return;
        }
        Set<String> bucket = pathsByContext.computeIfAbsent(activeContext,
                k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        bucket.add(normalized);
        bucket.add(ensureInjectSlot(normalized));
    }

    private static String ensureInjectSlot(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        if (path.contains("<")) {
            return normalizePath(path);
        }
        String[] parts = path.trim().split("\\s+");
        if (parts.length <= 1) {
            return normalizePath(path);
        }
        String slot = sanitizeSlotName(parts[parts.length - 1]);
        if (slot.isBlank()) {
            slot = "arg";
        }
        return normalizePath(path + " <" + slot + ">");
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String p = path.trim().toLowerCase(Locale.ROOT);
        while (p.startsWith("/")) {
            p = p.substring(1).trim();
        }
        return p.replaceAll("\\s+", " ");
    }

    private static boolean looksLikeSubcommand(String s) {
        if (s == null || s.isBlank() || s.contains(" ")) {
            return false;
        }
        if (s.startsWith("<") || s.startsWith("[") || s.contains(":")) {
            return false;
        }
        return s.matches("[a-zA-Z][a-zA-Z0-9_\\-]*");
    }

    private static Set<String> rootLiterals(Minecraft client) {
        Set<String> roots = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<String> paths = CommandEnumerator.brigadierPaths(client);
        for (String path : paths) {
            String[] parts = path.split("\\s+");
            if (parts.length > 0 && !parts[0].startsWith("<")) {
                roots.add(parts[0]);
            }
        }
        return roots;
    }

    private static String contextKey(Minecraft client) {
        if (client == null) {
            return "";
        }
        String addr = "sp";
        if (client.getCurrentServer() != null && client.getCurrentServer().ip != null) {
            addr = client.getCurrentServer().ip.trim().toLowerCase(Locale.ROOT);
        }
        String world = "";
        try {
            if (client.level != null && client.level.dimension() != null) {
                world = client.level.dimension().identifier().toString();
            }
        } catch (Exception ignored) {
        }
        return addr + "|" + world;
    }

    private static String stripFormatting(String raw) {
        return raw.replaceAll("§.", "");
    }

    private static Integer extractCompletionId(ClientboundCommandSuggestionsPacket packet) {
        try {
            Object value = packet.getClass().getMethod("getCompletionId").invoke(packet);
            if (value instanceof Integer i) {
                return i;
            }
        } catch (Exception ignored) {
        }
        try {
            Object value = packet.getClass().getMethod("completionId").invoke(packet);
            if (value instanceof Integer i) {
                return i;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private record TabProbe(String query, String pathPrefix, int depth) {
    }
}
