package com.ui_utils.mixin;

import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

/** Widgets are attached by {@link ScreenMixin}. */
@Mixin(InBedChatScreen.class)
public abstract class SleepingChatScreenMixin extends Screen {
    protected SleepingChatScreenMixin(Component title) {
        super(title);
    }
}
