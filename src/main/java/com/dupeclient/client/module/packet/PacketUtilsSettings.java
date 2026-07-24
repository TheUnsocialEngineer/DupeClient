package com.dupeclient.client.module.packet;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class PacketUtilsSettings {
    /** Bumped when defaults change; Gson leaves missing fields at 0. */
    public int configVersion = 5;

    // Controls whether utility action elements are shown in GUI contexts.
    public boolean uiElementsEnabled = true;
    public int uiElementsToggleKey = GLFW.GLFW_KEY_P;

    // Core queue mode (du-addon style): matched packets are queued and drained per tick.
    public boolean delayEnabled = false;
    public int delayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public double delayMs = 150.0;
    public EnumSet<PacketKind> delayKinds = EnumSet.of(PacketKind.MOVEMENT, PacketKind.INTERACTION);
    public int coreQueueToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int coreQueueFlushKey = GLFW.GLFW_KEY_UNKNOWN;

    public boolean desyncEnabled = false;
    public int desyncToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public EnumSet<PacketKind> desyncKinds = EnumSet.of(PacketKind.MOVEMENT, PacketKind.INTERACTION, PacketKind.INVENTORY);

    // Separate module inspired by du-addon packet helper.
    public boolean advancedModuleEnabled = false;
    public int advancedToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public double advancedDelayMs = 60.0;
    public EnumSet<PacketKind> advancedKinds = EnumSet.of(PacketKind.COMMAND, PacketKind.CHAT, PacketKind.CUSTOM_PAYLOAD);

    public boolean spamEnabled = false;
    public int spamToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public String spamMessage = "/ping";
    public double spamDelayMs = 350.0;
    public int maxSendPerTick = 20;

    // Meteor-like packet delay module: hold selected packets until disabled/flush.
    public boolean packetDelayEnabled = false;
    /** Legacy field; packet delay no longer uses kinds. Kept for Gson compatibility. */
    public EnumSet<PacketKind> packetDelayKinds = EnumSet.of(PacketKind.MOVEMENT, PacketKind.INTERACTION);
    /**
     * Outgoing packet delay matches these C2S registry names (e.g. {@code ClickSlotC2SPacket},
     * {@code PlayerMoveC2SPacket.Full}). When empty, no C2S packets are delayed. Names come from {@link PacketUtils#getName}.
     */
    public List<String> packetDelayC2sClassNames = new ArrayList<>();
    /**
     * When non-empty, inbound (S2C) packet delay matches these registry names (see {@link PacketUtils#getName}).
     */
    public List<String> packetDelayS2cClassNames = new ArrayList<>();
    public boolean logPacketNamesOnDelay = false;
    /** Grey chat line when a C2S/S2C packet is held by packet delay. */
    public boolean packetDelayBlockedChatNotify = true;
    /** Hotkey to toggle {@link #packetDelayEnabled} without opening the GUI. */
    public int packetDelayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public boolean packetDelayOverlayVisible = false;
    public int packetDelayOverlayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int packetDelayOverlayX = 16;
    public int packetDelayOverlayY = 120;

    // Ported UI-Utils controls exposed inside Packet Utils module.
    public boolean uiUtilsOverlayEnabled = true;
    public double uiUtilsDelayReleaseMs = 0.0;
    public int uiUtilsOverlayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int uiUtilsCloseWithoutPacketKey = GLFW.GLFW_KEY_UNKNOWN;
    public int uiUtilsDelayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int uiUtilsSendPacketsToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int uiUtilsSendQueuedKey = GLFW.GLFW_KEY_UNKNOWN;

    /** Chat lines for Packet Utils toggles, hotkeys, and notify-style status (not blocked-packet grey lines). */
    public boolean moduleChatFeedback = true;
    /** When true, active packet features turn off after leaving a world or disconnecting from a server. */
    public boolean disableActiveOnLeave = true;

    // Packet fabricator (YungLight PackUtil fabricator) — inventory click packet builder overlay.
    public boolean fabricatorEnabled = true;
    public boolean fabricatorVisible = false;
    public String fabricatorActiveTab = "fabricate";
    public int fabricatorOverlayX = 220;
    public int fabricatorOverlayY = 8;
    public String fabricatorSlot = "0";
    /** When true, clicking slots in a container toggles them in a comma-separated slot list. */
    public boolean fabricatorMultiSlot = false;
    public String fabricatorItemName = "";
    public String fabricatorTimes = "1";
    public int fabricatorActionIndex = 0;
    public int fabricatorClickButton = 0;
    public boolean fabricatorDropWholeStack = false;
    public int fabricatorToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    /** Max fabricated click packets sent per client tick while Send is running. */
    public int fabricatorPacketsPerTick = 1;
    /** Minimum milliseconds between fabricated packet sends (0 = tick batch only). */
    public int fabricatorSendDelayMs = 0;
    public boolean fabricatorPopOut = false;
    public int fabricatorPopOutWindowX = Integer.MIN_VALUE;
    public int fabricatorPopOutWindowY = Integer.MIN_VALUE;

    /** Scale inventory/container GUIs only (does not change Options → GUI Scale). */
    public boolean handledScreenScaleEnabled = true;
    public float handledScreenScale = 1.5f;
    /** Boost container scale for large modded panels (Axiom, etc.). */
    public boolean handledScreenAutoScaleLarge = true;

    /** Draw fabricator slot ids on container screens (0–40 = player, 100+ = GUI). */
    public boolean slotIdsOverlayEnabled = true;
    public int slotIdsToggleKey = GLFW.GLFW_KEY_UNKNOWN;
}
