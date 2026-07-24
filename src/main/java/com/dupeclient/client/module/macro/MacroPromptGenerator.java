package com.dupeclient.client.module.macro;

import com.dupeclient.client.module.macro.graph.MacroGraphCompiler;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Builds a saved {@link MacroDefinition} from a natural-language prompt.
 */
public final class MacroPromptGenerator {
    private MacroPromptGenerator() {
    }

    public record Generated(MacroDefinition definition, MacroPromptParser.ParseResult parse) {
    }

    public static Generated generate(String prompt, @Nullable String id, @Nullable String displayName) {
        MacroPromptParser.ParseResult parse = MacroPromptParser.parse(prompt);
        MacroDefinition def = new MacroDefinition();
        def.id = id == null || id.isBlank() ? suggestId(prompt) : MacroStorage.filenameId(id);
        def.displayName = displayName == null || displayName.isBlank()
                ? titleFromPrompt(prompt, def.id)
                : displayName.trim();
        def.steps = new java.util.ArrayList<>(parse.steps());
        def.formatVersion = 1;
        if (!parse.steps().isEmpty()) {
            MacroGraphCompiler.stepsToGraph(def, 80.0, 70.0, 50.0);
        }
        def.normalize();
        return new Generated(def, parse);
    }

    public static String suggestId(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "generated_macro";
        }
        String slug = prompt.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (slug.length() > 40) {
            slug = slug.substring(0, 40);
        }
        if (slug.isBlank()) {
            slug = "generated_macro";
        }
        String base = slug;
        String candidate = base;
        int n = 2;
        while (macroIdExists(candidate)) {
            candidate = base + "_" + n++;
        }
        return candidate;
    }

    private static boolean macroIdExists(String id) {
        for (String existing : MacroStorage.listMacroIds()) {
            if (existing.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private static String titleFromPrompt(String prompt, String fallbackId) {
        String t = prompt.trim().replaceAll("\\s+", " ");
        if (t.length() > 56) {
            t = t.substring(0, 53) + "...";
        }
        return t.isBlank() ? fallbackId : t;
    }
}
