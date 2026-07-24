package com.dupeclient.client.module.utility.nbtedit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class NbtEditCommand {
    private NbtEditCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommands.literal("nbtedit")
                        .executes(NbtEditCommand::open));
    }

    private static int open(CommandContext<FabricClientCommandSource> ctx) {
        Minecraft client = ctx.getSource().getClient();
        if (client.player == null || client.level == null) {
            feedback(ctx, "Not in world.");
            return 0;
        }
        ItemStack held = client.player.getMainHandItem();
        if (held.isEmpty()) {
            feedback(ctx, "Hold an item in your main hand.");
            return 0;
        }
        ItemStack snapshot = held.copy();
        client.execute(() -> client.setScreen(new NbtEditScreen(null, snapshot)));
        feedback(ctx, "Opened NBT editor for " + ItemStackNbtCodec.itemSummary(held));
        return 1;
    }

    private static void feedback(CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(Component.literal("[NBT Edit] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(message).withStyle(ChatFormatting.GRAY)));
    }
}
