package com.ui_utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.client.gui.screen.Screen;

public class SharedVariables {
    public static boolean sendUIPackets = true;
    public static boolean delayUIPackets = false;
    public static boolean shouldEditSign = true;
    public static ArrayList<Packet<?>> delayedUIPackets = new ArrayList<>();
    public static Screen storedScreen = null;
    public static ScreenHandler storedScreenHandler = null;
    public static Map<String, Screen> savedScreens = new HashMap<String, Screen>();
    public static Map<String, ScreenHandler> savedScreenHandlers = new HashMap<String, ScreenHandler>();
    public static boolean enabled = true;
    public static boolean bypassResourcePack = false;
    public static boolean resourcePackForceDeny = false;
    public static String commandPrefix = ",";
    public static int spamCount = 1;
}

