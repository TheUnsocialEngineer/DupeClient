package com.dupeclient.client.multiplayer;

import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class SsidLoginScreen extends Screen {
    private static final int PANEL_TOP = 24;

    private final Screen parent;
    private StylishTextFieldWidget sessionField;
    private Component status = Component.literal("Paste a Microsoft session token (SSID).");
    private int panelX;
    private int panelW;
    private int innerX;
    private int innerW;

    public SsidLoginScreen(Screen parent) {
        super(Component.literal("SSID Login"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        SessionManager.initialize();

        panelW = Math.min(440, width - 40);
        panelX = (width - panelW) / 2;
        innerX = panelX + 16;
        innerW = panelW - 32;

        int fieldY = PANEL_TOP + 58;
        sessionField = StylishTextFieldWidget.create(font, innerX, fieldY, innerW, Component.literal("Session token"));
        sessionField.setMaxLength(32767);
        sessionField.setPlaceholder("Bearer token / SSID");
        addRenderableWidget(sessionField);
        setInitialFocus(sessionField);

        addRenderableWidget(new StylishButtonWidget(innerX, fieldY + 28, innerW / 2 - 4, 20, Component.literal("Login"), this::login));
        addRenderableWidget(new StylishButtonWidget(innerX + innerW / 2 + 4, fieldY + 28, innerW / 2 - 4, 20, Component.literal("Restore"), () -> {
            SessionManager.restoreSession();
            status = Component.literal("Restored original session.").withStyle(ChatFormatting.GREEN);
            init();
        }));
        addRenderableWidget(new StylishButtonWidget(width / 2 - 100, height - 28, 200, 20, CommonComponents.GUI_BACK, () ->
                MultiplayerScreens.returnToMultiplayer(minecraft, parent)));
    }

    private void login() {
        String token = sessionField.getValue().trim();
        if (token.isEmpty()) {
            status = Component.literal("Session token cannot be empty.").withStyle(ChatFormatting.RED);
            return;
        }
        String[] profile = SessionAPI.getProfileInfo(token);
        if (profile == null) {
            status = Component.literal("Invalid or expired session token.").withStyle(ChatFormatting.RED);
            return;
        }
        SessionManager.setSession(SessionManager.createSession(profile[0], profile[1], token));
        status = Component.literal("Logged in as " + profile[0]).withStyle(ChatFormatting.GREEN);
    }

    @Override
    public void onClose() {
        MultiplayerScreens.returnToMultiplayer(minecraft, parent);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        UiDraw.fillMidnightBackground(context, width, height);
        int panelH = height - 48;
        UiDraw.cardElevated(context, panelX, PANEL_TOP, panelW, panelH, 10);

        context.drawCenteredString(font, title, width / 2, PANEL_TOP + 10, 0xFFE8EEF8);
        context.drawString(font, Component.literal("Current: "), innerX, PANEL_TOP + 28, 0xFF8FA3B8);
        String current = minecraft != null ? minecraft.getUser().getName() : "";
        context.drawString(font, Component.literal(current), innerX + font.width("Current: "), PANEL_TOP + 28, 0xFF4ADE80);
        context.drawString(font, status, innerX, PANEL_TOP + 48, UiTokens.argb(0xFF, 0xAFC7FF));

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (sessionField.keyPressed(input) || sessionField.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (sessionField.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }
}
