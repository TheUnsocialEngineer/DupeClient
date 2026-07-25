package com.dupeclient.client.multiplayer;

import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SsidLoginScreen extends Screen implements MultiplayerNavigable {
    private static final int PANEL_TOP = 24;

    private final Screen parent;
    private StylishTextFieldWidget sessionField;
    private Text status = Text.literal("Paste a Microsoft session token (SSID).");
    private int panelX;
    private int panelW;
    private int innerX;
    private int innerW;

    public SsidLoginScreen(Screen parent) {
        super(Text.literal("SSID Login"));
        this.parent = parent;
    }

    @Override
    public Screen getNavigationParent() {
        return parent;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        SessionManager.initialize();

        panelW = Math.min(440, width - 40);
        panelX = (width - panelW) / 2;
        innerX = panelX + 16;
        innerW = panelW - 32;

        int fieldY = PANEL_TOP + 58;
        sessionField = StylishTextFieldWidget.create(textRenderer, innerX, fieldY, innerW, Text.literal("Session token"));
        sessionField.setMaxLength(32767);
        sessionField.setPlaceholder("Bearer token / SSID");
        addDrawableChild(sessionField);
        setInitialFocus(sessionField);

        addDrawableChild(new StylishButtonWidget(innerX, fieldY + 28, innerW / 2 - 4, 20, Text.literal("Login"), this::login));
        addDrawableChild(new StylishButtonWidget(innerX + innerW / 2 + 4, fieldY + 28, innerW / 2 - 4, 20, Text.literal("Restore"), () -> {
            SessionManager.restoreSession();
            status = Text.literal("Restored original session.").formatted(Formatting.GREEN);
            init();
        }));
        addDrawableChild(new StylishButtonWidget(width / 2 - 100, height - 28, 200, 20, ScreenTexts.BACK, () ->
                MultiplayerScreens.returnToMultiplayer(client, parent)));
    }

    private void login() {
        String token = sessionField.getText().trim();
        if (token.isEmpty()) {
            status = Text.literal("Session token cannot be empty.").formatted(Formatting.RED);
            return;
        }
        String[] profile = SessionAPI.getProfileInfo(token);
        if (profile == null) {
            status = Text.literal("Invalid or expired session token.").formatted(Formatting.RED);
            return;
        }
        SessionManager.setSession(SessionManager.createSession(profile[0], profile[1], token));
        status = Text.literal("Logged in as " + profile[0]).formatted(Formatting.GREEN);
    }

    @Override
    public void close() {
        MultiplayerScreens.returnToMultiplayer(client, parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        UiDraw.fillMidnightBackground(context, width, height);
        int panelH = height - 48;
        UiDraw.cardElevated(context, panelX, PANEL_TOP, panelW, panelH, 10);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, PANEL_TOP + 10, 0xFFE8EEF8);
        context.drawTextWithShadow(textRenderer, Text.literal("Current: "), innerX, PANEL_TOP + 28, 0xFF8FA3B8);
        String current = client != null ? client.getSession().getUsername() : "";
        context.drawTextWithShadow(textRenderer, Text.literal(current), innerX + textRenderer.getWidth("Current: "), PANEL_TOP + 28, 0xFF4ADE80);
        context.drawTextWithShadow(textRenderer, status, innerX, PANEL_TOP + 48, UiTokens.argb(0xFF, 0xAFC7FF));

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (sessionField.keyPressed(input) || sessionField.isActive()) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (sessionField.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }
}
