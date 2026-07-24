package com.ui_utils.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

/** Widgets are attached by {@link ScreenMixin}. */
@Mixin(BookViewScreen.class)
public abstract class BookScreenMixin extends Screen {
    protected BookScreenMixin(Component title) {
        super(title);
    }
}
