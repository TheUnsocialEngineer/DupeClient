package com.dupeclient.client.module.acaudit;

import org.lwjgl.glfw.GLFW;

public final class AcAuditSettings {
    public boolean enabled;
    public boolean overlayVisible;
    public int overlayX = 8;
    public int overlayY = 80;
    public int overlayToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public int toggleKey = GLFW.GLFW_KEY_UNKNOWN;

    public boolean logProbeToChat;
    public boolean announcePlatform = true;

    public boolean setbackVerbose;
    public int setbackReportIntervalSec = 1;

    public boolean correctionVerbose = true;
    public boolean packetCadenceEnabled = true;
    public int packetCadenceTopN = 5;
    public int packetCadenceIntervalSec = 5;

    public boolean rawSlotOverlayEnabled;
    public boolean slotOverlayShowSyncId = true;
    public boolean slotOverlayShowHoveredItem = true;
    public boolean slotOverlayShadow = true;
    public int slotOverlayColor = 0xFFFFFF00;

    public boolean slotSyncProbeActive;
    public SlotSyncField slotSyncProbeField = SlotSyncField.ALL;
    public int slotSyncProbeDelayTicks = 10;
    public boolean slotSyncProbeLoop;

    public ManualSyncMode manualClickSyncMode = ManualSyncMode.CURRENT;
    public ManualRevMode manualClickRevMode = ManualRevMode.CURRENT;
    public int manualClickCustomSyncId;
    public int manualClickCustomRev;
    public int manualClickSlot;
    public int manualClickButton;
    public ManualClickAction manualClickAction = ManualClickAction.PICKUP;
    public int manualClickCount = 1;
    public int manualClickPrePickupSlot = -1;

    public boolean commandFingerprintActive;
    public boolean commandFingerprintSweep = true;
    public String commandFingerprintPrefix = "/";
    public int commandFingerprintDelayTicks = 8;

    public boolean disableOnLeave = true;
    public String overlayTab = "MONITOR";

    public enum SlotSyncField {
        SYNC_ID,
        SLOT,
        BUTTON,
        REVISION,
        ALL
    }

    public enum ManualSyncMode {
        CURRENT,
        CUSTOM
    }

    public enum ManualRevMode {
        CURRENT,
        ZERO,
        CUSTOM
    }

    public enum ManualClickAction {
        PICKUP,
        QUICK_MOVE,
        SWAP,
        CLONE,
        THROW,
        QUICK_CRAFT,
        PICKUP_ALL
    }
}
