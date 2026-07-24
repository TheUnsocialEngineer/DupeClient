package com.dupeclient.client.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public interface MinecraftClientUserApiAccessor {
    @Accessor("userApiService")
    UserApiService dupeClient$getUserApiService();

    @Accessor("userApiService")
    void dupeClient$setUserApiService(UserApiService service);

    @Accessor("userPropertiesFuture")
    CompletableFuture<UserApiService.UserProperties> dupeClient$getUserPropertiesFuture();

    @Accessor("userPropertiesFuture")
    void dupeClient$setUserPropertiesFuture(CompletableFuture<UserApiService.UserProperties> future);
}
