package com.ui_utils.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

/** Widgets are attached by {@link ScreenMixin}. */
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    protected BookEditScreenMixin(Component title) {
        super(title);
    }
}
