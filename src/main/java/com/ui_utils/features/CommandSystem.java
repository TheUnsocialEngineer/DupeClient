package com.ui_utils.features;

import com.ui_utils.SessionUtils;
import com.ui_utils.SharedVariables;
import com.ui_utils.features.ClipUtils;
import com.ui_utils.features.PluginScanner;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

public class CommandSystem {
    private static final String P = "\u00a77[\u00a7c*\u00a77] ";
    private static final Map<String, CommandHandler> commands = new LinkedHashMap<String, CommandHandler>();
    private static final Map<String, String> manuals = new LinkedHashMap<String, String>();

    public static void registerCommand(String name, CommandHandler handler, String manual) {
        commands.put(name.toLowerCase(), handler);
        manuals.put(name.toLowerCase(), manual);
    }

    public static String execute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cNo command";
        }
        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        CommandHandler handler = commands.get(commandName);
        if (handler == null) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cUnknown: \u00a77" + commandName;
        }
        try {
            return handler.execute(args);
        }
        catch (Exception e) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cError: \u00a77" + e.getMessage();
        }
    }

    private static String helpCommand(String args) {
        if (!args.isEmpty()) {
            return CommandSystem.manCommand(args);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(P).append("Commands: ");
        int i = 0;
        for (String cmd : commands.keySet()) {
            sb.append("\u00a7c").append(cmd);
            if (i < commands.size() - 1) {
                sb.append("\u00a77, ");
            }
            ++i;
        }
        return sb.toString();
    }

    private static String manCommand(String args) {
        if (args.isEmpty()) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: man <command>";
        }
        String manual = manuals.get(args.toLowerCase());
        if (manual == null) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cNo manual for: \u00a77" + args;
        }
        return P + manual.replace("\n", "\n\u00a77[\u00a7c*\u00a77] ");
    }

    private static String toggleCommand(String args) {
        SharedVariables.enabled = !SharedVariables.enabled;
        return "\u00a77UI-Utils: " + (SharedVariables.enabled ? "\u00a7aON" : "\u00a7cOFF");
    }

    private static String echoCommand(String args) {
        return args.isEmpty() ? "" : P + args;
    }

    private static String mathCommand(String args) {
        if (args.isEmpty()) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: math <expression>";
        }
        try {
            double result = CommandSystem.evaluateExpression(args);
            return "\u00a77[\u00a7c*\u00a77] Result: \u00a7c" + result;
        }
        catch (Exception e) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cInvalid expression";
        }
    }

    private static String closeCommand(String args) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(null));
        return "\u00a77[\u00a7c*\u00a77] Screen closed";
    }

    private static String desyncCommand(String args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cNot connected";
        }
        int syncId = mc.player.containerMenu.containerId;
        mc.getConnection().send((Packet)new ServerboundContainerClosePacket(syncId));
        return "\u00a77[\u00a7c*\u00a77] Desync sent \u00a77(syncId: \u00a7c" + syncId + "\u00a77)";
    }

    private static String chatCommand(String args) {
        if (args.isEmpty()) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: chat <message>";
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cNot in game";
        }
        mc.execute(() -> {
            if (args.startsWith("/")) {
                mc.player.connection.sendCommand(args.substring(1));
            } else {
                mc.player.connection.sendChat(args);
            }
        });
        return "\u00a77[\u00a7c*\u00a77] Sent: \u00a7c" + args;
    }

    private static String joinServerCommand(String args) {
        if (args.isEmpty()) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: joinserver <ip>";
        }
        Minecraft mc = Minecraft.getInstance();
        String serverIp = args.split("\\s+")[0];
        ServerAddress address = ServerAddress.parseString(serverIp);
        ServerData serverInfo = new ServerData("Server", serverIp, ServerData.Type.OTHER);
        mc.execute(() -> {
            try {
                if (mc.level != null) {
                    try {
                        mc.level.getClass().getMethod("disconnect", new Class[0]).invoke((Object)mc.level, new Object[0]);
                    }
                    catch (NoSuchMethodException e) {
                        mc.level.getClass().getMethod("disconnect", Component.class).invoke((Object)mc.level, Component.empty());
                    }
                }
                try {
                    mc.getClass().getMethod("disconnect", new Class[0]).invoke((Object)mc, new Object[0]);
                }
                catch (NoSuchMethodException e) {
                    mc.getClass().getMethod("disconnect", Screen.class, Boolean.TYPE).invoke((Object)mc, new TitleScreen(), false);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            ConnectScreen.startConnecting((Screen)new JoinMultiplayerScreen((Screen)new TitleScreen()), (Minecraft)mc, (ServerAddress)address, (ServerData)serverInfo, (boolean)false, null);
        });
        return "\u00a77[\u00a7c*\u00a77] Joining: \u00a7c" + serverIp;
    }

    private static String screenCommand(String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: screen <save|load|list|info> [slot]";
        }
        String action = parts[0].toLowerCase();
        String slot = parts.length > 1 ? parts[1] : "";
        Minecraft mc = Minecraft.getInstance();
        return switch (action) {
            case "save" -> {
                if (slot.isEmpty()) {
                    yield "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: screen save <slot>";
                }
                if (mc.screen == null || mc.player == null) {
                    yield "\u00a77[\u00a7c*\u00a77] \u00a7cNo screen to save";
                }
                SharedVariables.savedScreens.put(slot, mc.screen);
                SharedVariables.savedScreenHandlers.put(slot, mc.player.containerMenu);
                yield "\u00a77[\u00a7c*\u00a77] Saved to: \u00a7c" + slot;
            }
            case "load" -> {
                if (slot.isEmpty()) {
                    yield "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: screen load <slot>";
                }
                Screen screen = SharedVariables.savedScreens.get(slot);
                if (screen == null) {
                    yield "\u00a77[\u00a7c*\u00a77] \u00a7cNo screen in slot: \u00a77" + slot;
                }
                mc.execute(() -> {
                    mc.setScreen(screen);
                    if (mc.player != null && SharedVariables.savedScreenHandlers.containsKey(slot)) {
                        mc.player.containerMenu = SharedVariables.savedScreenHandlers.get(slot);
                    }
                });
                yield "\u00a77[\u00a7c*\u00a77] Loaded: \u00a7c" + slot;
            }
            case "list" -> {
                if (SharedVariables.savedScreens.isEmpty()) {
                    yield "\u00a77[\u00a7c*\u00a77] No saved screens";
                }
                yield "\u00a77[\u00a7c*\u00a77] Slots: \u00a7c" + String.join((CharSequence)"\u00a77, \u00a7c", SharedVariables.savedScreens.keySet());
            }
            case "info" -> {
                if (slot.isEmpty()) {
                    yield "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: screen info <slot>";
                }
                Screen screen = SharedVariables.savedScreens.get(slot);
                if (screen == null) {
                    yield "\u00a77[\u00a7c*\u00a77] \u00a7cNo screen in slot: \u00a77" + slot;
                }
                yield "\u00a77[\u00a7c*\u00a77] Slot: \u00a7c" + slot + " \u00a77| Screen: \u00a7c" + screen.getClass().getSimpleName();
            }
            default -> "\u00a77[\u00a7c*\u00a77] \u00a7cUnknown action";
        };
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static String accountCommand(String args) {
        String[] parts = args.split("\\s+", 3);
        if (parts.length == 0) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: account <dump|set> [args]";
        }
        if (parts[0].isEmpty()) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: account <dump|set> [args]";
        }
        String action = parts[0].toLowerCase();
        Minecraft mc = Minecraft.getInstance();
        switch (action) {
            case "dump": {
                User session = mc.getUser();
                return "\u00a77[\u00a7c*\u00a77] Username: \u00a7c" + session.getName()
                        + "\n\u00a77[\u00a7c*\u00a77] UUID: \u00a7c" + session.getProfileId();
            }
            case "set": {
                if (parts.length < 3) {
                    return "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: account set <username|uuid> <value>";
                }
                String type = parts[1].toLowerCase();
                String value = parts[2];
                try {
                    User oldSession = mc.getUser();
                    User newSession;
                    if (type.equals("username")) {
                        newSession = SessionUtils.copyWith(oldSession, value, null);
                    } else if (type.equals("uuid")) {
                        newSession = SessionUtils.copyWith(oldSession, null, UUID.fromString(value));
                    } else {
                        return "\u00a77[\u00a7c*\u00a77] \u00a7cUnknown type: \u00a77" + type + " \u00a7c(use username or uuid)";
                    }
                    Field sessionField = Minecraft.class.getDeclaredField("session");
                    sessionField.setAccessible(true);
                    sessionField.set(mc, newSession);
                    return "\u00a77[\u00a7c*\u00a77] Set \u00a7c" + type + " \u00a77= \u00a7c" + value;
                } catch (IllegalArgumentException e) {
                    return "\u00a77[\u00a7c*\u00a77] \u00a7cInvalid UUID format";
                } catch (Exception e) {
                    return "\u00a77[\u00a7c*\u00a77] \u00a7cFailed to set " + type + ": \u00a77" + e.getMessage();
                }
            }
            default:
                return "\u00a77[\u00a7c*\u00a77] \u00a7cUnknown action: \u00a77" + action;
        }
    }

    private static String prefixCommand(String args) {
        String newPrefix;
        if (args.isEmpty()) {
            return "\u00a77[\u00a7c*\u00a77] Current prefix: \u00a7c" + SharedVariables.commandPrefix;
        }
        SharedVariables.commandPrefix = newPrefix = args.split("\\s+")[0];
        return "\u00a77[\u00a7c*\u00a77] Prefix changed to: \u00a7c" + newPrefix;
    }

    private static String vclipCommand(String args) {
        if (args.isEmpty()) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: vclip <blocks>";
        }
        try {
            double blocks = Double.parseDouble(args.split("\\s+")[0]);
            ClipUtils.vClip(blocks);
            return "\u00a77[\u00a7c*\u00a77] VClip: \u00a7c" + blocks + " \u00a77blocks";
        }
        catch (NumberFormatException e) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cInvalid number";
        }
    }

    private static String hclipCommand(String args) {
        if (args.isEmpty()) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cUsage: hclip <blocks>";
        }
        try {
            double blocks = Double.parseDouble(args.split("\\s+")[0]);
            ClipUtils.hClip(blocks);
            return "\u00a77[\u00a7c*\u00a77] HClip: \u00a7c" + blocks + " \u00a77blocks";
        }
        catch (NumberFormatException e) {
            return "\u00a77[\u00a7c*\u00a77] \u00a7cInvalid number";
        }
    }

    private static String pluginsCommand(String args) {
        PluginScanner.startScan();
        return "\u00a77[\u00a7c*\u00a77] Scanning plugins...";
    }

    private static double evaluateExpression(String expression) {
        expression = expression.replaceAll("\\s+", "");
        return CommandSystem.parseExpression(expression, new int[]{0});
    }

    private static double parseExpression(String expr, int[] pos) {
        double result = CommandSystem.parseTerm(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '+') {
                pos[0] = pos[0] + 1;
                result += CommandSystem.parseTerm(expr, pos);
                continue;
            }
            if (op != '-') break;
            pos[0] = pos[0] + 1;
            result -= CommandSystem.parseTerm(expr, pos);
        }
        return result;
    }

    private static double parseTerm(String expr, int[] pos) {
        double result = CommandSystem.parseFactor(expr, pos);
        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op == '*') {
                pos[0] = pos[0] + 1;
                result *= CommandSystem.parseFactor(expr, pos);
                continue;
            }
            if (op != '/') break;
            pos[0] = pos[0] + 1;
            result /= CommandSystem.parseFactor(expr, pos);
        }
        return result;
    }

    private static double parseFactor(String expr, int[] pos) {
        return CommandSystem.parsePower(expr, pos);
    }

    private static double parsePower(String expr, int[] pos) {
        double base = CommandSystem.parseUnary(expr, pos);
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '^') {
            pos[0] = pos[0] + 1;
            return Math.pow(base, CommandSystem.parsePower(expr, pos));
        }
        return base;
    }

    private static double parseUnary(String expr, int[] pos) {
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '-') {
            pos[0] = pos[0] + 1;
            return -CommandSystem.parseUnary(expr, pos);
        }
        return CommandSystem.parsePrimary(expr, pos);
    }

    private static double parsePrimary(String expr, int[] pos) {
        if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
            pos[0] = pos[0] + 1;
            double result = CommandSystem.parseExpression(expr, pos);
            if (pos[0] < expr.length() && expr.charAt(pos[0]) == ')') {
                pos[0] = pos[0] + 1;
            }
            return result;
        }
        StringBuilder sb = new StringBuilder();
        while (pos[0] < expr.length() && (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {
            int n = pos[0];
            pos[0] = n + 1;
            sb.append(expr.charAt(n));
        }
        return Double.parseDouble(sb.toString());
    }

    static {
        CommandSystem.registerCommand("help", CommandSystem::helpCommand, "\u00a77Lists all commands or shows help for one\n\u00a7cUsage: \u00a77,help [command]");
        CommandSystem.registerCommand("man", CommandSystem::manCommand, "\u00a77Shows detailed manual for a command\n\u00a7cUsage: \u00a77,man <command>");
        CommandSystem.registerCommand("toggleuiutils", CommandSystem::toggleCommand, "\u00a77Toggles UI-Utils overlay on/off\n\u00a7cUsage: \u00a77,toggleuiutils");
        CommandSystem.registerCommand("echo", CommandSystem::echoCommand, "\u00a77Prints text to chat (client-side)\n\u00a7cUsage: \u00a77,echo <text>");
        CommandSystem.registerCommand("math", CommandSystem::mathCommand, "\u00a77Calculates math expression\n\u00a7cSupports: \u00a77+ - * / ^ ()\n\u00a7cUsage: \u00a77,math <expression>\n\u00a7cExample: \u00a77,math 2+2*3");
        CommandSystem.registerCommand("close", CommandSystem::closeCommand, "\u00a77Closes current screen without packet\n\u00a7cUsage: \u00a77,close");
        CommandSystem.registerCommand("desync", CommandSystem::desyncCommand, "\u00a77Sends close packet but keeps GUI open\n\u00a7cUsage: \u00a77,desync");
        CommandSystem.registerCommand("chat", CommandSystem::chatCommand, "\u00a77Sends message/command to server\n\u00a7cUsage: \u00a77,chat <message>\n\u00a7cExample: \u00a77,chat /spawn");
        CommandSystem.registerCommand("joinserver", CommandSystem::joinServerCommand, "\u00a77Connects to another server\n\u00a7cUsage: \u00a77,joinserver <ip[:port]>\n\u00a7cExample: \u00a77,joinserver mc.hypixel.net");
        CommandSystem.registerCommand("screen", CommandSystem::screenCommand, "\u00a77Manages saved screens\n\u00a7cActions:\n\u00a77  save <slot> - Save current screen\n\u00a77  load <slot> - Load saved screen\n\u00a77  list - List all saved slots\n\u00a77  info <slot> - Show screen info");
        CommandSystem.registerCommand("account", CommandSystem::accountCommand, "\u00a77View/change session info\n\u00a7cActions:\n\u00a77  dump - Show current username/UUID\n\u00a77  set username <name> - Change username\n\u00a77  set uuid <uuid> - Change UUID\n\u00a7cNote: \u00a77Changes are temporary until restart");
        CommandSystem.registerCommand("prefix", CommandSystem::prefixCommand, "\u00a77View/change command prefix\n\u00a7cUsage: \u00a77,prefix [new]\n\u00a7cExample: \u00a77,prefix .");
        CommandSystem.registerCommand("vclip", CommandSystem::vclipCommand, "\u00a77Teleports vertically (client-side)\n\u00a7cUsage: \u00a77,vclip <blocks>\n\u00a7cExample: \u00a77,vclip 5 (up), ,vclip -3 (down)");
        CommandSystem.registerCommand("hclip", CommandSystem::hclipCommand, "\u00a77Teleports horizontally in look direction\n\u00a7cUsage: \u00a77,hclip <blocks>\n\u00a7cExample: \u00a77,hclip 10");
        CommandSystem.registerCommand("plugins", CommandSystem::pluginsCommand, "\u00a77Scans server for plugins via tab-complete\n\u00a7cUsage: \u00a77,plugins\n\u00a74Red \u00a77= Anticheat/exploit related");
    }

    @FunctionalInterface
    public static interface CommandHandler {
        public String execute(String var1);
    }
}

