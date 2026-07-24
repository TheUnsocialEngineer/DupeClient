package com.dupeclient.client.multiplayer;

import com.dupeclient.client.module.security.SecurityManager;
import com.ui_utils.SessionUtils;
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
        User newSession = SessionUtils.copyWith(oldSession, account.username(), account.uuid());
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
