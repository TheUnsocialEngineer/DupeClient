package com.dupeclient.client.module.macro;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MacroHotkeyConflicts {
    private MacroHotkeyConflicts() {
    }

    public record Conflict(int key, List<String> macroIds) {
    }

    public static List<Conflict> findAll() {
        Map<Integer, List<String>> byKey = new HashMap<>();
        for (String id : MacroStorage.listMacroIds()) {
            try {
                MacroDefinition def = MacroStorage.load(id);
                int key = def.hotkeyKey;
                if (key < 0 || key == GLFW.GLFW_KEY_UNKNOWN) {
                    continue;
                }
                byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(id);
            } catch (Exception ignored) {
            }
        }
        List<Conflict> out = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> e : byKey.entrySet()) {
            if (e.getValue().size() > 1) {
                out.add(new Conflict(e.getKey(), List.copyOf(e.getValue())));
            }
        }
        return out;
    }

    public static String summaryLine() {
        List<Conflict> conflicts = findAll();
        if (conflicts.isEmpty()) {
            return "";
        }
        Conflict first = conflicts.get(0);
        return conflicts.size() + " hotkey conflict(s) — e.g. "
                + MacroKeyPress.keyLabel(first.key(), 0) + ": " + String.join(", ", first.macroIds());
    }

    public static Map<String, String> conflictByMacroId() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Conflict c : findAll()) {
            String label = MacroKeyPress.keyLabel(c.key(), 0);
            for (String id : c.macroIds()) {
                out.put(id, "Conflicts on " + label + " with " + c.macroIds().stream().filter(x -> !x.equals(id)).findFirst().orElse("?"));
            }
        }
        return out;
    }
}
