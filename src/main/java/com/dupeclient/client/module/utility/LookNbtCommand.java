package com.dupeclient.client.module.utility;

import com.dupeclient.client.core.LookTargetNbtUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public final class LookNbtCommand {
    private static final int CHAT_CHUNK = 32000;

    private LookNbtCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommands.literal("looknbt")
                .executes(ctx -> run(ctx, false))
                .then(ClientCommands.literal("print").executes(ctx -> run(ctx, true)))
                .then(ClientCommands.literal("chat").executes(ctx -> run(ctx, true)))
        );
    }

    private static int run(CommandContext<FabricClientCommandSource> ctx, boolean printToChat) {
        Minecraft client = ctx.getSource().getClient();
        if (client.player == null || client.level == null) {
            feedback(ctx, "Not in world.");
            return 0;
        }

        LookTargetNbtUtil.CaptureResult result = LookTargetNbtUtil.capture(client);
        if (result == null) {
            feedback(ctx, "No block or entity in crosshair.");
            return 0;
        }

        client.keyboardHandler.setClipboard(result.snbt());
        feedback(ctx, "Copied " + result.kind() + " NBT (" + result.snbt().length() + " chars) — " + result.summary());

        if (printToChat) {
            sendChunked(ctx, Component.literal(result.snbt()).withStyle(ChatFormatting.DARK_AQUA));
        } else {
            MutableComponent preview = Component.literal("[Preview] ").withStyle(ChatFormatting.GRAY)
                .append(preview(result.snbt()));
            ctx.getSource().sendFeedback(preview);
        }
        return 1;
    }

    private static MutableComponent preview(String snbt) {
        String shortText = snbt.length() <= 180 ? snbt : snbt.substring(0, 177) + "...";
        return Component.literal(shortText)
            .withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy full NBT again")))
                .withClickEvent(new ClickEvent.CopyToClipboard(snbt)));
    }

    private static void sendChunked(CommandContext<FabricClientCommandSource> ctx, Component text) {
        String raw = text.getString();
        if (raw.length() <= CHAT_CHUNK) {
            ctx.getSource().sendFeedback(text);
            return;
        }
        int part = 1;
        for (int i = 0; i < raw.length(); i += CHAT_CHUNK) {
            String slice = raw.substring(i, Math.min(raw.length(), i + CHAT_CHUNK));
            ctx.getSource().sendFeedback(Component.literal("[NBT " + part + "] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(slice).withStyle(ChatFormatting.DARK_AQUA)));
            part++;
        }
    }

    private static void feedback(CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(Component.literal("[LookNBT] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(message).withStyle(ChatFormatting.GRAY)));
    }
}
