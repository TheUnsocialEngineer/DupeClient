package com.dupeclient.client.module.security;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpSec-style "fake default keybinds": resolves keybind IDs to their vanilla default display values.
 * Uses reflection for version tolerance across nearby MC mappings.
 */
final class SecurityKeybindDefaults {
    private static final Map<String, String> defaults = new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private SecurityKeybindDefaults() {
    }

    static boolean hasDefault(String keyId) {
        ensureInit();
        return defaults.containsKey(keyId);
    }

    static String getDefault(String keyId) {
        ensureInit();
        return defaults.get(keyId);
    }

    private static void ensureInit() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c == null || c.options == null || c.options.allKeys == null) {
                return;
            }
            for (KeyBinding kb : c.options.allKeys) {
                if (kb == null) {
                    continue;
                }
                String id = resolveKeyId(kb);
                String value = resolveDefaultDisplay(kb);
                if (id != null && !id.isBlank() && value != null && !value.isBlank()) {
                    defaults.put(id.toLowerCase(), value);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static String resolveKeyId(KeyBinding kb) {
        String[] names = new String[] {
                "getBoundKeyTranslationKey",
                "getTranslationKey",
                "getKeyTranslationKey"
        };
        for (String name : names) {
            try {
                Method m = kb.getClass().getMethod(name);
                Object val = m.invoke(kb);
                if (val != null) {
                    return val.toString();
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static String resolveDefaultDisplay(KeyBinding kb) {
        try {
            Method mDefault = kb.getClass().getMethod("getDefaultKey");
            Object defaultKey = mDefault.invoke(kb);
            if (defaultKey == null) {
                return null;
            }
            Method mDisplay = defaultKey.getClass().getMethod("getLocalizedText");
            Object text = mDisplay.invoke(defaultKey);
            if (text == null) {
                return null;
            }
            Method mString = text.getClass().getMethod("getString");
            Object val = mString.invoke(text);
            return val == null ? null : val.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
