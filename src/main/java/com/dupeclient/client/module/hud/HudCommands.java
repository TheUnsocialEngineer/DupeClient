package com.dupeclient.client.module.hud;

import com.dupeclient.client.gui.IngameUiRouter;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class HudCommands {
    private HudCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("hud")
                .then(literal("editor").executes(ctx -> {
                    MinecraftClient client = ctx.getSource().getClient();
                    IngameUiRouter.openHudEditor(client.currentScreen);
                    feedback(ctx.getSource(), "Opened HUD editor.");
                    return 1;
                }))
                .then(literal("toggle").executes(ctx -> {
                    HudManager.INSTANCE.setActive(!HudManager.INSTANCE.isActive());
                    feedback(ctx.getSource(), "HUD " + (HudManager.INSTANCE.isActive() ? "enabled" : "disabled") + ".");
                    return 1;
                }))
                .then(literal("reset").executes(ctx -> {
                    HudManager.INSTANCE.resetToDefaultElements();
                    feedback(ctx.getSource(), "HUD elements reset to defaults.");
                    return 1;
                }))
                .executes(ctx -> {
                    feedback(ctx.getSource(), "Use /hud editor | toggle | reset");
                    return 1;
                }));
    }

    private static void feedback(FabricClientCommandSource src, String message) {
        src.sendFeedback(Text.literal("[HUD] ").formatted(Formatting.GOLD)
                .append(Text.literal(message).formatted(Formatting.GRAY)));
    }
}
