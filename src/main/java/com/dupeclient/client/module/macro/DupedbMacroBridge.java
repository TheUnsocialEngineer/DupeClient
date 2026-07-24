package com.dupeclient.client.module.macro;

import com.dupeclient.client.gui.MacroEditorScreen;
import com.dupeclient.client.module.macro.graph.MacroGraphCompiler;
import com.dupeclient.client.module.dupedb.DupedbManager;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DupedbMacroBridge {
    private DupedbMacroBridge() {
    }

    public static String createFromScan(String baseId) {
        Collection<String> commands = DupedbManager.INSTANCE.getObservedCommandRoots();
        Collection<String> plugins = DupedbManager.INSTANCE.getDiscoveredPlugins();
        if (commands.isEmpty() && plugins.isEmpty()) {
            return "";
        }
        List<MacroStep> steps = new ArrayList<>();
        steps.add(wait(20));
        for (String cmd : commands) {
            if (cmd == null || cmd.isBlank()) {
                continue;
            }
            MacroStep chat = new MacroStep();
            chat.type = MacroStepType.SEND_CHAT.name();
            chat.text = cmd.startsWith("/") ? cmd : "/" + cmd;
            steps.add(chat);
            steps.add(wait(10));
            if (steps.size() >= 40) {
                break;
            }
        }
        if (steps.size() <= 1) {
            for (String plugin : plugins) {
                MacroStep chat = new MacroStep();
                chat.type = MacroStepType.SEND_CHAT.name();
                chat.text = "/" + plugin + ":";
                steps.add(chat);
                steps.add(wait(8));
                if (steps.size() >= 30) {
                    break;
                }
            }
        }
        if (steps.isEmpty()) {
            return "";
        }
        String id = MacroStorage.uniqueMacroId(baseId == null || baseId.isBlank() ? "dupedb_scan" : baseId);
        MacroDefinition def = new MacroDefinition();
        def.id = id;
        def.displayName = "DupeDB scan commands";
        def.steps = steps;
        MacroGraphCompiler.stepsToGraph(def, 80, 80, 28);
        try {
            MacroStorage.save(def);
        } catch (IOException e) {
            return "";
        }
        MacroQuickPlay.markDirty();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            mc.execute(() -> MacroEditorScreen.open(mc, id));
        }
        return id;
    }

    private static MacroStep wait(int ticks) {
        MacroStep s = new MacroStep();
        s.type = MacroStepType.WAIT_TICKS.name();
        s.ticks = Math.max(1, ticks);
        return s;
    }
}
