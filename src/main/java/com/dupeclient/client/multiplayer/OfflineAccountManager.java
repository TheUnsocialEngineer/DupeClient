package com.dupeclient.client.multiplayer;

import com.dupeclient.client.module.security.SecurityManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

public final class OfflineAccountManager {
    private static volatile String activeUsername = "";

    private OfflineAccountManager() {
    }

    public static String getActiveUsername() {
        return activeUsername;
    }

    public static void apply(Minecraft client, OfflineAccount account) {
        if (client == null || account == null || account.username().isBlank()) {
            return;
        }
        User oldSession = client.getUser();
        User newSession = new User(
                account.username(),
                account.uuid(),
                oldSession.getAccessToken(),
                oldSession.getXuid(),
                oldSession.getClientId());
        SecurityManager.INSTANCE.onSessionUsernameChanged(account.username());
        SessionManager.setSession(newSession);
        activeUsername = account.username();
    }

    public static void applyUsername(Minecraft client, String username) {
        apply(client, OfflineAccount.ofUsername(username));
    }

    static void onSessionSwapped(String username) {
        if (username != null && !username.isBlank()) {
            activeUsername = username;
        }
    }
}
