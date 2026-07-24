package com.dupeclient.client.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public interface MinecraftClientSessionAccessor {
    @Mutable
    @Accessor("session")
    void dupeClient$setSession(Session session);

    @Mutable
    @Accessor("gameProfileFuture")
    void dupeClient$setGameProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("splashTextLoader")
    void dupeClient$setSplashTextLoader(SplashTextResourceSupplier splashTextLoader);

    @Mutable
    @Accessor("socialInteractionsManager")
    void dupeClient$setSocialInteractionsManager(SocialInteractionsManager socialInteractionsManager);

    @Mutable
    @Accessor("profileKeys")
    void dupeClient$setProfileKeys(ProfileKeys profileKeys);

    @Mutable
    @Accessor("abuseReportContext")
    void dupeClient$setAbuseReportContext(AbuseReportContext abuseReportContext);
}
