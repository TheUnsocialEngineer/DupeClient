package com.ui_utils.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

/** Widgets are attached by {@link ScreenMixin}. */
@Mixin(SignEditScreen.class)
public abstract class SignEditScreenMixin extends Screen {
    protected SignEditScreenMixin(Text title) {
        super(title);
    }
}
