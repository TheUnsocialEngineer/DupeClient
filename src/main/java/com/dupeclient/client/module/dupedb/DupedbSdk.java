package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.config.DupeClientConfigDir;
import com.dupedb.api.DupeDB;
import com.dupedb.api.DupeDBClient;
import com.dupedb.api.auth.Credentials;
import com.dupedb.api.auth.OAuthFlow;
import com.dupedb.api.auth.TokenStore;
import com.dupedb.api.exception.DupeDBException;
import com.dupedb.api.exception.OAuthException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * DupeDB auth via the official {@code DupeDB/java-api} SDK (OAuth 2.1 + PKCE or PAT).
 * See <a href="https://dupedb.net/resource/developer-documentation#quick-start-java-sdk">Developer docs</a>.
 */
public final class DupedbSdk {
    public static final DupedbSdk INSTANCE = new DupedbSdk();

    /** Registered Desktop OAuth app for DupeClient on dupedb.net. */
    public static final String DEFAULT_OAUTH_APP_ID = "dupeclient";
    public static final String PAT_PREFIX = "dupe_pat_";
    private static final int OAUTH_PORT = 38475;
    private static final Path TOKEN_STORE_PATH = DupeClientConfigDir.root().resolve("dupedb-token.json");

    private volatile DupeDBClient client;
    private volatile String personalAccessToken;
    private volatile String oauthAppId = DEFAULT_OAUTH_APP_ID;
    private volatile boolean oauthInFlight;

    private DupedbSdk() {
    }

    /** Uses Minecraft's OS browser hook when AWT Desktop is unavailable in-game. */
    public static void registerMinecraftBrowserOpener() {
        OAuthFlow.setBrowserOpener(url -> net.minecraft.util.Util.getPlatform().openUri(url));
    }

    public Path tokenStorePath() {
        return TOKEN_STORE_PATH;
    }

    public String oauthAppId() {
        String id = oauthAppId == null ? "" : oauthAppId.trim();
        return id.isBlank() ? DEFAULT_OAUTH_APP_ID : id;
    }

    public synchronized void configureOAuthAppId(String appId) {
        String trimmed = appId == null ? "" : appId.trim().toLowerCase();
        oauthAppId = trimmed.isBlank() ? DEFAULT_OAUTH_APP_ID : trimmed;
        resetClient();
    }

    public synchronized void setPersonalAccessToken(String token) {
        personalAccessToken = token == null ? "" : token.trim();
        resetClient();
    }

    public synchronized void clearAuth() {
        DupeDBClient current = client;
        if (current != null) {
            current.clearAuth();
        }
        personalAccessToken = null;
        try {
            Files.deleteIfExists(TOKEN_STORE_PATH);
        } catch (Exception ignored) {
        }
        resetClient();
    }

    public boolean isOauthInFlight() {
        return oauthInFlight;
    }

    public boolean isAuthenticated() {
        try {
            if (usingPersonalAccessToken()) {
                return true;
            }
            return client().isAuthenticated();
        } catch (Exception e) {
            return false;
        }
    }

    public DupeDBClient client() {
        DupeDBClient cached = client;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            cached = client;
            if (cached != null) {
                return cached;
            }
            DupeDB.Builder builder = DupeDB.client();
            if (usingPersonalAccessToken()) {
                if (personalAccessToken.startsWith(PAT_PREFIX)) {
                    builder.personalAccessToken(personalAccessToken);
                } else {
                    builder.token(personalAccessToken);
                }
            } else {
                builder.oauth(oauthAppId(), "http://localhost:" + OAUTH_PORT + "/dupedb-callback")
                        .tokenStore(TOKEN_STORE_PATH);
            }
            client = cached = builder.build();
            return cached;
        }
    }

    /** Resolves a Bearer token for legacy raw HTTP callers. Never opens the OAuth browser. */
    public String getAccessTokenOrEmpty() {
        if (usingPersonalAccessToken()) {
            return personalAccessToken;
        }
        if (!client().isAuthenticated()) {
            return "";
        }
        try {
            refreshTokenIfNearExpiry();
            Credentials credentials = new TokenStore(TOKEN_STORE_PATH).load();
            return credentials == null || credentials.accessToken() == null
                    ? ""
                    : credentials.accessToken();
        } catch (DupeDBException e) {
            DupeClient.LOGGER.debug("[DupeDB] could not resolve access token", e);
            return "";
        }
    }

    /**
     * Runs the SDK OAuth browser flow on a background thread. First authenticated call triggers PKCE + loopback.
     */
    public void startOAuthLogin(
            Runnable onOpeningBrowser,
            Runnable onSuccess,
            java.util.function.Consumer<String> onFailure
    ) {
        if (oauthInFlight) {
            onFailure.accept("OAuth is already in progress.");
            return;
        }
        oauthInFlight = true;
        Thread thread = new Thread(() -> {
            try {
                onOpeningBrowser.run();
                client().user().me();
                onSuccess.run();
            } catch (OAuthException e) {
                onFailure.accept(formatOAuthError(e));
            } catch (DupeDBException e) {
                onFailure.accept(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            } catch (Exception e) {
                DupeClient.LOGGER.error("[DupeDB] OAuth failed", e);
                onFailure.accept(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            } finally {
                oauthInFlight = false;
            }
        }, "DupeDB-OAuth");
        thread.setDaemon(true);
        thread.start();
    }

    /** Copies tokens from legacy {@code dupedb.json} into the SDK token store once. */
    public synchronized void migrateLegacyOAuth(
            String accessToken,
            String refreshToken,
            long expiresAtMs,
            String appId
    ) {
        if (Files.exists(TOKEN_STORE_PATH)) {
            return;
        }
        String access = accessToken == null ? "" : accessToken.trim();
        String refresh = refreshToken == null ? "" : refreshToken.trim();
        if (access.isBlank() || refresh.isBlank()) {
            return;
        }
        Instant expiresAt = expiresAtMs > 0L
                ? Instant.ofEpochMilli(expiresAtMs)
                : Instant.now().plusSeconds(3600L);
        String slug = appId == null || appId.isBlank() ? DEFAULT_OAUTH_APP_ID : appId.trim();
        try {
            new TokenStore(TOKEN_STORE_PATH).save(new Credentials(
                    access,
                    slug,
                    Instant.now().toString(),
                    refresh,
                    expiresAt
            ));
            oauthAppId = slug;
            resetClient();
        } catch (Exception e) {
            DupeClient.LOGGER.warn("[DupeDB] failed to migrate legacy OAuth tokens", e);
        }
    }

    private void refreshTokenIfNearExpiry() throws DupeDBException {
        TokenStore store = new TokenStore(TOKEN_STORE_PATH);
        Credentials credentials = store.load();
        if (credentials == null || credentials.expiresAt() == null) {
            return;
        }
        if (credentials.expiresAt().minusSeconds(60).isAfter(Instant.now())) {
            return;
        }
        client().metadata().tags();
        store.load();
    }

    private boolean usingPersonalAccessToken() {
        return personalAccessToken != null && !personalAccessToken.isBlank();
    }

    private synchronized void resetClient() {
        DupeDBClient current = client;
        client = null;
        if (current != null) {
            try {
                current.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static String formatOAuthError(OAuthException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("unknown client_id")) {
            return "unknown client_id — use app id \"dupeclient\" or register a Desktop OAuth app at dupedb.net/settings/developer";
        }
        return message.isBlank() ? "OAuth failed" : message;
    }
}
