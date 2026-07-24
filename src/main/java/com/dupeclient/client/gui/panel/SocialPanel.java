package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.IngameUiRouter;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.panel.Panel;
import com.dupeclient.client.core.session.SocialHubRules;
import com.dupeclient.client.module.cape.DupeClientPresenceConfigManager;
import com.dupeclient.client.module.cape.DupeClientPresenceSettings;
import java.util.Locale;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class SocialPanel
extends Panel {
    private static final int BTN_H = 22;
    private CaptureMode captureMode = CaptureMode.NONE;

    public SocialPanel(int x, int y) {
        super("social", Text.literal("Social"), x, y, 268, 200);
    }

    private static int contentHeight() {
        return 236;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!SocialHubRules.socialUiAllowed()) {
            super.render(context, mouseX, mouseY, delta);
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int cx = this.x + this.width / 2;
            int cy = this.y + this.height / 2 - 14;
            context.drawCenteredTextWithShadow(tr, Text.literal("Social features are unavailable."), cx, cy, -7363656);
            context.drawCenteredTextWithShadow(tr, Text.literal(SocialHubRules.blockReason()), cx, cy + 12, -10193781);
            return;
        }
        super.render(context, mouseX, mouseY, delta);
        if (this.collapsed) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
        int tx = this.x + 16;
        int ty = this.y + this.bodyTopOffset() + 8;
        int sw = this.width - 32;
        int inner = sw - 16;
        int rx = tx + 8;
        int sectionH = SocialPanel.contentHeight();
        UiComponents.drawInfoCard(tr, context, tx, ty, sw, sectionH, null);
        int lineY = ty + 16;
        UiComponents.drawOptionToggle(tr, context, rx, lineY, inner, "Presence API", Boolean.TRUE.equals(s.enabled), this.smoothToggle("social.presenceApi", Boolean.TRUE.equals(s.enabled), delta));
        UiComponents.drawOptionToggle(tr, context, rx, lineY += 30, inner, "Broadcast me (others see my cape)", Boolean.TRUE.equals(s.broadcastPresence), this.smoothToggle("social.broadcast", Boolean.TRUE.equals(s.broadcastPresence), delta));
        UiComponents.drawOptionToggle(tr, context, rx, lineY += 30, inner, "Share current server in heartbeat", Boolean.TRUE.equals(s.shareCurrentServer), this.smoothToggle("social.shareServer", Boolean.TRUE.equals(s.shareCurrentServer), delta));
        UiComponents.drawOptionToggle(tr, context, rx, lineY += 30, inner, "Show server column in Social list", Boolean.TRUE.equals(s.showServersInSocial), this.smoothToggle("social.showServerCol", Boolean.TRUE.equals(s.showServersInSocial), delta));
        UiComponents.drawPillActionButton(tr, context, rx, lineY += 30, inner, 22, "Open Social list", UiComponents.PillActionStyle.PRIMARY_BLUE);
        boolean listen = this.captureMode == CaptureMode.OPEN_SOCIAL_KEY;
        String keyText = listen ? "Press key..." : this.keyName(s.openSocialKey);
        UiComponents.drawPillKeybind(tr, context, rx, lineY += 30, inner, 20, "Open Social hotkey", keyText, listen);
        this.height = this.bodyTopOffset() + 8 + sectionH + 12;
        if (this.captureMode != CaptureMode.NONE) {
            context.drawTextWithShadow(tr, Text.literal("Press key for Social screen (ESC = unbind)"), rx, this.y + this.height - 11, -14217);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int sw;
        int inner;
        if (!SocialHubRules.socialUiAllowed()) {
            return false;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.collapsed || button != 0) {
            return false;
        }
        DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
        int tx = this.x + 16;
        int rx = tx + 8;
        int ty = this.y + this.bodyTopOffset() + 8;
        int lineY = ty + 16;
        if (SocialPanel.rect(mouseX, mouseY, rx, lineY, inner = (sw = this.width - 32) - 16, 30)) {
            s.enabled = !Boolean.TRUE.equals(s.enabled);
            DupeClientPresenceConfigManager.save(s);
            return true;
        }
        if (SocialPanel.rect(mouseX, mouseY, rx, lineY += 30, inner, 30)) {
            s.broadcastPresence = !Boolean.TRUE.equals(s.broadcastPresence);
            DupeClientPresenceConfigManager.save(s);
            return true;
        }
        if (SocialPanel.rect(mouseX, mouseY, rx, lineY += 30, inner, 30)) {
            s.shareCurrentServer = !Boolean.TRUE.equals(s.shareCurrentServer);
            DupeClientPresenceConfigManager.save(s);
            return true;
        }
        if (SocialPanel.rect(mouseX, mouseY, rx, lineY += 30, inner, 30)) {
            s.showServersInSocial = !Boolean.TRUE.equals(s.showServersInSocial);
            DupeClientPresenceConfigManager.save(s);
            return true;
        }
        if (SocialPanel.rect(mouseX, mouseY, rx, lineY += 30, inner, 22)) {
            IngameUiRouter.openSocial(MinecraftClient.getInstance().currentScreen);
            return true;
        }
        if (this.clickBindValue(mouseX, mouseY, rx, lineY += 30, inner)) {
            this.captureMode = CaptureMode.OPEN_SOCIAL_KEY;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key;
        if (this.captureMode == CaptureMode.NONE) {
            return false;
        }
        int n = key = keyCode == 256 ? -1 : keyCode;
        if (this.captureMode == CaptureMode.OPEN_SOCIAL_KEY) {
            DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
            s.openSocialKey = key == -1 ? -1 : key;
            DupeClientPresenceConfigManager.save(s);
            this.captureMode = CaptureMode.NONE;
            return true;
        }
        return false;
    }

    @Override
    public void onModuleHidden() {
        this.captureMode = CaptureMode.NONE;
    }

    @Override
    public boolean hasFocusedTextInput() {
        return this.isVisible() && this.captureMode != CaptureMode.NONE;
    }

    private boolean clickBindValue(double mouseX, double mouseY, int x, int y, int w) {
        int bindW = 98;
        int labelW = w - bindW - 8;
        int bx = x + labelW + 8;
        return SocialPanel.rect(mouseX, mouseY, bx, y, bindW, 20);
    }

    private String keyName(int keyCode) {
        if (keyCode < 0 || keyCode == -1) {
            return "UNBOUND";
        }
        String glfw = GLFW.glfwGetKeyName((int)keyCode, (int)0);
        if (glfw != null) {
            return glfw.toUpperCase(Locale.ROOT);
        }
        return "KEY_" + keyCode;
    }

    private static boolean rect(double mx, double my, int sx, int sy, int sw, int sh) {
        return mx >= (double)sx && mx <= (double)(sx + sw) && my >= (double)sy && my <= (double)(sy + sh);
    }

    private static enum CaptureMode {
        NONE,
        OPEN_SOCIAL_KEY;

    }
}

