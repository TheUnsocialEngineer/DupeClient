package com.ui_utils.gui;

import com.dupeclient.client.core.KeyboardConsumingScreen;
import com.dupeclient.client.multiplayer.SessionManager;
import com.ui_utils.SessionUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;

public class UsernameScreen extends Screen implements KeyboardConsumingScreen {
    private final Screen parent;
    private final MinecraftClient mc;
    private TextFieldWidget usernameField;

    public UsernameScreen(Screen parent, MinecraftClient mc) {
        super(Text.literal("Set Username"));
        this.parent = parent;
        this.mc = mc;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.usernameField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 20, 200, 20, Text.literal("Username"));
        this.usernameField.setText(this.mc.getSession().getUsername());
        this.usernameField.setMaxLength(16);
        this.addSelectableChild(this.usernameField);
        this.setInitialFocus(this.usernameField);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), button -> {
                    String newName = this.usernameField.getText().trim();
                    if (!newName.isEmpty()) {
                        Session oldSession = this.mc.getSession();
                        Session newSession = SessionUtils.copyWith(oldSession, newName, null);
                        SessionManager.setSession(newSession);
                    }
                    this.mc.setScreen(this.parent);
                })
                .dimensions(centerX - 100, centerY + 10, 95, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> this.mc.setScreen(this.parent))
                .dimensions(centerX + 5, centerY + 10, 95, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        this.usernameField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean consumesGlobalHotkeys() {
        return true;
    }
}
