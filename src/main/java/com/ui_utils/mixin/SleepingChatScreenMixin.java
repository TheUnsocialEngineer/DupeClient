package com.ui_utils.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

/** Widgets are attached by {@link ScreenMixin}. */
@Mixin(SleepingChatScreen.class)
public abstract class SleepingChatScreenMixin extends Screen {
    protected SleepingChatScreenMixin(Text title) {
        super(title);
    }
}
