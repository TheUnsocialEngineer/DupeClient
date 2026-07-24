package com.dupeclient.client.module.packet.sniffer;

import java.util.ArrayList;
import java.util.List;

public final class PacketSnifferSettings {
    public boolean enabled;
    public boolean paused;
    public boolean overlayVisible;
    public int overlayX = 8;
    public int overlayY = 180;
    public int overlayToggleKey = -1;
    /** {@code name}, {@code summary}, or {@code full}. */
    public String detailLevel = "summary";
    public boolean ignoreKeepAlive = true;
    public boolean ignorePlayerMove;
    public boolean logToConsole;
    public boolean logToFile;
    public boolean clearOnLeave = true;
    public boolean moduleChatFeedback = true;
    public int maxEntries = 2000;
    public int replayDelayMs = 50;
    public int replayPacketsPerTick = 1;
    public boolean snifferPopOut = false;
    public int snifferPopOutWindowX = Integer.MIN_VALUE;
    public int snifferPopOutWindowY = Integer.MIN_VALUE;

    /** Packets in these lists are never captured or shown in the sniffer log. */
    public List<String> logExcludeC2sNames = new ArrayList<>();
    public List<String> logExcludeS2cNames = new ArrayList<>();

    /** When enabled, packets in block lists are cancelled on the network. */
    public boolean blockEnabled;
    public boolean blockChatNotify = true;
    public List<String> blockC2sNames = new ArrayList<>();
    public List<String> blockS2cNames = new ArrayList<>();

    public void ensureLists() {
        if (logExcludeC2sNames == null) {
            logExcludeC2sNames = new ArrayList<>();
        }
        if (logExcludeS2cNames == null) {
            logExcludeS2cNames = new ArrayList<>();
        }
        if (blockC2sNames == null) {
            blockC2sNames = new ArrayList<>();
        }
        if (blockS2cNames == null) {
            blockS2cNames = new ArrayList<>();
        }
    }
}
