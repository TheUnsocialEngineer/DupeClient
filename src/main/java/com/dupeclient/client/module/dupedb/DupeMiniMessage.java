package com.dupeclient.client.module.dupedb;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Minimal MiniMessage-style parser for interactive DupeClient chat (click/hover/color tags).
 */
public final class DupeMiniMessage {
    private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z0-9_:-]+)(?:='([^']*)')?>");

    private DupeMiniMessage() {
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return parseNodes(input, 0, input.length(), Style.EMPTY).text();
    }

    public static Component markConfirmPrompt(String server, int scorePercent, String verificationHint) {
        String scorePart = scorePercent >= 0 ? " <gray>(score " + scorePercent + "%)</gray>" : "";
        String hintPart = verificationHint == null || verificationHint.isBlank()
                ? ""
                : " <dark_gray>" + escape(verificationHint) + "</dark_gray>";
        return parse("<gray>Mark <white>" + escape(server) + "</white> as P2W" + scorePart
                + "? </gray>" + hintPart + " "
                + "<green><click:run_command='/p2w confirm mark'><bold>[Confirm]</bold></click></green> "
                + "<red><click:run_command='/p2w abort'><bold>[Abort]</bold></click></red>");
    }

    public static Component unmarkConfirmPrompt(String server, String verificationHint) {
        String hintPart = verificationHint == null || verificationHint.isBlank()
                ? ""
                : " <dark_gray>" + escape(verificationHint) + "</dark_gray>";
        return parse("<gray>Mark <white>" + escape(server) + "</white> as <red>non-P2W</red> "
                + "(modules will be locked while connected)? </gray>" + hintPart + " "
                + "<green><click:run_command='/p2w confirm unmark'><bold>[Confirm]</bold></click></green> "
                + "<red><click:run_command='/p2w abort'><bold>[Abort]</bold></click></red>");
    }

    public static Component scoreWithMarkAction(int percent, String summary) {
        return parse("<green>" + escape(summary) + "</green> "
                + "<aqua><click:run_command='/p2w mark'><hover:show_text='Submit this server as P2W to the community list'>[Mark as P2W]</hover></click></aqua>");
    }

    /** Colored exploit line; plugin name in brackets opens the DupeDB page. */
    public static Component exploitMatchLine(String exploitName, String pluginName, String url) {
        String safeName = escape(exploitName);
        String safePlugin = escape(pluginName);
        String safeUrl = escapeAttr(url);
        return parse("<yellow>" + safeName + "</yellow> "
                + "<aqua><underlined><click:open_url='" + safeUrl + "'><hover:show_text='" + safeUrl + "'>"
                + "[" + safePlugin + "]"
                + "</hover></click></underlined></aqua>");
    }

    private static String escape(String s) {
        return s.replace("<", "").replace(">", "");
    }

    private static String escapeAttr(String s) {
        return escape(s).replace("'", "");
    }

    private static ParseResult parseNodes(String input, int start, int end, Style baseStyle) {
        MutableComponent out = Component.empty();
        int i = start;
        List<StyleFrame> stack = new ArrayList<>();
        stack.add(new StyleFrame(baseStyle, null, null));

        while (i < end) {
            Matcher m = TAG.matcher(input);
            m.region(i, end);
            if (!m.find()) {
                appendLiteral(out, input.substring(i, end), currentStyle(stack));
                break;
            }
            if (m.start() > i) {
                appendLiteral(out, input.substring(i, m.start()), currentStyle(stack));
            }
            boolean closing = "/".equals(m.group(1));
            String tag = m.group(2);
            String attr = m.group(3);
            if (closing) {
                popTag(stack, tag);
            } else {
                pushTag(stack, tag, attr);
            }
            i = m.end();
        }
        return new ParseResult(out);
    }

    private static void pushTag(List<StyleFrame> stack, String tag, String attr) {
        Style parent = currentStyle(stack);
        String lower = tag.toLowerCase(Locale.ROOT);
        if ("click:run_command".equals(lower) && attr != null) {
            stack.add(new StyleFrame(parent, new ClickEvent.RunCommand(attr), null));
            return;
        }
        if ("click:open_url".equals(lower) && attr != null) {
            stack.add(new StyleFrame(parent, new ClickEvent.OpenUrl(URI.create(attr)), null));
            return;
        }
        if (lower.startsWith("click:open_url:")) {
            String openUrl = tag.substring("click:open_url:".length());
            if (!openUrl.isBlank()) {
                stack.add(new StyleFrame(parent, new ClickEvent.OpenUrl(URI.create(openUrl)), null));
            }
            return;
        }
        if ("hover:show_text".equals(lower) && attr != null) {
            stack.add(new StyleFrame(parent, null, new HoverEvent.ShowText(Component.literal(attr))));
            return;
        }
        if (lower.startsWith("click:")) {
            String action = lower.substring("click:".length());
            String cmd = attr != null ? attr : tag.substring(tag.indexOf(':') + 1);
            if (action.equals("run_command") && cmd != null) {
                stack.add(new StyleFrame(parent, new ClickEvent.RunCommand(cmd), null));
                return;
            }
            if (action.startsWith("open_url:")) {
                String openUrl = action.substring("open_url:".length());
                if (!openUrl.isBlank()) {
                    stack.add(new StyleFrame(parent, new ClickEvent.OpenUrl(URI.create(openUrl)), null));
                }
                return;
            }
        }
        if (lower.startsWith("hover:")) {
            String action = lower.substring("hover:".length());
            if (action.equals("show_text") && attr != null) {
                stack.add(new StyleFrame(parent, null, new HoverEvent.ShowText(Component.literal(attr))));
                return;
            }
        }
        ChatFormatting color = colorForTag(lower);
        Style next = parent;
        if (color != null) {
            next = next.withColor(color);
        }
        if ("bold".equals(lower)) {
            next = next.withBold(true);
        } else if ("italic".equals(lower)) {
            next = next.withItalic(true);
        } else if ("underlined".equals(lower)) {
            next = next.withUnderlined(true);
        }
        stack.add(new StyleFrame(next, null, null));
    }

    private static void popTag(List<StyleFrame> stack, String tag) {
        if (stack.size() <= 1) {
            return;
        }
        String lower = tag.toLowerCase(Locale.ROOT);
        for (int i = stack.size() - 1; i >= 1; i--) {
            StyleFrame frame = stack.get(i);
            if (matchesClose(frame, lower)) {
                stack.remove(i);
                return;
            }
        }
        stack.remove(stack.size() - 1);
    }

    private static boolean matchesClose(StyleFrame frame, String tag) {
        if (tag.startsWith("click") && frame.click != null) {
            return true;
        }
        if (tag.startsWith("hover") && frame.hover != null) {
            return true;
        }
        ChatFormatting color = colorForTag(tag);
        if (color != null && frame.style.getColor() != null && frame.style.getColor().equals(color)) {
            return true;
        }
        return "bold".equals(tag) || "italic".equals(tag) || "underlined".equals(tag);
    }

    private static ChatFormatting colorForTag(String tag) {
        return switch (tag) {
            case "black" -> ChatFormatting.BLACK;
            case "dark_blue" -> ChatFormatting.DARK_BLUE;
            case "dark_green" -> ChatFormatting.DARK_GREEN;
            case "dark_aqua" -> ChatFormatting.DARK_AQUA;
            case "dark_red" -> ChatFormatting.DARK_RED;
            case "dark_purple" -> ChatFormatting.DARK_PURPLE;
            case "gold" -> ChatFormatting.GOLD;
            case "gray", "grey" -> ChatFormatting.GRAY;
            case "dark_gray", "dark_grey" -> ChatFormatting.DARK_GRAY;
            case "blue" -> ChatFormatting.BLUE;
            case "green" -> ChatFormatting.GREEN;
            case "aqua" -> ChatFormatting.AQUA;
            case "red" -> ChatFormatting.RED;
            case "light_purple" -> ChatFormatting.LIGHT_PURPLE;
            case "yellow" -> ChatFormatting.YELLOW;
            case "white" -> ChatFormatting.WHITE;
            default -> null;
        };
    }

    private static Style currentStyle(List<StyleFrame> stack) {
        StyleFrame top = stack.get(stack.size() - 1);
        Style style = top.style;
        if (top.click != null) {
            style = style.withClickEvent(top.click);
        }
        if (top.hover != null) {
            style = style.withHoverEvent(top.hover);
        }
        return style;
    }

    private static void appendLiteral(MutableComponent out, String literal, Style style) {
        if (literal.isEmpty()) {
            return;
        }
        out.append(Component.literal(literal).setStyle(style));
    }

    private record ParseResult(Component text) {
    }

    private record StyleFrame(Style style, ClickEvent click, HoverEvent hover) {
    }
}
