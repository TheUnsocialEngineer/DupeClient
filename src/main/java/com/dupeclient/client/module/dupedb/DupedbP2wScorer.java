package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.module.dupedb.DupeDbPluginMatcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pay-to-win heuristics ported from DupeDB companion logic: weighted signals from known P2W plugins,
 * store/key/rank command roots, chat history, and open GUI titles/items.
 */
public final class DupedbP2wScorer {
    private static final int CHAT_HISTORY_LIMIT = 256;
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}");

    private static final double WEIGHT_PLUGIN = 0.08;
    private static final double WEIGHT_STORE = 0.2;
    private static final double WEIGHT_KEY = 0.25;
    private static final double WEIGHT_RANK = 0.3;

    private static final List<String> CRATE_PLUGINS = List.of(
            "Phoenix Crates Lite", "Kits & Crates +", "Skye Crates", "Supply Crates", "CrazyCrates",
            "ExcellentCrates", "ShulkerLock Crates", "CratesAndDropevents", "CrazyEnvoys", "UserCrates",
            "BOBCASE", "CCase", "LootCrate", "CoreCrates", "FlezerBoxes"
    );

    private static final List<String> GAMBLING_PLUGINS = List.of(
            "CoinFlip", "CoinFlip [With GUI]", "Coinflip Plugin", "Custom Coinflip", "CoinFlipZ",
            "XLT-DeluxeCoinflipAddon", "CoinFlip\u200E", "CoinFlips", "CoinlifpXD", "Diamond Casino",
            "WinniePat's Casino plugin", "Bomba Casino", "Roulette", "MineGames", "Deal Or No Deal",
            "DeluxeCoinflip"
    );

    private static final List<String> STORE_PLUGINS = List.of(
            "Tebex", "Buycraft", "CraftingStore", "Enjin", "DonationCraft", "BuyCraft", "TebexPlugin"
    );

    private static final List<String> KEY_PLUGINS = List.of(
            "VirtualKeys", "CrazyKeys", "PlayerKeys", "CrateKeys", "TokenManager", "KeyAll"
    );

    private static final List<String> RANK_PLUGINS = List.of(
            "Rankup", "RankUp", "DonatorPlus", "BuyRank", "RankPurchase", "UltraRank", "PermissionsEx"
    );

    private static final Set<String> STORE_COMMANDS = Set.of(
            "store", "shop", "buy", "donate", "donation", "donations", "tebex", "buycraft", "purchase",
            "market", "webstore", "webshop", "buygems", "gems", "coins", "coinshop", "cashshop"
    );

    private static final Set<String> KEY_COMMANDS = Set.of(
            "key", "keys", "crate", "crates", "cratekey", "cratekeys", "virtualkeys", "openkey", "keyall"
    );

    private static final Set<String> RANK_COMMANDS = Set.of(
            "rank", "ranks", "buyrank", "rankup", "rankbuy", "donator", "donor", "vip", "upgrade", "prestige"
    );

    private static final Pattern STORE_TEXT = Pattern.compile(
            "\\b(store|shop|donate|donation|tebex|buycraft|webstore|webshop|coinshop|cashshop|buy\\s+now)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern KEY_TEXT = Pattern.compile(
            "\\b(crate\\s*key|virtual\\s*key|purchase\\s*key|buy\\s*key|keys?\\s+shop|open\\s*key)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern RANK_TEXT = Pattern.compile(
            "\\b(buy\\s*rank|purchase\\s*rank|rank\\s*shop|donator\\s*rank|vip\\s*rank|upgrade\\s*rank)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> STORE_DOMAINS = Set.of(
            "tebex.io", "tebex.com", "buycraft.net", "craftingstore.net", "enjin.com", "server.pro",
            "minecraft.net" // often store links; only counts when paired with store-ish chat
    );

    private final Deque<String> chatHistory = new ArrayDeque<>();
    private final Set<String> scannedGuiTexts = new LinkedHashSet<>();
    private String lastTrackedScreenTitle = "";

    public void clearSession() {
        chatHistory.clear();
        scannedGuiTexts.clear();
        lastTrackedScreenTitle = "";
    }

    public void onChatLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        chatHistory.addLast(line.trim());
        while (chatHistory.size() > CHAT_HISTORY_LIMIT) {
            chatHistory.removeFirst();
        }
        scanText(line);
    }

    public void trackOpenGui(MinecraftClient client) {
        if (client == null) {
            return;
        }
        Screen screen = client.currentScreen;
        if (screen == null) {
            lastTrackedScreenTitle = "";
            return;
        }
        String title = screen.getTitle().getString();
        if (title.equals(lastTrackedScreenTitle)) {
            return;
        }
        lastTrackedScreenTitle = title;
        scanText(title);
        if (screen instanceof HandledScreen<?> handled) {
            scanHandledScreen(handled);
        }
    }

    public Result compute(Collection<String> discoveredPlugins, Collection<String> commandRoots) {
        SignalState signals = new SignalState();

        List<String> matchedPlugins = matchKnownPlugins(discoveredPlugins);
        if (!matchedPlugins.isEmpty()) {
            signals.pluginMatches.addAll(matchedPlugins);
            signals.pluginHits = matchedPlugins.size();
            for (String match : matchedPlugins) {
                if (matchesAnyPlugin(match, STORE_PLUGINS)) {
                    signals.store = true;
                    signals.storeSources.add("plugin " + match);
                }
                if (matchesAnyPlugin(match, KEY_PLUGINS)) {
                    signals.key = true;
                    signals.keySources.add("plugin " + match);
                }
                if (matchesAnyPlugin(match, RANK_PLUGINS)) {
                    signals.rank = true;
                    signals.rankSources.add("plugin " + match);
                }
            }
        }

        for (String root : commandRoots) {
            if (root == null || root.isBlank()) {
                continue;
            }
            String token = root.toLowerCase(Locale.ROOT);
            if (STORE_COMMANDS.contains(token)) {
                signals.store = true;
                signals.storeSources.add("command /" + token);
            }
            if (KEY_COMMANDS.contains(token)) {
                signals.key = true;
                signals.keySources.add("command /" + token);
            }
            if (RANK_COMMANDS.contains(token)) {
                signals.rank = true;
                signals.rankSources.add("command /" + token);
            }
        }

        for (String line : chatHistory) {
            scanLineForSignals(line, signals);
        }
        for (String guiText : scannedGuiTexts) {
            scanLineForSignals(guiText, signals);
        }

        if (signals.pluginHits > 0 && hasAnyCrateGamblingMatch(signals.pluginMatches)) {
            signals.key = true;
            signals.keySources.add("crate/gambling plugin");
        }

        double score = Math.min(1.0,
                signals.pluginHits * WEIGHT_PLUGIN
                        + (signals.store ? WEIGHT_STORE : 0.0)
                        + (signals.key ? WEIGHT_KEY : 0.0)
                        + (signals.rank ? WEIGHT_RANK : 0.0));

        return new Result(
                (int) Math.round(score * 100.0),
                score,
                List.copyOf(signals.pluginMatches),
                signals.store,
                List.copyOf(signals.storeSources),
                signals.key,
                List.copyOf(signals.keySources),
                signals.rank,
                List.copyOf(signals.rankSources)
        );
    }

    public static List<String> extractDomains(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Matcher matcher = DOMAIN_PATTERN.matcher(text);
        List<String> domains = new ArrayList<>();
        while (matcher.find()) {
            domains.add(matcher.group());
        }
        return domains;
    }

    private void scanText(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        scannedGuiTexts.add(text.trim());
        while (scannedGuiTexts.size() > 128) {
            var it = scannedGuiTexts.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    private void scanHandledScreen(HandledScreen<?> handled) {
        ScreenHandler handler = handled.getScreenHandler();
        if (handler == null) {
            return;
        }
        for (Slot slot : handler.slots) {
            if (slot == null) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            scanText(stack.getName().getString());
        }
    }

    private void scanLineForSignals(String line, SignalState signals) {
        if (line == null || line.isBlank()) {
            return;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (STORE_TEXT.matcher(lower).find()) {
            signals.store = true;
            signals.storeSources.add("chat/gui text");
        }
        if (KEY_TEXT.matcher(lower).find()) {
            signals.key = true;
            signals.keySources.add("chat/gui text");
        }
        if (RANK_TEXT.matcher(lower).find()) {
            signals.rank = true;
            signals.rankSources.add("chat/gui text");
        }
        for (String domain : extractDomains(line)) {
            String domainLower = domain.toLowerCase(Locale.ROOT);
            if (STORE_DOMAINS.stream().anyMatch(d -> domainLower.equals(d) || domainLower.endsWith("." + d))) {
                if (STORE_TEXT.matcher(lower).find()
                        || lower.contains("store")
                        || lower.contains("shop")
                        || lower.contains("donate")
                        || lower.contains("buy")
                        || !domainLower.equals("minecraft.net")) {
                    signals.store = true;
                    signals.storeSources.add("domain " + domain);
                }
            }
        }
    }

    private List<String> matchKnownPlugins(Collection<String> discoveredPlugins) {
        LinkedHashSet<String> matched = new LinkedHashSet<>();
        if (discoveredPlugins == null) {
            return List.of();
        }
        for (String discovered : discoveredPlugins) {
            if (discovered == null || discovered.isBlank()) {
                continue;
            }
            for (String known : allKnownPlugins()) {
                if (pluginMatches(discovered, known)) {
                    matched.add(known);
                }
            }
        }
        return new ArrayList<>(matched);
    }

    private static List<String> allKnownPlugins() {
        List<String> all = new ArrayList<>(CRATE_PLUGINS.size() + GAMBLING_PLUGINS.size()
                + STORE_PLUGINS.size() + KEY_PLUGINS.size() + RANK_PLUGINS.size());
        all.addAll(CRATE_PLUGINS);
        all.addAll(GAMBLING_PLUGINS);
        all.addAll(STORE_PLUGINS);
        all.addAll(KEY_PLUGINS);
        all.addAll(RANK_PLUGINS);
        return all;
    }

    private static boolean pluginMatches(String discovered, String known) {
        String a = DupeDbPluginMatcher.normalizePluginKey(discovered);
        String b = DupeDbPluginMatcher.normalizePluginKey(known);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    private static boolean hasAnyCrateGamblingMatch(List<String> matchedPlugins) {
        for (String match : matchedPlugins) {
            if (matchesAnyPlugin(match, CRATE_PLUGINS) || matchesAnyPlugin(match, GAMBLING_PLUGINS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAnyPlugin(String discovered, List<String> knownList) {
        for (String known : knownList) {
            if (pluginMatches(discovered, known)) {
                return true;
            }
        }
        return false;
    }

    private static final class SignalState {
        int pluginHits;
        final List<String> pluginMatches = new ArrayList<>();
        boolean store;
        final LinkedHashSet<String> storeSources = new LinkedHashSet<>();
        boolean key;
        final LinkedHashSet<String> keySources = new LinkedHashSet<>();
        boolean rank;
        final LinkedHashSet<String> rankSources = new LinkedHashSet<>();
    }

    public record Result(
            int percent,
            double score,
            List<String> matchedPlugins,
            boolean storeDetected,
            List<String> storeSources,
            boolean keyDetected,
            List<String> keySources,
            boolean rankDetected,
            List<String> rankSources
    ) {
        public String summaryLine() {
            StringBuilder sb = new StringBuilder("P2W score: ").append(percent).append('%');
            if (!matchedPlugins.isEmpty()) {
                sb.append(" | plugins: ").append(String.join(", ", matchedPlugins));
            }
            if (storeDetected) {
                sb.append(" | store");
            }
            if (keyDetected) {
                sb.append(" | keys");
            }
            if (rankDetected) {
                sb.append(" | ranks");
            }
            return sb.toString();
        }

        public String detailLine() {
            List<String> parts = new ArrayList<>();
            if (!storeSources.isEmpty()) {
                parts.add("store (" + String.join(", ", storeSources) + ")");
            }
            if (!keySources.isEmpty()) {
                parts.add("keys (" + String.join(", ", keySources) + ")");
            }
            if (!rankSources.isEmpty()) {
                parts.add("ranks (" + String.join(", ", rankSources) + ")");
            }
            return parts.isEmpty() ? "" : String.join("; ", parts);
        }
    }
}
