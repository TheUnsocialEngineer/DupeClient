package com.dupeclient.client.module.macro;

import com.dupeclient.client.core.session.SlashCommandGate;
import com.dupeclient.client.gui.MacroEditorScreen;
import com.dupeclient.client.gui.MacroPromptScreen;
import com.dupeclient.client.gui.MacroShareScreen;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.nio.file.Path;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class MacroCommands {
    private MacroCommands() {
    }

    public static void register(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> macro = literal("macro")
                .then(literal("list").executes(ctx -> {
                    MacroStorage.prepare();
                    var ids = MacroStorage.listMacroIds();
                    if (ids.isEmpty()) {
                        feedback(ctx, "No macros in config/dupeclient/macros/ (example_linear.json is created on first use).");
                        return 1;
                    }
                    feedback(ctx, "Macros: " + String.join(", ", ids));
                    return 1;
                }))
                .then(literal("run").then(argument("id", StringArgumentType.word()).executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    String id = StringArgumentType.getString(ctx, "id");
                    MacroEngine.INSTANCE.start(ctx.getSource().getClient(), id);
                    return 1;
                })))
                .then(literal("stop").executes(ctx -> {
                    MacroEngine.INSTANCE.stop(ctx.getSource().getClient());
                    return 1;
                }))
                .then(literal("delete").then(argument("id", StringArgumentType.word()).executes(ctx -> {
                    String id = StringArgumentType.getString(ctx, "id");
                    Minecraft client = ctx.getSource().getClient();
                    String active = MacroEngine.INSTANCE.getActiveMacroId();
                    try {
                        MacroStorage.deleteMacro(id);
                        if (active != null && !active.isEmpty()
                                && MacroStorage.filenameId(active).equalsIgnoreCase(MacroStorage.filenameId(id))) {
                            MacroEngine.INSTANCE.stop(client);
                        }
                        feedback(ctx, "Deleted macro \"" + MacroStorage.filenameId(id) + "\".");
                    } catch (IOException e) {
                        feedback(ctx, e.getMessage() == null ? "Delete failed" : e.getMessage());
                    }
                    return 1;
                })))
                .then(literal("folder").executes(ctx -> {
                    MacroStorage.prepare();
                    feedback(ctx, "Macro folder: " + MacroStorage.macrosDirectory().toAbsolutePath());
                    return 1;
                }))
                .then(literal("studio").executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    MacroEditorScreen.open(ctx.getSource().getClient(), null);
                    feedback(ctx, "Opening macro editor (use macro studio <id> to load one).");
                    return 1;
                }).then(argument("id", StringArgumentType.word()).executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    String id = StringArgumentType.getString(ctx, "id");
                    MacroEditorScreen.open(ctx.getSource().getClient(), id);
                    feedback(ctx, "Opening macro editor: " + id);
                    return 1;
                })))
                .then(literal("prompt").executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    MacroPromptScreen.open(ctx.getSource().getClient(), ctx.getSource().getClient().gui.screen());
                    feedback(ctx, "Opening macro prompt generator.");
                    return 1;
                }).then(argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    String prompt = StringArgumentType.getString(ctx, "text");
                    return generateFromPrompt(ctx, prompt, null, null, false);
                })))
                .then(literal("generate")
                        .then(argument("id", StringArgumentType.word())
                                .then(argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                                        return 0;
                                    }
                                    String id = StringArgumentType.getString(ctx, "id");
                                    String prompt = StringArgumentType.getString(ctx, "text");
                                    return generateFromPrompt(ctx, prompt, id, null, false);
                                }))))
                .then(literal("export").then(argument("id", StringArgumentType.word()).executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    String id = StringArgumentType.getString(ctx, "id");
                    Minecraft client = ctx.getSource().getClient();
                    if (MacroShare.exportMacroToClipboard(client, id)) {
                        feedback(ctx, "Copied export bundle for \"" + MacroStorage.filenameId(id) + "\" to clipboard.");
                    }
                    return 1;
                })))
                .then(literal("import").executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    MacroShareScreen.open(ctx.getSource().getClient(), ctx.getSource().getClient().gui.screen(), null);
                    feedback(ctx, "Opening macro import screen (clipboard JSON).");
                    return 1;
                }))
                .then(literal("importclip").executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    MacroImportResult result = MacroShare.importFromClipboard(ctx.getSource().getClient(), null, false);
                    return result.success() ? 1 : 0;
                }).then(argument("id", StringArgumentType.word()).executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    String targetId = StringArgumentType.getString(ctx, "id");
                    MacroImportResult result = MacroShare.importFromClipboard(ctx.getSource().getClient(), targetId, false);
                    return result.success() ? 1 : 0;
                })))
                .then(literal("importoverwrite").then(argument("id", StringArgumentType.word()).executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    String targetId = StringArgumentType.getString(ctx, "id");
                    MacroImportResult result = MacroShare.importFromClipboard(ctx.getSource().getClient(), targetId, true);
                    return result.success() ? 1 : 0;
                })))
                .then(literal("importjson").then(argument("json", StringArgumentType.greedyString()).executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    String json = StringArgumentType.getString(ctx, "json");
                    MacroImportResult result = MacroShare.importJson(ctx.getSource().getClient(), json, null, false);
                    return result.success() ? 1 : 0;
                })))
                .then(literal("importfile").then(argument("path", StringArgumentType.greedyString()).executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    String pathStr = StringArgumentType.getString(ctx, "path");
                    try {
                        MacroImportResult result = MacroStorage.importFromFile(Path.of(pathStr), null, false, true);
                        MacroShare.reportImportResult(ctx.getSource().getClient(), result);
                        return result.success() ? 1 : 0;
                    } catch (IOException e) {
                        feedback(ctx, e.getMessage() == null ? "Import failed" : e.getMessage());
                        return 0;
                    }
                })));

        dispatcher.register(literal("dupeclient")
                .then(macro)
                .executes(ctx -> {
                    feedback(ctx, "Use /dupeclient macro list | run <id> | stop | delete <id> | folder | studio [id] | prompt [text] | generate <id> <text> | export <id> | import | importclip [id] | importoverwrite <id> | importjson <json> | importfile <path>");
                    return 1;
                }));
    }

    private static int generateFromPrompt(
            com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx,
            String prompt,
            String id,
            String displayName,
            boolean openEditor) {
        MacroPromptGenerator.Generated gen = MacroPromptGenerator.generate(prompt, id, displayName);
        if (!gen.parse().ok()) {
            feedback(ctx, String.join(" ", gen.parse().warnings()));
            return 0;
        }
        try {
            MacroStorage.save(gen.definition());
            String msg = "Saved \"" + gen.definition().id + "\" with " + gen.definition().steps.size() + " steps.";
            if (!gen.parse().warnings().isEmpty()) {
                msg += " Warnings: " + String.join("; ", gen.parse().warnings());
            }
            feedback(ctx, msg);
            if (openEditor) {
                MacroEditorScreen.open(ctx.getSource().getClient(), gen.definition().id);
            }
            return 1;
        } catch (IOException e) {
            feedback(ctx, e.getMessage() == null ? "Save failed" : e.getMessage());
            return 0;
        }
    }

    private static void feedback(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(Component.literal("[Macro] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(message).withStyle(ChatFormatting.GRAY)));
    }
}
