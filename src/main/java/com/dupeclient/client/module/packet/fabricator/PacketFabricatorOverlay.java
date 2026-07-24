package com.dupeclient.client.module.packet.fabricator;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.overlay.AbstractDraggableOverlay;
import com.dupeclient.client.gui.overlay.IngameModuleOverlay;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import com.dupeclient.client.module.packet.fabricator.FabricatorAction;
import com.dupeclient.client.module.packet.fabricator.FabricatorDelayTab;
import com.dupeclient.client.module.packet.fabricator.FabricatorInventorySlots;
import com.dupeclient.client.module.packet.fabricator.FabricatorSendScheduler;
import com.dupeclient.client.module.packet.fabricator.FabricatorSlotList;
import com.dupeclient.client.module.packet.fabricator.FabricatorTab;
import com.dupeclient.client.module.packet.fabricator.PacketFabricator;
import com.ui_utils.SharedVariables;
import java.util.Locale;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.lwjgl.glfw.GLFW;

public final class PacketFabricatorOverlay
extends AbstractDraggableOverlay
implements IngameModuleOverlay {
    public static final PacketFabricatorOverlay INSTANCE = new PacketFabricatorOverlay();
    private static final int PANEL_W = 300;
    private static final int FABRICATE_H = 310;
    private static final int GAP = 5;
    private static final int TITLE_H = 12;
    private static final int TAB_H = 14;
    private static final int INNER_PAD = 8;
    private static final int INPUT_ROW = 20;
    private static final int SET_ROW = 12;
    private static final int BTN_H = 16;
    private static final int FIELD_H = 18;
    private static final int VALUE_W = 72;
    private static final int LABEL_W = 54;
    private static final int MULTI_TOGGLE_W = 22;
    private static final int VALUE_X = 66;
    private static final int SLOT_FIELD_X = 92;
    private static final int UI_UTILS_PANEL_RIGHT = 155;
    private boolean dragging;
    private double dragOffX;
    private double dragOffY;
    private FocusedField focused = FocusedField.NONE;
    private KeyCapture keyCapture = KeyCapture.NONE;
    private int hitActionBtnX;
    private int hitActionBtnY;
    private int hitActionBtnW;
    private int hitOptionBtnX;
    private int hitOptionBtnY;
    private int hitOptionBtnW;
    private int hitSendX;
    private int hitSendY;
    private int hitSendW;
    private int hitQueueX;
    private int hitQueueW;
    private int hitPauseX;
    private int hitPauseY;
    private int hitPauseW;
    private int hitStopX;
    private int hitStopW;
    private int hitCloseX;
    private int hitCloseY;
    private int hitCloseW;
    private int hitBiggerContainersX;
    private int hitBiggerContainersY;
    private int hitBiggerContainersW;
    private int hitContainerScaleX;
    private int hitContainerScaleY;
    private int hitSlotIdsX;
    private int hitSlotIdsY;
    private int hitSlotIdsW;
    private int hitPacketsPerTickX;
    private int hitPacketsPerTickY;
    private int hitSendDelayX;
    private int hitSendDelayY;
    private int hitOverlayKeyX;
    private int hitOverlayKeyY;
    private int hitSlotIdsKeyX;
    private int hitSlotIdsKeyY;
    private int hitMultiSlotX;
    private int hitMultiSlotY;
    private int hitMultiSlotW;
    private int hitTabFabricateX;
    private int hitTabFabricateY;
    private int hitTabFabricateW;
    private int hitTabDelayX;
    private int hitTabDelayY;
    private int hitTabDelayW;
    private final FabricatorDelayTab delayTab = new FabricatorDelayTab();

    private PacketFabricatorOverlay() {
    }

    @Override
    public String id() {
        return "packet_fabricator";
    }

    @Override
    public boolean isModuleEnabled() {
        return PacketUtilsManager.INSTANCE.getSettings().fabricatorEnabled;
    }

    @Override
    public boolean isActive() {
        return this.isModuleEnabled() && this.isOverlayVisible();
    }

    @Override
    public boolean isOverlayVisible() {
        return this.isVisible();
    }

    @Override
    public void setOverlayVisible(boolean visible) {
        this.setVisible(visible);
    }

    @Override
    public int overlayX() {
        return PacketFabricator.INSTANCE.settings().fabricatorOverlayX;
    }

    @Override
    public int overlayY() {
        return PacketFabricator.INSTANCE.settings().fabricatorOverlayY;
    }

    @Override
    public void setOverlayPosition(int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        int maxX = client != null ? client.getWindow().getScaledWidth() - 300 : x;
        int maxY = client != null ? client.getWindow().getScaledHeight() - this.panelHeight() : y;
        PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
        s.fabricatorOverlayX = Math.max(0, Math.min(maxX, x));
        s.fabricatorOverlayY = Math.max(0, Math.min(maxY, y));
        PacketUtilsManager.INSTANCE.save();
    }

    @Override
    public int panelWidth() {
        return 300;
    }

    @Override
    public int panelHeight() {
        return this.panelHeight(this.activeTab());
    }

    @Override
    public boolean hasTextFocus() {
        return this.keyCapture != KeyCapture.NONE || this.activeTab() == FabricatorTab.DELAY && this.delayTab.hasTextFocus() || this.activeTab() == FabricatorTab.FABRICATE && this.focused != FocusedField.NONE;
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public boolean blocksGameInput() {
        return false;
    }

    public boolean isVisible() {
        return PacketFabricator.INSTANCE.settings().fabricatorVisible;
    }

    public FabricatorTab activeTab() {
        PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
        return "delay".equalsIgnoreCase(s.fabricatorActiveTab) ? FabricatorTab.DELAY : FabricatorTab.FABRICATE;
    }

    public void setActiveTab(FabricatorTab tab) {
        PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
        String string = s.fabricatorActiveTab = tab == FabricatorTab.DELAY ? "delay" : "fabricate";
        if (tab == FabricatorTab.FABRICATE) {
            this.delayTab.onHide();
        } else {
            this.focused = FocusedField.NONE;
            this.keyCapture = KeyCapture.NONE;
        }
        PacketUtilsManager.INSTANCE.save();
    }

    public void showDelayTab() {
        this.setActiveTab(FabricatorTab.DELAY);
        this.setVisible(true);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            IngameOverlayHost.onModuleOverlayOpening(this);
        }
        PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
        s.fabricatorVisible = visible;
        s.packetDelayOverlayVisible = false;
        if (!visible) {
            this.keyCapture = KeyCapture.NONE;
            this.delayTab.onHide();
        }
        PacketUtilsManager.INSTANCE.save();
    }

    public void toggleVisible() {
        this.setVisible(!this.isVisible());
    }

    public void showClickslotFabricator() {
        PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
        s.fabricatorEnabled = true;
        s.fabricatorVisible = true;
        PacketFabricatorOverlay.snapOverlayBesideUiUtils(s);
        this.setActiveTab(FabricatorTab.FABRICATE);
        this.setVisible(true);
        PacketUtilsManager.INSTANCE.save();
    }

    public void toggleClickslotFabricator() {
        if (this.isModuleEnabled()) {
            PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
            s.fabricatorEnabled = false;
            this.setVisible(false);
            PacketFabricatorOverlay.feedback("Clickslot fabricator off");
        } else {
            this.showClickslotFabricator();
        }
        PacketUtilsManager.INSTANCE.save();
    }

    private static void snapOverlayBesideUiUtils(PacketUtilsSettings s) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !(client.currentScreen instanceof HandledScreen)) {
            return;
        }
        if (!SharedVariables.enabled && !s.uiUtilsOverlayEnabled) {
            return;
        }
        if (s.fabricatorOverlayX < 155) {
            s.fabricatorOverlayX = 155;
        }
        if (s.fabricatorOverlayY < 4) {
            s.fabricatorOverlayY = 4;
        }
    }

    private int panelHeight(FabricatorTab tab) {
        if (tab == FabricatorTab.DELAY) {
            return 36 + this.delayTab.contentHeight(this.delayTab.isPickerOpen()) + 6;
        }
        return 346;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderPanel(context, MinecraftClient.getInstance().textRenderer, mouseX, mouseY, delta);
    }

    private void renderTabs(DrawContext context, TextRenderer tr, int px, int py, int innerW, FabricatorTab tab) {
        int half = (innerW - 5) / 2;
        this.hitTabFabricateX = px;
        this.hitTabFabricateY = py;
        this.hitTabFabricateW = half;
        this.hitTabDelayX = px + half + 5;
        this.hitTabDelayY = py;
        this.hitTabDelayW = half;
        UiComponents.drawSegmentTab(tr, context, this.hitTabFabricateX, this.hitTabFabricateY, this.hitTabFabricateW, 14, "Fabricate", tab == FabricatorTab.FABRICATE);
        UiComponents.drawSegmentTab(tr, context, this.hitTabDelayX, this.hitTabDelayY, this.hitTabDelayW, 14, "Delay", tab == FabricatorTab.DELAY);
    }

    public void render(HandledScreen<?> screen, DrawContext context, int mouseX, int mouseY, float delta) {
        if (!PacketFabricator.INSTANCE.settings().fabricatorEnabled) {
            return;
        }
        this.renderPanel(context, screen.getTextRenderer(), mouseX, mouseY, delta);
    }

    private void renderPanel(DrawContext context, TextRenderer tr, int mouseX, int mouseY, float delta) {
        PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
        if (!s.fabricatorEnabled || !s.fabricatorVisible) {
            return;
        }
        FabricatorTab tab = this.activeTab();
        int px = s.fabricatorOverlayX;
        int py = s.fabricatorOverlayY;
        int ph = this.panelHeight(tab);
        int innerW = 284;
        context.fill(px, py, px + 300, py + ph, -535291877);
        context.fill(px, py, px + 300, py + 12, -14211286);
        int titleColor = tab == FabricatorTab.DELAY ? -10443270 : -13315175;
        context.drawTextWithShadow(tr, (Text)Text.literal((String)"Packet Fabricator"), px + 6, py + 2, titleColor);
        this.drawTitleControls(context, tr, px, py, mouseX, mouseY);
        int tabY = py + 12 + 5;
        this.renderTabs(context, tr, px + 8, tabY, innerW, tab);
        if (tab == FabricatorTab.DELAY) {
            this.delayTab.render(context, tr, px + 8, tabY + 14 + 5, innerW, mouseX, mouseY);
            return;
        }
        int y = tabY + 14 + 5;
        int valueX = px + 66;
        int valueW = 226;
        int slotFieldX = px + 92;
        int slotFieldW = 200;
        FabricatorAction action = PacketFabricator.INSTANCE.currentAction();
        PacketFabricatorOverlay.drawLabel(context, tr, "Action", px + 8, y);
        this.hitActionBtnX = valueX;
        this.hitActionBtnY = y - 1;
        this.hitActionBtnW = valueW;
        PacketFabricatorOverlay.drawButton(context, tr, this.hitActionBtnX, this.hitActionBtnY, this.hitActionBtnW, 16, action.displayName, mouseX, mouseY, true);
        y += 20;
        if (action.usesClickButton || action.usesDropToggle) {
            PacketFabricatorOverlay.drawLabel(context, tr, action.usesDropToggle ? "Drop mode" : "Button", px + 8, y);
            this.hitOptionBtnX = valueX;
            this.hitOptionBtnY = y - 1;
            this.hitOptionBtnW = valueW;
            String opt = action.usesDropToggle ? (s.fabricatorDropWholeStack ? "Whole stack" : "Single item") : (s.fabricatorClickButton == 0 ? "Left click" : "Right click");
            PacketFabricatorOverlay.drawButton(context, tr, this.hitOptionBtnX, this.hitOptionBtnY, this.hitOptionBtnW, 16, opt, mouseX, mouseY, true);
            y += 20;
        } else {
            this.hitOptionBtnW = 0;
        }
        String slotLabel = s.fabricatorMultiSlot ? "Slot(s)" : "Slot";
        PacketFabricatorOverlay.drawInputLabel(context, tr, slotLabel, px + 8, y, this.focused == FocusedField.SLOT);
        this.hitMultiSlotX = valueX;
        this.hitMultiSlotY = y - 2;
        this.hitMultiSlotW = 22;
        PacketFabricatorOverlay.drawMultiToggle(context, tr, this.hitMultiSlotX, this.hitMultiSlotY, this.hitMultiSlotW, s.fabricatorMultiSlot);
        PacketFabricatorOverlay.drawField(context, tr, slotFieldX, y - 2, slotFieldW, s.fabricatorSlot, this.focused == FocusedField.SLOT);
        PacketFabricatorOverlay.drawInputLabel(context, tr, "Item", px + 8, y += 20, this.focused == FocusedField.ITEM);
        PacketFabricatorOverlay.drawField(context, tr, valueX, y - 2, valueW, s.fabricatorItemName, this.focused == FocusedField.ITEM);
        PacketFabricatorOverlay.drawInputLabel(context, tr, "Times", px + 8, y += 20, this.focused == FocusedField.TIMES);
        PacketFabricatorOverlay.drawField(context, tr, valueX, y - 2, valueW, s.fabricatorTimes, this.focused == FocusedField.TIMES);
        context.drawTextWithShadow(tr, (Text)Text.literal((String)"Settings"), px + 6, y += 16, -9735552);
        this.hitBiggerContainersX = px + 6;
        this.hitBiggerContainersY = y += 12;
        this.hitBiggerContainersW = innerW;
        PacketFabricatorOverlay.drawSettingToggle(context, tr, this.hitBiggerContainersX, y, this.hitBiggerContainersW, "Bigger containers", s.handledScreenScaleEnabled);
        this.hitContainerScaleX = px + 300 - 8 - 72;
        this.hitContainerScaleY = y += 12;
        PacketFabricatorOverlay.drawSettingValue(context, tr, px + 6, y, innerW, "Container scale", HandledScreenGuiScale.formatScale(s.handledScreenScale));
        this.hitSlotIdsX = px + 6;
        this.hitSlotIdsY = y += 12;
        this.hitSlotIdsW = innerW;
        PacketFabricatorOverlay.drawSettingToggle(context, tr, this.hitSlotIdsX, y, this.hitSlotIdsW, "Slot ID overlay", s.slotIdsOverlayEnabled);
        this.hitPacketsPerTickX = px + 300 - 8 - 72;
        this.hitPacketsPerTickY = y += 12;
        PacketFabricatorOverlay.drawSettingValue(context, tr, px + 6, y, innerW, "Packets / tick", Integer.toString(s.fabricatorPacketsPerTick));
        this.hitSendDelayX = px + 300 - 8 - 72;
        this.hitSendDelayY = y += 12;
        PacketFabricatorOverlay.drawSettingValue(context, tr, px + 6, y, innerW, "Send delay (ms)", Integer.toString(s.fabricatorSendDelayMs));
        this.hitOverlayKeyX = px + 300 - 8 - 72;
        this.hitOverlayKeyY = y += 12;
        PacketFabricatorOverlay.drawSettingBind(context, tr, px + 6, y, innerW, "Toggle overlay", s.fabricatorToggleKey, this.keyCapture == KeyCapture.FABRICATOR_TOGGLE);
        this.hitSlotIdsKeyX = px + 300 - 8 - 72;
        this.hitSlotIdsKeyY = y += 12;
        PacketFabricatorOverlay.drawSettingBind(context, tr, px + 6, y, innerW, "Toggle slot IDs", s.slotIdsToggleKey, this.keyCapture == KeyCapture.SLOT_IDS_TOGGLE);
        y += 14;
        String status = PacketFabricator.INSTANCE.getLastStatus();
        if (status != null && !status.isBlank()) {
            context.drawTextWithShadow(tr, (Text)Text.literal((String)PacketFabricatorOverlay.trim(tr, status, innerW)), px + 6, y, -6511697);
            y += 10;
        }
        if (this.keyCapture != KeyCapture.NONE) {
            context.drawTextWithShadow(tr, (Text)Text.literal((String)"Press key (ESC = unbind)"), px + 6, y, -14217);
            y += 10;
        }
        int btnY = py + ph - 16 - 6;
        int ctrlY = btnY - 16 - 4;
        int half = 105;
        FabricatorSendScheduler scheduler = FabricatorSendScheduler.INSTANCE;
        boolean sendActive = scheduler.isActive();
        String pauseLabel = scheduler.getState() == FabricatorSendScheduler.State.PAUSED ? "Resume" : "Pause";
        this.hitPauseX = px + 6;
        this.hitPauseY = ctrlY;
        this.hitPauseW = half;
        this.hitStopX = px + 8 + half;
        this.hitStopW = half;
        PacketFabricatorOverlay.drawButton(context, tr, this.hitPauseX, ctrlY, half, 16, pauseLabel, mouseX, mouseY, sendActive);
        PacketFabricatorOverlay.drawButton(context, tr, this.hitStopX, ctrlY, half, 16, "Stop", mouseX, mouseY, sendActive);
        this.hitSendX = px + 6;
        this.hitSendY = btnY;
        this.hitSendW = half;
        this.hitQueueX = px + 8 + half;
        this.hitQueueW = half;
        PacketFabricatorOverlay.drawButton(context, tr, this.hitSendX, btnY, half, 16, "Send", mouseX, mouseY, true);
        PacketFabricatorOverlay.drawButton(context, tr, this.hitQueueX, btnY, half, 16, "Queue", mouseX, mouseY, true);
    }

    private void drawTitleControls(DrawContext context, TextRenderer tr, int px, int py, int mouseX, int mouseY) {
        this.hitCloseW = 12;
        this.hitCloseX = px + 300 - 14;
        this.hitCloseY = py + 1;
        boolean closeHover = mouseX >= this.hitCloseX && mouseX < this.hitCloseX + this.hitCloseW && mouseY >= this.hitCloseY && mouseY < this.hitCloseY + 10;
        context.drawTextWithShadow(tr, (Text)Text.literal((String)"\u00d7"), this.hitCloseX, this.hitCloseY, closeHover ? -495247 : -9735552);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.mouseClicked(null, mouseX, mouseY, button);
    }

    public boolean mouseClicked(HandledScreen<?> screen, double mouseX, double mouseY, int button) {
        PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
        if (!s.fabricatorEnabled || button != 0) {
            return false;
        }
        int px = s.fabricatorOverlayX;
        int py = s.fabricatorOverlayY;
        int ph = this.panelHeight();
        if (mouseX < (double)px || mouseX >= (double)(px + 300) || mouseY < (double)py || mouseY >= (double)(py + ph)) {
            return false;
        }
        if (mouseX >= (double)this.hitCloseX && mouseX < (double)(this.hitCloseX + this.hitCloseW) && mouseY >= (double)this.hitCloseY && mouseY < (double)(this.hitCloseY + 10)) {
            PacketUtilsSettings closeSettings = PacketFabricator.INSTANCE.settings();
            closeSettings.fabricatorEnabled = false;
            this.setVisible(false);
            PacketFabricatorOverlay.feedback("Clickslot fabricator off");
            return true;
        }
        if (this.beginTitleDrag(mouseX, mouseY, button, px, py, 300, 12)) {
            return true;
        }
        int tabY = py + 12 + 5;
        if (PacketFabricatorOverlay.inRect(mouseX, mouseY, this.hitTabFabricateX, this.hitTabFabricateY, this.hitTabFabricateW, 14)) {
            this.setActiveTab(FabricatorTab.FABRICATE);
            return true;
        }
        if (PacketFabricatorOverlay.inRect(mouseX, mouseY, this.hitTabDelayX, this.hitTabDelayY, this.hitTabDelayW, 14)) {
            this.setActiveTab(FabricatorTab.DELAY);
            return true;
        }
        if (this.activeTab() == FabricatorTab.DELAY) {
            int contentY = tabY + 14 + 5;
            return this.delayTab.mouseClicked(mouseX, mouseY, button, px + 8, contentY, 284);
        }
        if (mouseY >= (double)this.hitPauseY && mouseY < (double)(this.hitPauseY + 16)) {
            if (mouseX >= (double)this.hitPauseX && mouseX < (double)(this.hitPauseX + this.hitPauseW) && FabricatorSendScheduler.INSTANCE.isActive()) {
                FabricatorSendScheduler.INSTANCE.togglePause(MinecraftClient.getInstance());
                return true;
            }
            if (mouseX >= (double)this.hitStopX && mouseX < (double)(this.hitStopX + this.hitStopW) && FabricatorSendScheduler.INSTANCE.isActive()) {
                FabricatorSendScheduler.INSTANCE.stop();
                return true;
            }
        }
        if (mouseY >= (double)this.hitSendY && mouseY < (double)(this.hitSendY + 16)) {
            if (mouseX >= (double)this.hitSendX && mouseX < (double)(this.hitSendX + this.hitSendW)) {
                PacketFabricator.INSTANCE.send(false);
                return true;
            }
            if (mouseX >= (double)this.hitQueueX && mouseX < (double)(this.hitQueueX + this.hitQueueW)) {
                PacketFabricator.INSTANCE.send(true);
                return true;
            }
        }
        if (PacketFabricatorOverlay.inBtn(mouseX, mouseY, this.hitActionBtnX, this.hitActionBtnY, this.hitActionBtnW)) {
            PacketFabricator.INSTANCE.cycleAction();
            return true;
        }
        if (this.hitOptionBtnW > 0 && PacketFabricatorOverlay.inBtn(mouseX, mouseY, this.hitOptionBtnX, this.hitOptionBtnY, this.hitOptionBtnW)) {
            FabricatorAction action = PacketFabricator.INSTANCE.currentAction();
            if (action.usesDropToggle) {
                PacketFabricator.INSTANCE.toggleDropWholeStack();
            } else {
                PacketFabricator.INSTANCE.cycleClickButton();
            }
            return true;
        }
        if (PacketFabricatorOverlay.inRect(mouseX, mouseY, this.hitMultiSlotX, this.hitMultiSlotY, this.hitMultiSlotW, 18)) {
            PacketFabricator.INSTANCE.toggleMultiSlot();
            return true;
        }
        if (PacketFabricatorOverlay.inSettingRow(mouseX, mouseY, this.hitBiggerContainersX, this.hitBiggerContainersY, this.hitBiggerContainersW)) {
            s.handledScreenScaleEnabled = !s.handledScreenScaleEnabled;
            PacketFabricatorOverlay.feedback("Container scale " + (s.handledScreenScaleEnabled ? "ON" : "OFF"));
            return true;
        }
        if (PacketFabricatorOverlay.inSettingValue(mouseX, mouseY, this.hitContainerScaleX, this.hitContainerScaleY)) {
            s.handledScreenScale = HandledScreenGuiScale.cyclePreset(s.handledScreenScale);
            PacketFabricatorOverlay.feedback("Container scale: " + HandledScreenGuiScale.formatScale(s.handledScreenScale));
            return true;
        }
        if (PacketFabricatorOverlay.inSettingRow(mouseX, mouseY, this.hitSlotIdsX, this.hitSlotIdsY, this.hitSlotIdsW)) {
            s.slotIdsOverlayEnabled = !s.slotIdsOverlayEnabled;
            PacketFabricatorOverlay.feedback("Slot ID overlay " + (s.slotIdsOverlayEnabled ? "ON" : "OFF"));
            return true;
        }
        if (PacketFabricatorOverlay.inSettingValue(mouseX, mouseY, this.hitPacketsPerTickX, this.hitPacketsPerTickY)) {
            s.fabricatorPacketsPerTick = PacketFabricatorOverlay.cyclePacketsPerTick(s.fabricatorPacketsPerTick);
            PacketFabricatorOverlay.feedback("Fabricator rate: " + s.fabricatorPacketsPerTick + " pkt/tick");
            return true;
        }
        if (PacketFabricatorOverlay.inSettingValue(mouseX, mouseY, this.hitSendDelayX, this.hitSendDelayY)) {
            s.fabricatorSendDelayMs = PacketFabricatorOverlay.cycleSendDelay(s.fabricatorSendDelayMs);
            PacketFabricatorOverlay.feedback("Fabricator delay: " + s.fabricatorSendDelayMs + " ms");
            return true;
        }
        if (PacketFabricatorOverlay.inSettingValue(mouseX, mouseY, this.hitOverlayKeyX, this.hitOverlayKeyY)) {
            this.keyCapture = KeyCapture.FABRICATOR_TOGGLE;
            return true;
        }
        if (PacketFabricatorOverlay.inSettingValue(mouseX, mouseY, this.hitSlotIdsKeyX, this.hitSlotIdsKeyY)) {
            this.keyCapture = KeyCapture.SLOT_IDS_TOGGLE;
            return true;
        }
        FocusedField next = this.fieldAt(mouseX, mouseY, px, py);
        if (this.focused != FocusedField.NONE && next != this.focused) {
            PacketFabricatorOverlay.normalizeFocusedField(s);
            PacketUtilsManager.INSTANCE.save();
        }
        if (next != FocusedField.NONE) {
            this.focused = next;
            return true;
        }
        if (this.focused != FocusedField.NONE) {
            this.focused = FocusedField.NONE;
            PacketUtilsManager.INSTANCE.save();
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = this.dragging;
        this.endTitleDrag(button);
        if (wasDragging) {
            PacketUtilsManager.INSTANCE.save();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return this.updateTitleDrag(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.isModuleEnabled() || this.activeTab() != FabricatorTab.DELAY) {
            return false;
        }
        return this.delayTab.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (!this.isModuleEnabled()) {
            return false;
        }
        if (this.activeTab() == FabricatorTab.DELAY && this.delayTab.keyPressed(keyCode)) {
            return true;
        }
        if (this.keyCapture != KeyCapture.NONE) {
            int key = keyCode == 256 ? -1 : keyCode;
            PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
            String label = switch (this.keyCapture.ordinal()) {
                case 1 -> {
                    s.fabricatorToggleKey = key;
                    yield "Toggle overlay";
                }
                case 2 -> {
                    s.slotIdsToggleKey = key;
                    yield "Toggle slot IDs";
                }
                default -> "";
            };
            this.keyCapture = KeyCapture.NONE;
            PacketUtilsManager.INSTANCE.save();
            PacketFabricatorOverlay.feedback(label + " hotkey \u2192 " + PacketFabricatorOverlay.keyName(key));
            return true;
        }
        if (keyCode == 256) {
            if (FabricatorSendScheduler.INSTANCE.isActive()) {
                FabricatorSendScheduler.INSTANCE.stop();
                return true;
            }
            if (this.focused != FocusedField.NONE) {
                PacketFabricatorOverlay.normalizeFocusedField(PacketFabricator.INSTANCE.settings());
                this.focused = FocusedField.NONE;
                PacketUtilsManager.INSTANCE.save();
                return true;
            }
            PacketUtilsSettings escSettings = PacketFabricator.INSTANCE.settings();
            escSettings.fabricatorEnabled = false;
            this.setVisible(false);
            return true;
        }
        if (keyCode == 32 && FabricatorSendScheduler.INSTANCE.isActive() && this.focused == FocusedField.NONE) {
            FabricatorSendScheduler.INSTANCE.togglePause(MinecraftClient.getInstance());
            return true;
        }
        if (this.focused == FocusedField.NONE) {
            return false;
        }
        PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
        String cur = PacketFabricatorOverlay.fieldValue(s);
        if (keyCode == 259 || keyCode == 261) {
            if (!cur.isEmpty()) {
                PacketFabricatorOverlay.setFieldValue(s, cur.substring(0, cur.length() - 1));
            }
            PacketUtilsManager.INSTANCE.save();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(int codePoint) {
        if (!this.isModuleEnabled()) {
            return false;
        }
        if (this.activeTab() == FabricatorTab.DELAY && this.delayTab.charTyped(codePoint)) {
            return true;
        }
        if (this.focused == FocusedField.NONE || !Character.isValidCodePoint(codePoint)) {
            return false;
        }
        if (this.focused == FocusedField.SLOT) {
            PacketUtilsSettings slotSettings = PacketFabricator.INSTANCE.settings();
            boolean allowList = slotSettings.fabricatorMultiSlot || FabricatorSlotList.hasMultiple(slotSettings.fabricatorSlot);
            boolean bl = allowList;
            if (!Character.isDigit(codePoint) && codePoint != 45 && (!allowList || codePoint != 44 && codePoint != 32)) {
                return false;
            }
        } else if (this.focused != FocusedField.ITEM && !Character.isDigit(codePoint) && codePoint != 45) {
            return false;
        }
        PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
        PacketFabricatorOverlay.setFieldValue(s, PacketFabricatorOverlay.fieldValue(s) + Character.toString(codePoint));
        PacketUtilsManager.INSTANCE.save();
        return true;
    }

    public void onSlotClick(HandledScreen<?> screen, int handlerSlotId) {
        if (!this.isModuleEnabled() || this.activeTab() != FabricatorTab.FABRICATE) {
            return;
        }
        PacketUtilsSettings s = PacketFabricator.INSTANCE.settings();
        MinecraftClient client = MinecraftClient.getInstance();
        int visible = FabricatorInventorySlots.toUserVisibleSlot(client, handlerSlotId);
        if (s.fabricatorMultiSlot) {
            s.fabricatorSlot = FabricatorSlotList.toggleVisible(s.fabricatorSlot, visible);
            PacketFabricator.INSTANCE.setLastStatus(s.fabricatorSlot.isBlank() ? "Removed slot " + visible : "Slots: " + s.fabricatorSlot);
        } else {
            s.fabricatorSlot = Integer.toString(visible);
            PacketFabricator.INSTANCE.setLastStatus("Slot " + visible);
        }
        PacketUtilsManager.INSTANCE.save();
    }

    private FocusedField fieldAt(double mouseX, double mouseY, int px, int py) {
        int y = py + 12 + 5 + 14 + 5 + 20;
        FabricatorAction action = PacketFabricator.INSTANCE.currentAction();
        if (action.usesClickButton || action.usesDropToggle) {
            y += 20;
        }
        int slotFieldX = px + 92;
        int valueX = px + 66;
        int right = px + 300 - 8;
        if (mouseX >= (double)slotFieldX && mouseX < (double)right && mouseY >= (double)(y - 2) && mouseY < (double)(y - 2 + 18)) {
            return FocusedField.SLOT;
        }
        y += 20;
        if (mouseX >= (double)valueX && mouseX < (double)right && mouseY >= (double)(y - 2) && mouseY < (double)(y - 2 + 18)) {
            return FocusedField.ITEM;
        }
        y += 20;
        if (mouseX >= (double)valueX && mouseX < (double)right && mouseY >= (double)(y - 2) && mouseY < (double)(y - 2 + 18)) {
            return FocusedField.TIMES;
        }
        return FocusedField.NONE;
    }

    private static String fieldValue(PacketUtilsSettings s) {
        return switch (PacketFabricatorOverlay.INSTANCE.focused.ordinal()) {
            case 1 -> {
                if (s.fabricatorSlot == null) {
                    yield "";
                }
                yield s.fabricatorSlot;
            }
            case 2 -> {
                if (s.fabricatorItemName == null) {
                    yield "";
                }
                yield s.fabricatorItemName;
            }
            case 3 -> {
                if (s.fabricatorTimes == null) {
                    yield "";
                }
                yield s.fabricatorTimes;
            }
            default -> "";
        };
    }

    private static void setFieldValue(PacketUtilsSettings s, String value) {
        switch (PacketFabricatorOverlay.INSTANCE.focused.ordinal()) {
            case 1: {
                s.fabricatorSlot = value;
                break;
            }
            case 2: {
                s.fabricatorItemName = value;
                break;
            }
            case 3: {
                s.fabricatorTimes = value;
            }
        }
    }

    private static void normalizeFocusedField(PacketUtilsSettings s) {
        switch (PacketFabricatorOverlay.INSTANCE.focused.ordinal()) {
            case 3: {
                if (s.fabricatorTimes != null && !s.fabricatorTimes.isBlank()) break;
                s.fabricatorTimes = "1";
                break;
            }
            case 1: {
                if (s.fabricatorMultiSlot || s.fabricatorSlot != null && !s.fabricatorSlot.isBlank()) break;
                s.fabricatorSlot = "0";
            }
        }
    }

    private static void drawLabel(DrawContext c, TextRenderer tr, String label, int x, int y) {
        c.drawTextWithShadow(tr, (Text)Text.literal((String)label), x, y + 3, -6184534);
    }

    private static void drawMultiToggle(DrawContext c, TextRenderer tr, int x, int y, int w, boolean on) {
        int bg = on ? -15445203 : -14211286;
        int border = on ? -13315175 : -11840157;
        c.fill(x, y, x + w, y + 18, bg);
        c.fill(x, y, x + w, y + 1, border);
        c.fill(x, y + 18 - 1, x + w, y + 18, border);
        c.fill(x, y, x + 1, y + 18, border);
        c.fill(x + w - 1, y, x + w, y + 18, border);
        String label = on ? "M+" : "M";
        int tw = tr.getWidth(label);
        c.drawTextWithShadow(tr, (Text)Text.literal((String)label), x + (w - tw) / 2, y + 5, on ? -13315175 : -6511697);
    }

    private static void drawInputLabel(DrawContext c, TextRenderer tr, String label, int x, int y, boolean focused) {
        int color = focused ? -13315175 : -6184534;
        Object text = focused ? "> " + label : label;
        c.drawTextWithShadow(tr, (Text)Text.literal((String)((String)text)), x, y + 4, color);
    }

    private static void drawField(DrawContext c, TextRenderer tr, int x, int y, int w, String value, boolean focused) {
        com.dupeclient.client.gui.modern.ModernTextInputChrome.drawField(c, x, y, w, 18, focused);
        String shown = value == null ? "" : value;
        String display = shown.isEmpty() ? (focused ? "" : " ") : PacketFabricatorOverlay.trim(tr, shown, w - 16);
        int textX = x + com.dupeclient.client.gui.modern.ModernTextInputChrome.PAD_X;
        int textY = com.dupeclient.client.gui.modern.ModernTextInputChrome.textY(y, 18);
        int textColor = focused ? -1 : -1710101;
        c.drawTextWithShadow(tr, (Text)Text.literal((String)(display.isEmpty() ? " " : display)), textX, textY, textColor);
        if (focused && com.dupeclient.client.gui.modern.ModernTextInputChrome.caretVisible()) {
            int caretX = textX + tr.getWidth(display.isEmpty() ? "" : display);
            c.fill(caretX, textY - 1, caretX + 1, textY + 9, com.dupeclient.client.gui.modern.ModernTextInputChrome.CARET_COLOR);
        }
    }

    private static void drawSettingToggle(DrawContext c, TextRenderer tr, int x, int y, int w, String label, boolean on) {
        c.drawTextWithShadow(tr, (Text)Text.literal((String)label), x, y + 2, -6184534);
        String state = on ? "ON" : "OFF";
        int sw = tr.getWidth(state) + 10;
        int sx = x + w - sw;
        int color = on ? -13315175 : -11382181;
        c.fill(sx, y, x + w, y + 12, -14737629);
        c.drawTextWithShadow(tr, (Text)Text.literal((String)state), sx + 5, y + 2, color);
    }

    private static void drawSettingValue(DrawContext c, TextRenderer tr, int x, int y, int w, String label, String value) {
        c.drawTextWithShadow(tr, (Text)Text.literal((String)label), x, y + 2, -6184534);
        int vx = x + w - 72;
        c.fill(vx, y, x + w, y + 12, -14211286);
        String shown = PacketFabricatorOverlay.trim(tr, value, 64);
        c.drawTextWithShadow(tr, (Text)Text.literal((String)shown), vx + 4, y + 2, -1710101);
    }

    private static void drawSettingBind(DrawContext c, TextRenderer tr, int x, int y, int w, String label, int keyCode, boolean listening) {
        c.drawTextWithShadow(tr, (Text)Text.literal((String)label), x, y + 2, -6184534);
        int vx = x + w - 72;
        int border = listening ? -14217 : -12632250;
        c.fill(vx, y, x + w, y + 12, -15790318);
        c.fill(vx, y, x + w, y + 1, border);
        c.drawTextWithShadow(tr, (Text)Text.literal((String)PacketFabricatorOverlay.trim(tr, PacketFabricatorOverlay.keyName(keyCode), 64)), vx + 4, y + 2, listening ? -14217 : -1710101);
    }

    private static void drawButton(DrawContext c, TextRenderer tr, int x, int y, int w, int h, String label, int mx, int my, boolean enabled) {
        boolean hover = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        boolean bl = hover;
        int bg = !enabled ? -15066594 : (hover ? -12632250 : -14211286);
        int fg = enabled ? -1710101 : -11382181;
        c.fill(x, y, x + w, y + h, bg);
        int tw = tr.getWidth(label);
        c.drawTextWithShadow(tr, (Text)Text.literal((String)label), x + Math.max(4, (w - tw) / 2), y + 4, fg);
    }

    private static boolean inBtn(double mx, double my, int x, int y, int w) {
        return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + 16);
    }

    private static boolean inSettingRow(double mx, double my, int x, int y, int w) {
        return mx >= (double)x && mx < (double)(x + w) && my >= (double)y && my < (double)(y + 12);
    }

    private static boolean inSettingValue(double mx, double my, int valueX, int y) {
        return mx >= (double)valueX && mx < (double)(valueX + 72) && my >= (double)y && my < (double)(y + 12);
    }

    private static int cyclePacketsPerTick(int current) {
        int[] steps = new int[]{1, 2, 5, 10, 20, 50, 100, 200, 500};
        return PacketFabricatorOverlay.cycleInt(steps, current);
    }

    private static int cycleSendDelay(int current) {
        int[] steps = new int[]{0, 25, 50, 100, 250, 500};
        return PacketFabricatorOverlay.cycleInt(steps, current);
    }

    private static int cycleInt(int[] steps, int current) {
        for (int i = 0; i < steps.length; ++i) {
            if (steps[i] != current) continue;
            return steps[(i + 1) % steps.length];
        }
        return steps[0];
    }

    private static void feedback(String message) {
        PacketUtilsManager.INSTANCE.save();
        PacketUtilsManager.INSTANCE.moduleFeedback(message);
    }

    private static String keyName(int keyCode) {
        if (keyCode == -1) {
            return "UNBOUND";
        }
        String glfw = GLFW.glfwGetKeyName((int)keyCode, (int)0);
        if (glfw != null) {
            return glfw.toUpperCase(Locale.ROOT);
        }
        return switch (keyCode) {
            case 345 -> "RCTRL";
            case 341 -> "LCTRL";
            case 340 -> "LSHIFT";
            case 344 -> "RSHIFT";
            case 342 -> "LALT";
            case 346 -> "RALT";
            default -> "KEY_" + keyCode;
        };
    }

    private static String trim(TextRenderer tr, String text, int maxPx) {
        return tr.trimToWidth(text, Math.max(8, maxPx));
    }

    private static enum FocusedField {
        NONE,
        SLOT,
        ITEM,
        TIMES;

    }

    private static enum KeyCapture {
        NONE,
        FABRICATOR_TOGGLE,
        SLOT_IDS_TOGGLE;

    }
}

