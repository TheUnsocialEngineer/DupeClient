package com.dupeclient.client.gui.panel;

import com.dupeclient.client.module.utility.crashes.CrashesManager;
import com.dupeclient.client.module.utility.crashes.CrashesSettings;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerManager;
import com.dupeclient.client.module.fuzzer.FuzzerOverlay;
import com.dupeclient.client.module.fuzzer.MinimessageFuzzerManager;
import com.dupeclient.client.module.fuzzer.SqliFuzzerManager;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerSettings;
import com.dupeclient.client.module.utility.ChatGamesOverlay;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.module.utility.ChatGamesManager;
import com.dupeclient.client.module.utility.ChatGamesSettings;
import com.dupeclient.client.module.utility.UtilityConfigManager;
import com.dupeclient.client.module.utility.UtilitySettings;
import com.dupeclient.client.module.utility.UtilitySubTab;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class UtilityPanel extends Panel {
    private static final int GAP = 5;
    private static final int SUBTAB_H = 14;
    private static final int TOGGLE_H = UiTokens.ROW_STEP;
    private static final int SLIDER_H = 16;
    private static final int KEYBIND_H = 20;

    private final CrashesManager crashesManager = CrashesManager.INSTANCE;
    private final ChatGamesManager chatGamesManager = ChatGamesManager.INSTANCE;
    private final EconomyFuzzerManager economyFuzzerManager = EconomyFuzzerManager.INSTANCE;
    private UtilitySettings utilitySettings = UtilityConfigManager.load();

    private SliderId draggingSlider = SliderId.NONE;
    private CaptureMode captureMode = CaptureMode.NONE;

    public UtilityPanel(int x, int y) {
        super("utility", Component.literal("Utility"), x, y, 320, 420);
    }

    private UtilitySubTab subTab() {
        return utilitySettings.selectedSubTab == null ? UtilitySubTab.CHAT_GAMES : utilitySettings.selectedSubTab;
    }

    private static int rowBlock(int... rowHeights) {
        int total = 0;
        for (int h : rowHeights) {
            total += h + GAP;
        }
        return total;
    }

    private static int rowY(int bodyTop, int rowIndex, int... rowHeights) {
        int y = bodyTop;
        for (int i = 0; i < rowIndex; i++) {
            y += rowHeights[i] + GAP;
        }
        return y;
    }

    private static int chatGamesCardHeight() {
        return UiTokens.CARD_CONTENT_TOP
                + rowBlock(TOGGLE_H, TOGGLE_H, KEYBIND_H, KEYBIND_H, TOGGLE_H, TOGGLE_H);
    }

    private static int chestCardHeight() {
        return UiTokens.CARD_CONTENT_TOP
                + rowBlock(TOGGLE_H, KEYBIND_H, TOGGLE_H, SLIDER_H, SLIDER_H, TOGGLE_H, TOGGLE_H);
    }

    private static int armorCardHeight() {
        return UiTokens.CARD_CONTENT_TOP
                + rowBlock(TOGGLE_H, KEYBIND_H, TOGGLE_H, SLIDER_H, SLIDER_H, SLIDER_H, SLIDER_H, TOGGLE_H, TOGGLE_H);
    }

    private static int economyFuzzerCardHeight() {
        return UiTokens.CARD_CONTENT_TOP + SUBTAB_H + GAP + rowBlock(TOGGLE_H, TOGGLE_H, KEYBIND_H, TOGGLE_H, TOGGLE_H, TOGGLE_H, TOGGLE_H);
    }

    private int contentHeight() {
        int sub = SUBTAB_H + GAP;
        if (subTab() == UtilitySubTab.CHAT_GAMES) {
            return sub + chatGamesCardHeight();
        }
        if (subTab() == UtilitySubTab.ECONOMY_FUZZER) {
            return sub + economyFuzzerCardHeight();
        }
        return sub + chestCardHeight() + GAP + armorCardHeight();
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (collapsed) {
            return;
        }
        var mc = Minecraft.getInstance();
        Font tr = mc.font;
        Layout layout = layout();
        UtilitySubTab tab = subTab();

        drawSubTabs(tr, context, layout.rx, layout.contentTop, layout.inner);

        if (tab == UtilitySubTab.CHAT_GAMES) {
            renderChatGames(tr, context, layout, delta);
        } else if (tab == UtilitySubTab.ECONOMY_FUZZER) {
            renderEconomyFuzzer(tr, context, layout, delta);
        } else {
            renderCrashes(tr, context, layout, delta);
        }

        height = bodyTopOffset() + UiTokens.UI_GAP + contentHeight() + UiTokens.SP_3;

        if (captureMode != CaptureMode.NONE) {
            context.text(
                    tr,
                    Component.literal("Press key to bind (ESC = unbind)"),
                    layout.rx,
                    layout.contentTop + contentHeight() - SUBTAB_H,
                    UiTokens.ACCENT);
        }
    }

    private void drawSubTabs(Font tr, GuiGraphicsExtractor context, int rx, int y, int inner) {
        int third = (inner - GAP * 2) / 3;
        int x0 = rx;
        int x1 = rx + third + GAP;
        int x2 = rx + (third + GAP) * 2;
        UiComponents.drawSegmentTab(tr, context, x0, y, third, SUBTAB_H, "Chat Games", subTab() == UtilitySubTab.CHAT_GAMES);
        UiComponents.drawSegmentTab(tr, context, x1, y, third, SUBTAB_H, "Crashes", subTab() == UtilitySubTab.CRASHES);
        UiComponents.drawSegmentTab(tr, context, x2, y, third, SUBTAB_H, "Fuzzer", subTab() == UtilitySubTab.ECONOMY_FUZZER);
    }

    private void renderEconomyFuzzer(Font tr, GuiGraphicsExtractor context, Layout layout, float delta) {
        EconomyFuzzerSettings s = economyFuzzerManager.getSettings();
        SqliFuzzerManager sqli = SqliFuzzerManager.INSTANCE;
        MinimessageFuzzerManager mini = MinimessageFuzzerManager.INSTANCE;
        int cardTop = layout.cardTop;
        int cardH = economyFuzzerCardHeight();
        UiComponents.drawInfoCard(tr, context, layout.tx, cardTop, layout.sw, cardH, "Fuzzer");

        int body = UiComponents.titledCardBodyY(cardTop);
        int third = (layout.inner - GAP * 2) / 3;
        UiComponents.drawSegmentTab(tr, context, layout.rx, body, third, SUBTAB_H, "Economy", "economy".equals(s.fuzzerTab));
        UiComponents.drawSegmentTab(tr, context, layout.rx + third + GAP, body, third, SUBTAB_H, "SQLi", "sqli".equals(s.fuzzerTab));
        UiComponents.drawSegmentTab(tr, context, layout.rx + (third + GAP) * 2, body, third, SUBTAB_H, "MiniMsg", "minimessage".equals(s.fuzzerTab));
        body += SUBTAB_H + GAP;

        int[] rows = {TOGGLE_H, TOGGLE_H, KEYBIND_H, TOGGLE_H, TOGGLE_H, TOGGLE_H, TOGGLE_H};
        int y = rowY(body, 0, rows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Show overlay", s.overlayVisible,
                smoothToggle("util.fuzzVis", s.overlayVisible, delta));

        y = rowY(body, 1, rows);
        drawBindRow(tr, context, layout.rx, y, layout.inner, "Overlay hotkey", s.overlayToggleKey, CaptureMode.FUZZER_OVERLAY);

        y = rowY(body, 2, rows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Chat feedback", s.moduleChatFeedback,
                smoothToggle("util.fuzzChat", s.moduleChatFeedback, delta));

        y = rowY(body, 3, rows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Disable on leave", s.disableOnLeave,
                smoothToggle("util.fuzzLeave", s.disableOnLeave, delta));

        String tab = s.fuzzerTab == null ? "economy" : s.fuzzerTab.toLowerCase(Locale.ROOT);
        y = rowY(body, 4, rows);
        if ("sqli".equals(tab)) {
            UiComponents.drawOptionToggle(tr, context, layout.rx, y, layout.inner, "SQLi running", sqli.isRunning(), smoothToggle("util.sqliRun", sqli.isRunning(), delta));
            y = rowY(body, 5, rows);
            UiComponents.drawOptionToggle(tr, context, layout.rx, y, layout.inner, "Destructive SQLi payloads", sqli.isDestructivePayloadsEnabled(), smoothToggle("util.sqliDest", sqli.isDestructivePayloadsEnabled(), delta));
            y = rowY(body, 6, rows);
            context.text(tr, Component.literal("Command: /" + (sqli.getCommand().isBlank() ? "(set in overlay)" : sqli.getCommand())), layout.rx, y + 2, UiTokens.TEXT_DIM);
        } else if ("minimessage".equals(tab)) {
            UiComponents.drawOptionToggle(tr, context, layout.rx, y, layout.inner, "MiniMessage running", mini.isRunning(), smoothToggle("util.miniRun", mini.isRunning(), delta));
            y = rowY(body, 5, rows);
            context.text(tr, Component.literal("Target: " + (mini.getTarget().isBlank() ? "(set in overlay)" : mini.getTarget())), layout.rx, y + 2, UiTokens.TEXT_DIM);
            y = rowY(body, 6, rows);
            context.text(tr, Component.literal("Send: " + mini.sendModeLabel()), layout.rx, y + 2, UiTokens.TEXT_DIM);
        } else {
            UiComponents.drawOptionToggle(
                    tr, context, layout.rx, y, layout.inner, "Economy enabled", s.enabled,
                    smoothToggle("util.fuzzOn", s.enabled, delta));
            y = rowY(body, 5, rows);
            context.text(tr, Component.literal("Pay command: /" + (s.payCommand == null || s.payCommand.isBlank() ? "pay" : s.payCommand)), layout.rx, y + 2, UiTokens.TEXT_DIM);
        }
    }

    private void renderChatGames(Font tr, GuiGraphicsExtractor context, Layout layout, float delta) {
        ChatGamesSettings s = chatGamesManager.getSettings();
        int cardTop = layout.cardTop;
        int cardH = chatGamesCardHeight();
        UiComponents.drawInfoCard(tr, context, layout.tx, cardTop, layout.sw, cardH, "Chat Games");

        int body = UiComponents.titledCardBodyY(cardTop);
        int[] rows = {TOGGLE_H, TOGGLE_H, KEYBIND_H, KEYBIND_H, TOGGLE_H, TOGGLE_H};

        int y = rowY(body, 0, rows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Enabled", s.enabled,
                smoothToggle("util.cgOn", s.enabled, delta));

        y = rowY(body, 1, rows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Show overlay", s.overlayVisible,
                smoothToggle("util.cgVis", s.overlayVisible, delta));

        y = rowY(body, 2, rows);
        drawBindRow(tr, context, layout.rx, y, layout.inner, "Overlay hotkey", s.overlayToggleKey, CaptureMode.CHAT_GAMES_OVERLAY);

        y = rowY(body, 3, rows);
        drawBindRow(tr, context, layout.rx, y, layout.inner, "Toggle hotkey", s.toggleKey, CaptureMode.CHAT_GAMES_TOGGLE);

        y = rowY(body, 4, rows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Chat feedback", s.chatFeedback,
                smoothToggle("util.cgChat", s.chatFeedback, delta));

        y = rowY(body, 5, rows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Disable on leave", s.disableOnLeave,
                smoothToggle("util.cgLeave", s.disableOnLeave, delta));
    }

    private void renderCrashes(Font tr, GuiGraphicsExtractor context, Layout layout, float delta) {
        CrashesSettings s = crashesManager.getSettings();

        int chestH = chestCardHeight();
        UiComponents.drawInfoCard(tr, context, layout.tx, layout.chestTop, layout.sw, chestH, "Chest Crash");

        int body = layout.chestBody;
        int[] chestRows = {TOGGLE_H, KEYBIND_H, TOGGLE_H, SLIDER_H, SLIDER_H, TOGGLE_H, TOGGLE_H};

        int y = rowY(body, 0, chestRows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Enabled", s.chestCrashEnabled,
                smoothToggle("util.chestOn", s.chestCrashEnabled, delta));

        y = rowY(body, 1, chestRows);
        drawBindRow(tr, context, layout.rx, y, layout.inner, "Toggle hotkey", s.chestToggleKey, CaptureMode.CHEST_TOGGLE);

        y = rowY(body, 2, chestRows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Chat feedback", s.chestChatFeedback,
                smoothToggle("util.chestChat", s.chestChatFeedback, delta));

        y = rowY(body, 3, chestRows);
        drawIntSlider(tr, context, layout.rx, y, layout.inner, s.chestRange, 1, 10, "Range", SliderId.CHEST_RANGE);

        y = rowY(body, 4, chestRows);
        drawIntSlider(tr, context, layout.rx, y, layout.inner, s.chestPackets, 0, 1000, "Packets (0=∞)", SliderId.CHEST_PACKETS);

        y = rowY(body, 5, chestRows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Only with written book", s.chestOnlyWithWrittenBook,
                smoothToggle("util.chestBook", s.chestOnlyWithWrittenBook, delta));

        y = rowY(body, 6, chestRows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Disable on leave", s.chestDisableOnDisconnect,
                smoothToggle("util.chestLeave", s.chestDisableOnDisconnect, delta));

        int armorH = armorCardHeight();
        UiComponents.drawInfoCard(tr, context, layout.tx, layout.armorTop, layout.sw, armorH, "Armor Stand Placer");

        body = layout.armorBody;
        int[] armorRows = {TOGGLE_H, KEYBIND_H, TOGGLE_H, SLIDER_H, SLIDER_H, SLIDER_H, SLIDER_H, TOGGLE_H, TOGGLE_H};

        y = rowY(body, 0, armorRows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Enabled", s.armorPlaceEnabled,
                smoothToggle("util.armorOn", s.armorPlaceEnabled, delta));

        y = rowY(body, 1, armorRows);
        drawBindRow(tr, context, layout.rx, y, layout.inner, "Toggle hotkey", s.armorToggleKey, CaptureMode.ARMOR_TOGGLE);

        y = rowY(body, 2, armorRows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Chat feedback", s.armorChatFeedback,
                smoothToggle("util.armorChat", s.armorChatFeedback, delta));

        y = rowY(body, 3, armorRows);
        drawIntSlider(tr, context, layout.rx, y, layout.inner, s.armorDelay, 0, 10, "Delay (ticks)", SliderId.ARMOR_DELAY);

        y = rowY(body, 4, armorRows);
        drawIntSlider(tr, context, layout.rx, y, layout.inner, s.armorPacketsPerTick, 1, 150, "Packets/tick", SliderId.ARMOR_PPT);

        y = rowY(body, 5, armorRows);
        drawIntSlider(tr, context, layout.rx, y, layout.inner, s.armorLength, 1, 6, "Length", SliderId.ARMOR_LENGTH);

        y = rowY(body, 6, armorRows);
        drawIntSlider(tr, context, layout.rx, y, layout.inner, s.armorVerticality, 1, 6, "Verticality", SliderId.ARMOR_VERT);

        y = rowY(body, 7, armorRows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Disable on empty", s.armorDisableOnEmpty,
                smoothToggle("util.armorEmpty", s.armorDisableOnEmpty, delta));

        y = rowY(body, 8, armorRows);
        UiComponents.drawOptionToggle(
                tr, context, layout.rx, y, layout.inner, "Disable on leave", s.armorDisableOnLeave,
                smoothToggle("util.armorLeave", s.armorDisableOnLeave, delta));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (collapsed || button != 0) {
            return false;
        }
        Layout layout = layout();
        if (clickSubTab(mouseX, mouseY, layout.rx, layout.contentTop, layout.inner, UtilitySubTab.CHAT_GAMES)) {
            utilitySettings.selectedSubTab = UtilitySubTab.CHAT_GAMES;
            UtilityConfigManager.save(utilitySettings);
            return true;
        }
        if (clickSubTab(mouseX, mouseY, layout.rx, layout.contentTop, layout.inner, UtilitySubTab.CRASHES)) {
            utilitySettings.selectedSubTab = UtilitySubTab.CRASHES;
            UtilityConfigManager.save(utilitySettings);
            return true;
        }
        if (clickSubTab(mouseX, mouseY, layout.rx, layout.contentTop, layout.inner, UtilitySubTab.ECONOMY_FUZZER)) {
            utilitySettings.selectedSubTab = UtilitySubTab.ECONOMY_FUZZER;
            UtilityConfigManager.save(utilitySettings);
            return true;
        }

        if (subTab() == UtilitySubTab.CHAT_GAMES) {
            return clickChatGames(mouseX, mouseY, layout);
        }
        if (subTab() == UtilitySubTab.ECONOMY_FUZZER) {
            return clickEconomyFuzzer(mouseX, mouseY, layout);
        }
        return clickCrashes(mouseX, mouseY, layout);
    }

    private boolean clickSubTab(double mx, double my, int rx, int y, int inner, UtilitySubTab tab) {
        int third = (inner - GAP * 2) / 3;
        int x = switch (tab) {
            case CHAT_GAMES -> rx;
            case CRASHES -> rx + third + GAP;
            case ECONOMY_FUZZER -> rx + (third + GAP) * 2;
        };
        return rect(mx, my, x, y, third, SUBTAB_H);
    }

    private boolean clickEconomyFuzzer(double mouseX, double mouseY, Layout layout) {
        EconomyFuzzerSettings s = economyFuzzerManager.getSettings();
        SqliFuzzerManager sqli = SqliFuzzerManager.INSTANCE;
        MinimessageFuzzerManager mini = MinimessageFuzzerManager.INSTANCE;
        int body = UiComponents.titledCardBodyY(layout.cardTop);
        int third = (layout.inner - GAP * 2) / 3;
        if (rect(mouseX, mouseY, layout.rx, body, third, SUBTAB_H)) {
            s.fuzzerTab = "economy";
            economyFuzzerManager.save();
            return true;
        }
        if (rect(mouseX, mouseY, layout.rx + third + GAP, body, third, SUBTAB_H)) {
            s.fuzzerTab = "sqli";
            economyFuzzerManager.save();
            return true;
        }
        if (rect(mouseX, mouseY, layout.rx + (third + GAP) * 2, body, third, SUBTAB_H)) {
            s.fuzzerTab = "minimessage";
            economyFuzzerManager.save();
            return true;
        }
        body += SUBTAB_H + GAP;
        int[] rows = {TOGGLE_H, TOGGLE_H, KEYBIND_H, TOGGLE_H, TOGGLE_H, TOGGLE_H, TOGGLE_H};

        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 0, rows), layout.inner)) {
            FuzzerOverlay.INSTANCE.toggleOverlayVisible();
            economyFuzzerManager.feedback("Fuzzer overlay " + (s.overlayVisible ? "shown" : "hidden"));
            return true;
        }
        if (clickBindValue(mouseX, mouseY, layout.rx, rowY(body, 1, rows), layout.inner)) {
            captureMode = CaptureMode.FUZZER_OVERLAY;
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 2, rows), layout.inner)) {
            s.moduleChatFeedback = !s.moduleChatFeedback;
            economyFuzzerManager.save();
            economyFuzzerManager.feedback("Chat feedback " + (s.moduleChatFeedback ? "on" : "off"));
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 3, rows), layout.inner)) {
            s.disableOnLeave = !s.disableOnLeave;
            economyFuzzerManager.save();
            return true;
        }
        String tab = s.fuzzerTab == null ? "economy" : s.fuzzerTab.toLowerCase(Locale.ROOT);
        if ("sqli".equals(tab)) {
            if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 4, rows), layout.inner)) {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null) {
                    if (sqli.isRunning()) {
                        sqli.stop("Stopped from hub.");
                    } else {
                        sqli.start(mc);
                    }
                }
                return true;
            }
            if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 5, rows), layout.inner)) {
                sqli.setDestructivePayloadsEnabled(!sqli.isDestructivePayloadsEnabled());
                return true;
            }
        } else if ("minimessage".equals(tab)) {
            if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 4, rows), layout.inner)) {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null) {
                    if (mini.isRunning()) {
                        mini.stop("Stopped from hub.");
                    } else {
                        mini.start(mc);
                    }
                }
                return true;
            }
        } else if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 4, rows), layout.inner)) {
            s.enabled = !s.enabled;
            if (!s.enabled) {
                economyFuzzerManager.stop("Disabled.");
                FuzzerOverlay.INSTANCE.setOverlayVisible(false);
            }
            economyFuzzerManager.save();
            economyFuzzerManager.feedback("Economy fuzzer " + (s.enabled ? "enabled" : "disabled"));
            return true;
        }
        return false;
    }

    private boolean clickChatGames(double mouseX, double mouseY, Layout layout) {
        ChatGamesSettings s = chatGamesManager.getSettings();
        int body = UiComponents.titledCardBodyY(layout.cardTop);
        int[] rows = {TOGGLE_H, TOGGLE_H, KEYBIND_H, KEYBIND_H, TOGGLE_H, TOGGLE_H};

        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 0, rows), layout.inner)) {
            chatGamesManager.setEnabled(!s.enabled);
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 1, rows), layout.inner)) {
            ChatGamesOverlay.INSTANCE.toggleOverlayVisible();
            chatGamesManager.feedbackConfigToggle("Chat Games overlay " + (s.overlayVisible ? "shown" : "hidden"));
            return true;
        }
        if (clickBindValue(mouseX, mouseY, layout.rx, rowY(body, 2, rows), layout.inner)) {
            captureMode = CaptureMode.CHAT_GAMES_OVERLAY;
            return true;
        }
        if (clickBindValue(mouseX, mouseY, layout.rx, rowY(body, 3, rows), layout.inner)) {
            captureMode = CaptureMode.CHAT_GAMES_TOGGLE;
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 4, rows), layout.inner)) {
            s.chatFeedback = !s.chatFeedback;
            chatGamesManager.save();
            chatGamesManager.feedbackConfigToggle("Chat feedback " + (s.chatFeedback ? "on" : "off"));
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 5, rows), layout.inner)) {
            s.disableOnLeave = !s.disableOnLeave;
            chatGamesManager.save();
            return true;
        }
        return false;
    }

    private boolean clickCrashes(double mouseX, double mouseY, Layout layout) {
        CrashesSettings s = crashesManager.getSettings();
        int[] chestRows = {TOGGLE_H, KEYBIND_H, TOGGLE_H, SLIDER_H, SLIDER_H, TOGGLE_H, TOGGLE_H};
        int[] armorRows = {TOGGLE_H, KEYBIND_H, TOGGLE_H, SLIDER_H, SLIDER_H, SLIDER_H, SLIDER_H, TOGGLE_H};
        int body = layout.chestBody;

        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 0, chestRows), layout.inner)) {
            crashesManager.setChestCrashEnabled(!s.chestCrashEnabled);
            return true;
        }
        if (clickBindValue(mouseX, mouseY, layout.rx, rowY(body, 1, chestRows), layout.inner)) {
            captureMode = CaptureMode.CHEST_TOGGLE;
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 2, chestRows), layout.inner)) {
            s.chestChatFeedback = !s.chestChatFeedback;
            crashesManager.save();
            crashesManager.feedbackChestConfigToggle("Chat feedback " + (s.chestChatFeedback ? "on" : "off"));
            return true;
        }
        if (clickSlider(mouseX, mouseY, layout.rx, rowY(body, 3, chestRows), layout.inner, 1, 10, SliderId.CHEST_RANGE)) {
            return true;
        }
        if (clickSlider(mouseX, mouseY, layout.rx, rowY(body, 4, chestRows), layout.inner, 0, 1000, SliderId.CHEST_PACKETS)) {
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 5, chestRows), layout.inner)) {
            s.chestOnlyWithWrittenBook = !s.chestOnlyWithWrittenBook;
            crashesManager.save();
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 6, chestRows), layout.inner)) {
            s.chestDisableOnDisconnect = !s.chestDisableOnDisconnect;
            crashesManager.save();
            return true;
        }

        body = layout.armorBody;
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 0, armorRows), layout.inner)) {
            crashesManager.setArmorPlaceEnabled(!s.armorPlaceEnabled);
            return true;
        }
        if (clickBindValue(mouseX, mouseY, layout.rx, rowY(body, 1, armorRows), layout.inner)) {
            captureMode = CaptureMode.ARMOR_TOGGLE;
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 2, armorRows), layout.inner)) {
            s.armorChatFeedback = !s.armorChatFeedback;
            crashesManager.save();
            crashesManager.feedbackArmorConfigToggle("Chat feedback " + (s.armorChatFeedback ? "on" : "off"));
            return true;
        }
        if (clickSlider(mouseX, mouseY, layout.rx, rowY(body, 3, armorRows), layout.inner, 0, 10, SliderId.ARMOR_DELAY)) {
            return true;
        }
        if (clickSlider(mouseX, mouseY, layout.rx, rowY(body, 4, armorRows), layout.inner, 1, 150, SliderId.ARMOR_PPT)) {
            return true;
        }
        if (clickSlider(mouseX, mouseY, layout.rx, rowY(body, 5, armorRows), layout.inner, 1, 6, SliderId.ARMOR_LENGTH)) {
            return true;
        }
        if (clickSlider(mouseX, mouseY, layout.rx, rowY(body, 6, armorRows), layout.inner, 1, 6, SliderId.ARMOR_VERT)) {
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 7, armorRows), layout.inner)) {
            s.armorDisableOnEmpty = !s.armorDisableOnEmpty;
            crashesManager.save();
            return true;
        }
        if (clickToggle(mouseX, mouseY, layout.rx, rowY(body, 8, armorRows), layout.inner)) {
            s.armorDisableOnLeave = !s.armorDisableOnLeave;
            crashesManager.save();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (super.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        if (draggingSlider == SliderId.NONE || button != 0) {
            return false;
        }
        applySlider(mouseX, draggingSlider);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSlider = SliderId.NONE;
        }
        super.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (captureMode == CaptureMode.NONE) {
            return false;
        }
        int key = keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
        CaptureMode finished = captureMode;
        captureMode = CaptureMode.NONE;
        switch (finished) {
            case CHAT_GAMES_OVERLAY -> {
                ChatGamesSettings cg = chatGamesManager.getSettings();
                cg.overlayToggleKey = key;
                chatGamesManager.save();
                chatGamesManager.feedbackConfigToggle("Overlay hotkey → " + keyName(key));
            }
            case CHAT_GAMES_TOGGLE -> {
                ChatGamesSettings cg = chatGamesManager.getSettings();
                cg.toggleKey = key;
                chatGamesManager.save();
                chatGamesManager.feedbackConfigToggle("Toggle hotkey → " + keyName(key));
            }
            case FUZZER_OVERLAY -> {
                EconomyFuzzerSettings fs = economyFuzzerManager.getSettings();
                fs.overlayToggleKey = key;
                economyFuzzerManager.save();
                economyFuzzerManager.feedback("Overlay hotkey → " + keyName(key));
            }
            case CHEST_TOGGLE -> {
                CrashesSettings cs = crashesManager.getSettings();
                cs.chestToggleKey = key;
                crashesManager.save();
                crashesManager.feedbackChestConfigToggle("Toggle hotkey → " + keyName(key));
            }
            case ARMOR_TOGGLE -> {
                CrashesSettings cs = crashesManager.getSettings();
                cs.armorToggleKey = key;
                crashesManager.save();
                crashesManager.feedbackArmorConfigToggle("Toggle hotkey → " + keyName(key));
            }
            default -> {
            }
        }
        return true;
    }

    @Override
    public void onModuleHidden() {
        captureMode = CaptureMode.NONE;
    }

    @Override
    public boolean hasFocusedTextInput() {
        return isVisible() && captureMode != CaptureMode.NONE;
    }

    private Layout layout() {
        int tx = x + UiTokens.BODY_INSET;
        int ty = y + bodyTopOffset() + UiTokens.UI_GAP;
        int sw = width - UiTokens.BODY_INSET * 2;
        int inner = sw - UiTokens.SP_4;
        int rx = tx + UiTokens.SP_2;
        int contentTop = ty;
        int cardTop = contentTop + SUBTAB_H + GAP;
        int chestTop = cardTop;
        int chestBody = UiComponents.titledCardBodyY(chestTop);
        int armorTop = chestTop + chestCardHeight() + GAP;
        int armorBody = UiComponents.titledCardBodyY(armorTop);
        return new Layout(tx, sw, inner, rx, contentTop, cardTop, chestTop, chestBody, armorTop, armorBody);
    }

    private void drawBindRow(
            Font tr,
            GuiGraphicsExtractor context,
            int x,
            int y,
            int w,
            String label,
            int keyCode,
            CaptureMode mode) {
        boolean listening = captureMode == mode;
        UiComponents.drawPillKeybind(
                tr, context, x, y, w, KEYBIND_H, label,
                listening ? "Press key..." : keyName(keyCode), listening);
    }

    private void drawIntSlider(
            Font tr,
            GuiGraphicsExtractor context,
            int x,
            int y,
            int w,
            int value,
            int min,
            int max,
            String label,
            SliderId id) {
        UiComponents.drawLabeledValueSlider(
                tr, context, x, y, w, value, min, max, label, 86, 40,
                draggingSlider == id,
                id == SliderId.CHEST_PACKETS && value == 0 ? "∞" : String.valueOf(value));
    }

    private boolean clickSlider(double mouseX, double mouseY, int x, int y, int w, int min, int max, SliderId id) {
        int barX = x + 86;
        int barW = w - 92 - 40;
        if (!rect(mouseX, mouseY, barX, y + 1, barW, 8)) {
            return false;
        }
        draggingSlider = id;
        applySliderValue(mouseX, barX, barW, min, max, id);
        saveForSlider(id);
        return true;
    }

    private void applySlider(double mouseX, SliderId id) {
        Layout layout = layout();
        int barX = layout.rx + 86;
        int barW = layout.inner - 92 - 40;
        switch (id) {
            case CHAT_COOLDOWN -> applySliderValue(mouseX, barX, barW, 1, 30, id);
            case CHEST_RANGE -> applySliderValue(mouseX, barX, barW, 1, 10, id);
            case CHEST_PACKETS -> applySliderValue(mouseX, barX, barW, 0, 1000, id);
            case ARMOR_DELAY -> applySliderValue(mouseX, barX, barW, 0, 10, id);
            case ARMOR_PPT -> applySliderValue(mouseX, barX, barW, 1, 150, id);
            case ARMOR_LENGTH -> applySliderValue(mouseX, barX, barW, 1, 6, id);
            case ARMOR_VERT -> applySliderValue(mouseX, barX, barW, 1, 6, id);
            default -> {
            }
        }
        saveForSlider(id);
    }

    private void applySliderValue(double mouseX, int barX, int barW, int min, int max, SliderId id) {
        int value = (int) Math.round(sliderValue(mouseX, barX, barW, min, max));
        switch (id) {
            case CHAT_COOLDOWN -> chatGamesManager.getSettings().cooldownSeconds = value;
            case CHEST_RANGE -> crashesManager.getSettings().chestRange = value;
            case CHEST_PACKETS -> crashesManager.getSettings().chestPackets = value;
            case ARMOR_DELAY -> crashesManager.getSettings().armorDelay = value;
            case ARMOR_PPT -> crashesManager.getSettings().armorPacketsPerTick = value;
            case ARMOR_LENGTH -> crashesManager.getSettings().armorLength = value;
            case ARMOR_VERT -> crashesManager.getSettings().armorVerticality = value;
            default -> {
            }
        }
    }

    private void saveForSlider(SliderId id) {
        if (id == SliderId.CHAT_COOLDOWN) {
            chatGamesManager.save();
        } else {
            crashesManager.save();
        }
    }

    private static double sliderValue(double mouseX, int x, int w, double min, double max) {
        double t = (mouseX - x) / w;
        t = Math.max(0.0, Math.min(1.0, t));
        return min + (max - min) * t;
    }

    private static boolean rect(double mouseX, double mouseY, int sx, int sy, int sw, int sh) {
        return mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + sh;
    }

    private boolean clickToggle(double mx, double my, int sx, int sy, int inner) {
        return rect(mx, my, sx, sy, inner, TOGGLE_H);
    }

    private static boolean clickBindValue(double mouseX, double mouseY, int x, int y, int w) {
        int bindW = 98;
        int labelW = w - bindW - UiTokens.SP_2;
        int bx = x + labelW + UiTokens.SP_2;
        return rect(mouseX, mouseY, bx, y, bindW, KEYBIND_H);
    }

    private static String keyName(int keyCode) {
        if (keyCode < 0 || keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return "UNBOUND";
        }
        String glfw = GLFW.glfwGetKeyName(keyCode, 0);
        if (glfw != null) {
            return glfw.toUpperCase(Locale.ROOT);
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            default -> "KEY_" + keyCode;
        };
    }

    private record Layout(
            int tx, int sw, int inner, int rx,
            int contentTop, int cardTop,
            int chestTop, int chestBody, int armorTop, int armorBody) {
    }

    private enum SliderId {
        NONE,
        CHAT_COOLDOWN,
        CHEST_RANGE,
        CHEST_PACKETS,
        ARMOR_DELAY,
        ARMOR_PPT,
        ARMOR_LENGTH,
        ARMOR_VERT
    }

    private enum CaptureMode {
        NONE,
        CHAT_GAMES_OVERLAY,
        CHAT_GAMES_TOGGLE,
        FUZZER_OVERLAY,
        CHEST_TOGGLE,
        ARMOR_TOGGLE
    }
}
