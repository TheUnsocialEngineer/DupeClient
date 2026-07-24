package com.ui_utils.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

/** Widgets are attached by {@link ScreenMixin}. */
@Mixin(BookScreen.class)
public abstract class BookScreenMixin extends Screen {
    protected BookScreenMixin(Text title) {
        super(title);
    }
}
