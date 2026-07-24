package com.dupeclient.client.module.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

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
                ClientCommandManager.literal("dupe")
                        .then(ClientCommandManager.argument("item", IdentifierArgumentType.identifier())
                                .suggests((ctx, builder) -> CommandSource.suggestIdentifiers(Registries.ITEM.getIds(), builder))
                                .executes(ctx -> run(ctx, 0))
                                .then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, 99))
                                        .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
                        .executes(ctx -> {
                            feedback(ctx, "Use /dupe <item> [count] — e.g. /dupe diamond_block 64");
                            return 0;
                        }));
    }

    private static int run(CommandContext<FabricClientCommandSource> ctx, int requestedCount) {
        ClientPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            feedback(ctx, "Not in world.");
            return 0;
        }

        Identifier id = ctx.getArgument("item", Identifier.class);
        Item item = Registries.ITEM.getOptionalValue(id).orElse(null);
        if (item == null || item == Items.AIR) {
            feedback(ctx, "Unknown item: " + id);
            return 0;
        }

        int perStack = requestedCount > 0
                ? Math.min(requestedCount, item.getMaxCount())
                : item.getMaxCount();
        ItemStack prototype = new ItemStack(item, perStack);

        PlayerInventory inv = player.getInventory();
        int slots = 0;
        for (int i = 0; i < inv.size(); i++) {
            inv.setStack(i, prototype.copy());
            slots++;
        }

        int totalItems = slots * perStack;
        sendFakeDupeConfirm(player, id, perStack, slots, totalItems);
        return 1;
    }

    private static void sendFakeDupeConfirm(
            ClientPlayerEntity player,
            Identifier itemId,
            int perStack,
            int slots,
            int totalItems) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        String tx = String.format(Locale.ROOT, "%06X", rng.nextInt(0x100000, 0xFFFFFF));
        String crc = String.format(Locale.ROOT, "%04X", rng.nextInt(0x1000, 0xFFFF));
        String ack = String.format(Locale.ROOT, "0x%08X", rng.nextInt());

        player.sendMessage(Text.literal("Initiating inventory shard replication…").formatted(Formatting.GRAY), false);
        player.sendMessage(
                Text.literal("Transaction ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal("#" + tx).formatted(Formatting.WHITE))
                        .append(Text.literal(" — checksum ").formatted(Formatting.GRAY))
                        .append(Text.literal("OK").formatted(Formatting.GREEN))
                        .append(Text.literal(" (crc32 ").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal(crc).formatted(Formatting.GREEN))
                        .append(Text.literal(")").formatted(Formatting.DARK_GRAY)),
                false);
        player.sendMessage(
                Text.literal("Slot map aligned — ACK ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(ack).formatted(Formatting.AQUA))
                        .append(Text.literal(" received from replication buffer.").formatted(Formatting.GRAY)),
                false);
        player.sendMessage(
                Text.literal("SUCCESS ")
                        .formatted(Formatting.GREEN, Formatting.BOLD)
                        .append(Text.literal("— ").formatted(Formatting.GRAY))
                        .append(Text.literal("x" + perStack + " ").formatted(Formatting.YELLOW))
                        .append(Text.literal(itemId.toString()).formatted(Formatting.WHITE))
                        .append(Text.literal(" × ").formatted(Formatting.GRAY))
                        .append(Text.literal(Integer.toString(slots)).formatted(Formatting.YELLOW))
                        .append(Text.literal(" slots (").formatted(Formatting.GRAY))
                        .append(Text.literal(Integer.toString(totalItems)).formatted(Formatting.GREEN))
                        .append(Text.literal(" items total)").formatted(Formatting.GRAY)),
                false);
        player.sendMessage(
                Text.literal("Duplication routine complete. Do not relog for 30 seconds.")
                        .formatted(Formatting.DARK_GRAY, Formatting.ITALIC),
                false);
    }

    private static void feedback(CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(Text.literal("[Dupe] ").formatted(Formatting.DARK_PURPLE)
                .append(Text.literal(message).formatted(Formatting.GRAY)));
    }
}
