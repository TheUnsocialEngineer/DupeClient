package com.dupeclient.client.mixin;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.module.dupedb.DupedbManager;
import com.dupeclient.client.module.fuzzer.CommandArgDiscovery;
import com.dupeclient.client.module.fuzzer.MinimessageFuzzerManager;
import com.dupeclient.client.module.fuzzer.SqliFuzzerManager;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerManager;
import com.dupeclient.client.module.packet.command.CommandPacketSender;
import com.dupeclient.client.module.payall.PayAllManager;
import com.dupeclient.client.module.security.SecurityKeyResolution;
import com.dupeclient.client.module.security.SecurityManager;
import com.dupeclient.client.module.security.SecurityTextMarking;
import com.dupeclient.client.module.serverpassword.ServerPasswordManager;
import com.dupeclient.client.module.utility.ChatGamesManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.SignEditorOpenS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onSignEditorOpen", at = @At("HEAD"), cancellable = true)
    private void dupeClient$onSignEditorOpen(SignEditorOpenS2CPacket packet, CallbackInfo ci) {
        if (packet == null
                || !SecurityManager.INSTANCE.getSettings().keyResolutionBlockSignEditorOnKeyProbe
                || !SecurityManager.INSTANCE.getSettings().keyResolutionProtection) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        BlockEntity be = client.world.getBlockEntity(packet.getPos());
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

    @Inject(method = "onCommandSuggestions", at = @At("HEAD"))
    private void dupeClient$onCommandSuggestions(CommandSuggestionsS2CPacket packet, CallbackInfo ci) {
        DupedbManager.INSTANCE.onCommandSuggestions(packet);
        CommandArgDiscovery.INSTANCE.onCommandSuggestions(packet);
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void dupeClient$onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet != null && packet.content() != null) {
            SecurityTextMarking.markServerSourced(packet.content());
            PayAllManager.INSTANCE.onIncomingChatLine(packet.content().getString());
            EconomyFuzzerManager.INSTANCE.onIncomingChatLine(packet.content().getString());
            CommandArgDiscovery.INSTANCE.onChatLine(packet.content().getString());
            SqliFuzzerManager.INSTANCE.onIncomingChatLine(packet.content().getString());
            MinimessageFuzzerManager.INSTANCE.onIncomingChatLine(packet.content().getString());
            ChatGamesManager.INSTANCE.onIncomingGameMessage(packet.content().getString());
            DupedbManager.INSTANCE.onIncomingChatLine(packet.content().getString());
            CommandPacketSender.INSTANCE.onIncomingChatLine(packet.content().getString());
            ServerPasswordManager.INSTANCE.onIncomingChatLine(MinecraftClient.getInstance(), packet.content().getString());
        }
    }

    @Inject(method = "onTitle", at = @At("HEAD"))
    private void dupeClient$onTitle(TitleS2CPacket packet, CallbackInfo ci) {
        if (packet != null && packet.text() != null) {
            SecurityTextMarking.markServerSourced(packet.text());
        }
    }

    @Inject(method = "onOverlayMessage", at = @At("HEAD"))
    private void dupeClient$onOverlayMessage(OverlayMessageS2CPacket packet, CallbackInfo ci) {
        if (packet != null && packet.text() != null) {
            SecurityTextMarking.markServerSourced(packet.text());
        }
    }

    @Inject(method = "onPlayerListHeader", at = @At("HEAD"))
    private void dupeClient$onPlayerListHeader(PlayerListHeaderS2CPacket packet, CallbackInfo ci) {
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

    @Inject(method = "onBossBar", at = @At("HEAD"))
    private void dupeClient$onBossBar(BossBarS2CPacket packet, CallbackInfo ci) {
        if (packet == null) {
            return;
        }
        packet.accept(new BossBarS2CPacket.Consumer() {
            @Override
            public void remove(UUID uuid) {
            }

            @Override
            public void updateProgress(UUID uuid, float percent) {
            }

            @Override
            public void updateStyle(UUID uuid, BossBar.Color color, BossBar.Style style) {
            }

            @Override
            public void updateName(UUID uuid, Text name) {
                SecurityTextMarking.markServerSourced(name);
            }

            @Override
            public void add(UUID uuid, Text name, float percent, BossBar.Color color, BossBar.Style style,
                    boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
                SecurityTextMarking.markServerSourced(name);
            }

            @Override
            public void updateProperties(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
            }
        });
    }
}
