package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.GitHubRepoCard;
import com.dupeclient.client.gui.widget.InlineTextField;
import com.dupeclient.client.core.session.HubModuleRules;
import com.dupeclient.client.core.session.PresenceRosterSync;
import com.dupeclient.client.module.serverpassword.ServerPasswordScreen;
import com.dupeclient.client.module.security.SecurityManager;
import com.dupeclient.client.module.security.SecurityProfileStore;
import com.dupeclient.client.module.security.SecurityStaffTimeline;
import com.dupeclient.client.module.security.SecuritySettings;
import com.dupeclient.client.module.security.nochatrestrictions.NoChatRestrictionsRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class SecurityPanel extends Panel {
    private static final int ROW = UiTokens.ROW_STEP;
    private static final int BIND_ROW_H = 18;
    private static final int BIND_W = 96;
    private static final int MAX_OPSEC_WHITELIST_LEN = 160;
    private static final int MAX_NAME_CHANGER_LEN = 32;
    private static final int STAFF_RADIUS_LABEL_OFFSET = 120;
    private static final int STAFF_RADIUS_VALUE_W = 40;

    private static final int OPSEC_GITHUB_TOP = 4;
    private static final int OPSEC_BODY_TOP = OPSEC_GITHUB_TOP + GitHubRepoCard.height() + 6;

    private final SecurityManager manager = SecurityManager.INSTANCE;
    private final InlineTextField opsecWhitelistField = new InlineTextField(MAX_OPSEC_WHITELIST_LEN);
    private final InlineTextField nameChangerField = new InlineTextField(MAX_NAME_CHANGER_LEN);
    private boolean draggingStaffRadius;
    private int staffRadiusRowY = -1;
    private int vaultRowY = -1;
    private int saveProfileRowY = -1;
    private int opsecGithubX;
    private int opsecGithubY;

    public SecurityPanel(int x, int y) {
        super("security", Text.literal("Security"), x, y, 320, 420);
        this.opsecWhitelistField.setPlaceholder("voicechat,xaero,minimap");
        this.nameChangerField.setPlaceholder("Display name");
    }

    /** Matches {@link #render} row layout; card must fit staff radius + last two toggles. */
    private static int contentHeight(SecuritySettings s) {
        int opsecCardH = opsecCardHeight(s);
        int lastRowY = 16 + 12 + ROW * 6;
        lastRowY += ROW + 4 + 12 + ROW;
        lastRowY += ROW + opsecCardH + 8;
        lastRowY += ROW;
        lastRowY += ROW + 4 + 12 + ROW * 6;
        lastRowY += ROW + 4 + 12 + ROW * 5 + 22;
        lastRowY += ROW + 4 + 12 + ROW * 3 + 28;
        return lastRowY + ROW + 12;
    }

    private static int opsecCardHeight(SecuritySettings s) {
        int base = s.opsecWhitelistMode == SecuritySettings.OpsecWhitelistMode.CUSTOM ? 154 : 120;
        return base - 14 + OPSEC_BODY_TOP;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.collapsed) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        SecuritySettings s = this.manager.getSettings();
        int tx = this.x + 16;
        int ty = this.y + this.bodyTopOffset() + 8;
        int sw = this.width - 32;
        int rx = tx + 8;
        int rowInner = sw - 16;
        int nestX = tx + 16;
        int nestInner = sw - 32;
        int sectionH = SecurityPanel.contentHeight(s);

        UiComponents.drawInfoCard(tr, context, tx, ty, sw, sectionH, null);
        int row = ty + 16;

        if (HubModuleRules.viewerRestricted() || PresenceRosterSync.isRosterPending()) {
            int color = HubModuleRules.viewerRestricted() ? 0xFFFF9A6A : UiTokens.ACCENT;
            context.drawTextWithShadow(tr, Text.literal("Staff roster: " + PresenceRosterSync.statusLine()), rx, row, color);
            context.drawTextWithShadow(tr, Text.literal(HubModuleRules.blockReason()), rx, row + 11, UiTokens.TEXT_DIM);
            row += 24;
        }

        UiComponents.drawAccentLabel(tr, context, rx, row, "Chat & multiplayer");
        UiComponents.drawOptionToggle(tr, context, rx, row += 12, rowInner, "No chat restrictions", s.noChatRestrictions, this.smoothToggle("sec.noChat", s.noChatRestrictions, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Chat alerts", s.moduleChatFeedback, this.smoothToggle("sec.chatAlerts", s.moduleChatFeedback, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Toasts", s.showToasts, this.smoothToggle("sec.toasts", s.showToasts, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Log detections", s.logDetections, this.smoothToggle("sec.logDetections", s.logDetections, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Telemetry blocking", s.telemetryBlocking, this.smoothToggle("sec.telemetry", s.telemetryBlocking, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Block local pack URLs", s.blockLocalPackUrls, this.smoothToggle("sec.blockPack", s.blockLocalPackUrls, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Key probe alerts", s.keyProbeAlerts, this.smoothToggle("sec.keyProbe", s.keyProbeAlerts, delta));

        UiComponents.drawAccentLabel(tr, context, rx, row += ROW + 4, "Key resolution");
        UiComponents.drawOptionToggle(tr, context, rx, row += 12, rowInner, "Protect on remote servers", s.keyResolutionProtection, this.smoothToggle("sec.keyRes", s.keyResolutionProtection, delta));

        int opsecCardH = opsecCardHeight(s);
        int opsecCardX = nestX - 8;
        int opsecCardY = row += ROW;
        UiComponents.drawSurfaceCard(context, opsecCardX, opsecCardY, nestInner + 16, opsecCardH);
        this.opsecGithubX = opsecCardX + nestInner + 16 - GitHubRepoCard.width() - 6;
        this.opsecGithubY = opsecCardY + OPSEC_GITHUB_TOP;
        GitHubRepoCard.OPSEC.render(context, tr, this.opsecGithubX, this.opsecGithubY, mouseX, mouseY);
        int opsecRow = opsecCardY + OPSEC_BODY_TOP;
        UiComponents.drawAccentLabel(tr, context, nestX, opsecRow, "OpSec spoofing");
        UiComponents.drawOptionToggle(tr, context, nestX, opsecRow += 14, nestInner, "Fake default keybinds", s.opsecFakeDefaultKeybinds, this.smoothToggle("sec.fakeKb", s.opsecFakeDefaultKeybinds, delta));
        UiComponents.drawPillKeybindEx(tr, context, nestX, opsecRow += ROW, nestInner, BIND_ROW_H, "Brand mode", s.opsecBrandMode.name(), false, BIND_W);
        UiComponents.drawPillKeybindEx(tr, context, nestX, opsecRow += 22, nestInner, BIND_ROW_H, "Whitelist mode", s.opsecWhitelistMode.name(), false, BIND_W);
        if (s.opsecWhitelistMode == SecuritySettings.OpsecWhitelistMode.CUSTOM) {
            UiComponents.drawAccentLabel(tr, context, nestX, opsecRow += 22, "Whitelist mods (comma-separated)");
            if (!this.opsecWhitelistField.isFocused()) {
                this.opsecWhitelistField.setText(s.opsecWhitelistedModsCsv == null ? "" : s.opsecWhitelistedModsCsv);
            }
            this.opsecWhitelistField.setBounds(nestX, opsecRow += 12, nestInner, 20);
            UiComponents.drawInlineTextField(tr, context, this.opsecWhitelistField);
            opsecRow += 22;
        }
        row = opsecCardY + opsecCardH + 8;
        UiComponents.drawOptionToggle(tr, context, rx, row, rowInner, "Spoof server-marked text only", s.keyResolutionServerMarkedOnly, this.smoothToggle("sec.keySpoof", s.keyResolutionServerMarkedOnly, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Block sign open (key-probe)", s.keyResolutionBlockSignEditorOnKeyProbe, this.smoothToggle("sec.blockSign", s.keyResolutionBlockSignEditorOnKeyProbe, delta));

        UiComponents.drawAccentLabel(tr, context, rx, row += ROW + 4, "Staff detection");
        UiComponents.drawOptionToggle(tr, context, rx, row += 12, rowInner, "Staff detection", s.staffDetectionEnabled, this.smoothToggle("sec.staffDet", s.staffDetectionEnabled, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Staff detected alerts", s.staffDetectedAlerts, this.smoothToggle("sec.staffAlert", s.staffDetectedAlerts, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Staff online/offline alerts", s.staffOnlineOfflineAlerts, this.smoothToggle("sec.staffOnline", s.staffOnlineOfflineAlerts, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Staff nearby alerts", s.staffProximityAlerts, this.smoothToggle("sec.staffNear", s.staffProximityAlerts, delta));
        this.staffRadiusRowY = row += ROW;
        UiComponents.drawLabeledValueSlider(tr, context, rx, this.staffRadiusRowY, rowInner, s.staffProximityRadius, 8.0, 256.0, "Staff radius (m)", STAFF_RADIUS_LABEL_OFFSET, STAFF_RADIUS_VALUE_W, this.draggingStaffRadius, s.staffProximityRadius + "m");
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Staff glow", s.staffGlowEnabled, this.smoothToggle("sec.staffGlow", s.staffGlowEnabled, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Anti-invisible entities", s.antiInvisibleEntities, this.smoothToggle("sec.antiInvis", s.antiInvisibleEntities, delta));

        UiComponents.drawAccentLabel(tr, context, rx, row += ROW + 4, "Display privacy");
        UiComponents.drawOptionToggle(tr, context, rx, row += 12, rowInner, "Name changer", s.nameChangerEnabled, this.smoothToggle("sec.nameChanger", s.nameChangerEnabled, delta));
        UiComponents.drawAccentLabel(tr, context, rx, row += ROW, "Display name");
        if (!this.nameChangerField.isFocused()) {
            this.nameChangerField.setText(s.nameChangerDisplayName == null ? "" : s.nameChangerDisplayName);
        }
        this.nameChangerField.setBounds(rx, row += 12, rowInner, 20);
        UiComponents.drawInlineTextField(tr, context, this.nameChangerField);
        UiComponents.drawOptionToggle(tr, context, rx, row += 22, rowInner, "Censor real name", s.nameChangerCensor, this.smoothToggle("sec.nameCensor", s.nameChangerCensor, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "Only in-game", s.nameChangerOnlyInGame, this.smoothToggle("sec.nameInGame", s.nameChangerOnlyInGame, delta));
        UiComponents.drawOptionToggle(tr, context, rx, row += ROW, rowInner, "No texture rotations", s.noTextureRotations, this.smoothToggle("sec.noTexRot", s.noTextureRotations, delta));

        UiComponents.drawAccentLabel(tr, context, rx, row += ROW + 4, "Vault & staff log");
        this.vaultRowY = row += 12;
        UiComponents.drawPillActionButton(tr, context, rx, this.vaultRowY, rowInner, 18, "Open password vault", UiComponents.PillActionStyle.SECONDARY_SLATE);
        int timelineY = this.vaultRowY + 22;
        int shown = 0;
        for (SecurityStaffTimeline.Entry entry : SecurityStaffTimeline.snapshot()) {
            if (shown >= 3) {
                break;
            }
            context.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(entry.line(), rowInner)), rx, timelineY + shown * 10, UiTokens.TEXT_DIM);
            shown++;
        }
        row = timelineY + Math.max(1, shown) * 10 + 4;
        UiComponents.drawOptionToggle(tr, context, rx, row, rowInner, "Auto OpSec profile per server", s.profileAutoSwitchPerServer, this.smoothToggle("sec.profileAuto", s.profileAutoSwitchPerServer, delta));
        this.saveProfileRowY = row += ROW;
        UiComponents.drawPillActionButton(tr, context, rx, this.saveProfileRowY, rowInner, 18, "Save OpSec profile for server", UiComponents.PillActionStyle.PRIMARY_MINT);

        this.height = this.bodyTopOffset() + 8 + sectionH + 12;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.collapsed || button != 0) {
            return false;
        }
        if (GitHubRepoCard.OPSEC.mouseClicked(mouseX, mouseY, this.opsecGithubX, this.opsecGithubY, button)) {
            return true;
        }
        SecuritySettings s = this.manager.getSettings();
        if (this.opsecWhitelistField.isFocused() && !this.opsecWhitelistField.contains(mouseX, mouseY)) {
            this.syncOpsecWhitelistFromField();
            this.opsecWhitelistField.blur();
        }
        if (this.nameChangerField.isFocused() && !this.nameChangerField.contains(mouseX, mouseY)) {
            this.syncNameChangerFromField();
            this.nameChangerField.blur();
        }
        int tx = this.x + 16;
        int ty = this.y + this.bodyTopOffset() + 8;
        int sw = this.width - 32;
        int rx = tx + 8;
        int rowInner = sw - 16;
        int nestX = tx + 16;
        int nestInner = sw - 32;
        int row = ty + 16 + 12;

        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row, rowInner)) {
            boolean next = !s.noChatRestrictions;
            s.noChatRestrictions = next;
            this.manager.save();
            NoChatRestrictionsRuntime.sync(next);
            this.manager.feedback(next ? "No chat restrictions enabled." : "No chat restrictions disabled.");
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.moduleChatFeedback = !s.moduleChatFeedback;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.showToasts = !s.showToasts;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.logDetections = !s.logDetections;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.telemetryBlocking = !s.telemetryBlocking;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.blockLocalPackUrls = !s.blockLocalPackUrls;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.keyProbeAlerts = !s.keyProbeAlerts;
            this.manager.save();
            return true;
        }
        row += ROW + 4 + 12;
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row, rowInner)) {
            s.keyResolutionProtection = !s.keyResolutionProtection;
            this.manager.save();
            return true;
        }
        row += ROW;
        int opsecBodyY = row + OPSEC_BODY_TOP;
        if (SecurityPanel.clickToggle(mouseX, mouseY, nestX, opsecBodyY + 14, nestInner)) {
            s.opsecFakeDefaultKeybinds = !s.opsecFakeDefaultKeybinds;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickPillValue(mouseX, mouseY, nestX, opsecBodyY + 14 + ROW, nestInner)) {
            s.opsecBrandMode = s.opsecBrandMode == SecuritySettings.OpsecBrandMode.VANILLA
                    ? SecuritySettings.OpsecBrandMode.FABRIC
                    : SecuritySettings.OpsecBrandMode.VANILLA;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickPillValue(mouseX, mouseY, nestX, opsecBodyY + 14 + ROW + 22, nestInner)) {
            s.opsecWhitelistMode = switch (s.opsecWhitelistMode) {
                case OFF -> SecuritySettings.OpsecWhitelistMode.AUTO;
                case AUTO -> SecuritySettings.OpsecWhitelistMode.CUSTOM;
                case CUSTOM -> SecuritySettings.OpsecWhitelistMode.OFF;
            };
            this.manager.save();
            return true;
        }
        if (s.opsecWhitelistMode == SecuritySettings.OpsecWhitelistMode.CUSTOM) {
            int fieldY = opsecBodyY + 14 + ROW + 22 + 22 + 12;
            if (this.opsecWhitelistField.mouseClicked(mouseX, mouseY, button)) {
                this.nameChangerField.blur();
                this.syncOpsecWhitelistFromField();
                return true;
            }
        }
        int opsecCardH = opsecCardHeight(s);
        row = row + opsecCardH + 8;
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row, rowInner)) {
            s.keyResolutionServerMarkedOnly = !s.keyResolutionServerMarkedOnly;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.keyResolutionBlockSignEditorOnKeyProbe = !s.keyResolutionBlockSignEditorOnKeyProbe;
            this.manager.save();
            return true;
        }
        row += ROW + 4 + 12;
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row, rowInner)) {
            s.staffDetectionEnabled = !s.staffDetectionEnabled;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.staffDetectedAlerts = !s.staffDetectedAlerts;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.staffOnlineOfflineAlerts = !s.staffOnlineOfflineAlerts;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.staffProximityAlerts = !s.staffProximityAlerts;
            this.manager.save();
            return true;
        }
        row += ROW;
        if (this.staffRadiusRowY >= 0 && this.clickStaffRadiusSlider(mouseX, mouseY, rx, this.staffRadiusRowY, rowInner)) {
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.staffGlowEnabled = !s.staffGlowEnabled;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.antiInvisibleEntities = !s.antiInvisibleEntities;
            this.manager.save();
            return true;
        }
        row += ROW + 4 + 12;
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row, rowInner)) {
            s.nameChangerEnabled = !s.nameChangerEnabled;
            if (s.nameChangerEnabled) {
                this.manager.refreshNameChangerUsername();
            }
            this.manager.save();
            return true;
        }
        row += ROW + 12;
        if (this.nameChangerField.mouseClicked(mouseX, mouseY, button)) {
            this.opsecWhitelistField.blur();
            this.syncNameChangerFromField();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += 22, rowInner)) {
            s.nameChangerCensor = !s.nameChangerCensor;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            s.nameChangerOnlyInGame = !s.nameChangerOnlyInGame;
            this.manager.save();
            return true;
        }
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row += ROW, rowInner)) {
            boolean next = !s.noTextureRotations;
            s.noTextureRotations = next;
            this.manager.save();
            this.manager.onNoTextureRotationsChanged(next);
            return true;
        }
        row += ROW + 4 + 12;
        if (this.vaultRowY >= 0 && SecurityPanel.rect(mouseX, mouseY, rx, this.vaultRowY, rowInner, 18)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                mc.setScreen(new ServerPasswordScreen(mc.currentScreen));
            }
            return true;
        }
        row = this.vaultRowY + 22 + 30;
        if (SecurityPanel.clickToggle(mouseX, mouseY, rx, row, rowInner)) {
            s.profileAutoSwitchPerServer = !s.profileAutoSwitchPerServer;
            this.manager.save();
            return true;
        }
        if (this.saveProfileRowY >= 0 && SecurityPanel.rect(mouseX, mouseY, rx, this.saveProfileRowY, rowInner, 18)) {
            this.manager.saveProfileForCurrentServer(MinecraftClient.getInstance());
            return true;
        }
        this.opsecWhitelistField.blur();
        this.nameChangerField.blur();
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingStaffRadius = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (this.draggingStaffRadius && button == 0 && !this.collapsed && this.staffRadiusRowY >= 0) {
            SecuritySettings s = this.manager.getSettings();
            int rx = this.x + 16 + 8;
            int rowInner = this.width - 32 - 16;
            int barX = rx + STAFF_RADIUS_LABEL_OFFSET;
            int barW = rowInner - STAFF_RADIUS_LABEL_OFFSET - STAFF_RADIUS_VALUE_W - 4;
            s.staffProximityRadius = (int) Math.round(SecurityPanel.sliderValue(mouseX, barX, barW, 8.0, 256.0));
            this.manager.save();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.opsecWhitelistField.isFocused()) {
            if (this.opsecWhitelistField.keyPressed(keyCode)) {
                this.syncOpsecWhitelistFromField();
                return true;
            }
        }
        if (this.nameChangerField.isFocused()) {
            if (this.nameChangerField.keyPressed(keyCode)) {
                this.syncNameChangerFromField();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(int codePoint, int modifiers) {
        if (this.opsecWhitelistField.isFocused()) {
            if (this.opsecWhitelistField.charTyped(codePoint)) {
                this.syncOpsecWhitelistFromField();
                return true;
            }
        }
        if (this.nameChangerField.isFocused()) {
            if (this.nameChangerField.charTyped(codePoint)) {
                this.syncNameChangerFromField();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasFocusedTextInput() {
        return this.opsecWhitelistField.isFocused() || this.nameChangerField.isFocused();
    }

    private void syncOpsecWhitelistFromField() {
        this.manager.getSettings().opsecWhitelistedModsCsv = this.opsecWhitelistField.getText();
        this.manager.save();
    }

    private void syncNameChangerFromField() {
        this.manager.getSettings().nameChangerDisplayName = this.nameChangerField.getText();
        this.manager.save();
    }

    private static boolean clickToggle(double mx, double my, int sx, int sy, int rowInner) {
        return SecurityPanel.rect(mx, my, sx, sy, rowInner, ROW);
    }

    private static boolean clickPillValue(double mx, double my, int sx, int sy, int rowW) {
        int bx = sx + rowW - BIND_W;
        return SecurityPanel.rect(mx, my, bx, sy, BIND_W, BIND_ROW_H);
    }

    private static boolean rect(double mx, double my, int sx, int sy, int sw, int sh) {
        return mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh;
    }

    private boolean clickStaffRadiusSlider(double mouseX, double mouseY, int rx, int row, int rowInner) {
        int barX = rx + STAFF_RADIUS_LABEL_OFFSET;
        int barW = rowInner - STAFF_RADIUS_LABEL_OFFSET - STAFF_RADIUS_VALUE_W - 4;
        if (!SecurityPanel.rect(mouseX, mouseY, barX, row, barW, ROW)) {
            return false;
        }
        SecuritySettings s = this.manager.getSettings();
        this.draggingStaffRadius = true;
        s.staffProximityRadius = (int) Math.round(SecurityPanel.sliderValue(mouseX, barX, barW, 8.0, 256.0));
        this.manager.save();
        return true;
    }

    private static double sliderValue(double mouseX, int barX, int barW, double min, double max) {
        double t = barW <= 0 ? 0.0 : (mouseX - barX) / barW;
        t = Math.max(0.0, Math.min(1.0, t));
        return min + (max - min) * t;
    }
}
