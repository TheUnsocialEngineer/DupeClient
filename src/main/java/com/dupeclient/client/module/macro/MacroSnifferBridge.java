package com.dupeclient.client.module.macro;

import com.dupeclient.client.gui.MacroEditorScreen;
import com.dupeclient.client.module.macro.graph.MacroGraphCompiler;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferEntry;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;

public final class MacroSnifferBridge {
    private static final Pattern SLOT_PATTERN = Pattern.compile("slot[=:]\\s*(\\-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHAT_PATTERN = Pattern.compile("(?:message|chat)[=:]\\s*[\"']?([^\"']+)", Pattern.CASE_INSENSITIVE);

    private MacroSnifferBridge() {
    }

    public static String createMacroFromSelection(long entryId) {
        PacketSnifferEntry entry = PacketSnifferManager.INSTANCE.getEntry(entryId);
        if (entry == null) {
            return "";
        }
        return saveMacro(List.of(entry), "sniffer_" + entryId);
    }

    public static String createMacroFromAllVisibleC2s() {
        List<PacketSnifferEntry> c2s = new ArrayList<>();
        for (PacketSnifferEntry e : PacketSnifferManager.INSTANCE.snapshot(null)) {
            if (e.isC2s()) {
                c2s.add(e);
            }
        }
        if (c2s.isEmpty()) {
            return "";
        }
        int max = Math.min(c2s.size(), 64);
        return saveMacro(c2s.subList(Math.max(0, c2s.size() - max), c2s.size()), "sniffer_capture");
    }

    private static String saveMacro(List<PacketSnifferEntry> entries, String baseId) {
        List<MacroStep> steps = new ArrayList<>();
        for (PacketSnifferEntry entry : entries) {
            MacroStep step = toStep(entry);
            if (step != null) {
                steps.add(step);
                steps.add(waitStep(2));
            }
        }
        while (!steps.isEmpty() && MacroStepType.WAIT_TICKS.name().equals(steps.get(steps.size() - 1).type)) {
            steps.remove(steps.size() - 1);
        }
        if (steps.isEmpty()) {
            return "";
        }
        String id = MacroStorage.uniqueMacroId(baseId);
        MacroDefinition def = new MacroDefinition();
        def.id = id;
        def.displayName = "Sniffer capture";
        def.steps = steps;
        MacroGraphCompiler.stepsToGraph(def, 80, 80, 28);
        try {
            MacroStorage.save(def);
        } catch (IOException e) {
            return "";
        }
        MacroQuickPlay.markDirty();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.execute(() -> MacroEditorScreen.open(mc, id));
        }
        return id;
    }

    private static MacroStep waitStep(int ticks) {
        MacroStep s = new MacroStep();
        s.type = MacroStepType.WAIT_TICKS.name();
        s.ticks = Math.max(1, ticks);
        return s;
    }

    private static MacroStep toStep(PacketSnifferEntry entry) {
        if (entry == null || !entry.isC2s()) {
            return null;
        }
        String name = entry.name == null ? "" : entry.name.toLowerCase(Locale.ROOT);
        String blob = (entry.detail + " " + entry.editableText).trim();

        if (name.contains("chat") || name.contains("command")) {
            MacroStep chat = new MacroStep();
            chat.type = MacroStepType.SEND_CHAT.name();
            chat.text = extractChat(blob);
            if (chat.text.isBlank()) {
                chat.text = blob.isBlank() ? "/help" : blob;
            }
            return chat;
        }
        if (name.contains("clickslot") || name.contains("slotaction")) {
            MacroStep click = new MacroStep();
            click.type = MacroStepType.CLICK_SLOT.name();
            click.clickSlotId = extractSlot(blob);
            click.clickSlotAction = blob.toLowerCase(Locale.ROOT).contains("pickup") ? "PICKUP" : "QUICK_MOVE";
            click.clickSlotButton = 0;
            return click;
        }
        if (name.contains("closehandled") || name.contains("closescree")) {
            MacroStep close = new MacroStep();
            close.type = MacroStepType.CLOSE_GUI.name();
            return close;
        }
        if (name.contains("playeraction") && blob.toLowerCase(Locale.ROOT).contains("swap")) {
            MacroStep hotbar = new MacroStep();
            hotbar.type = MacroStepType.USE_HOTBAR_ITEM.name();
            hotbar.hotbarSlot = 0;
            return hotbar;
        }
        if (!blob.isBlank() && blob.startsWith("/")) {
            MacroStep cmd = new MacroStep();
            cmd.type = MacroStepType.SEND_CHAT.name();
            cmd.text = blob.split("\\s+", 2)[0];
            return cmd;
        }
        MacroStep note = new MacroStep();
        note.type = MacroStepType.WAIT_TICKS.name();
        note.ticks = 5;
        return note;
    }

    private static String extractChat(String blob) {
        Matcher m = CHAT_PATTERN.matcher(blob);
        if (m.find()) {
            return m.group(1).trim();
        }
        if (blob.startsWith("/")) {
            return blob;
        }
        return blob;
    }

    private static int extractSlot(String blob) {
        Matcher m = SLOT_PATTERN.matcher(blob);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }
}
