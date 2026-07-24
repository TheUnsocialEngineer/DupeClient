package com.dupeclient.client.module.packet.fabricator;

import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class FabricatorPresetCodec {
    private static final Gson GSON = new GsonBuilder().create();

    private FabricatorPresetCodec() {
    }

    public static String captureCurrent() {
        PacketUtilsSettings s = PacketUtilsManager.INSTANCE.getSettings();
        Snapshot snap = new Snapshot();
        snap.activeTab = s.fabricatorActiveTab;
        snap.slot = s.fabricatorSlot;
        snap.multiSlot = s.fabricatorMultiSlot;
        snap.itemName = s.fabricatorItemName;
        snap.times = s.fabricatorTimes;
        snap.actionIndex = s.fabricatorActionIndex;
        snap.clickButton = s.fabricatorClickButton;
        snap.dropWholeStack = s.fabricatorDropWholeStack;
        snap.packetsPerTick = s.fabricatorPacketsPerTick;
        snap.sendDelayMs = s.fabricatorSendDelayMs;
        return GSON.toJson(snap);
    }

    public static void apply(String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        Snapshot snap = GSON.fromJson(json, Snapshot.class);
        if (snap == null) {
            return;
        }
        PacketUtilsSettings s = PacketUtilsManager.INSTANCE.getSettings();
        if (snap.activeTab != null) {
            s.fabricatorActiveTab = snap.activeTab;
        }
        if (snap.slot != null) {
            s.fabricatorSlot = snap.slot;
        }
        s.fabricatorMultiSlot = snap.multiSlot;
        if (snap.itemName != null) {
            s.fabricatorItemName = snap.itemName;
        }
        if (snap.times != null) {
            s.fabricatorTimes = snap.times;
        }
        s.fabricatorActionIndex = snap.actionIndex;
        s.fabricatorClickButton = snap.clickButton;
        s.fabricatorDropWholeStack = snap.dropWholeStack;
        s.fabricatorPacketsPerTick = snap.packetsPerTick;
        s.fabricatorSendDelayMs = snap.sendDelayMs;
        PacketUtilsManager.INSTANCE.save();
    }

    private static final class Snapshot {
        String activeTab;
        String slot;
        boolean multiSlot;
        String itemName;
        String times;
        int actionIndex;
        int clickButton;
        boolean dropWholeStack;
        int packetsPerTick;
        int sendDelayMs;
    }
}
