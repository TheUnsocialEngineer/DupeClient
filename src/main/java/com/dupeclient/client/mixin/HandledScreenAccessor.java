package com.dupeclient.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor
    int getX();

    @Accessor
    int getY();

    @Accessor
    int getBackgroundWidth();

    @Accessor
    int getBackgroundHeight();

    @Mutable
    @Accessor
    void setX(int x);

    @Mutable
    @Accessor
    void setY(int y);

    @Accessor
    ScreenHandler getHandler();

    @Mutable
    @Accessor
    void setQuickMovingStack(ItemStack stack);
}
