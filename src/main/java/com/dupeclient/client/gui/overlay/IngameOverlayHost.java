package com.dupeclient.client.gui.overlay;

import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import com.dupeclient.client.gui.overlay.OverlayMouse;
import com.dupeclient.client.module.acaudit.AcAuditOverlay;
import com.dupeclient.client.module.dupedb.DupedbOverlay;
import com.dupeclient.client.module.fuzzer.FuzzerOverlay;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferOverlay;
import com.dupeclient.client.module.payall.PayAllOverlay;
import com.dupeclient.client.module.mcptools.McpToolsOverlay;
import com.dupeclient.client.module.utility.ChatGamesOverlay;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class IngameOverlayHost {
    @Nullable
    private static IngameModuleOverlay handledScreenMouseCapture;
    private static final List<IngameModuleOverlay> OVERLAYS;

    private IngameOverlayHost() {
    }

    public static List<IngameModuleOverlay> all() {
        return OVERLAYS;
    }

    public static boolean hasAnyActive() {
        return IngameOverlayHost.topActive() != null;
    }

    public static boolean needsBlockingOverlayScreen() {
        return IngameOverlayHost.topBlockingActive() != null;
    }

    public static IngameModuleOverlay topActive() {
        IngameModuleOverlay best = null;
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || best != null && overlay.priority() <= best.priority()) continue;
            best = overlay;
        }
        return best;
    }

    @Nullable
    public static IngameModuleOverlay topBlockingActive() {
        IngameModuleOverlay best = null;
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.blocksGameInput() || best != null && overlay.priority() <= best.priority()) continue;
            best = overlay;
        }
        return best;
    }

    public static void hideAllOverlays() {
        IngameOverlayHost.hideAllExcept(null);
    }

    public static void hideAllExcept(@Nullable IngameModuleOverlay keep) {
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (overlay == keep || !overlay.isOverlayVisible()) continue;
            overlay.setOverlayVisible(false);
        }
        if (keep != PacketFabricatorOverlay.INSTANCE) {
            IngameOverlayHost.hideFabricatorOverlay();
        }
        handledScreenMouseCapture = null;
    }

    /** Hides every module overlay when leaving a play session; toggles can show them again after rejoin. */
    public static void onPlaySessionLeave() {
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isOverlayVisible()) continue;
            overlay.setOverlayVisible(false);
        }
        IngameOverlayHost.hideFabricatorOverlay();
        handledScreenMouseCapture = null;
    }

    public static void hideFabricatorOverlay() {
        if (PacketFabricatorOverlay.INSTANCE.isVisible()) {
            PacketFabricatorOverlay.INSTANCE.setVisible(false);
        }
    }

    public static void onModuleOverlayOpening(IngameModuleOverlay opening) {
        IngameOverlayHost.hideAllExcept(opening);
    }

    public static void onScreenChanged(@Nullable Screen screen) {
        handledScreenMouseCapture = null;
        if (isConnectionOrLoadingScreen(screen)) {
            IngameOverlayHost.hideAllOverlays();
        }
    }

    public static boolean isConnectionOrLoadingScreen(@Nullable Screen screen) {
        return screen instanceof ConnectScreen
                || screen instanceof LevelLoadingScreen
                || screen instanceof ProgressScreen;
    }

    public static void renderAll(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        IngameOverlayHost.renderClickslotFabricator(context, mouseX, mouseY, delta);
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (overlay == PacketFabricatorOverlay.INSTANCE || !overlay.isActive()) continue;
            overlay.render(context, mouseX, mouseY, delta);
        }
    }

    public static void renderClickslotFabricator(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        PacketFabricatorOverlay fabricator = PacketFabricatorOverlay.INSTANCE;
        if (!fabricator.isModuleEnabled() || !fabricator.isVisible()) {
            return;
        }
        fabricator.render(context, mouseX, mouseY, delta);
    }

    public static boolean isClickslotFabricatorActive() {
        PacketFabricatorOverlay fabricator = PacketFabricatorOverlay.INSTANCE;
        return fabricator.isModuleEnabled() && fabricator.isVisible();
    }

    public static void renderOnHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (!IngameOverlayHost.shouldRenderOnHud(client)) {
            return;
        }
        int mouseX = (int)OverlayMouse.scaledX(client);
        int mouseY = (int)OverlayMouse.scaledY(client);
        float delta = tickCounter.getGameTimeDeltaPartialTick(false);
        IngameOverlayHost.renderAll(context, mouseX, mouseY, delta);
    }

    public static boolean shouldRenderOnHud(Minecraft client) {
        if (client == null || client.player == null || client.screen != null) {
            return false;
        }
        return IngameOverlayHost.hasAnyActive() || IngameOverlayHost.isClickslotFabricatorActive();
    }

    public static boolean shouldRouteHudMouse(Minecraft client) {
        return IngameOverlayHost.shouldRouteOverlayMouse(client);
    }

    public static boolean shouldRouteOverlayMouse(Minecraft client) {
        if (client == null || client.screen != null) {
            return false;
        }
        return IngameOverlayHost.hasAnyActive() || IngameOverlayHost.isClickslotFabricatorActive();
    }

    public static boolean anyActiveDragging() {
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.isDragging()) continue;
            return true;
        }
        return false;
    }

    @Nullable
    public static IngameModuleOverlay topHudActive() {
        return IngameOverlayHost.topActive();
    }

    public static boolean anyHasTextFocus() {
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.hasTextFocus()) continue;
            return true;
        }
        return false;
    }

    @Nullable
    public static IngameModuleOverlay overlayAtPointer(double mouseX, double mouseY) {
        return OVERLAYS.stream().filter(IngameModuleOverlay::isActive).filter(o -> o.containsPoint(mouseX, mouseY)).max(Comparator.comparingInt(IngameModuleOverlay::priority)).orElse(null);
    }

    public static boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (!IngameOverlayHost.hasAnyActive()) {
            return false;
        }
        IngameModuleOverlay underPointer = IngameOverlayHost.overlayAtPointer(mouseX, mouseY);
        if (underPointer != null) {
            return underPointer.mouseClicked(mouseX, mouseY, button);
        }
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.mouseClicked(mouseX, mouseY, button)) continue;
            return true;
        }
        return false;
    }

    public static boolean onHandledScreenMouseClicked(double mouseX, double mouseY, int button) {
        IngameModuleOverlay underPointer = IngameOverlayHost.overlayAtPointer(mouseX, mouseY);
        if (underPointer == null) {
            handledScreenMouseCapture = null;
            return false;
        }
        if (underPointer.mouseClicked(mouseX, mouseY, button)) {
            handledScreenMouseCapture = underPointer;
            return true;
        }
        return false;
    }

    public static boolean onScreenOverlayMouseClicked(double mouseX, double mouseY, int button) {
        return IngameOverlayHost.onHandledScreenMouseClicked(mouseX, mouseY, button);
    }

    public static boolean onHandledScreenMouseDragged(double mouseX, double mouseY, int button) {
        if (handledScreenMouseCapture != null && handledScreenMouseCapture.isActive() && handledScreenMouseCapture.isDragging()) {
            return handledScreenMouseCapture.mouseDragged(mouseX, mouseY, button);
        }
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.isDragging() || !overlay.mouseDragged(mouseX, mouseY, button)) continue;
            handledScreenMouseCapture = overlay;
            return true;
        }
        return false;
    }

    public static boolean onScreenOverlayMouseDragged(double mouseX, double mouseY, int button) {
        return IngameOverlayHost.onHandledScreenMouseDragged(mouseX, mouseY, button);
    }

    public static boolean onHandledScreenMouseReleased(double mouseX, double mouseY, int button) {
        if (handledScreenMouseCapture != null && handledScreenMouseCapture.isActive()) {
            boolean onPanel = handledScreenMouseCapture.containsPoint(mouseX, mouseY);
            if (handledScreenMouseCapture.isDragging() || onPanel) {
                handledScreenMouseCapture.mouseReleased(mouseX, mouseY, button);
                if (!handledScreenMouseCapture.isDragging()) {
                    handledScreenMouseCapture = null;
                }
                return true;
            }
            handledScreenMouseCapture = null;
        }
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.isDragging() || !overlay.mouseReleased(mouseX, mouseY, button)) continue;
            handledScreenMouseCapture = !overlay.isDragging() ? null : overlay;
            return true;
        }
        return false;
    }

    public static boolean onScreenOverlayMouseReleased(double mouseX, double mouseY, int button) {
        return IngameOverlayHost.onHandledScreenMouseReleased(mouseX, mouseY, button);
    }

    public static boolean onScreenOverlayMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return IngameOverlayHost.onHandledScreenMouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    public static boolean onHudMouseClicked(double mouseX, double mouseY, int button) {
        return IngameOverlayHost.onHandledScreenMouseClicked(mouseX, mouseY, button);
    }

    public static boolean onHudMouseReleased(double mouseX, double mouseY, int button) {
        return IngameOverlayHost.onHandledScreenMouseReleased(mouseX, mouseY, button);
    }

    public static boolean onHudMouseDragged(double mouseX, double mouseY, int button) {
        return IngameOverlayHost.onHandledScreenMouseDragged(mouseX, mouseY, button);
    }

    public static boolean onHudMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return IngameOverlayHost.onHandledScreenMouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    public static boolean onHandledScreenMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        IngameModuleOverlay underPointer = IngameOverlayHost.overlayAtPointer(mouseX, mouseY);
        if (underPointer != null) {
            return underPointer.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }
        return false;
    }

    public static boolean onMouseReleased(double mouseX, double mouseY, int button) {
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.mouseReleased(mouseX, mouseY, button)) continue;
            return true;
        }
        return false;
    }

    public static boolean onMouseDragged(double mouseX, double mouseY, int button) {
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.mouseDragged(mouseX, mouseY, button)) continue;
            return true;
        }
        return false;
    }

    public static boolean onMouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        IngameModuleOverlay underPointer = IngameOverlayHost.overlayAtPointer(mouseX, mouseY);
        if (underPointer != null) {
            return underPointer.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.mouseScrolled(mouseX, mouseY, horizontal, vertical)) continue;
            return true;
        }
        return false;
    }

    public static boolean onFocusedOverlayKeyPressed(int keyCode) {
        double my;
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.hasTextFocus() || !overlay.keyPressed(keyCode)) continue;
            return true;
        }
        IngameModuleOverlay target = IngameOverlayHost.topActive();
        if (target == null || !target.isActive()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        double mx = OverlayMouse.scaledX(client);
        if (target.containsPoint(mx, my = OverlayMouse.scaledY(client))) {
            return target.keyPressed(keyCode);
        }
        return false;
    }

    public static boolean onKeyPressed(int keyCode) {
        return IngameOverlayHost.onFocusedOverlayKeyPressed(keyCode);
    }

    public static boolean onFocusedOverlayCharTyped(int codePoint) {
        for (IngameModuleOverlay overlay : OVERLAYS) {
            if (!overlay.isActive() || !overlay.hasTextFocus() || !overlay.charTyped(codePoint)) continue;
            return true;
        }
        IngameModuleOverlay target = IngameOverlayHost.topActive();
        if (target == null) {
            return false;
        }
        return target.charTyped(codePoint);
    }

    public static boolean onCharTyped(int codePoint) {
        return IngameOverlayHost.onFocusedOverlayCharTyped(codePoint);
    }

    public static boolean shouldBlockGameInput(Minecraft client) {
        double my;
        if (client == null || client.screen != null) {
            return false;
        }
        IngameModuleOverlay top = IngameOverlayHost.topBlockingActive();
        if (top == null) {
            return false;
        }
        double mx = OverlayMouse.scaledX(client);
        return top.containsPoint(mx, my = OverlayMouse.scaledY(client)) || top.hasTextFocus();
    }

    static {
        OVERLAYS = List.of(FuzzerOverlay.INSTANCE, PayAllOverlay.INSTANCE, McpToolsOverlay.INSTANCE, ChatGamesOverlay.INSTANCE, DupedbOverlay.INSTANCE, AcAuditOverlay.INSTANCE, PacketFabricatorOverlay.INSTANCE, PacketSnifferOverlay.INSTANCE);
    }
}

