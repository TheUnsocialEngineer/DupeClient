package com.ui_utils;

import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.User;

public final class SessionUtils {
    private SessionUtils() {
    }

    public static User copyWith(User oldSession, String username, UUID uuid) {
        String targetUsername = username != null ? username : oldSession.getName();
        UUID targetUuid = uuid != null ? uuid : oldSession.getProfileId();
        String token = oldSession.getAccessToken();

        try {
            for (Constructor<?> ctor : User.class.getDeclaredConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length != 6) {
                    continue;
                }
                ctor.setAccessible(true);
                Object accountType = resolveAccountType(params[5], oldSession);
                return (User) ctor.newInstance(targetUsername, targetUuid, token, Optional.empty(), Optional.empty(), accountType);
            }
        } catch (Exception ignored) {
        }

        return oldSession;
    }

    private static Object resolveAccountType(Class<?> accountTypeClass, User oldSession) {
        try {
            return User.class.getMethod("getAccountType").invoke(oldSession);
        } catch (Exception ignored) {
        }
        try {
            Object[] constants = accountTypeClass.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return constants[0];
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
