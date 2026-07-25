package com.dupeclient.client.multiplayer;

import com.dupeclient.client.module.security.SecurityManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

public final class OfflineAccountManager {
    private static volatile String activeUsername = "";

    private OfflineAccountManager() {
    }

    public static String getActiveUsername() {
        return activeUsername;
    }

    public static void apply(MinecraftClient client, OfflineAccount account) {
        if (client == null || account == null || account.username().isBlank()) {
            return;
        }
        Session oldSession = client.getSession();
        Session newSession = new Session(
                account.username(),
                account.uuid(),
                oldSession.getAccessToken(),
                oldSession.getXuid(),
                oldSession.getClientId());
        SecurityManager.INSTANCE.onSessionUsernameChanged(account.username());
        SessionManager.setSession(newSession);
        activeUsername = account.username();
    }

    public static void applyUsername(MinecraftClient client, String username) {
        apply(client, OfflineAccount.ofUsername(username));
    }

    static void onSessionSwapped(String username) {
        if (username != null && !username.isBlank()) {
            activeUsername = username;
        }
    }
}
