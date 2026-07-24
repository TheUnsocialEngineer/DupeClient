package com.dupeclient.client.module.security.nochatrestrictions;

import com.dupeclient.client.module.security.SecurityConfigManager;
import com.dupeclient.client.module.security.SecurityManager;

/**
 * Feature gate for the built-in No Chat Restrictions implementation.
 * Reads security.json directly during early bootstrap because {@link SecurityManager}
 * is not loaded yet when {@code MinecraftClient#createUserApiService} runs.
 */
public final class NoChatRestrictionsGate {
    private static volatile Boolean cached;

    private NoChatRestrictionsGate() {
    }

    public static boolean active() {
        Boolean local = cached;
        if (local != null) {
            return local;
        }
        return SecurityConfigManager.load().noChatRestrictions;
    }

    public static void setCached(boolean enabled) {
        cached = enabled;
    }
}
