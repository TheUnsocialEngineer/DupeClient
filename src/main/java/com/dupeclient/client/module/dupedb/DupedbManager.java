package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.core.session.HubModuleRules;
import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.overlay.ServerProfileCard;
import com.dupeclient.client.module.acaudit.AcAuditManager;
import com.dupeclient.client.module.acaudit.AcAuditPluginClassifier;
import com.dupeclient.client.module.packet.FeatureHotkeyManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import org.jetbrains.annotations.Nullable;

public final class DupedbManager {
    public static final DupedbManager INSTANCE = new DupedbManager();

    private static final String PREFIX = "[DupeDB] ";
    private static final String API_BASE = "https://dupedb.net/api";
    private static final String EXPLOITS_SEARCH_URL = API_BASE + "/exploits/search";
    private static final String SETTINGS_URL = "https://dupedb.net/settings";
    private static final String DEVELOPER_SETTINGS_URL = "https://dupedb.net/settings/developer";
    private static final String ROOT_PROBE_PREFIXES = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String[] PLUGIN_LIST_PROBE_COMMANDS = new String[]{"/plugins", "/pl", "/bukkit:plugins", "/bukkit:pl"};
    private static final String[] VERSION_PROBE_COMMANDS = new String[]{"/ver", "/version", "/about", "/icanhasbukkit", "/bukkit:ver", "/bukkit:version"};
    private static final String[] HELP_PROBE_COMMANDS = new String[]{"/help", "/?", "/bukkit:help", "/minecraft:help"};
    private static final String[] COMMON_PLUGIN_NAMESPACES = new String[]{
            "essentials", "essentialsx", "worldedit", "worldguard", "luckperms", "vault", "citizens", "cmi",
            "cmilib", "multiverse-core", "multiverse", "viaversion", "viabackwards", "viarewind", "geysermc",
            "geyser", "floodgate", "protocollib", "coreprotect", "griefprevention", "shopkeepers", "dynmap",
            "placeholderapi", "skinsrestorer", "skript", "vulcan", "grimac", "matrix", "spartan", "aac",
            "karhu", "verus", "nocheatplus", "authme", "deluxemenus", "plotsquared", "supervanish"
    };

    private void parseCommandTree(Minecraft client) {
        try {
            if (client.getConnection() == null) {
                return;
            }
            CommandDispatcher<?> dispatcher = client.getConnection().getCommands();
            if (dispatcher == null) {
                return;
            }
            RootCommandNode<?> root = dispatcher.getRoot();
            if (root == null) {
                return;
            }
            for (CommandNode<?> child : root.getChildren()) {
                String name = child.getName();
                if (name == null || name.isBlank()) {
                    continue;
                }
                String token = normalizeToken(name);
                if (token.contains(":")) {
                    String[] parts = token.split(":", 2);
                    String ns = parts[0];
                    if (!ns.isBlank() && !VANILLA_NAMESPACES.contains(ns)) {
                        discoveredPlugins.add(ns);
                        pluginEvidence.put(ns, PluginEvidence.COMMAND_TREE);
                    }
                    if (parts.length > 1 && !parts[1].isBlank()) {
                        observedCommandRoots.add(parts[1]);
                    }
                } else {
                    observedCommandRoots.add(token);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private int addProbeVariants(Map<Integer, PluginProbeSpec> probes, int nextId, PluginProbeKind kind, String[] baseCommands) {
        for (String base : baseCommands) {
            if (base == null || base.isBlank()) continue;
            String trimmed = base.trim();
            probes.put(nextId++, new PluginProbeSpec(trimmed, kind, null));
            probes.put(nextId++, new PluginProbeSpec(trimmed + " ", kind, null));
        }
        return nextId;
    }

    private Map<Integer, PluginProbeSpec> buildPluginProbes() {
        LinkedHashMap<Integer, PluginProbeSpec> probes = new LinkedHashMap<>();
        int nextId = COMPLETION_ID_START;
        probes.put(nextId++, new PluginProbeSpec("/", PluginProbeKind.ROOT, null));
        probes.put(nextId++, new PluginProbeSpec("/ ", PluginProbeKind.ROOT, null));
        nextId = addProbeVariants(probes, nextId, PluginProbeKind.PLUGIN_LIST, PLUGIN_LIST_PROBE_COMMANDS);
        nextId = addProbeVariants(probes, nextId, PluginProbeKind.VERSION, VERSION_PROBE_COMMANDS);
        nextId = addProbeVariants(probes, nextId, PluginProbeKind.HELP, HELP_PROBE_COMMANDS);
        for (int i = 0; i < ROOT_PROBE_PREFIXES.length(); i++) {
            char prefix = ROOT_PROBE_PREFIXES.charAt(i);
            probes.put(nextId++, new PluginProbeSpec("/" + prefix, PluginProbeKind.ROOT, String.valueOf(prefix)));
            probes.put(nextId++, new PluginProbeSpec("/help " + prefix, PluginProbeKind.HELP, String.valueOf(prefix)));
            probes.put(nextId++, new PluginProbeSpec("/? " + prefix, PluginProbeKind.HELP, String.valueOf(prefix)));
        }
        for (String ns : prioritizedNamespaces()) {
            probes.put(nextId++, new PluginProbeSpec("/" + ns + ":", PluginProbeKind.NAMESPACE, ns));
        }
        return probes;
    }

    private String[] prioritizedNamespaces() {
        LinkedHashSet<String> ordered = new LinkedHashSet<>(List.of(COMMON_PLUGIN_NAMESPACES));
        try {
            var metrics = AcAuditManager.INSTANCE.getMetrics();
            if (metrics.anticheatPlugins != null) {
                for (String ac : metrics.anticheatPlugins) {
                    if (ac != null && !ac.isBlank()) {
                        ordered.remove(ac);
                        LinkedHashSet<String> next = new LinkedHashSet<>();
                        next.add(ac.toLowerCase(Locale.ROOT));
                        next.addAll(ordered);
                        ordered = next;
                    }
                }
            }
            if (metrics.pluginNamespaces != null) {
                for (String ns : metrics.pluginNamespaces) {
                    if (ns != null && AcAuditPluginClassifier.isAnticheatNamespace(ns)) {
                        ordered.remove(ns);
                        LinkedHashSet<String> next = new LinkedHashSet<>();
                        next.add(ns.toLowerCase(Locale.ROOT));
                        next.addAll(ordered);
                        ordered = next;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return ordered.toArray(String[]::new);
    }

    private int totalProbeCount() {
        return pendingProbeIds.size() + queuedProbes.size();
    }

    private long getFinishedSendingAt() {
        if (scanStartedAt <= 0L || !queuedProbes.isEmpty()) {
            return 0L;
        }
        return Math.max(scanStartedAt, nextProbeSendAt - Math.max(10, settings.probeDelayMs));
    }

    private Integer extractCompletionId(ClientboundCommandSuggestionsPacket packet) {
        try {
            Object value = packet.getClass().getMethod("getCompletionId").invoke(packet);
            if (value instanceof Integer i) return i;
        } catch (Exception ignored) {
        }
        try {
            Object value = packet.getClass().getMethod("completionId").invoke(packet);
            if (value instanceof Integer i) return i;
        } catch (Exception ignored) {
        }
        try {
            Object value = packet.getClass().getMethod("method_11396").invoke(packet);
            if (value instanceof Integer i) return i;
        } catch (Exception ignored) {
        }
        return null;
    }

    private int getTotalPages(JsonElement parsed) {
        if (parsed == null || !parsed.isJsonObject()) return 1;
        JsonObject obj = parsed.getAsJsonObject();
        try {
            if (obj.has("total_pages")) return Math.max(1, obj.get("total_pages").getAsInt());
            if (obj.has("pages")) return Math.max(1, obj.get("pages").getAsInt());
            if (obj.has("meta") && obj.get("meta").isJsonObject()) {
                JsonObject meta = obj.getAsJsonObject("meta");
                if (meta.has("total_pages")) return Math.max(1, meta.get("total_pages").getAsInt());
                if (meta.has("pages")) return Math.max(1, meta.get("pages").getAsInt());
            }
            if (obj.has("data") && obj.get("data").isJsonObject()) {
                JsonObject data = obj.getAsJsonObject("data");
                if (data.has("pagination") && data.get("pagination").isJsonObject()) {
                    JsonObject pagination = data.getAsJsonObject("pagination");
                    if (pagination.has("total_pages")) return Math.max(1, pagination.get("total_pages").getAsInt());
                    if (pagination.has("pages")) return Math.max(1, pagination.get("pages").getAsInt());
                }
            }
        } catch (Exception ignored) {
        }
        return 1;
    };
    private static final Set<String> VANILLA_NAMESPACES = Set.of("minecraft", "brigadier", "bukkit", "spigot", "paper", "purpur", "velocity", "bungeecord", "waterfall");
    private static final Map<String, String> ROOT_ALIASES = createRootAliases();
    private static final long SCAN_IDLE_MS = 700L;
    private static final long SCAN_SETTLE_MS = 450L;
    /** Minimum gap between automatic plugin rescans (proxy lobbies can churn context keys). */
    private static final long AUTO_SCAN_COOLDOWN_MS = 20_000L;
    private static final int COMPLETION_ID_START = 1337;

    private final DupedbSettings settings = DupedbConfigManager.load();
    private final FeatureHotkeyManager overlayHotkeys = new FeatureHotkeyManager();
    private final DupedbP2wScorer p2wScorer = new DupedbP2wScorer();
    private DupedbP2wScorer.Result lastP2wResult;
    private final Set<String> discoveredPlugins = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<Integer> pendingProbeIds = new HashSet<>();
    private final Map<Integer, PluginProbeSpec> pluginProbes = new HashMap<>();
    private final Deque<PluginProbeRequest> queuedProbes = new ArrayDeque<>();
    private final Set<String> observedCommandRoots = new LinkedHashSet<>();
    private final Map<String, PluginEvidence> pluginEvidence = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private long nextProbeSendAt;
    private long scanStartedAt;
    private long scanLastResponseAt;
    private boolean scanning;
    private boolean pluginListOnlyScan;
    private String scanServerAddress = "";
    private String lastAutoServerAddress = "";
    private String lastCompletedScanServer = "";
    private long lastCompletedScanAt;
    private int lastCompletedScanPluginCount;
    private long nextBackgroundScanAt;
    private String joinedServerHost = "";

    private DupedbManager() {
    }

    private static Map<String, String> createRootAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("lp", "luckperms");
        aliases.put("we", "worldedit");
        aliases.put("rg", "worldguard");
        aliases.put("mv", "multiverse-core");
        aliases.put("npc", "citizens");
        aliases.put("papi", "placeholderapi");
        aliases.put("cmi", "cmi");
        aliases.put("co", "coreprotect");
        aliases.put("grim", "grimac");
        aliases.put("geyser", "geysermc");
        aliases.put("floodgate", "floodgate");
        aliases.put("viaver", "viaversion");
        aliases.put("sr", "skinsrestorer");
        aliases.put("authme", "authme");
        aliases.put("dm", "deluxemenus");
        aliases.put("plots", "plotsquared");
        aliases.put("sv", "supervanish");
        return aliases;
    }

    public DupedbSettings getSettings() {
        return settings;
    }

    @Nullable
    public DupedbP2wScorer.Result getLastP2wResult() {
        return lastP2wResult;
    }

    public void onIncomingChatLine(String line) {
        p2wScorer.onChatLine(line);
    }

    public void initialize() {
        DupedbSdk.registerMinecraftBrowserOpener();
        migrateLegacyAuthFromSettings();
        DupedbSdk.INSTANCE.configureOAuthAppId(resolveOAuthAppId());
        save();
    }

    private void migrateLegacyAuthFromSettings() {
        String legacyToken = settings.oauthToken == null ? "" : settings.oauthToken.trim();
        if (legacyToken.startsWith(DupedbSdk.PAT_PREFIX)) {
            DupedbSdk.INSTANCE.setPersonalAccessToken(legacyToken);
            return;
        }
        if (!legacyToken.isBlank()) {
            DupedbSdk.INSTANCE.migrateLegacyOAuth(
                    legacyToken,
                    settings.oauthRefreshToken,
                    settings.oauthAccessTokenExpiresAtMs,
                    resolveOAuthAppId()
            );
        }
        settings.oauthToken = "";
        settings.oauthRefreshToken = "";
        settings.oauthAccessTokenExpiresAtMs = 0L;
    }

    public void save() {
        DupedbConfigManager.save(settings);
    }

    /** Gray status line from the DupeDB panel; respects {@link DupedbSettings#moduleChatFeedback}. */
    public void chatFeedback(String message) {
        sendInfo(message);
    }

    /** Shown even when chat feedback is off (e.g. toggling the feedback option itself). */
    public void chatFeedbackConfigToggle(String message) {
        sendClientFormatted(Component.literal(message).withStyle(ChatFormatting.GRAY));
    }

    public boolean isAuthenticated() {
        return DupedbSdk.INSTANCE.isAuthenticated();
    }

    /** Bearer token for DupeDB API requests. */
    public String getOAuthTokenOrEmpty() {
        return DupedbSdk.INSTANCE.getAccessTokenOrEmpty();
    }

    public boolean isOauthInFlight() {
        return DupedbSdk.INSTANCE.isOauthInFlight();
    }

    public boolean isScanning() {
        return scanning;
    }

    public int getDiscoveredPluginCount() {
        return discoveredPlugins.size();
    }

    public Collection<String> getDiscoveredPlugins() {
        return List.copyOf(discoveredPlugins);
    }

    public Collection<String> getObservedCommandRoots() {
        return List.copyOf(observedCommandRoots);
    }

    public String currentServerHost() {
        Minecraft client = Minecraft.getInstance();
        return client == null ? "" : currentServerAddress(client);
    }

    public void onPlaySessionJoin(Minecraft client) {
        joinedServerHost = client == null ? "" : currentServerAddress(client);
        lastAutoServerAddress = "";
        nextBackgroundScanAt = System.currentTimeMillis()
                + Math.max(5, settings.backgroundScanIntervalMinutes) * 60_000L;
        ServerProfileCard.showOnJoin();
    }

    public void onPlaySessionLeave() {
        joinedServerHost = "";
        abortActiveScan();
    }

    public boolean hasRecentScanForServer(String server) {
        if (server == null || server.isBlank() || lastCompletedScanServer.isBlank()) {
            return false;
        }
        if (!server.equalsIgnoreCase(lastCompletedScanServer)) {
            return false;
        }
        return System.currentTimeMillis() - lastCompletedScanAt <= P2wVerification.scanMaxAgeMs();
    }

    public int lastScanPluginCountForServer(String server) {
        return hasRecentScanForServer(server) ? lastCompletedScanPluginCount : 0;
    }

    public void clearToken() {
        DupedbSdk.INSTANCE.clearAuth();
        settings.oauthToken = "";
        settings.oauthRefreshToken = "";
        settings.oauthAccessTokenExpiresAtMs = 0L;
        save();
        chatFeedback("DupeDB credentials cleared.");
    }

    public void setPersonalAccessToken(String token) {
        String trimmed = token == null ? "" : token.trim();
        if (trimmed.isBlank()) {
            sendClientMessage("Paste a Personal Access Token from " + DEVELOPER_SETTINGS_URL);
            return;
        }
        if (!trimmed.startsWith(DupedbSdk.PAT_PREFIX)) {
            sendClientMessage("Warning: expected a token starting with " + DupedbSdk.PAT_PREFIX);
        }
        DupedbSdk.INSTANCE.setPersonalAccessToken(trimmed);
        settings.oauthToken = trimmed;
        settings.oauthRefreshToken = "";
        settings.oauthAccessTokenExpiresAtMs = 0L;
        save();
        sendClientMessage("Authenticated with DupeDB (Personal Access Token).");
    }

    public void setOAuthAppId(String appId) {
        String trimmed = appId == null ? "" : appId.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isBlank()) {
            settings.oauthAppId = "";
            DupedbSdk.INSTANCE.configureOAuthAppId(DupedbSdk.DEFAULT_OAUTH_APP_ID);
            save();
            sendClientMessage("OAuth app id reset to default \"" + DupedbSdk.DEFAULT_OAUTH_APP_ID + "\".");
            return;
        }
        if (!trimmed.matches("[a-z0-9-]{3,32}")) {
            sendClientMessage("Invalid app id. Use 3-32 lowercase letters, digits, or hyphens (your DupeDB OAuth app slug).");
            return;
        }
        settings.oauthAppId = trimmed;
        DupedbSdk.INSTANCE.configureOAuthAppId(trimmed);
        save();
        sendClientMessage("OAuth app id set to \"" + trimmed + "\". Use /dupedb login to sign in.");
    }

    public void openSettingsPage() {
        try {
            net.minecraft.util.Util.getPlatform().openUri(SETTINGS_URL);
        } catch (Exception ignored) {
        }
    }

    public void openDeveloperSettingsPage() {
        try {
            net.minecraft.util.Util.getPlatform().openUri(DEVELOPER_SETTINGS_URL);
        } catch (Exception ignored) {
        }
    }

    public void startLoginFlow() {
        DupedbSdk.INSTANCE.startOAuthLogin(
                () -> sendClientMessage("Opening browser for DupeDB login…"),
                () -> sendClientMessage("Authenticated with DupeDB."),
                error -> sendClientMessage("OAuth failed: " + error)
        );
    }

    public void tick(Minecraft client) {
        if (client == null) {
            return;
        }
        if (!HubModuleRules.exploitFeaturesAllowed()) {
            abortActiveScan();
            return;
        }
        if (client.getWindow() != null
                && !InputFocusGuards.shouldBlockOverlayToggleHotkeys(client)
                && overlayHotkeys.consumePress(client, settings.overlayToggleKey)) {
            DupedbOverlay.INSTANCE.toggleOverlayVisible();
            chatFeedbackConfigToggle("DupeDB overlay " + (settings.overlayVisible ? "shown" : "hidden"));
        }
        if (client.player == null || client.getConnection() == null) {
            scanning = false;
            if (client.getConnection() == null) {
                clearDiscoveredPluginState("");
            }
            return;
        }

        invalidateStalePluginInventory(client);
        p2wScorer.trackOpenGui(client);

        if (settings.mode == DupedbMode.AUTO) {
            String current = currentServerAddress(client);
            if (!current.isBlank() && !current.equalsIgnoreCase(lastAutoServerAddress) && !scanning) {
                lastAutoServerAddress = current;
                startScan(true);
            }
        }

        if (settings.backgroundScanEnabled && !scanning && client.player != null) {
            long now = System.currentTimeMillis();
            String host = currentServerAddress(client);
            if (!host.isBlank() && now >= nextBackgroundScanAt) {
                nextBackgroundScanAt = now + Math.max(5, settings.backgroundScanIntervalMinutes) * 60_000L;
                startScan(false);
            }
        }

        if (!scanning) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now >= nextProbeSendAt && !queuedProbes.isEmpty()) {
            PluginProbeRequest probe = queuedProbes.pollFirst();
            client.getConnection().send(new ServerboundCommandSuggestionPacket(probe.id, probe.spec.query));
            nextProbeSendAt = now + Math.max(10, settings.probeDelayMs);
        }

        long hardTimeoutMs = Math.max(12000L, ((long) totalProbeCount() * Math.max(10, settings.probeDelayMs)) + 2000L);
        boolean allSent = queuedProbes.isEmpty();
        boolean allAnswered = pendingProbeIds.isEmpty();
        boolean quiet = scanLastResponseAt > 0L && now - scanLastResponseAt > SCAN_IDLE_MS;
        boolean timedOut = scanStartedAt > 0L && now - scanStartedAt > hardTimeoutMs;
        long sentDoneAt = getFinishedSendingAt();
        boolean settled = allSent && sentDoneAt > 0L && now - sentDoneAt >= (allAnswered ? SCAN_IDLE_MS : SCAN_SETTLE_MS);

        if ((allSent && allAnswered) || settled || (allSent && quiet) || timedOut) {
            finishScanAndQuery();
        }
    }

    public void onCommandSuggestions(ClientboundCommandSuggestionsPacket packet) {
        if (!scanning) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            return;
        }
        if (!serverAutoContextKey(client).equals(scanContextAtStart)) {
            return;
        }
        Integer id = extractCompletionId(packet);
        PluginProbeSpec probe = id != null ? pluginProbes.get(id) : null;
        if (id != null) {
            pendingProbeIds.remove(id);
        }
        scanLastResponseAt = System.currentTimeMillis();
        for (Suggestion suggestion : packet.toSuggestions().getList()) {
            consumeSuggestion(suggestion.getText(), probe);
        }
    }

    public void startScan(boolean fromAutoMode) {
        if (!HubModuleRules.exploitFeaturesAllowed()) {
            return;
        }
        startScan(fromAutoMode, false);
    }

    public void abortActiveScan() {
        scanning = false;
        pluginListOnlyScan = false;
        pendingProbeIds.clear();
        pluginProbes.clear();
        queuedProbes.clear();
    }

    public void startPluginListScan() {
        if (!HubModuleRules.exploitFeaturesAllowed()) {
            return;
        }
        startScan(false, true);
    }

    private void startScan(boolean fromAutoMode, boolean pluginListOnly) {
        pluginListOnlyScan = pluginListOnly;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            return;
        }
        discoveredPlugins.clear();
        pendingProbeIds.clear();
        pluginProbes.clear();
        queuedProbes.clear();
        observedCommandRoots.clear();
        pluginEvidence.clear();

        parseCommandTree(client);
        int id = COMPLETION_ID_START;
        Map<Integer, PluginProbeSpec> probes = buildPluginProbes();
        for (Map.Entry<Integer, PluginProbeSpec> entry : probes.entrySet()) {
            id = Math.max(id, entry.getKey() + 1);
            pendingProbeIds.add(entry.getKey());
            pluginProbes.put(entry.getKey(), entry.getValue());
            queuedProbes.addLast(new PluginProbeRequest(entry.getKey(), entry.getValue()));
        }

        scanning = true;
        scanStartedAt = System.currentTimeMillis();
        scanLastResponseAt = scanStartedAt;
        nextProbeSendAt = scanStartedAt + Math.max(10, settings.probeDelayMs);
        scanServerAddress = currentServerAddress(client);
        scanContextAtStart = serverAutoContextKey(client);
        sendClientStatus(fromAutoMode ? "Auto mode: probing server plugins..." : "Probing server plugins...", ChatFormatting.GRAY);
    }

    private void finishScanAndQuery() {
        scanning = false;
        lastCompletedScanServer = scanServerAddress == null ? "" : scanServerAddress;
        lastCompletedScanAt = System.currentTimeMillis();
        lastCompletedScanPluginCount = discoveredPlugins.size();
        pendingProbeIds.clear();
        pluginProbes.clear();
        queuedProbes.clear();
        inferFromObservedRoots();
        if (pluginListOnlyScan) {
            pluginListOnlyScan = false;
            sendClientStatus("Plugin list scan: " + discoveredPlugins.size() + " plugin(s).", ChatFormatting.AQUA);
            return;
        }
        if (discoveredPlugins.isEmpty()) {
            sendClientStatus("No plugins discovered.", ChatFormatting.GRAY);
            if (settings.announceNoMatches) {
                sendClientStatus("No DupeDB matches found.", ChatFormatting.GRAY);
            }
            return;
        }
        sendClientStatus("Discovered " + discoveredPlugins.size() + " plugin(s), querying DupeDB...", ChatFormatting.AQUA);
        Thread t = new Thread(this::queryDupedbForMatches, "DupeDB-MatchQuery");
        t.setDaemon(true);
        t.start();
    }

    private void queryDupedbForMatches() {
        try {
            String url = EXPLOITS_SEARCH_URL + "?page=1&limit=250&sort=date_submitted&order=desc";
            String json = fetch(url);
            if (json == null || json.isBlank()) {
                sendClientStatus("Failed to query DupeDB API.", ChatFormatting.RED);
                return;
            }

            JsonElement parsed = JsonParser.parseString(json);
            JsonArray exploits = parseExploitsArray(parsed);
            if (exploits == null || exploits.isEmpty()) {
                sendClientStatus("No DupeDB exploits were returned.", ChatFormatting.GRAY);
                return;
            }

            List<ExploitMatchLine> matches = new ArrayList<>();
            Set<String> pluginKeys = new HashSet<>();
            for (String plugin : discoveredPlugins) {
                pluginKeys.add(plugin.toLowerCase(Locale.ROOT));
            }

            for (JsonElement element : exploits) {
                if (!element.isJsonObject()) continue;
                JsonObject ex = element.getAsJsonObject();
                String pluginName = readString(ex, "plugin_name");
                if (pluginName.isBlank()) continue;
                String pluginKey = pluginName.toLowerCase(Locale.ROOT);
                if (!pluginKeys.contains(pluginKey) && pluginKeys.stream().noneMatch(p -> pluginKey.contains(p) || p.contains(pluginKey))) {
                    continue;
                }
                String id = readString(ex, "id");
                if (id.isBlank()) id = readString(ex, "_id");
                String name = readString(ex, "name");
                if (name.isBlank()) name = readString(ex, "title");
                if (name.isBlank()) name = "Exploit";
                String link = id.isBlank() ? "https://dupedb.net" : "https://dupedb.net/exploit/" + id;
                matches.add(new ExploitMatchLine(name, pluginName, link));
            }

            if (matches.isEmpty()) {
                if (settings.announceNoMatches) {
                    sendClientStatus("No plugin-specific DupeDB matches found.", ChatFormatting.GRAY);
                }
                return;
            }

            sendClientStatus("Found " + matches.size() + " DupeDB match(es):", ChatFormatting.GREEN);
            for (ExploitMatchLine match : matches) {
                sendClientFormatted(DupeMiniMessage.exploitMatchLine(match.exploitName(), match.pluginName(), match.url()));
            }
        } catch (Exception e) {
            sendClientStatus("DupeDB query failed: " + e.getMessage(), ChatFormatting.RED);
        }
    }

    private record ExploitMatchLine(String exploitName, String pluginName, String url) {
    }

    private void consumeSuggestion(String text, PluginProbeSpec probe) {
        if (text == null || text.isBlank()) {
            return;
        }
        String normalized = text.trim();
        if (normalized.contains(":")) {
            String[] parts = normalized.split(":", 2);
            String namespace = normalizeToken(parts[0]);
            String root = normalizeToken(parts[1]);
            if (!namespace.isBlank() && !VANILLA_NAMESPACES.contains(namespace)) {
                discoveredPlugins.add(namespace);
                pluginEvidence.put(namespace, PluginEvidence.COMMAND_TREE);
            }
            if (!root.isBlank()) {
                observedCommandRoots.add(root);
            }
            return;
        }

        String token = normalizeToken(normalized);
        if (token.isBlank()) {
            return;
        }
        if (ROOT_ALIASES.containsKey(token)) {
            String plugin = ROOT_ALIASES.get(token);
            discoveredPlugins.add(plugin);
            pluginEvidence.put(plugin, PluginEvidence.ROOT_HINT);
        }
        observedCommandRoots.add(token);
        if (probe != null && probe.kind == PluginProbeKind.NAMESPACE && probe.hint != null && !probe.hint.isBlank()) {
            discoveredPlugins.add(probe.hint);
            pluginEvidence.put(probe.hint, PluginEvidence.NAMESPACE);
        } else if (probe != null && (probe.kind == PluginProbeKind.PLUGIN_LIST || probe.kind == PluginProbeKind.VERSION)
                && isLikelyPluginNameCandidate(token, probe.kind)) {
            discoveredPlugins.add(token);
            pluginEvidence.put(token, probe.kind == PluginProbeKind.PLUGIN_LIST ? PluginEvidence.PLUGIN_LIST : PluginEvidence.VERSION_HINT);
        } else if (looksLikePlugin(token)) {
            discoveredPlugins.add(token);
        }
    }

    private void inferFromObservedRoots() {
        for (String root : observedCommandRoots) {
            String mapped = ROOT_ALIASES.get(root);
            if (mapped != null && !mapped.isBlank()) {
                discoveredPlugins.add(mapped);
                pluginEvidence.put(mapped, PluginEvidence.ROOT_HINT);
            } else if (isKnownPluginNamespace(root)) {
                discoveredPlugins.add(root);
                pluginEvidence.put(root, PluginEvidence.ROOT_HINT);
            }
        }
    }

    private boolean looksLikePlugin(String token) {
        if (token.length() < 2) return false;
        if (VANILLA_NAMESPACES.contains(token)) return false;
        if (token.equals("plugins") || token.equals("help") || token.equals("version")) return false;
        return token.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.');
    }

    private boolean isKnownPluginNamespace(String key) {
        for (String ns : COMMON_PLUGIN_NAMESPACES) {
            if (ns.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLikelyPluginNameCandidate(String token, PluginProbeKind kind) {
        if (token == null || token.isBlank()) {
            return false;
        }
        if (token.length() < 2 || VANILLA_NAMESPACES.contains(token)) {
            return false;
        }
        if ((kind == PluginProbeKind.PLUGIN_LIST || kind == PluginProbeKind.VERSION)
                && (token.equals("plugins") || token.equals("plugin") || token.equals("version") || token.equals("about"))) {
            return false;
        }
        return token.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.');
    }

    private String normalizeToken(String raw) {
        String token = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        while (token.startsWith("/")) token = token.substring(1).trim();
        int space = token.indexOf(' ');
        if (space >= 0) token = token.substring(0, space).trim();
        while (token.endsWith(":")) token = token.substring(0, token.length() - 1).trim();
        return token;
    }

    private String fetch(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");
        String token = getOAuthTokenOrEmpty();
        if (!token.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            return null;
        }
        try (java.io.InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private JsonArray parseExploitsArray(JsonElement parsed) {
        if (parsed == null || parsed.isJsonNull()) {
            return null;
        }
        if (parsed.isJsonArray()) {
            return parsed.getAsJsonArray();
        }
        if (!parsed.isJsonObject()) {
            return null;
        }
        JsonObject obj = parsed.getAsJsonObject();
        if (obj.has("success") && obj.has("data") && obj.get("data").isJsonObject()) {
            JsonObject data = obj.getAsJsonObject("data");
            if (data.has("exploits") && data.get("exploits").isJsonArray()) return data.getAsJsonArray("exploits");
        }
        if (obj.has("exploits") && obj.get("exploits").isJsonArray()) return obj.getAsJsonArray("exploits");
        if (obj.has("data") && obj.get("data").isJsonArray()) return obj.getAsJsonArray("data");
        if (obj.has("results") && obj.get("results").isJsonArray()) return obj.getAsJsonArray("results");
        return null;
    }

    private String readString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString().trim();
    }

    @SuppressWarnings("unused")
    private String readPluginName(JsonObject exploit) {
        String pluginName = readString(exploit, "plugin_name");
        if (!pluginName.isBlank()) return pluginName;
        pluginName = readString(exploit, "plugin");
        if (!pluginName.isBlank()) return pluginName;
        if (exploit.has("plugin_data") && exploit.get("plugin_data").isJsonObject()) {
            JsonObject pluginData = exploit.getAsJsonObject("plugin_data");
            pluginName = readString(pluginData, "name");
            if (!pluginName.isBlank()) return pluginName;
            pluginName = readString(pluginData, "plugin_name");
            if (!pluginName.isBlank()) return pluginName;
        }
        return "";
    }

    private String resolveOAuthAppId() {
        String configured = settings.oauthAppId == null ? "" : settings.oauthAppId.trim();
        return configured.isBlank() ? DupedbSdk.DEFAULT_OAUTH_APP_ID : configured;
    }

    private String currentServerAddress(Minecraft client) {
        if (client.getCurrentServer() != null && client.getCurrentServer().ip != null) {
            return client.getCurrentServer().ip.trim().toLowerCase(Locale.ROOT);
        }
        if (client.getConnection() != null && client.getConnection().getConnection() != null
                && client.getConnection().getConnection().getRemoteAddress() != null) {
            String raw = client.getConnection().getConnection().getRemoteAddress().toString();
            return raw == null ? "" : raw.replaceFirst("^/", "").trim().toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private void sendClientMessage(String message) {
        sendClientStatus(message, ChatFormatting.WHITE);
    }

    private void sendClientStatus(String message, ChatFormatting color) {
        sendClientFormatted(Component.literal(message).withStyle(color));
    }

    private void sendClientFormatted(Component body) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        Component message = dupeDbPrefix().copy().append(body);
        client.execute(() -> {
            if (client.player != null) {
                client.player.displayClientMessage(message, false);
            }
        });
    }

    private String scanContextAtStart = "";
    private String lastPluginInventoryKey = "";

    private net.minecraft.network.chat.MutableComponent dupeDbPrefix() {
        return net.minecraft.network.chat.Component.literal(PREFIX)
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GOLD).withBold(true));
    }

    private void sendClientText(net.minecraft.network.chat.MutableComponent text) {
        sendClientFormatted(text);
    }

    private void sendInfo(String message) {
        if (!settings.moduleChatFeedback) {
            return;
        }
        sendClientFormatted(Component.literal(message).withStyle(ChatFormatting.GRAY));
    }

    private void clearDiscoveredPluginState(String reason) {
        discoveredPlugins.clear();
        pluginEvidence.clear();
        observedCommandRoots.clear();
        pendingProbeIds.clear();
        pluginProbes.clear();
        queuedProbes.clear();
    }

    private void invalidateStalePluginInventory(Minecraft client) {
        String key = serverAutoContextKey(client);
        if (key.isBlank()) {
            return;
        }
        if (!key.equals(lastPluginInventoryKey)) {
            lastPluginInventoryKey = key;
            clearDiscoveredPluginState("");
        }
    }

    private static String reasonOrEmpty(String reason) {
        return reason == null ? "" : reason;
    }

    private String serverAutoContextKey(Minecraft client) {
        if (client == null || client.player == null) {
            return "";
        }
        return currentServerAddress(client) + "|" + commandTreeFingerprint(client);
    }

    private String commandTreeFingerprint(Minecraft client) {
        try {
            if (client.getConnection() == null) {
                return "";
            }
            CommandDispatcher<?> dispatcher = client.getConnection().getCommands();
            if (dispatcher == null || dispatcher.getRoot() == null) {
                return "";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String name : dispatcher.getRoot().getChildren().stream().map(CommandNode::getName).sorted().toList()) {
                digest.update(name.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (Exception ex) {
            return "";
        }
    }

    private enum PluginEvidence {
        COMMAND_TREE,
        NAMESPACE,
        ROOT_HINT,
        HELP_HINT,
        PLUGIN_LIST,
        VERSION_HINT,
        UNKNOWN
    }

    private enum PluginProbeKind {
        ROOT,
        HELP,
        PLUGIN_LIST,
        VERSION,
        NAMESPACE
    }

    private record PluginProbeSpec(String query, PluginProbeKind kind, String hint) {
    }

    private record PluginProbeRequest(int id, PluginProbeSpec spec) {
    }
}
