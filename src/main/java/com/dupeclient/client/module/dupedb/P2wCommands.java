package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.core.session.SlashCommandGate;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class P2wCommands {
    private P2wCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("p2w")
                        .then(ClientCommandManager.literal("mark").executes(ctx -> {
                            if (SlashCommandGate.blockExploit(ctx.getSource())) {
                                return 0;
                            }
                            P2wMarkManager.INSTANCE.requestMark();
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("unmark").executes(ctx -> {
                            if (SlashCommandGate.blockExploit(ctx.getSource())) {
                                return 0;
                            }
                            P2wMarkManager.INSTANCE.requestUnmark();
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("confirm")
                                .then(ClientCommandManager.literal("mark").executes(ctx -> {
                                    P2wMarkManager.INSTANCE.confirm("mark");
                                    return 1;
                                }))
                                .then(ClientCommandManager.literal("unmark").executes(ctx -> {
                                    P2wMarkManager.INSTANCE.confirm("unmark");
                                    return 1;
                                })))
                        .then(ClientCommandManager.literal("abort").executes(ctx -> {
                            P2wMarkManager.INSTANCE.abort();
                            return 1;
                        }))
        );
    }
}
