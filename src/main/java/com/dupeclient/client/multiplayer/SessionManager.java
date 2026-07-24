package com.dupeclient.client.multiplayer;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.dupeclient.client.mixin.MinecraftClientSessionAccessor;
import com.dupeclient.client.mixin.MinecraftClientUserApiAccessor;
import com.dupeclient.client.module.security.SecurityManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.util.Util;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SessionManager {
    public static User originalSession;
    public static Boolean isSessionValid;
    public static boolean hasValidationStarted;

    private SessionManager() {
    }

    public static void initialize() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && originalSession == null) {
            originalSession = client.getUser();
        }
    }

    public static void restoreSession() {
        if (originalSession == null) {
            initialize();
        }
        if (originalSession != null) {
            SecurityManager.INSTANCE.onSessionUsernameChanged(originalSession.getName());
            setSession(originalSession);
        }
    }

    public static boolean isSessionValid() {
        return isSessionValid != null && isSessionValid;
    }

    public static User getSession() {
        return Minecraft.getInstance().getUser();
    }

    public static String getUsername() {
        return Minecraft.getInstance().getUser().getName();
    }

    public static User createSession(String username, String uuidString, String ssid) {
        if (uuidString.length() == 32) {
            uuidString = uuidString.substring(0, 8) + "-"
                    + uuidString.substring(8, 12) + "-"
                    + uuidString.substring(12, 16) + "-"
                    + uuidString.substring(16, 20) + "-"
                    + uuidString.substring(20, 32);
        }
        SecurityManager.INSTANCE.onSessionUsernameChanged(username);
        User current = Minecraft.getInstance().getUser();
        return new User(
                username,
                UUID.fromString(uuidString),
                ssid,
                current.getXuid(),
                current.getClientId());
    }

    public static void setSession(User session) {
        isSessionValid = null;
        hasValidationStarted = false;

        Minecraft client = Minecraft.getInstance();
        MinecraftClientSessionAccessor accessor = (MinecraftClientSessionAccessor) client;
        accessor.dupeClient$setSession(session);
        accessor.dupeClient$setGameProfileFuture(CompletableFuture.supplyAsync(
                () -> client.services().sessionService().fetchProfile(session.getProfileId(), true),
                Util.nonCriticalIoPool()));
        accessor.dupeClient$setSplashTextLoader(new SplashManager(session));
        UserApiService userApiService = new YggdrasilAuthenticationService(client.getProxy())
                .createUserApiService(session.getAccessToken());
        MinecraftClientUserApiAccessor userApi = (MinecraftClientUserApiAccessor) client;
        userApi.dupeClient$setUserApiService(userApiService);
        accessor.dupeClient$setSocialInteractionsManager(new PlayerSocialManager(client, userApiService));
        accessor.dupeClient$setProfileKeys(ProfileKeyPairManager.create(
                userApiService, session, FabricLoader.getInstance().getGameDir()));
        accessor.dupeClient$setAbuseReportContext(ReportingContext.create(
                ReportEnvironment.local(), userApiService));
        OfflineAccountManager.onSessionSwapped(session.getName());
    }

    public static boolean isUsingOriginalSession() {
        initialize();
        if (originalSession == null) {
            return true;
        }
        return getSession().equals(originalSession);
    }
}
