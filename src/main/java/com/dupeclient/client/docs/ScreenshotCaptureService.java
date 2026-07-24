package com.dupeclient.client.docs;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.config.ClientGuiLayoutStorage;
import com.dupeclient.client.gui.ClientGuiScreen;
import com.dupeclient.client.gui.MacroEditorScreen;
import com.dupeclient.client.gui.SocialScreen;
import com.dupeclient.client.gui.WaypointsScreen;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import com.dupeclient.client.gui.panel.Panel;
import com.dupeclient.client.module.acaudit.AcAuditManager;
import com.dupeclient.client.module.acaudit.AcAuditOverlay;
import com.dupeclient.client.module.dupedb.DupedbOverlay;
import com.dupeclient.client.module.dupedb.search.ServerScannerScreen;
import com.dupeclient.client.module.dupedb.search.ServerSearchAuthScreen;
import com.dupeclient.client.module.dupedb.search.api.ApiClient;
import com.dupeclient.client.module.dupedb.search.auth.AddonAuth;
import com.dupeclient.client.module.fuzzer.FuzzerOverlay;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerManager;
import com.dupeclient.client.module.hud.HudEditorScreen;
import com.dupeclient.client.module.mcptools.McpToolsManager;
import com.dupeclient.client.module.mcptools.McpToolsOverlay;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferManager;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferOverlay;
import com.dupeclient.client.module.payall.PayAllManager;
import com.dupeclient.client.module.payall.PayAllOverlay;
import com.dupeclient.client.module.serverpassword.ServerPasswordScreen;
import com.dupeclient.client.module.utility.ChatGamesManager;
import com.dupeclient.client.module.utility.ChatGamesOverlay;
import com.dupeclient.client.module.utility.nbtedit.NbtEditScreen;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class ScreenshotCaptureService {
    public static final ScreenshotCaptureService INSTANCE = new ScreenshotCaptureService();

    private static final int SETTLE_TICKS = 50;
    private static final int SETTLE_TICKS_HEAVY = 120;
    private static final int BETWEEN_TICKS = 15;
    private static final int CAPTURE_TIMEOUT_TICKS = 120;

    private enum Phase {
        WAIT_WORLD,
        PREPARE,
        SETTLE,
        CAPTURE,
        GAP,
        DONE
    }

    private record CaptureTarget(String id, int settleTicks, Consumer<MinecraftClient> prepare) {
        CaptureTarget(String id, Consumer<MinecraftClient> prepare) {
            this(id, SETTLE_TICKS, prepare);
        }
    }

    private List<CaptureTarget> targets;
    private Path outputDir;
    private Phase phase = Phase.WAIT_WORLD;
    private int index;
    private int timer;
    private int worldWait;
    private int settleTicks;
    private boolean worldBootstrapRequested;
    private boolean finished;
    private CompletableFuture<NativeImage> pendingCapture;
    private String pendingCaptureId;
    private int captureWaitTicks;
    private final Map<String, String> manifest = new LinkedHashMap<>();

    private ScreenshotCaptureService() {
    }

    public void initialize() {
        if (!ScreenshotCaptureMode.isActive()) {
            return;
        }
        outputDir = ScreenshotCaptureMode.outputDir();
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            DupeClient.LOGGER.error("Could not create screenshot output dir: {}", outputDir, e);
        }
        targets = buildTargets();
        DupeClient.LOGGER.info("Screenshot capture enabled. Output: {}", outputDir);
    }

    public void tick(MinecraftClient client) {
        if (!ScreenshotCaptureMode.isActive() || client == null || targets == null) {
            return;
        }
        switch (phase) {
            case WAIT_WORLD -> {
                if (client.player == null || client.world == null) {
                    worldWait++;
                    if (!worldBootstrapRequested && worldWait >= 40) {
                        worldBootstrapRequested = true;
                        ScreenshotWorldBootstrap.ensureWorld(client);
                    }
                    return;
                }
                if (++settleTicks < 100) {
                    return;
                }
                hideAllOverlays();
                client.setScreen(null);
                phase = Phase.PREPARE;
            }
            case PREPARE -> {
                if (index >= targets.size()) {
                    phase = Phase.DONE;
                    return;
                }
                CaptureTarget target = targets.get(index);
                target.prepare().accept(client);
                timer = target.settleTicks();
                phase = Phase.SETTLE;
            }
            case SETTLE -> {
                if (--timer > 0) {
                    return;
                }
                phase = Phase.CAPTURE;
            }
            case CAPTURE -> {
                if (pendingCapture == null) {
                    CaptureTarget target = targets.get(index);
                    pendingCaptureId = target.id();
                    pendingCapture = new CompletableFuture<>();
                    ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), pendingCapture::complete);
                    captureWaitTicks = 0;
                    return;
                }
                if (!pendingCapture.isDone()) {
                    if (++captureWaitTicks > CAPTURE_TIMEOUT_TICKS) {
                        DupeClient.LOGGER.warn("Screenshot timed out for {}", pendingCaptureId);
                        clearPendingCapture();
                        timer = BETWEEN_TICKS;
                        phase = Phase.GAP;
                    }
                    return;
                }
                finishPendingCapture();
                timer = BETWEEN_TICKS;
                phase = Phase.GAP;
            }
            case GAP -> {
                if (--timer > 0) {
                    return;
                }
                index++;
                phase = Phase.PREPARE;
            }
            case DONE -> {
                if (finished) {
                    return;
                }
                finished = true;
                writeManifest();
                DupeClient.LOGGER.info("Screenshot capture complete ({} images).", manifest.size());
                client.scheduleStop();
            }
        }
    }

    private void finishPendingCapture() {
        if (pendingCapture == null || pendingCaptureId == null || outputDir == null) {
            clearPendingCapture();
            return;
        }
        try {
            NativeImage image = pendingCapture.getNow(null);
            if (image != null) {
                Path file = outputDir.resolve(pendingCaptureId + ".png");
                image.writeTo(file);
                image.close();
                manifest.put(pendingCaptureId, pendingCaptureId + ".png");
                DupeClient.LOGGER.info("Captured {}", file);
            }
        } catch (Exception e) {
            DupeClient.LOGGER.warn("Screenshot failed for {}: {}", pendingCaptureId, e.toString());
        } finally {
            clearPendingCapture();
        }
    }

    private void clearPendingCapture() {
        pendingCapture = null;
        pendingCaptureId = null;
        captureWaitTicks = 0;
    }

    private void writeManifest() {
        if (outputDir == null) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("generatedAt", Instant.now().toString());
        root.addProperty("count", manifest.size());
        JsonObject images = new JsonObject();
        manifest.forEach(images::addProperty);
        root.add("images", images);
        try {
            Files.writeString(
                    outputDir.resolve("manifest.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(root)
            );
        } catch (IOException e) {
            DupeClient.LOGGER.warn("Could not write manifest.json", e);
        }
    }

    private static void hideAllOverlays() {
        for (IngameModuleOverlay overlay : IngameOverlayHost.all()) {
            overlay.setOverlayVisible(false);
        }
        IngameOverlayHost.hideFabricatorOverlay();
    }

    private static void showOverlay(IngameModuleOverlay overlay) {
        hideAllOverlays();
        if (overlay != null) {
            overlay.setOverlayVisible(true);
        }
    }

    private static void openHub(int panelIndex) {
        int count = DupeClient.getGuiManager().getPanels().size();
        int idx = Math.max(0, Math.min(panelIndex, count - 1));
        ClientGuiLayoutStorage.saveClientGuiLayout(idx, new double[count]);
    }

    private static void equipSampleItem(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        ItemStack sample = new ItemStack(Items.DIAMOND_SWORD);
        client.player.getInventory().setSelectedSlot(0);
        client.player.getInventory().setStack(0, sample);
    }

    private static List<CaptureTarget> buildTargets() {
        List<CaptureTarget> out = new ArrayList<>();
        out.add(new CaptureTarget("hub", c -> {
            hideAllOverlays();
            c.setScreen(new ClientGuiScreen(c.currentScreen));
        }));

        List<Panel> panels = DupeClient.getGuiManager().getPanels();
        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            String id = "panel-" + panel.getId().replace('_', '-');
            int idx = i;
            out.add(new CaptureTarget(id, c -> {
                openHub(idx);
                c.setScreen(new ClientGuiScreen(c.currentScreen));
            }));
        }

        out.add(new CaptureTarget("overlay-dupedb", c -> {
            c.setScreen(null);
            showOverlay(DupedbOverlay.INSTANCE);
        }));
        out.add(new CaptureTarget("overlay-sniffer", c -> {
            c.setScreen(null);
            PacketSnifferManager.INSTANCE.getSettings().enabled = true;
            PacketSnifferManager.INSTANCE.save();
            showOverlay(PacketSnifferOverlay.INSTANCE);
        }));
        out.add(new CaptureTarget("overlay-fabricator", c -> {
            c.setScreen(null);
            PacketUtilsManager.INSTANCE.getSettings().fabricatorEnabled = true;
            PacketUtilsManager.INSTANCE.getSettings().fabricatorVisible = true;
            PacketUtilsManager.INSTANCE.save();
            showOverlay(PacketFabricatorOverlay.INSTANCE);
        }));
        out.add(new CaptureTarget("overlay-payall", c -> {
            c.setScreen(null);
            PayAllManager.INSTANCE.getSettings().enabled = true;
            PayAllManager.INSTANCE.getSettings().overlayVisible = true;
            PayAllManager.INSTANCE.saveSettings();
            showOverlay(PayAllOverlay.INSTANCE);
        }));
        out.add(new CaptureTarget("overlay-mcptools", c -> {
            c.setScreen(null);
            McpToolsManager.INSTANCE.getSettings().enabled = true;
            McpToolsManager.INSTANCE.getSettings().overlayVisible = true;
            McpToolsManager.INSTANCE.saveSettings();
            showOverlay(McpToolsOverlay.INSTANCE);
        }));
        out.add(new CaptureTarget("overlay-ac-audit", c -> {
            c.setScreen(null);
            AcAuditManager.INSTANCE.setEnabled(true);
            AcAuditManager.INSTANCE.getSettings().overlayVisible = true;
            AcAuditManager.INSTANCE.save();
            showOverlay(AcAuditOverlay.INSTANCE);
        }));
        out.add(new CaptureTarget("overlay-fuzzer", c -> {
            c.setScreen(null);
            EconomyFuzzerManager.INSTANCE.getSettings().enabled = true;
            EconomyFuzzerManager.INSTANCE.getSettings().overlayVisible = true;
            EconomyFuzzerManager.INSTANCE.save();
            showOverlay(FuzzerOverlay.INSTANCE);
        }));
        out.add(new CaptureTarget("overlay-chat-games", c -> {
            c.setScreen(null);
            ChatGamesManager.INSTANCE.getSettings().overlayVisible = true;
            ChatGamesManager.INSTANCE.save();
            showOverlay(ChatGamesOverlay.INSTANCE);
        }));

        out.add(new CaptureTarget("screen-social", c -> {
            hideAllOverlays();
            c.setScreen(new SocialScreen(c.currentScreen));
        }));
        out.add(new CaptureTarget("screen-waypoints", c -> {
            hideAllOverlays();
            c.setScreen(new WaypointsScreen(c.currentScreen));
        }));
        out.add(new CaptureTarget("screen-macro-studio", c -> {
            hideAllOverlays();
            MacroEditorScreen.open(c, null);
        }));
        out.add(new CaptureTarget("screen-hud-editor", SETTLE_TICKS_HEAVY, c -> {
            hideAllOverlays();
            c.setScreen(new HudEditorScreen(c.currentScreen));
        }));
        out.add(new CaptureTarget("screen-vault", SETTLE_TICKS_HEAVY, c -> {
            hideAllOverlays();
            c.setScreen(new ServerPasswordScreen(c.currentScreen));
        }));
        out.add(new CaptureTarget("screen-nbt-edit", SETTLE_TICKS_HEAVY, c -> {
            hideAllOverlays();
            equipSampleItem(c);
            if (c.player != null) {
                c.setScreen(new NbtEditScreen(c.currentScreen, c.player.getMainHandStack().copy()));
            }
        }));
        out.add(new CaptureTarget("screen-server-search", SETTLE_TICKS_HEAVY, c -> {
            hideAllOverlays();
            c.setScreen(new ServerScannerScreen(c.currentScreen, new ApiClient(new AddonAuth())));
        }));
        out.add(new CaptureTarget("screen-server-search-auth", c -> {
            hideAllOverlays();
            c.setScreen(new ServerSearchAuthScreen(c.currentScreen));
        }));
        return out;
    }
}
