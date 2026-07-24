package com.dupeclient.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Accessor
    int getX();

    @Accessor
    int getY();

    @Accessor
    int getImageWidth();

    @Accessor
    int getImageHeight();

    @Mutable
    @Accessor
    void setX(int x);

    @Mutable
    @Accessor
    void setY(int y);

    @Accessor
    AbstractContainerMenu getMenu();

    @Mutable
    @Accessor
    void setLastQuickMoved(ItemStack stack);
}
