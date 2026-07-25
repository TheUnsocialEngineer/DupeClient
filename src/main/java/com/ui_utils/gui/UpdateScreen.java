package com.ui_utils.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class UpdateScreen extends Screen {

    public UpdateScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        Component message1 = Component.nullToEmpty("In order to update UI-Utils, first quit the game then");
        Component message2 = Component.nullToEmpty("delete the old UI-Utils jar file, and replace it with the new one you got on the website.");
        int centerX = this.width / 2;

        this.addRenderableWidget(new StringWidget(centerX - font.width(message1) / 2, 80, font.width(message1), 20, message1, this.font));
        this.addRenderableWidget(new StringWidget(centerX - font.width(message2) / 2, 95, font.width(message2), 20, Component.translationArg(message2), this.font));

        int quitX = centerX - 85;
        int backX = centerX + 5;

        this.addRenderableWidget(Button.builder(Component.nullToEmpty("Quit"), (button) -> {
            this.minecraft.stop();
        }).width(80).pos(quitX, 145).build());

        this.addRenderableWidget(Button.builder(Component.nullToEmpty("Back"), (button) -> {
            this.minecraft.gui.setScreen(null);
        }).width(80).pos(backX, 145).build());
    }

}