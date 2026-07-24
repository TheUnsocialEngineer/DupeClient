package com.dupeclient.client.module.utility.nbtedit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class NbtEditCommand {
    private NbtEditCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("nbtedit")
                        .executes(NbtEditCommand::open));
    }

    private static int open(CommandContext<FabricClientCommandSource> ctx) {
        MinecraftClient client = ctx.getSource().getClient();
        if (client.player == null || client.world == null) {
            feedback(ctx, "Not in world.");
            return 0;
        }
        ItemStack held = client.player.getMainHandStack();
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
        ctx.getSource().sendFeedback(Text.literal("[NBT Edit] ").formatted(Formatting.GOLD)
                .append(Text.literal(message).formatted(Formatting.GRAY)));
    }
}
