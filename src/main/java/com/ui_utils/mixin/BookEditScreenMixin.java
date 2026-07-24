package com.ui_utils.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

/** Widgets are attached by {@link ScreenMixin}. */
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    protected BookEditScreenMixin(Text title) {
        super(title);
    }
}
