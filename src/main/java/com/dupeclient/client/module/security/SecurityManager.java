package com.dupeclient.client.module.security;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.security.nochatrestrictions.NoChatRestrictionsGate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;

import net.minecraft.client.toast.SystemToast;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SecurityManager {
    public static final SecurityManager INSTANCE = new SecurityManager();

    private SecuritySettings settings = new SecuritySettings();
    private final Map<String, StaffWatchEntry> knownStaff = new LinkedHashMap<>();
    private final Set<UUID> onlineStaff = new HashSet<>();
    private final Set<UUID> detectedThisSession = new HashSet<>();
    private final Set<UUID> staffProximityAlerted = new HashSet<>();
    private final Set<Integer> invisibleEntityAlerted = new HashSet<>();
    private boolean staffBaselineReady;
    private volatile boolean staffGlowActive;
    private String nameChangerSourceUsername = "";

    private long lastSignKeyProbeAlertMs;
    private static final long SIGN_KEY_PROBE_ALERT_COOLDOWN_MS = 4000L;

    private SecurityManager() {
    }

    private static boolean safeEq(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private static String currentServerAddress(MinecraftClient client) {
        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address != null) {
            return client.getCurrentServerEntry().address;
        }
        return "unknown";
    }

    public void initialize() {
        settings = SecurityConfigManager.load();
        knownStaff.clear();
        knownStaff.putAll(SecurityStaffStore.load());
        refreshDerivedFlags();
        refreshNameChangerUsername();
    }

    public SecuritySettings getSettings() {
        return settings;
    }

    public void save() {
        SecurityConfigManager.save(settings);
        NoChatRestrictionsGate.setCached(settings.noChatRestrictions);
        refreshDerivedFlags();
    }

    public void onPlaySessionJoin(MinecraftClient client) {
        if (client == null || !settings.profileAutoSwitchPerServer) {
            return;
        }
        String host = SecurityProfileStore.normalizeHost(currentServerAddress(client));
        if (host.isBlank()) {
            return;
        }
        SecuritySettings profile = SecurityProfileStore.profileForHost(host);
        if (profile != null) {
            SecurityProfileStore.applyProfileTo(settings, profile);
            save();
            feedback("Loaded OpSec profile for " + host);
        }
    }

    public void saveProfileForCurrentServer(MinecraftClient client) {
        if (client == null) {
            return;
        }
        String host = SecurityProfileStore.normalizeHost(currentServerAddress(client));
        if (host.isBlank()) {
            feedback("Not on a server.");
            return;
        }
        SecurityProfileStore.saveProfileForHost(host, settings);
        feedback("Saved OpSec profile for " + host);
    }

    public boolean isStaffGlowActive() {
        return staffGlowActive;
    }

    private void refreshDerivedFlags() {
        staffGlowActive = settings.staffDetectionEnabled && settings.staffGlowEnabled;
    }

    public void refreshNameChangerUsername() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSession() != null) {
            nameChangerSourceUsername = client.getSession().getUsername();
        }
    }

    public void onSessionUsernameChanged(String username) {
        if (username != null && !username.isBlank()) {
            nameChangerSourceUsername = username;
        }
    }

    public String replaceDisplayedName(String text) {
        if (!settings.nameChangerEnabled || text == null || text.isBlank()) {
            return text;
        }
        if (settings.nameChangerOnlyInGame) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null) {
                return text;
            }
        }
        if (nameChangerSourceUsername == null || nameChangerSourceUsername.isBlank()) {
            refreshNameChangerUsername();
        }
        String username = nameChangerSourceUsername;
        if (username == null || username.isBlank()) {
            return text;
        }
        if (settings.nameChangerCensor) {
            if (username.length() <= 2) {
                return text;
            }
            return replaceIgnoreCase(text, username, censoredForm(username));
        }
        String nick = settings.nameChangerDisplayName == null ? "Duper" : settings.nameChangerDisplayName;
        nick = nick.replace("&", "§");
        // Undo stale censored fragments from when censor mode was previously enabled.
        text = replaceCensoredForm(text, username, nick);
        text = replaceIgnoreCase(text, username, nick);
        return text;
    }

    private static String censoredForm(String username) {
        return username.substring(0, 2) + "*".repeat(username.length() - 2);
    }

    private static String replaceCensoredForm(String text, String username, String replacement) {
        if (username.length() <= 2) {
            return text;
        }
        return replaceIgnoreCase(text, censoredForm(username), replacement);
    }

    private static String replaceIgnoreCase(String text, String target, String replacement) {
        if (target == null || target.isEmpty()) {
            return text;
        }
        return text.replaceAll("(?i)" + Pattern.quote(target), Matcher.quoteReplacement(replacement));
    }

    public void onNoTextureRotationsChanged(boolean enabled) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        if (client.worldRenderer != null) {
            client.worldRenderer.reload();
        }
    }

    public void feedback(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (settings.moduleChatFeedback) {
            sendHud(prefix().copy().append(Text.literal(message).formatted(Formatting.GRAY)));
        }
    }

    /** Whether staff detection has classified this username as staff (known list or live tab rank). */
    public boolean isStaffUsername(MinecraftClient client, String username) {
        if (!settings.staffDetectionEnabled || username == null || username.isBlank()) {
            return false;
        }
        for (StaffWatchEntry entry : knownStaff.values()) {
            if (entry.username != null && entry.username.equalsIgnoreCase(username)) {
                return true;
            }
        }
        if (client == null || client.getNetworkHandler() == null) {
            return false;
        }
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (entry == null || entry.getProfile() == null || entry.getProfile().name() == null) {
                continue;
            }
            if (!entry.getProfile().name().equalsIgnoreCase(username)) {
                continue;
            }
            String display = entry.getDisplayName() == null ? "" : entry.getDisplayName().getString();
            return rankFromPlayerText(display + " " + username) != null;
        }
        return false;
    }

    public boolean isStaffPlayer(MinecraftClient client, PlayerEntity player) {
        if (!settings.staffDetectionEnabled || player == null) {
            return false;
        }
        return staffRankForEntity(client, player) != null;
    }

    public int countOnlineStaff(MinecraftClient client) {
        if (!settings.staffDetectionEnabled || client == null || client.getNetworkHandler() == null) {
            return 0;
        }
        int count = 0;
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (entry == null || entry.getProfile() == null || entry.getProfile().name() == null) {
                continue;
            }
            String username = entry.getProfile().name();
            String display = entry.getDisplayName() == null ? "" : entry.getDisplayName().getString();
            if (rankFromPlayerText(display + " " + username) != null) {
                count++;
            }
        }
        return count;
    }

    public void tick(MinecraftClient client) {
        if (client == null || client.getNetworkHandler() == null || client.player == null) {
            onlineStaff.clear();
            detectedThisSession.clear();
            staffProximityAlerted.clear();
            invisibleEntityAlerted.clear();
            staffBaselineReady = false;
            return;
        }
        tickAntiInvisible(client);
        tickStaffProximity(client);
        if (!settings.staffDetectionEnabled) {
            onlineStaff.clear();
            detectedThisSession.clear();
            staffBaselineReady = false;
            return;
        }
        Set<UUID> currentlyOnline = new HashSet<>();
        Set<UUID> newlyDetectedThisTick = new HashSet<>();
        boolean dirty = false;
        String server = currentServerAddress(client);
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (entry == null || entry.getProfile() == null || entry.getProfile().id() == null) {
                continue;
            }
            UUID uuid = entry.getProfile().id();
            String username = entry.getProfile().name() == null ? "" : entry.getProfile().name();
            String display = entry.getDisplayName() == null ? "" : entry.getDisplayName().getString();
            String rank = rankFromPlayerText(display + " " + username);
            if (rank == null) {
                continue;
            }
            currentlyOnline.add(uuid);
            String key = uuid.toString();
            StaffWatchEntry prev = knownStaff.get(key);
            StaffWatchEntry now = prev == null ? new StaffWatchEntry() : prev;
            long ts = System.currentTimeMillis();
            if (prev == null) {
                now.firstSeenAtMs = ts;
                dirty = true;
            }
            now.uuid = key;
            if (!safeEq(now.username, username)) {
                now.username = username;
                dirty = true;
            }
            if (!safeEq(now.rank, rank)) {
                now.rank = rank;
                dirty = true;
            }
            if (!safeEq(now.lastServer, server)) {
                now.lastServer = server;
                dirty = true;
            }
            if (now.lastSeenAtMs != ts) {
                now.lastSeenAtMs = ts;
                dirty = true;
            }
            knownStaff.put(key, now);
            if (settings.staffDetectedAlerts && detectedThisSession.add(uuid)) {
                detection("Staff detected: " + username + " [" + rank + "]");
                newlyDetectedThisTick.add(uuid);
            }
        }

        if (settings.staffOnlineOfflineAlerts && staffBaselineReady) {
            for (UUID joined : currentlyOnline) {
                if (!onlineStaff.contains(joined)) {
                    if (newlyDetectedThisTick.contains(joined)) {
                        continue; // avoid duplicate "detected" + "online" spam for same staff on join/load
                    }
                    StaffWatchEntry e = knownStaff.get(joined.toString());
                    String name = e != null && e.username != null && !e.username.isBlank() ? e.username : joined.toString();
                    String rank = e != null && e.rank != null ? e.rank : "staff";
                    detection("Staff online: " + name + " [" + rank + "]");
                }
            }
            for (UUID left : onlineStaff) {
                if (!currentlyOnline.contains(left)) {
                    StaffWatchEntry e = knownStaff.get(left.toString());
                    String name = e != null && e.username != null && !e.username.isBlank() ? e.username : left.toString();
                    String rank = e != null && e.rank != null ? e.rank : "staff";
                    detection("Staff offline: " + name + " [" + rank + "]");
                }
            }
        }

        onlineStaff.clear();
        onlineStaff.addAll(currentlyOnline);
        staffBaselineReady = true;
        if (dirty) {
            SecurityStaffStore.save(knownStaff);
        }
    }

    private void tickStaffProximity(MinecraftClient client) {
        if (!settings.staffDetectionEnabled || !settings.staffProximityAlerts
                || client.world == null || client.player == null) {
            staffProximityAlerted.clear();
            return;
        }
        int radius = Math.max(8, Math.min(256, settings.staffProximityRadius));
        double radiusSq = (double) radius * radius;
        Set<UUID> inRange = new HashSet<>();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == null || player == client.player) {
                continue;
            }
            if (client.player.squaredDistanceTo(player) > radiusSq) {
                continue;
            }
            UUID uuid = player.getUuid();
            inRange.add(uuid);
            String rank = staffRankForEntity(client, player);
            if (rank == null) {
                continue;
            }
            if (staffProximityAlerted.add(uuid)) {
                int dist = (int) Math.round(Math.sqrt(client.player.squaredDistanceTo(player)));
                detection("Staff nearby: " + player.getName().getString() + " [" + rank + "] (" + dist + "m)");
            }
        }
        staffProximityAlerted.retainAll(inRange);
    }

    private String staffRankForEntity(MinecraftClient client, PlayerEntity player) {
        if (player == null) {
            return null;
        }
        String username = player.getName().getString();
        UUID uuid = player.getUuid();
        PlayerListEntry entry = client.getNetworkHandler() == null
                ? null
                : client.getNetworkHandler().getPlayerListEntry(uuid);
        if (entry != null) {
            String display = entry.getDisplayName() == null ? "" : entry.getDisplayName().getString();
            String rank = rankFromPlayerText(display + " " + username);
            if (rank != null) {
                return rank;
            }
        }
        for (StaffWatchEntry known : knownStaff.values()) {
            if (known.username != null && known.username.equalsIgnoreCase(username)) {
                return known.rank == null || known.rank.isBlank() ? "Staff" : known.rank;
            }
        }
        return null;
    }

    private void tickAntiInvisible(MinecraftClient client) {
        if (!settings.antiInvisibleEntities || client.world == null) {
            invisibleEntityAlerted.clear();
            return;
        }
        Set<Integer> seen = new HashSet<>();
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player) || player == client.player) {
                continue;
            }
            seen.add(player.getId());
            boolean invisible = player.isInvisible()
                    || (player.getStatusEffects() != null && player.hasStatusEffect(StatusEffects.INVISIBILITY));
            if (invisible && invisibleEntityAlerted.add(player.getId())) {
                detection("Invisible player entity: " + player.getName().getString());
            }
        }
        invisibleEntityAlerted.retainAll(seen);
    }

    public boolean onOutgoingPacket(Packet<?> packet) {
        if (packet == null || !settings.telemetryBlocking) {
            return false;
        }
        String simple = packet.getClass().getSimpleName();
        if (simple.toLowerCase(Locale.ROOT).contains("telemetry")) {
            detection("Blocked outgoing telemetry packet: " + simple);
            return true;
        }
        return false;
    }

    public boolean onIncomingPacket(Packet<?> packet) {
        if (packet == null) {
            return false;
        }
        if (settings.blockLocalPackUrls && packet instanceof ResourcePackSendS2CPacket) {
            String url = extractStringProperty(packet, "url", "getUrl", "URL", "getURL");
            if (!url.isBlank() && isLocalUrl(url)) {
                detection("Blocked local/private resource pack URL: " + url);
                return true;
            }
        }
        if (settings.keyProbeAlerts && packet instanceof GameMessageS2CPacket) {
            String text = extractGameMessageText(packet);
            if (looksLikeKeyProbe(text)) {
                detection("Possible keybind probe in server message: " + shorten(text, 90));
            }
        }
        return false;
    }

    /**
     * Call when the sign editor open packet is cancelled because the sign text matches a mod key-translation/probe
     * pattern (see security settings: block sign on key probe).
     */
    public void notifySignEditorBlockedKeyProbe(String posStr) {
        if (!shouldAlertSignKeyProbe()) {
            return;
        }
        String extra = (posStr == null || posStr.isBlank()) ? "" : " at " + posStr;
        signKeyProbeAlert("Blocked sign editor: key-translation probe" + extra);
    }

    /**
     * Call when the player opens a sign for editing and the text contains a key-resolution / translation probe
     * (so spoofing is active for that sign).
     */
    public void notifySignEditScreenKeyProbe() {
        if (!shouldAlertSignKeyProbe()) {
            return;
        }
        signKeyProbeAlert("Sign key-translation probe: key responses are being protected on this sign");
    }

    private boolean shouldAlertSignKeyProbe() {
        return settings.keyProbeAlerts && settings.keyResolutionProtection
                && SecurityKeyResolution.inRemoteMultiplayer();
    }

    private void signKeyProbeAlert(String message) {
        long now = System.currentTimeMillis();
        if (now - lastSignKeyProbeAlertMs < SIGN_KEY_PROBE_ALERT_COOLDOWN_MS) {
            return;
        }
        lastSignKeyProbeAlertMs = now;
        if (settings.logDetections) {
            DupeClient.LOGGER.warn("[Security] {}", message);
        }
        if (settings.moduleChatFeedback) {
            sendHud(prefix().copy().append(Text.literal(message).formatted(Formatting.YELLOW)));
        }
        if (settings.showToasts) {
            showToast("Sign key probe", message);
        }
    }

    private void detection(String message) {
        SecurityStaffTimeline.record(message);
        if (settings.logDetections) {
            DupeClient.LOGGER.warn("[Security] {}", message);
        }
        if (settings.moduleChatFeedback) {
            sendHud(prefix().copy().append(Text.literal(message).formatted(Formatting.YELLOW)));
        }
        if (settings.showToasts) {
            showToast("Security", message);
        }
    }

    private String rankFromPlayerText(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.toLowerCase(Locale.ROOT);
        for (String token : buildStaffKeywordCandidates()) {
            if (containsWord(t, token)) {
                return formatRankLabel(token);
            }
        }
        return null;
    }

    private List<String> buildStaffKeywordCandidates() {
        Set<String> out = new LinkedHashSet<>();
        List<String> suffixes = parseCsv(settings.staffRoleSuffixesCsv);
        List<String> prefixes = parseCsv(settings.staffRolePrefixesCsv);
        for (String s : suffixes) {
            out.add(s);
        }
        for (String p : prefixes) {
            for (String s : suffixes) {
                out.add(p + s); // e.g. srmod, jrhelper, srdev
            }
        }
        for (String extra : parseCsv(settings.staffRankKeywordsCsv)) {
            out.add(extra);
        }
        return new ArrayList<>(out);
    }

    private static List<String> parseCsv(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            String token = part == null ? "" : part.trim().toLowerCase(Locale.ROOT);
            if (!token.isEmpty()) {
                out.add(token);
            }
        }
        return out;
    }

    private static String formatRankLabel(String token) {
        if (token == null || token.isBlank()) {
            return "Staff";
        }
        String t = token.toLowerCase(Locale.ROOT);
        if (t.startsWith("sr") && t.length() > 2) {
            return "SR" + capitalize(t.substring(2));
        }
        if (t.startsWith("jr") && t.length() > 2) {
            return "JR" + capitalize(t.substring(2));
        }
        if ("dev".equals(t) || "developer".equals(t)) {
            return "Developer";
        }
        if ("mod".equals(t) || "moderator".equals(t)) {
            return "Moderator";
        }
        if ("helper".equals(t)) {
            return "Helper";
        }
        if ("owner".equals(t)) {
            return "Owner";
        }
        return capitalize(t);
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static boolean containsWord(String text, String word) {
        int i = text.indexOf(word);
        while (i >= 0) {
            boolean left = i == 0 || !Character.isLetterOrDigit(text.charAt(i - 1));
            int end = i + word.length();
            boolean right = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (left && right) {
                return true;
            }
            i = text.indexOf(word, i + 1);
        }
        return false;
    }

    private static String extractStringProperty(Object target, String... names) {
        for (String name : names) {
            try {
                Method m = target.getClass().getMethod(name);
                Object value = m.invoke(target);
                if (value != null) {
                    return value.toString().trim();
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private static Object invokeNoArgs(Object target, String... names) {
        for (String name : names) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean isLocalUrl(String raw) {
        try {
            URI uri = URI.create(raw.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            String h = host.trim().toLowerCase(Locale.ROOT);
            if (h.equals("localhost") || h.equals("0.0.0.0") || h.equals("127.0.0.1") || h.equals("::1")) {
                return true;
            }
            InetAddress address = InetAddress.getByName(h);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String extractGameMessageText(Object packet) {
        try {
            Object content = invokeNoArgs(packet, "content", "getContent");
            return content == null ? "" : content.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean looksLikeKeyProbe(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        String t = s.toLowerCase(Locale.ROOT);
        return t.contains("key.")
                && (t.contains("meteor")
                || t.contains("fabric")
                || t.contains("mod")
                || t.contains("open-gui")
                || t.contains("open gui"));
    }

    private static String shorten(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 3) + "...";
    }

    private static MutableText prefix() {
        return Text.literal("[Security] ").formatted(Formatting.GOLD, Formatting.BOLD);
    }

    private static void sendHud(Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(text, false);
            }
        });
    }

    private static void showToast(String title, String body) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> client.getToastManager().add(
                SystemToast.create(client, SystemToast.Type.PERIODIC_NOTIFICATION, Text.literal(title), Text.literal(shorten(body, 120)))
        ));
    }
}
