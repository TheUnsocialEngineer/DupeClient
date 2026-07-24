package com.dupeclient.client.module.serverpassword;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class ServerPasswordCommands {
    private ServerPasswordCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("vault")
                .then(ClientCommands.literal("save").executes(ctx -> {
                    if (!ServerPasswordManager.INSTANCE.isUnlocked()) {
                        ctx.getSource().sendFeedback(Component.literal("Unlock the vault first.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    ServerPasswordManager.INSTANCE.confirmPendingSave();
                    return 1;
                }))
                .then(ClientCommands.literal("dismiss").executes(ctx -> {
                    ServerPasswordManager.INSTANCE.dismissPendingSave();
                    return 1;
                }))
                .then(ClientCommands.literal("lock").executes(ctx -> {
                    if (!ServerPasswordManager.INSTANCE.isVaultInitialized()) {
                        ctx.getSource().sendFeedback(Component.literal("Vault is not initialized.").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    ServerPasswordManager.INSTANCE.lock();
                    ctx.getSource().sendFeedback(Component.literal("Vault locked.").withStyle(ChatFormatting.YELLOW));
                    return 1;
                }))
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("Vault: /vault save | dismiss | lock").withStyle(ChatFormatting.GRAY));
                    return 1;
                }));
    }
}
