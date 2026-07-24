package com.dupeclient.client.core.session;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class SlashCommandGate {
    private SlashCommandGate() {
    }

    public static boolean blockExploit(FabricClientCommandSource source) {
        if (HubModuleRules.exploitFeaturesAllowed()) {
            return false;
        }
        source.sendFeedback(Component.literal("[DupeClient] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(HubModuleRules.blockReason()).withStyle(ChatFormatting.RED)));
        return true;
    }

    public static boolean blockSocial(FabricClientCommandSource source) {
        if (HubModuleRules.socialFeaturesAllowed()) {
            return false;
        }
        source.sendFeedback(Component.literal("[DupeClient] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(HubModuleRules.blockReason()).withStyle(ChatFormatting.RED)));
        return true;
    }
}
