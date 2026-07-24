package com.dupeclient.client.module.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-only troll: fills the local inventory with ghost items and prints fake dupe confirm lines.
 */
public final class DupeTrollCommand {
    private DupeTrollCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommands.literal("dupe")
                        .then(ClientCommands.argument("item", IdentifierArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                                .executes(ctx -> run(ctx, 0))
                                .then(ClientCommands.argument("count", IntegerArgumentType.integer(1, 99))
                                        .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
                        .executes(ctx -> {
                            feedback(ctx, "Use /dupe <item> [count] — e.g. /dupe diamond_block 64");
                            return 0;
                        }));
    }

    private static int run(CommandContext<FabricClientCommandSource> ctx, int requestedCount) {
        LocalPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            feedback(ctx, "Not in world.");
            return 0;
        }

        Identifier id = ctx.getArgument("item", Identifier.class);
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || item == Items.AIR) {
            feedback(ctx, "Unknown item: " + id);
            return 0;
        }

        int perStack = requestedCount > 0
                ? Math.min(requestedCount, item.getDefaultMaxStackSize())
                : item.getDefaultMaxStackSize();
        ItemStack prototype = new ItemStack(item, perStack);

        Inventory inv = player.getInventory();
        int slots = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            inv.setItem(i, prototype.copy());
            slots++;
        }

        int totalItems = slots * perStack;
        sendFakeDupeConfirm(player, id, perStack, slots, totalItems);
        return 1;
    }

    private static void sendFakeDupeConfirm(
            LocalPlayer player,
            Identifier itemId,
            int perStack,
            int slots,
            int totalItems) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String tx = String.format(Locale.ROOT, "%06X", rng.nextInt(0x100000, 0xFFFFFF));
        String crc = String.format(Locale.ROOT, "%04X", rng.nextInt(0x1000, 0xFFFF));
        String ack = String.format(Locale.ROOT, "0x%08X", rng.nextInt());

        player.sendSystemMessage(Component.literal("Initiating inventory shard replication…").withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(
                Component.literal("Transaction ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("#" + tx).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" — checksum ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("OK").withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" (crc32 ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(crc).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY)));
        player.sendSystemMessage(
                Component.literal("Slot map aligned — ACK ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(ack).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" received from replication buffer.").withStyle(ChatFormatting.GRAY)));
        player.sendSystemMessage(
                Component.literal("SUCCESS ")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                        .append(Component.literal("— ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("x" + perStack + " ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" × ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(Integer.toString(slots)).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" slots (").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(Integer.toString(totalItems)).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" items total)").withStyle(ChatFormatting.GRAY)));
        player.sendSystemMessage(
                Component.literal("Duplication routine complete. Do not relog for 30 seconds.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private static void feedback(CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(Component.literal("[Dupe] ").withStyle(ChatFormatting.DARK_PURPLE)
                .append(Component.literal(message).withStyle(ChatFormatting.GRAY)));
    }
}
