package com.dupeclient.client.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
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
