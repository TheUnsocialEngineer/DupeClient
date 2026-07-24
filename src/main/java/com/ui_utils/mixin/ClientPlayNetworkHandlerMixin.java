package com.ui_utils.mixin;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.dupedb.DupedbManager;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerManager;
import com.dupeclient.client.module.payall.PayAllManager;
import com.dupeclient.client.module.utility.ChatGamesManager;
import com.dupeclient.client.module.security.SecurityKeyResolution;
import com.dupeclient.client.module.security.SecurityManager;
import com.dupeclient.client.module.security.SecurityTextMarking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleOpenSignEditor", at = @At("HEAD"), cancellable = true)
    private void dupeClient$onSignEditorOpen(ClientboundOpenSignEditorPacket packet, CallbackInfo ci) {
        if (packet == null
                || !SecurityManager.INSTANCE.getSettings().keyResolutionBlockSignEditorOnKeyProbe
                || !SecurityManager.INSTANCE.getSettings().keyResolutionProtection) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        BlockEntity be = client.level.getBlockEntity(packet.getPos());
        if (!(be instanceof SignBlockEntity sign)) {
            return;
        }
        if (SecurityKeyResolution.signTextHasKeyResolutionProbe(sign.getFrontText())
                || SecurityKeyResolution.signTextHasKeyResolutionProbe(sign.getBackText())) {
            if (SecurityManager.INSTANCE.getSettings().logDetections) {
                DupeClient.LOGGER.info("[Security] Blocked sign editor (key probe on sign text at {})", packet.getPos());
            }
            SecurityManager.INSTANCE.notifySignEditorBlockedKeyProbe(String.valueOf(packet.getPos()));
            ci.cancel();
        }
    }

    @Inject(method = "handleCommandSuggestions", at = @At("HEAD"))
    private void dupeClient$onCommandSuggestions(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci) {
        DupedbManager.INSTANCE.onCommandSuggestions(packet);
        com.dupeclient.client.module.fuzzer.CommandArgDiscovery.INSTANCE.onCommandSuggestions(packet);
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void dupeClient$onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (packet != null && packet.content() != null) {
            SecurityTextMarking.markServerSourced(packet.content());
            PayAllManager.INSTANCE.onIncomingChatLine(packet.content().getString());
            EconomyFuzzerManager.INSTANCE.onIncomingChatLine(packet.content().getString());
            com.dupeclient.client.module.fuzzer.CommandArgDiscovery.INSTANCE.onChatLine(packet.content().getString());
            com.dupeclient.client.module.fuzzer.SqliFuzzerManager.INSTANCE.onIncomingChatLine(packet.content().getString());
            com.dupeclient.client.module.fuzzer.MinimessageFuzzerManager.INSTANCE.onIncomingChatLine(packet.content().getString());
            ChatGamesManager.INSTANCE.onIncomingGameMessage(packet.content().getString());
            DupedbManager.INSTANCE.onIncomingChatLine(packet.content().getString());
        }
    }

    @Inject(method = "setTitleText", at = @At("HEAD"))
    private void dupeClient$onTitle(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        if (packet != null && packet.text() != null) {
            SecurityTextMarking.markServerSourced(packet.text());
        }
    }

    @Inject(method = "setActionBarText", at = @At("HEAD"))
    private void dupeClient$onOverlayMessage(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
        if (packet != null && packet.text() != null) {
            SecurityTextMarking.markServerSourced(packet.text());
        }
    }

    @Inject(method = "handleTabListCustomisation", at = @At("HEAD"))
    private void dupeClient$onPlayerListHeader(ClientboundTabListPacket packet, CallbackInfo ci) {
        if (packet == null) {
            return;
        }
        if (packet.header() != null) {
            SecurityTextMarking.markServerSourced(packet.header());
        }
        if (packet.footer() != null) {
            SecurityTextMarking.markServerSourced(packet.footer());
        }
    }

    @Inject(method = "handleBossUpdate", at = @At("HEAD"))
    private void dupeClient$onBossBar(ClientboundBossEventPacket packet, CallbackInfo ci) {
        if (packet == null) {
            return;
        }
        packet.dispatch(new ClientboundBossEventPacket.Handler() {
            @Override
            public void remove(UUID uuid) {
            }

            @Override
            public void updateProgress(UUID uuid, float percent) {
            }

            @Override
            public void updateStyle(UUID uuid, BossEvent.BossBarColor color, BossEvent.BossBarOverlay style) {
            }

            @Override
            public void updateName(UUID uuid, Component name) {
                SecurityTextMarking.markServerSourced(name);
            }

            @Override
            public void add(UUID uuid, Component name, float percent, BossEvent.BossBarColor color, BossEvent.BossBarOverlay style,
                    boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
                SecurityTextMarking.markServerSourced(name);
            }

            @Override
            public void updateProperties(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
            }
        });
    }
}
