package com.ui_utils;

import net.minecraft.client.session.Session;

import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.UUID;

public final class SessionUtils {
    private SessionUtils() {
    }

    public static Session copyWith(Session oldSession, String username, UUID uuid) {
        String targetUsername = username != null ? username : oldSession.getUsername();
        UUID targetUuid = uuid != null ? uuid : oldSession.getUuidOrNull();
        String token = oldSession.getAccessToken();

        try {
            return new Session(
                    targetUsername,
                    targetUuid,
                    token,
                    oldSession.getXuid(),
                    oldSession.getClientId());
        } catch (Exception ignored) {
        }

        try {
            for (Constructor<?> ctor : Session.class.getDeclaredConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length != 6) {
                    continue;
                }
                ctor.setAccessible(true);
                Object accountType = resolveAccountType(params[5], oldSession);
                return (Session) ctor.newInstance(targetUsername, targetUuid, token, Optional.empty(), Optional.empty(), accountType);
            }
        } catch (Exception ignored) {
        }

        return oldSession;
    }

    private static Object resolveAccountType(Class<?> accountTypeClass, Session oldSession) {
        try {
            return Session.class.getMethod("getAccountType").invoke(oldSession);
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
