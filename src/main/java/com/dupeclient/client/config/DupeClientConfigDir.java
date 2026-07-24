package com.dupeclient.client.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * All DupeClient files live under {@code .minecraft/config/dupeclient/}.
 * {@link #migrateFromLegacyLocations()} moves known older paths from {@code .minecraft/config/} once.
 */
public final class DupeClientConfigDir {
    public static final String FOLDER_NAME = "dupeclient";

    public static final String FILE_SECURITY = "security.json";
    public static final String FILE_SECURITY_STAFF = "security_staff.json";
    public static final String FILE_PRESENCE = "presence.json";
    public static final String FILE_SOCIAL_FRIENDS = "social_friends.json";
    public static final String FILE_PACKET_UTILS = "packet_utils.json";
    public static final String FILE_DUPEDB = "dupedb.json";
    public static final String FILE_VISUAL = "visual.json";
    public static final String FILE_PANELS = "panels.json";
    public static final String FILE_CLIENT_GUI_LAYOUT = "client_gui_layout.json";
    public static final String FILE_HUD = "hud.json";
    public static final String FILE_OFFLINE_ACCOUNTS = "offline_accounts.json";
    public static final String FILE_PROXIES = "proxies.json";
    public static final String FILE_SERVER_SEARCH_COOKIE = "minecraft_server_search_cookie.txt";
    public static final String FILE_SERVER_SCANNER_DUPEDB_MATCHES = "server_scanner_dupedb_matches.json";
    public static final String DIR_MACROS = "macros";
    public static final String FILE_MACRO_EDITOR_PREFERENCES = "macro_editor_preferences.json";
    public static final String FILE_PAY_EVERYONE = "pay-everyone.json";
    public static final String FILE_CRASHES = "crashes.json";
    public static final String FILE_CHAT_GAMES = "chat_games.json";
    public static final String FILE_UTILITY = "utility.json";
    public static final String FILE_ECONOMY_FUZZER = "economy_fuzzer.json";
    public static final String FILE_PACKET_SNIFFER = "packet_sniffer.json";
    public static final String FILE_AC_AUDIT = "ac_audit.json";
    public static final String FILE_MCP_TOOLS = "mcp_tools.json";
    public static final String DIR_PACKET_SNIFFER = "packet_sniffer";
    public static final String DIR_SERVER_PASSWORD = "server_password";
    public static final String FILE_SERVER_PASSWORD_DB = "vault.db";

    private static volatile boolean migrated;

    private DupeClientConfigDir() {
    }

    public static Path root() {
        return FabricLoader.getInstance().getConfigDir().resolve(FOLDER_NAME);
    }

    /**
     * Call once on client startup before any config load. Safe to call multiple times.
     */
    public static void migrateFromLegacyLocations() {
        if (migrated) {
            return;
        }
        migrated = true;
        Path cfg = FabricLoader.getInstance().getConfigDir();
        Path dest = root();
        try {
            Files.createDirectories(dest);
        } catch (IOException ignored) {
        }
        moveIfTargetMissing(cfg.resolve("dupeclient_security.json"), dest.resolve(FILE_SECURITY));
        moveIfTargetMissing(cfg.resolve("dupeclient_security_staff.json"), dest.resolve(FILE_SECURITY_STAFF));
        moveIfTargetMissing(cfg.resolve("dupeclient_presence.json"), dest.resolve(FILE_PRESENCE));
        moveIfTargetMissing(cfg.resolve("dupeclient_social_friends.json"), dest.resolve(FILE_SOCIAL_FRIENDS));
        moveIfTargetMissing(cfg.resolve("dupeclient_packet_utils.json"), dest.resolve(FILE_PACKET_UTILS));
        moveIfTargetMissing(cfg.resolve("dupeclient_dupedb.json"), dest.resolve(FILE_DUPEDB));
        moveIfTargetMissing(cfg.resolve("dupeclient_visual.json"), dest.resolve(FILE_VISUAL));
        moveIfTargetMissing(cfg.resolve("dupeclient_panels.json"), dest.resolve(FILE_PANELS));
        // HUD was already under config/dupeclient/hud.json in older builds — also accept legacy at config root.
        moveIfTargetMissing(cfg.resolve("dupeclient_hud.json"), dest.resolve(FILE_HUD));
        moveIfTargetMissing(cfg.resolve("pay-everyone.json"), dest.resolve(FILE_PAY_EVERYONE));
    }

    private static void moveIfTargetMissing(Path legacy, Path target) {
        try {
            if (Files.exists(target) || !Files.exists(legacy)) {
                return;
            }
            Files.createDirectories(target.getParent());
            try {
                Files.move(legacy, target);
            } catch (IOException moveFailed) {
                Files.copy(legacy, target, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(legacy);
            }
        } catch (IOException ignored) {
        }
    }
}
