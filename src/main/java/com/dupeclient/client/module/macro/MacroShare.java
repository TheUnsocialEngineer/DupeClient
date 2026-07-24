package com.dupeclient.client.module.macro;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Clipboard helpers and in-game feedback for macro export/import.
 */
public final class MacroShare {
    private MacroShare() {
    }

    public static void sendFeedback(Minecraft client, Component message) {
        if (client == null || client.player == null) {
            return;
        }
        client.player.displayClientMessage(Component.literal("[Macro] ").withStyle(ChatFormatting.GOLD).append(message), false);
    }

    public static boolean exportMacroToClipboard(Minecraft client, String id) {
        try {
            String json = MacroStorage.exportBundleJson(id);
            client.keyboardHandler.setClipboard(json);
            MacroDefinition def = MacroStorage.load(id);
            sendFeedback(client, Component.literal("Exported \"" + def.displayName + "\" (" + MacroStorage.filenameId(id) + ") to clipboard.")
                    .withStyle(ChatFormatting.GREEN));
            return true;
        } catch (Exception e) {
            sendFeedback(client, Component.literal(e.getMessage() == null ? "Export failed" : e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return false;
        }
    }

    public static boolean exportDefinitionToClipboard(Minecraft client, MacroDefinition def) {
        try {
            String json = MacroStorage.exportBundleJson(def);
            client.keyboardHandler.setClipboard(json);
            String label = def.displayName == null || def.displayName.isBlank() ? def.id : def.displayName;
            sendFeedback(client, Component.literal("Exported \"" + label + "\" to clipboard.").withStyle(ChatFormatting.GREEN));
            return true;
        } catch (Exception e) {
            sendFeedback(client, Component.literal(e.getMessage() == null ? "Export failed" : e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return false;
        }
    }

    public static MacroImportResult importFromClipboard(
            Minecraft client, @Nullable String targetId, boolean overwrite) {
        String clip = client.keyboardHandler.getClipboard();
        if (clip == null || clip.isBlank()) {
            MacroImportResult result = MacroImportResult.fail("Clipboard is empty.");
            sendFeedback(client, Component.literal(result.error()).withStyle(ChatFormatting.RED));
            return result;
        }
        return importJson(client, clip, targetId, overwrite);
    }

    public static MacroImportResult importJson(
            Minecraft client, String json, @Nullable String targetId, boolean overwrite) {
        MacroImportResult result = MacroStorage.importMacro(json, targetId, overwrite, true);
        reportImportResult(client, result);
        return result;
    }

    public static void reportImportResult(Minecraft client, MacroImportResult result) {
        if (result.success()) {
            Component msg = Component.literal("Imported as \"" + result.savedId() + "\"")
                    .withStyle(ChatFormatting.GREEN);
            if (result.displayName() != null && !result.displayName().isBlank()
                    && !result.displayName().equalsIgnoreCase(result.savedId())) {
                msg = msg.copy().append(Component.literal(" (" + result.displayName() + ")").withStyle(ChatFormatting.GRAY));
            }
            if (!result.warnings().isEmpty()) {
                msg = msg.copy().append(Component.literal(" — " + String.join("; ", result.warnings()))
                        .withStyle(ChatFormatting.YELLOW));
            }
            sendFeedback(client, msg);
        } else {
            sendFeedback(client, Component.literal(result.error() == null ? "Import failed" : result.error())
                    .withStyle(ChatFormatting.RED));
        }
    }
}
