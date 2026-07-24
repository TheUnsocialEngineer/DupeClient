package com.dupeclient.client.module.utility;

import com.dupeclient.client.core.LookTargetNbtUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class LookNbtCommand {
    private static final int CHAT_CHUNK = 32000;

    private LookNbtCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("looknbt")
                .executes(ctx -> run(ctx, false))
                .then(ClientCommandManager.literal("print").executes(ctx -> run(ctx, true)))
                .then(ClientCommandManager.literal("chat").executes(ctx -> run(ctx, true)))
        );
    }

    private static int run(CommandContext<FabricClientCommandSource> ctx, boolean printToChat) {
        MinecraftClient client = ctx.getSource().getClient();
        if (client.player == null || client.world == null) {
            feedback(ctx, "Not in world.");
            return 0;
        }

        LookTargetNbtUtil.CaptureResult result = LookTargetNbtUtil.capture(client);
        if (result == null) {
            feedback(ctx, "No block or entity in crosshair.");
            return 0;
        }

        client.keyboard.setClipboard(result.snbt());
        feedback(ctx, "Copied " + result.kind() + " NBT (" + result.snbt().length() + " chars) — " + result.summary());

        if (printToChat) {
            sendChunked(ctx, Text.literal(result.snbt()).formatted(Formatting.DARK_AQUA));
        } else {
            MutableText preview = Text.literal("[Preview] ").formatted(Formatting.GRAY)
                .append(preview(result.snbt()));
            ctx.getSource().sendFeedback(preview);
        }
        return 1;
    }

    private static MutableText preview(String snbt) {
        String shortText = snbt.length() <= 180 ? snbt : snbt.substring(0, 177) + "...";
        return Text.literal(shortText)
            .styled(style -> style
                .withColor(Formatting.AQUA)
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to copy full NBT again")))
                .withClickEvent(new ClickEvent.CopyToClipboard(snbt)));
    }

    private static void sendChunked(CommandContext<FabricClientCommandSource> ctx, Text text) {
        String raw = text.getString();
        if (raw.length() <= CHAT_CHUNK) {
            ctx.getSource().sendFeedback(text);
            return;
        }
        int part = 1;
        for (int i = 0; i < raw.length(); i += CHAT_CHUNK) {
            String slice = raw.substring(i, Math.min(raw.length(), i + CHAT_CHUNK));
            ctx.getSource().sendFeedback(Text.literal("[NBT " + part + "] ").formatted(Formatting.GOLD)
                .append(Text.literal(slice).formatted(Formatting.DARK_AQUA)));
            part++;
        }
    }

    private static void feedback(CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(Text.literal("[LookNBT] ").formatted(Formatting.GOLD)
            .append(Text.literal(message).formatted(Formatting.GRAY)));
    }
}
