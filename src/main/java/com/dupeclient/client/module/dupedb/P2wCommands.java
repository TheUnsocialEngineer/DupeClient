package com.dupeclient.client.module.dupedb;

import com.dupeclient.client.core.session.SlashCommandGate;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class P2wCommands {
    private P2wCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommands.literal("p2w")
                        .then(ClientCommands.literal("mark").executes(ctx -> {
                            if (SlashCommandGate.blockExploit(ctx.getSource())) {
                                return 0;
                            }
                            P2wMarkManager.INSTANCE.requestMark();
                            return 1;
                        }))
                        .then(ClientCommands.literal("unmark").executes(ctx -> {
                            if (SlashCommandGate.blockExploit(ctx.getSource())) {
                                return 0;
                            }
                            P2wMarkManager.INSTANCE.requestUnmark();
                            return 1;
                        }))
                        .then(ClientCommands.literal("confirm")
                                .then(ClientCommands.literal("mark").executes(ctx -> {
                                    P2wMarkManager.INSTANCE.confirm("mark");
                                    return 1;
                                }))
                                .then(ClientCommands.literal("unmark").executes(ctx -> {
                                    P2wMarkManager.INSTANCE.confirm("unmark");
                                    return 1;
                                })))
                        .then(ClientCommands.literal("abort").executes(ctx -> {
                            P2wMarkManager.INSTANCE.abort();
                            return 1;
                        }))
        );
    }
}
