package com.dupeclient.client.module.macro;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns natural-language macro prompts into linear {@link MacroStep} lists.
 * Example: {@code run /pv enter all items close pv open pv take all items, close without packet}
 */
public final class MacroPromptParser {
    public static final String ANY_ITEM = "*";

    private static final Pattern WAIT_TICKS = Pattern.compile("^(?:wait|delay|pause)\\s+(\\d+)\\s*(?:ticks?|t)?\\s*");
    private static final Pattern WAIT_SECONDS = Pattern.compile("^(?:wait|delay|pause)\\s+(\\d+(?:\\.\\d+)?)\\s*s(?:ec(?:onds?)?)?\\s*");
    private static final Pattern WALK = Pattern.compile("^(?:walk|move|forward)\\s+(\\d+)\\s*(ticks?|t|blocks?|b)?\\s*");
    private static final Pattern TURN = Pattern.compile("^(?:turn|rotate|yaw)\\s+(-?\\d+)\\s*(?:deg(?:rees?)?|°)?\\s*");
    private static final Pattern PUT_TAKE_COUNT_ITEM = Pattern.compile("^(put|deposit|store|take|withdraw|grab|pull)\\s+(\\d+|all)\\s+([a-z0-9_:.-]+)\\s*");
    private static final Pattern SAY = Pattern.compile("^(?:say|chat|message|send\\s+chat)\\s+(.+?)\\s*(?=,|$|(?=(?:then|and)\\s))");
    private static final Pattern RUN_CMD = Pattern.compile("^(?:run|exec(?:ute)?|send|cmd|command)\\s+(/\\S+(?:\\s+\\S+)*)\\s*");
    private static final Pattern SLASH_CMD = Pattern.compile("^(/\\S+(?:\\s+\\S+)*)\\s*");

    /** Longest-match keyword phrases (lowercase), checked before regex. */
    private static final String[][] KEYWORD_STEPS = {
            {"close without packet", "CLOSE_SCREEN"},
            {"close instantly", "CLOSE_SCREEN"},
            {"close screen", "CLOSE_SCREEN"},
            {"instant close", "CLOSE_SCREEN"},
            {"dismiss screen", "CLOSE_SCREEN"},
            {"close with packet", "CLOSE_GUI"},
            {"vanilla close", "CLOSE_GUI"},
            {"close gui", "CLOSE_GUI"},
            {"close container", "CLOSE_GUI"},
            {"close inventory", "CLOSE_GUI"},
            {"flush packet delay", "PACKET_DELAY_FLUSH"},
            {"packet delay flush", "PACKET_DELAY_FLUSH"},
            {"flush packets", "PACKET_DELAY_FLUSH"},
            {"packet flush", "PACKET_DELAY_FLUSH"},
            {"toggle packet delay", "PACKET_DELAY_TOGGLE"},
            {"packet delay toggle", "PACKET_DELAY_TOGGLE"},
            {"flush ui queue", "UI_UTILS_FLUSH_QUEUE"},
            {"ui utils flush", "UI_UTILS_FLUSH_QUEUE"},
            {"toggle ui delay", "UI_UTILS_TOGGLE_DELAY"},
            {"take all items", "GUI_TAKE_ALL"},
            {"withdraw all items", "GUI_TAKE_ALL"},
            {"grab all items", "GUI_TAKE_ALL"},
            {"pull all items", "GUI_TAKE_ALL"},
            {"put all items", "GUI_PUT_ALL"},
            {"deposit all items", "GUI_PUT_ALL"},
            {"store all items", "GUI_PUT_ALL"},
            {"all items take", "GUI_TAKE_ALL"},
            {"all items put", "GUI_PUT_ALL"},
            {"all items", "GUI_PUT_ALL"},
            {"take all", "GUI_TAKE_ALL"},
            {"put all", "GUI_PUT_ALL"},
            {"deposit all", "GUI_PUT_ALL"},
            {"close pv", "CHAT_/pv close"},
            {"pv close", "CHAT_/pv close"},
            {"open pv", "CHAT_/pv open"},
            {"pv open", "CHAT_/pv open"},
            {"enter pv", "CHAT_/pv enter"},
            {"pv enter", "CHAT_/pv enter"},
            {"open chest", "BLOCK_CHEST"},
            {"interact chest", "BLOCK_CHEST"},
            {"use chest", "BLOCK_CHEST"},
            {"open ender chest", "BLOCK_ENDER"},
    };

    private MacroPromptParser() {
    }

    public record ParseResult(List<MacroStep> steps, List<String> notes, List<String> warnings) {
        public boolean ok() {
            return !steps.isEmpty();
        }
    }

    public static ParseResult parse(String raw) {
        List<MacroStep> steps = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            warnings.add("Prompt is empty.");
            return new ParseResult(steps, notes, warnings);
        }
        for (String segment : splitSegments(raw)) {
            parseSegmentGreedy(segment.trim(), steps, warnings);
        }
        if (steps.isEmpty()) {
            warnings.add("No macro steps were recognized.");
        } else {
            steps = insertAutomaticWaits(steps, notes);
            notes.add(steps.size() + " step(s) generated.");
        }
        return new ParseResult(steps, notes, warnings);
    }

    private static void parseSegmentGreedy(String segment, List<MacroStep> out, List<String> warnings) {
        String rest = segment;
        int guard = 0;
        while (!rest.isBlank() && guard++ < 64) {
            rest = rest.trim();
            rest = stripLeadingThen(rest);
            if (rest.isBlank()) {
                break;
            }
            int before = rest.length();
            rest = consumeOnePhrase(rest, out);
            if (rest.length() == before) {
                warnings.add("Unrecognized: \"" + truncate(rest, 48) + "\"");
                break;
            }
        }
    }

    private static String consumeOnePhrase(String rest, List<MacroStep> out) {
        String lower = rest.toLowerCase(Locale.ROOT);

        for (String[] kw : KEYWORD_STEPS) {
            if (lower.startsWith(kw[0])) {
                appendKeywordStep(kw[1], out);
                return rest.substring(kw[0].length());
            }
        }

        Matcher pvNum = Pattern.compile("^(?:open|enter|visit)?\\s*pv\\s+(\\d+)\\s*", Pattern.CASE_INSENSITIVE).matcher(rest);
        if (pvNum.find() && pvNum.start() == 0) {
            out.add(chatStep("/pv " + pvNum.group(1)));
            return rest.substring(pvNum.end());
        }

        Matcher m;
        if ((m = WAIT_SECONDS.matcher(lower)).find()) {
            out.add(waitTicks((int) Math.round(Double.parseDouble(m.group(1)) * 20.0)));
            return rest.substring(m.end());
        }
        if ((m = WAIT_TICKS.matcher(lower)).find()) {
            out.add(waitTicks(Integer.parseInt(m.group(1))));
            return rest.substring(m.end());
        }
        if ((m = WALK.matcher(lower)).find()) {
            int amount = Integer.parseInt(m.group(1));
            String unit = m.group(2) == null ? "t" : m.group(2);
            MacroStep s = step(MacroStepType.MOVE_FORWARD);
            if (unit.startsWith("b")) {
                s.moveMeasure = "BLOCKS";
                s.moveDistanceBlocks = Math.max(1, amount);
            } else {
                s.ticks = Math.max(1, amount);
            }
            out.add(s);
            return rest.substring(m.end());
        }
        if ((m = TURN.matcher(lower)).find()) {
            MacroStep s = step(MacroStepType.LOOK_TURN);
            s.ticks = Integer.parseInt(m.group(1));
            out.add(s);
            return rest.substring(m.end());
        }
        if ((m = PUT_TAKE_COUNT_ITEM.matcher(lower)).find()) {
            out.add(guiItem(
                    modeFromVerb(m.group(1)),
                    normalizeItemId(m.group(3)),
                    "all".equals(m.group(2)) ? -1 : Integer.parseInt(m.group(2))));
            return rest.substring(m.end());
        }
        if ((m = RUN_CMD.matcher(rest)).find()) {
            out.add(chatStep(m.group(1).trim()));
            return rest.substring(m.end());
        }
        if ((m = SLASH_CMD.matcher(rest)).find()) {
            out.add(chatStep(m.group(1).trim()));
            return rest.substring(m.end());
        }
        if ((m = SAY.matcher(rest)).find()) {
            out.add(chatStep(m.group(1).trim()));
            return rest.substring(m.end());
        }

        return rest;
    }

    private static void appendKeywordStep(String id, List<MacroStep> out) {
        switch (id) {
            case "CLOSE_SCREEN" -> out.add(step(MacroStepType.CLOSE_SCREEN));
            case "CLOSE_GUI" -> out.add(step(MacroStepType.CLOSE_GUI));
            case "PACKET_DELAY_FLUSH" -> out.add(step(MacroStepType.PACKET_DELAY_FLUSH));
            case "PACKET_DELAY_TOGGLE" -> out.add(step(MacroStepType.PACKET_DELAY_TOGGLE));
            case "UI_UTILS_FLUSH_QUEUE" -> out.add(step(MacroStepType.UI_UTILS_FLUSH_QUEUE));
            case "UI_UTILS_TOGGLE_DELAY" -> out.add(step(MacroStepType.UI_UTILS_TOGGLE_DELAY));
            case "GUI_PUT_ALL" -> out.add(guiItem("PUT", ANY_ITEM, -1));
            case "GUI_TAKE_ALL" -> out.add(guiItem("TAKE", ANY_ITEM, -1));
            case "BLOCK_CHEST" -> {
                MacroStep s = step(MacroStepType.BLOCK_INTERACT);
                s.blockPreset = "CHEST";
                out.add(s);
            }
            case "BLOCK_ENDER" -> {
                MacroStep s = step(MacroStepType.BLOCK_INTERACT);
                s.blockPreset = "ENDER_CHEST";
                out.add(s);
            }
            default -> {
                if (id.startsWith("CHAT_")) {
                    out.add(chatStep(id.substring(5)));
                }
            }
        }
    }

    private static List<String> splitSegments(String raw) {
        String normalized = raw.replace('\n', ' ').replace('\r', ' ').trim();
        normalized = normalized.replace(" and then ", ",");
        normalized = normalized.replace(" then ", ",");
        List<String> parts = new ArrayList<>();
        for (String chunk : normalized.split(",")) {
            String t = chunk.trim();
            if (!t.isEmpty()) {
                parts.add(t);
            }
        }
        if (parts.isEmpty() && !normalized.isEmpty()) {
            parts.add(normalized);
        }
        return parts;
    }

    private static String stripLeadingThen(String rest) {
        String lower = rest.toLowerCase(Locale.ROOT);
        if (lower.startsWith("then ")) {
            return rest.substring(5);
        }
        if (lower.startsWith("and ")) {
            return rest.substring(4);
        }
        return rest;
    }

    private static List<MacroStep> insertAutomaticWaits(List<MacroStep> steps, List<String> notes) {
        List<MacroStep> out = new ArrayList<>(steps.size() + 8);
        for (int i = 0; i < steps.size(); i++) {
            MacroStep s = steps.get(i);
            out.add(s);
            if (i + 1 >= steps.size()) {
                continue;
            }
            MacroStep next = steps.get(i + 1);
            if (MacroStepType.WAIT_TICKS.name().equals(next.type)) {
                continue;
            }
            if (MacroStepType.SEND_CHAT.name().equals(s.type) && likelyOpensGui(s.text)) {
                out.add(waitTicks(40));
                notes.add("Inserted 40t wait after \"" + truncate(s.text, 32) + "\".");
            } else if (MacroStepType.GUI_ITEM.name().equals(s.type)
                    && (MacroStepType.SEND_CHAT.name().equals(next.type) || MacroStepType.GUI_ITEM.name().equals(next.type))) {
                out.add(waitTicks(10));
            }
        }
        return out;
    }

    private static boolean likelyOpensGui(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String t = text.toLowerCase(Locale.ROOT);
        return t.contains("pv") || t.contains("chest") || t.contains("shop") || t.contains("menu")
                || t.contains("inv") || t.contains("open") || t.contains("enter") || t.contains("gui");
    }

    private static MacroStep step(MacroStepType type) {
        MacroStep s = new MacroStep();
        s.type = type.name();
        return s;
    }

    private static MacroStep waitTicks(int ticks) {
        MacroStep s = step(MacroStepType.WAIT_TICKS);
        s.ticks = Math.max(1, ticks);
        return s;
    }

    private static MacroStep chatStep(String text) {
        MacroStep s = step(MacroStepType.SEND_CHAT);
        s.text = normalizeCommand(text);
        return s;
    }

    private static MacroStep guiItem(String mode, String itemId, int count) {
        MacroStep s = step(MacroStepType.GUI_ITEM);
        s.guiItemMode = mode;
        s.guiItemId = itemId;
        s.guiItemCount = count;
        s.guiItemDelayTicks = 0;
        return s;
    }

    private static String modeFromVerb(String verb) {
        String v = verb.toLowerCase(Locale.ROOT);
        if (v.startsWith("take") || v.startsWith("withdraw") || v.startsWith("grab") || v.startsWith("pull")) {
            return "TAKE";
        }
        return "PUT";
    }

    private static String normalizeCommand(String text) {
        String t = text.trim();
        if (t.startsWith("/")) {
            return t;
        }
        return "/" + t;
    }

    private static String normalizeItemId(String raw) {
        String t = raw.trim().toLowerCase(Locale.ROOT);
        if (t.equals("items") || t.equals("item") || t.equals("everything") || t.equals("all")) {
            return ANY_ITEM;
        }
        if (t.contains(":")) {
            return t;
        }
        return "minecraft:" + t.replace(' ', '_');
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "…";
    }
}
