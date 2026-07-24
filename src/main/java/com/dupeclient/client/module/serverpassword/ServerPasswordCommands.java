package com.dupeclient.client.module.serverpassword;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ServerPasswordCommands {
    private ServerPasswordCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("vault")
                .then(ClientCommandManager.literal("save").executes(ctx -> {
                    if (!ServerPasswordManager.INSTANCE.isUnlocked()) {
                        ctx.getSource().sendFeedback(Text.literal("Unlock the vault first.").formatted(Formatting.RED));
                        return 0;
                    }
                    ServerPasswordManager.INSTANCE.confirmPendingSave();
                    return 1;
                }))
                .then(ClientCommandManager.literal("dismiss").executes(ctx -> {
                    ServerPasswordManager.INSTANCE.dismissPendingSave();
                    return 1;
                }))
                .then(ClientCommandManager.literal("lock").executes(ctx -> {
                    if (!ServerPasswordManager.INSTANCE.isVaultInitialized()) {
                        ctx.getSource().sendFeedback(Text.literal("Vault is not initialized.").formatted(Formatting.RED));
                        return 0;
                    }
                    ServerPasswordManager.INSTANCE.lock();
                    ctx.getSource().sendFeedback(Text.literal("Vault locked.").formatted(Formatting.YELLOW));
                    return 1;
                }))
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Text.literal("Vault: /vault save | dismiss | lock").formatted(Formatting.GRAY));
                    return 1;
                }));
    }
}
