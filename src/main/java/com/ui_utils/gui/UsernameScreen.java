package com.ui_utils.gui;

import com.dupeclient.client.core.KeyboardConsumingScreen;
import com.dupeclient.client.multiplayer.SessionManager;
import com.ui_utils.SessionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UsernameScreen extends Screen implements KeyboardConsumingScreen {
    private final Screen parent;
    private final Minecraft mc;
    private EditBox usernameField;

    public UsernameScreen(Screen parent, Minecraft mc) {
        super(Component.literal("Set Username"));
        this.parent = parent;
        this.mc = mc;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.usernameField = new EditBox(this.font, centerX - 100, centerY - 20, 200, 20, Component.literal("Username"));
        this.usernameField.setValue(this.mc.getUser().getName());
        this.usernameField.setMaxLength(16);
        this.addRenderableWidget(this.usernameField);
        this.setInitialFocus(this.usernameField);
        this.addRenderableWidget(Button.builder(Component.literal("Apply"), button -> {
                    String newName = this.usernameField.getValue().trim();
                    if (!newName.isEmpty()) {
                        User oldSession = this.mc.getUser();
                        User newSession = SessionUtils.copyWith(oldSession, newName, null);
                        SessionManager.setSession(newSession);
                    }
                    this.mc.gui.setScreen(this.parent);
                })
                .bounds(centerX - 100, centerY + 10, 95, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.mc.gui.setScreen(this.parent))
                .bounds(centerX + 5, centerY + 10, 95, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
    }

    @Override
    public boolean consumesGlobalHotkeys() {
        return true;
    }
}
