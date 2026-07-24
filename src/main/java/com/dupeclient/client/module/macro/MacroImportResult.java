package com.dupeclient.client.module.macro;

import java.util.ArrayList;
import java.util.List;

public record MacroImportResult(
        boolean success,
        String savedId,
        String displayName,
        List<String> warnings,
        String error) {

    public MacroImportResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static MacroImportResult ok(String savedId, String displayName, List<String> warnings) {
        return new MacroImportResult(true, savedId, displayName, warnings, null);
    }

    public static MacroImportResult fail(String error) {
        return new MacroImportResult(false, null, null, List.of(), error);
    }

    public List<String> allMessages() {
        ArrayList<String> out = new ArrayList<>();
        if (error != null && !error.isBlank()) {
            out.add(error);
        }
        out.addAll(warnings);
        return out;
    }
}
