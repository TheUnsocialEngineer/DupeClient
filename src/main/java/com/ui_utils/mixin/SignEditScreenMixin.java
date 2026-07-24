package com.ui_utils.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

/** Widgets are attached by {@link ScreenMixin}. */
@Mixin(SignEditScreen.class)
public abstract class SignEditScreenMixin extends Screen {
    protected SignEditScreenMixin(Component title) {
        super(title);
    }
}
