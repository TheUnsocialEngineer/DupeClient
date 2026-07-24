package com.dupeclient.client.core.session;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class SlashCommandGate {
    private SlashCommandGate() {
    }

    public static boolean blockExploit(FabricClientCommandSource source) {
        if (HubModuleRules.exploitFeaturesAllowed()) {
            return false;
        }
        source.sendFeedback(Text.literal("[DupeClient] ").formatted(Formatting.GOLD)
                .append(Text.literal(HubModuleRules.blockReason()).formatted(Formatting.RED)));
        return true;
    }

    public static boolean blockSocial(FabricClientCommandSource source) {
        if (HubModuleRules.socialFeaturesAllowed()) {
            return false;
        }
        source.sendFeedback(Text.literal("[DupeClient] ").formatted(Formatting.GOLD)
                .append(Text.literal(HubModuleRules.blockReason()).formatted(Formatting.RED)));
        return true;
    }
}
