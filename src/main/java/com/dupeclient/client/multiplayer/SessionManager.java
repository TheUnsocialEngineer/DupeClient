package com.dupeclient.client.multiplayer;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.dupeclient.client.mixin.MinecraftClientSessionAccessor;
import com.dupeclient.client.mixin.MinecraftClientUserApiAccessor;
import com.dupeclient.client.module.security.SecurityManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.util.Util;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SessionManager {
    public static Session originalSession;
    public static Boolean isSessionValid;
    public static boolean hasValidationStarted;

    private SessionManager() {
    }

    public static void initialize() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && originalSession == null) {
            originalSession = client.getSession();
        }
    }

    public static void restoreSession() {
        if (originalSession == null) {
            initialize();
        }
        if (originalSession != null) {
            SecurityManager.INSTANCE.onSessionUsernameChanged(originalSession.getUsername());
            setSession(originalSession);
        }
    }

    public static boolean isSessionValid() {
        return isSessionValid != null && isSessionValid;
    }

    public static Session getSession() {
        return MinecraftClient.getInstance().getSession();
    }

    public static String getUsername() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }

    public static Session createSession(String username, String uuidString, String ssid) {
        if (uuidString.length() == 32) {
            uuidString = uuidString.substring(0, 8) + "-"
                    + uuidString.substring(8, 12) + "-"
                    + uuidString.substring(12, 16) + "-"
                    + uuidString.substring(16, 20) + "-"
                    + uuidString.substring(20, 32);
        }
        SecurityManager.INSTANCE.onSessionUsernameChanged(username);
        Session current = MinecraftClient.getInstance().getSession();
        return new Session(
                username,
                UUID.fromString(uuidString),
                ssid,
                current.getXuid(),
                current.getClientId());
    }

    public static void setSession(Session session) {
        isSessionValid = null;
        hasValidationStarted = false;

        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftClientSessionAccessor accessor = (MinecraftClientSessionAccessor) client;
        accessor.dupeClient$setSession(session);
        accessor.dupeClient$setGameProfileFuture(CompletableFuture.supplyAsync(
                () -> client.getApiServices().sessionService().fetchProfile(session.getUuidOrNull(), true),
                Util.getDownloadWorkerExecutor()));
        accessor.dupeClient$setSplashTextLoader(new SplashTextResourceSupplier(session));
        UserApiService userApiService = new YggdrasilAuthenticationService(client.getNetworkProxy())
                .createUserApiService(session.getAccessToken());
        MinecraftClientUserApiAccessor userApi = (MinecraftClientUserApiAccessor) client;
        userApi.dupeClient$setUserApiService(userApiService);
        accessor.dupeClient$setSocialInteractionsManager(new SocialInteractionsManager(client, userApiService));
        accessor.dupeClient$setProfileKeys(ProfileKeys.create(
                userApiService, session, FabricLoader.getInstance().getGameDir()));
        accessor.dupeClient$setAbuseReportContext(AbuseReportContext.create(
                ReporterEnvironment.ofIntegratedServer(), userApiService));
        OfflineAccountManager.onSessionSwapped(session.getUsername());
    }

    public static boolean isUsingOriginalSession() {
        initialize();
        if (originalSession == null) {
            return true;
        }
        return getSession().equals(originalSession);
    }
}
