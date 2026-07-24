package com.ui_utils.features;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

public class PluginScanner {
    private static final String P = "\u00a77[\u00a7c*\u00a77] ";
    private static final Set<String> ANTICHEAT_LIST = Set.of("nocheatplus", "negativity", "warden", "horizon", "illegalstack", "coreprotect", "exploitsx", "vulcan", "abc", "spartan", "kauri", "anticheatreloaded", "witherac", "godseye", "matrix", "wraith", "antixrayheuristics", "grimac", "themis", "foxaddition", "guardianac", "ggintegrity", "lightanticheat", "anarchyexploitfixes");
    private static final Random RANDOM = new Random();
    private static boolean scanning = false;
    private static int ticksWaiting = 0;
    private static final List<String> foundPlugins = new ArrayList<String>();

    public static void startScan() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) {
            return;
        }
        if (scanning) {
            return;
        }
        foundPlugins.clear();
        scanning = true;
        ticksWaiting = 0;
        mc.getConnection().send((Packet)new ServerboundCommandSuggestionPacket(RANDOM.nextInt(200), "ver "));
    }

    public static void onTick() {
        if (!scanning) {
            return;
        }
        if (++ticksWaiting >= 60) {
            PluginScanner.printResults();
        }
    }

    public static void onCommandSuggestions(ClientboundCommandSuggestionsPacket packet) {
        if (!scanning) {
            return;
        }
        scanning = false;
        try {
            Suggestions suggestions = packet.toSuggestions();
            Minecraft mc = Minecraft.getInstance();
            if (suggestions.isEmpty()) {
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.nullToEmpty((String)"\u00a77[\u00a7c*\u00a77] \u00a7cNo plugins found or blocked"));
                }
                return;
            }
            for (Suggestion suggestion : suggestions.getList()) {
                String pluginName = suggestion.getText().trim();
                if (pluginName.isEmpty() || foundPlugins.contains(pluginName.toLowerCase())) continue;
                foundPlugins.add(pluginName);
            }
            PluginScanner.printResults();
        }
        catch (Exception e) {
            scanning = false;
        }
    }

    private static void printResults() {
        scanning = false;
        ticksWaiting = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (foundPlugins.isEmpty()) {
            mc.player.sendSystemMessage(Component.nullToEmpty((String)"\u00a77[\u00a7c*\u00a77] \u00a7cNo plugins found"));
            return;
        }
        foundPlugins.sort(String.CASE_INSENSITIVE_ORDER);
        StringBuilder sb = new StringBuilder();
        sb.append(P).append("Plugins \u00a77(\u00a7c").append(foundPlugins.size()).append("\u00a77): ");
        for (int i = 0; i < foundPlugins.size(); ++i) {
            String plugin = foundPlugins.get(i);
            if (ANTICHEAT_LIST.contains(plugin.toLowerCase()) || plugin.toLowerCase().contains("exploit") || plugin.toLowerCase().contains("cheat") || plugin.toLowerCase().contains("illegal")) {
                sb.append("\u00a74");
            } else {
                sb.append("\u00a7c");
            }
            sb.append(plugin);
            if (i >= foundPlugins.size() - 1) continue;
            sb.append("\u00a77, ");
        }
        mc.player.sendSystemMessage(Component.nullToEmpty((String)sb.toString()));
        foundPlugins.clear();
    }

    public static boolean isScanning() {
        return scanning;
    }
}

