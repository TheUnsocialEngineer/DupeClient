/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.ImmutableSet$Builder
 *  com.mojang.authlib.minecraft.TelemetrySession
 *  com.mojang.authlib.minecraft.UserApiService
 *  com.mojang.authlib.minecraft.UserApiService$UserFlag
 *  com.mojang.authlib.minecraft.UserApiService$UserProperties
 *  com.mojang.authlib.minecraft.report.AbuseReportLimits
 *  com.mojang.authlib.yggdrasil.request.AbuseReportRequest
 *  com.mojang.authlib.yggdrasil.response.KeyPairResponse
 */
package com.dupeclient.client.module.security.nochatrestrictions;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

public final class NoChatRestrictionsUserApiService
implements UserApiService {
    private static final UserApiService.UserProperties FORCED_PROPERTIES;
    private final UserApiService delegate;

    public static UserApiService.UserProperties forcedProperties() {
        return FORCED_PROPERTIES;
    }

    public UserApiService delegate() {
        return this.delegate;
    }

    public NoChatRestrictionsUserApiService(UserApiService delegate) {
        this.delegate = delegate;
    }

    public UserApiService.UserProperties fetchProperties() {
        return FORCED_PROPERTIES;
    }

    public boolean isBlockedPlayer(UUID playerId) {
        return this.delegate.isBlockedPlayer(playerId);
    }

    public void refreshBlockList() {
        this.delegate.refreshBlockList();
    }

    public TelemetrySession newTelemetrySession(Executor executor) {
        return TelemetrySession.DISABLED;
    }

    public KeyPairResponse getKeyPair() {
        return this.delegate.getKeyPair();
    }

    public void reportAbuse(AbuseReportRequest request) {
        this.delegate.reportAbuse(request);
    }

    public boolean canSendReports() {
        return this.delegate.canSendReports();
    }

    public AbuseReportLimits getAbuseReportLimits() {
        return this.delegate.getAbuseReportLimits();
    }

    static {
        ImmutableSet.Builder flags = ImmutableSet.builder();
        flags.add((Object)UserApiService.UserFlag.CHAT_ALLOWED);
        flags.add((Object)UserApiService.UserFlag.SERVERS_ALLOWED);
        flags.add((Object)UserApiService.UserFlag.REALMS_ALLOWED);
        FORCED_PROPERTIES = new UserApiService.UserProperties((Set)flags.build(), Map.of());
    }
}

