package com.dupeclient.client.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SplashManager;

@Mixin(Minecraft.class)
public interface MinecraftClientSessionAccessor {
    @Mutable
    @Accessor("user")
    void dupeClient$setSession(User session);

    @Mutable
    @Accessor("profileFuture")
    void dupeClient$setGameProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable
    @Accessor("splashManager")
    void dupeClient$setSplashTextLoader(SplashManager splashTextLoader);

    @Mutable
    @Accessor("playerSocialManager")
    void dupeClient$setSocialInteractionsManager(PlayerSocialManager socialInteractionsManager);

    @Mutable
    @Accessor("profileKeyPairManager")
    void dupeClient$setProfileKeys(ProfileKeyPairManager profileKeys);

    @Mutable
    @Accessor("reportingContext")
    void dupeClient$setAbuseReportContext(ReportingContext abuseReportContext);
}
