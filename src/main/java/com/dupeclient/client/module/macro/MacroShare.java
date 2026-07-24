package com.dupeclient.client.module.macro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/**
 * Clipboard helpers and in-game feedback for macro export/import.
 */
public final class MacroShare {
    private MacroShare() {
    }

    public static void sendFeedback(MinecraftClient client, Text message) {
        if (client == null || client.player == null) {
            return;
        }
        client.player.sendMessage(Text.literal("[Macro] ").formatted(Formatting.GOLD).append(message), false);
    }

    public static boolean exportMacroToClipboard(MinecraftClient client, String id) {
        try {
            String json = MacroStorage.exportBundleJson(id);
            client.keyboard.setClipboard(json);
            MacroDefinition def = MacroStorage.load(id);
            sendFeedback(client, Text.literal("Exported \"" + def.displayName + "\" (" + MacroStorage.filenameId(id) + ") to clipboard.")
                    .formatted(Formatting.GREEN));
            return true;
        } catch (Exception e) {
            sendFeedback(client, Text.literal(e.getMessage() == null ? "Export failed" : e.getMessage())
                    .formatted(Formatting.RED));
            return false;
        }
    }

    public static boolean exportDefinitionToClipboard(MinecraftClient client, MacroDefinition def) {
        try {
            String json = MacroStorage.exportBundleJson(def);
            client.keyboard.setClipboard(json);
            String label = def.displayName == null || def.displayName.isBlank() ? def.id : def.displayName;
            sendFeedback(client, Text.literal("Exported \"" + label + "\" to clipboard.").formatted(Formatting.GREEN));
            return true;
        } catch (Exception e) {
            sendFeedback(client, Text.literal(e.getMessage() == null ? "Export failed" : e.getMessage())
                    .formatted(Formatting.RED));
            return false;
        }
    }

    public static MacroImportResult importFromClipboard(
            MinecraftClient client, @Nullable String targetId, boolean overwrite) {
        String clip = client.keyboard.getClipboard();
        if (clip == null || clip.isBlank()) {
            MacroImportResult result = MacroImportResult.fail("Clipboard is empty.");
            sendFeedback(client, Text.literal(result.error()).formatted(Formatting.RED));
            return result;
        }
        return importJson(client, clip, targetId, overwrite);
    }

    public static MacroImportResult importJson(
            MinecraftClient client, String json, @Nullable String targetId, boolean overwrite) {
        MacroImportResult result = MacroStorage.importMacro(json, targetId, overwrite, true);
        reportImportResult(client, result);
        return result;
    }

    public static void reportImportResult(MinecraftClient client, MacroImportResult result) {
        if (result.success()) {
            Text msg = Text.literal("Imported as \"" + result.savedId() + "\"")
                    .formatted(Formatting.GREEN);
            if (result.displayName() != null && !result.displayName().isBlank()
                    && !result.displayName().equalsIgnoreCase(result.savedId())) {
                msg = msg.copy().append(Text.literal(" (" + result.displayName() + ")").formatted(Formatting.GRAY));
            }
            if (!result.warnings().isEmpty()) {
                msg = msg.copy().append(Text.literal(" — " + String.join("; ", result.warnings()))
                        .formatted(Formatting.YELLOW));
            }
            sendFeedback(client, msg);
        } else {
            sendFeedback(client, Text.literal(result.error() == null ? "Import failed" : result.error())
                    .formatted(Formatting.RED));
        }
    }
}
