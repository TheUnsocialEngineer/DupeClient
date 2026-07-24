package com.dupeclient.client.gui.panel;

import com.dupeclient.client.core.session.SocialHubRules;
import com.dupeclient.client.module.cape.DupeClientPresenceConfigManager;
import com.dupeclient.client.module.cape.DupeClientPresenceSettings;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.IngameUiRouter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class WaypointsPanel extends Panel {
    private static final int BTN_H = 22;

    private CaptureMode captureMode = CaptureMode.NONE;

    public WaypointsPanel(int x, int y) {
        super("waypoints", Text.literal("Waypoints"), x, y, 268, 160);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!SocialHubRules.socialUiAllowed()) {
            super.render(context, mouseX, mouseY, delta);
            var tr = MinecraftClient.getInstance().textRenderer;
            int cx = x + width / 2;
            int cy = y + height / 2 - 14;
            context.drawCenteredTextWithShadow(tr, Text.literal("Waypoints require social access."), cx, cy, 0xFF8FA3B8);
            context.drawCenteredTextWithShadow(tr, Text.literal(SocialHubRules.blockReason()), cx, cy + 12, 0xFF64748B);
            return;
        }
        super.render(context, mouseX, mouseY, delta);
        if (collapsed) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        var tr = mc.textRenderer;
        DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
        int tx = x + UiTokens.BODY_INSET;
        int ty = y + bodyTopOffset() + UiTokens.UI_GAP;
        int sw = width - UiTokens.BODY_INSET * 2;
        int inner = sw - UiTokens.SP_4;
        int rx = tx + UiTokens.SP_2;
        int sectionH = UiTokens.CARD_BODY_TOP + UiTokens.ROW_STEP * 3 + UiTokens.SP_3 + BTN_H + UiTokens.ROW_STEP + 20 + UiTokens.SP_4;

        UiComponents.drawInfoCard(tr, context, tx, ty, sw, sectionH, null);
        int lineY = ty + UiTokens.CARD_BODY_TOP;

        UiComponents.drawOptionToggle(tr, context, rx, lineY, inner, "Share waypoints", Boolean.TRUE.equals(s.shareWaypoints),
            smoothToggle("waypoints.share", Boolean.TRUE.equals(s.shareWaypoints), delta));
        lineY += UiTokens.ROW_STEP;
        UiComponents.drawOptionToggle(tr, context, rx, lineY, inner, "Show waypoints in world", Boolean.TRUE.equals(s.showSharedWaypointsInWorld),
            smoothToggle("waypoints.showWorld", Boolean.TRUE.equals(s.showSharedWaypointsInWorld), delta));
        lineY += UiTokens.ROW_STEP;
        UiComponents.drawOptionToggle(tr, context, rx, lineY, inner, "Shared list: friends only", Boolean.TRUE.equals(s.waypointsFriendsOnlyView),
            smoothToggle("waypoints.friendsView", Boolean.TRUE.equals(s.waypointsFriendsOnlyView), delta));
        lineY += UiTokens.ROW_STEP;

        UiComponents.drawPillActionButton(tr, context, rx, lineY, inner, BTN_H, "Manage waypoints", UiComponents.PillActionStyle.PRIMARY_BLUE);
        lineY += BTN_H + UiTokens.SP_2;

        boolean listen = captureMode == CaptureMode.OPEN_WAYPOINTS_KEY;
        String keyText = listen ? "Press key..." : keyName(s.openWaypointsKey);
        UiComponents.drawPillKeybind(tr, context, rx, lineY, inner, 20, "Open Waypoints hotkey", keyText, listen);

        height = bodyTopOffset() + UiTokens.UI_GAP + sectionH + UiTokens.SP_3;
        if (captureMode != CaptureMode.NONE) {
            context.drawTextWithShadow(tr, Text.literal("Press key for Waypoints (ESC = unbind)"), rx, y + height - 11, 0xFFFFC877);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!SocialHubRules.socialUiAllowed()) {
            return false;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (collapsed || button != 0) {
            return false;
        }
        DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
        int tx = x + UiTokens.BODY_INSET;
        int ty = y + bodyTopOffset() + UiTokens.UI_GAP;
        int sw = width - UiTokens.BODY_INSET * 2;
        int inner = sw - UiTokens.SP_4;
        int rx = tx + UiTokens.SP_2;
        int lineY = ty + UiTokens.CARD_BODY_TOP;

        if (rect(mouseX, mouseY, rx, lineY, inner, UiTokens.ROW_STEP)) {
            s.shareWaypoints = !Boolean.TRUE.equals(s.shareWaypoints);
            DupeClientPresenceConfigManager.save(s);
            com.dupeclient.client.module.waypoint.DupeClientWaypointManager.INSTANCE.markSyncDirty();
            return true;
        }
        lineY += UiTokens.ROW_STEP;
        if (rect(mouseX, mouseY, rx, lineY, inner, UiTokens.ROW_STEP)) {
            s.showSharedWaypointsInWorld = !Boolean.TRUE.equals(s.showSharedWaypointsInWorld);
            DupeClientPresenceConfigManager.save(s);
            return true;
        }
        lineY += UiTokens.ROW_STEP;
        if (rect(mouseX, mouseY, rx, lineY, inner, UiTokens.ROW_STEP)) {
            s.waypointsFriendsOnlyView = !Boolean.TRUE.equals(s.waypointsFriendsOnlyView);
            DupeClientPresenceConfigManager.save(s);
            return true;
        }
        lineY += UiTokens.ROW_STEP;
        if (rect(mouseX, mouseY, rx, lineY, inner, BTN_H)) {
            IngameUiRouter.openWaypoints(MinecraftClient.getInstance().currentScreen);
            return true;
        }
        lineY += BTN_H + UiTokens.SP_2;
        if (rect(mouseX, mouseY, rx, lineY, inner, 20)) {
            captureMode = CaptureMode.OPEN_WAYPOINTS_KEY;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (captureMode == CaptureMode.NONE) {
            return false;
        }
        int key = keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
        if (captureMode == CaptureMode.OPEN_WAYPOINTS_KEY) {
            DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
            s.openWaypointsKey = key == GLFW.GLFW_KEY_UNKNOWN ? -1 : key;
            DupeClientPresenceConfigManager.save(s);
            captureMode = CaptureMode.NONE;
            return true;
        }
        return false;
    }

    @Override
    public void onModuleHidden() {
        captureMode = CaptureMode.NONE;
    }

    @Override
    public boolean hasFocusedTextInput() {
        return isVisible() && captureMode != CaptureMode.NONE;
    }

    private String keyName(int keyCode) {
        if (keyCode < 0 || keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return "UNBOUND";
        }
        String glfw = GLFW.glfwGetKeyName(keyCode, 0);
        if (glfw != null) {
            return glfw.toUpperCase(Locale.ROOT);
        }
        return "KEY_" + keyCode;
    }

    private static boolean rect(double mx, double my, int sx, int sy, int sw, int sh) {
        return mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh;
    }

    private enum CaptureMode {
        NONE,
        OPEN_WAYPOINTS_KEY
    }
}
