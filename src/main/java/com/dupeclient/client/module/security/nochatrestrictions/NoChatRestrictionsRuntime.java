package com.dupeclient.client.module.security.nochatrestrictions;

import com.dupeclient.client.mixin.MinecraftClientUserApiAccessor;
import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;

/** Applies or removes the wrapped UserApiService on the live client. */
public final class NoChatRestrictionsRuntime {
    private NoChatRestrictionsRuntime() {
    }

    public static void sync(boolean enabled) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        MinecraftClientUserApiAccessor access = (MinecraftClientUserApiAccessor) client;
        UserApiService current = access.dupeClient$getUserApiService();
        if (enabled) {
            if (current != null && !(current instanceof NoChatRestrictionsUserApiService)) {
                access.dupeClient$setUserApiService(new NoChatRestrictionsUserApiService(current));
            }
            access.dupeClient$setUserPropertiesFuture(
                    CompletableFuture.completedFuture(NoChatRestrictionsUserApiService.forcedProperties()));
        } else if (current instanceof NoChatRestrictionsUserApiService wrapped) {
            access.dupeClient$setUserApiService(wrapped.delegate());
            access.dupeClient$setUserPropertiesFuture(null);
        }
    }
}
