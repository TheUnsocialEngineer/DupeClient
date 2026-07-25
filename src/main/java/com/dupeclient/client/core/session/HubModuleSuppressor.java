package com.dupeclient.client.core.session;

import com.dupeclient.client.docs.ScreenshotCaptureMode;
import com.dupeclient.client.module.acaudit.AcAuditManager;
import com.dupeclient.client.module.fuzzer.MinimessageFuzzerManager;
import com.dupeclient.client.module.fuzzer.SqliFuzzerManager;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerManager;
import com.dupeclient.client.module.macro.MacroEngine;
import com.dupeclient.client.module.macro.MacroQuickPlay;
import com.dupeclient.client.module.packet.fabricator.FabricatorSendScheduler;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import com.dupeclient.client.module.packet.sniffer.PacketReplayScheduler;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferManager;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferOverlay;
import com.dupeclient.client.module.payall.PayAllManager;
import com.dupeclient.client.module.mcptools.McpToolsManager;
import com.dupeclient.client.module.dupedb.DupedbManager;
import com.dupeclient.client.module.utility.ChatGamesManager;
import com.dupeclient.client.module.utility.crashes.CrashesManager;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class HubModuleSuppressor {
    private static volatile boolean lastRestricted;

    private HubModuleSuppressor() {
    }

    public static void tick(@Nullable Minecraft client) {
        boolean restricted = shouldSuppressExploits();
        if (restricted && !lastRestricted) {
            suppressExploits(client, HubModuleRules.viewerRestricted());
        } else if (restricted) {
            suppressExploitsSilent(client);
        }
        lastRestricted = restricted;
    }

    private static boolean shouldSuppressExploits() {
        if (ScreenshotCaptureMode.isActive()) {
            return false;
        }
        if (HubModuleRules.viewerRestricted()) {
            return true;
        }
        return !HubModuleRules.exploitFeaturesAllowed();
    }

    private static void suppressExploitsSilent(@Nullable Minecraft client) {
        if (client != null) {
            MacroEngine.INSTANCE.stop(client);
        }
        MacroQuickPlay.disableForStaffLock();
        PayAllManager.INSTANCE.cancelIfActive();
        McpToolsManager.INSTANCE.onStaffLock();
        EconomyFuzzerManager.INSTANCE.stop(null);
        SqliFuzzerManager.INSTANCE.stop(null);
        MinimessageFuzzerManager.INSTANCE.stop(null);
        PacketSnifferManager.INSTANCE.setEnabled(false);
        PacketSnifferOverlay.INSTANCE.setOverlayVisible(false);
        AcAuditManager.INSTANCE.setEnabled(false);
        ChatGamesManager.INSTANCE.setEnabled(false);
        CrashesManager.INSTANCE.setChestCrashEnabled(false);
        CrashesManager.INSTANCE.setArmorPlaceEnabled(false);
        FabricatorSendScheduler.INSTANCE.stop(null);
        PacketFabricatorOverlay.INSTANCE.setVisible(false);
        PacketReplayScheduler.INSTANCE.stopQuietly();
        DupedbManager.INSTANCE.abortActiveScan();
    }

    public static boolean wasRestrictedLastTick() {
        return lastRestricted;
    }

    private static void suppressExploits(@Nullable Minecraft client, boolean staffLock) {
        if (client != null) {
            MacroEngine.INSTANCE.stop(client);
        }
        MacroQuickPlay.disableForStaffLock();
        PayAllManager.INSTANCE.cancelIfActive();
        McpToolsManager.INSTANCE.onStaffLock();
        String reason = staffLock ? "Staff account restricted." : "Module access restricted.";
        EconomyFuzzerManager.INSTANCE.stop(reason);
        SqliFuzzerManager.INSTANCE.stop(reason);
        MinimessageFuzzerManager.INSTANCE.stop(reason);
        PacketSnifferManager.INSTANCE.setEnabled(false);
        PacketSnifferOverlay.INSTANCE.setOverlayVisible(false);
        AcAuditManager.INSTANCE.setEnabled(false);
        ChatGamesManager.INSTANCE.setEnabled(false);
        CrashesManager.INSTANCE.setChestCrashEnabled(false);
        CrashesManager.INSTANCE.setArmorPlaceEnabled(false);
        FabricatorSendScheduler.INSTANCE.stop(reason);
        PacketFabricatorOverlay.INSTANCE.setVisible(false);
        PacketReplayScheduler.INSTANCE.stopQuietly();
        DupedbManager.INSTANCE.abortActiveScan();
    }
}
