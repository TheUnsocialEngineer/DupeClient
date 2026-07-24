package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Repositions the recipe-book toggle button when {@link HandledScreenGuiScale} is active.
 * Vanilla places the button using unscaled {@code x + offset}, which overlaps scaled slots.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class RecipeBookScreenGuiScaleMixin {
    private static final int BOOK_BUTTON_W = 20;
    private static final int BOOK_BUTTON_H = 18;

    @Shadow
    protected abstract ScreenPosition getRecipeBookButtonPosition();

    @Unique
    private ImageButton dupeclient$recipeBookButton() {
        AbstractRecipeBookScreen self = (AbstractRecipeBookScreen) (Object) this;
        for (GuiEventListener child : self.children()) {
            if (child instanceof ImageButton button) {
                return button;
            }
        }
        return null;
    }

    @Unique
    private void dupeclient$layoutRecipeBookButton() {
        if (!HandledScreenGuiScale.isActive()) {
            return;
        }
        ImageButton button = dupeclient$recipeBookButton();
        if (button == null) {
            return;
        }
        HandledScreenAccessor gui = (HandledScreenAccessor) this;
        ScreenPosition pos = getRecipeBookButtonPosition();
        int localX = pos.x() - gui.getX();
        int localY = pos.y() - gui.getY();
        HandledScreenGuiScale.layoutWidget(
                button,
                localX,
                localY,
                BOOK_BUTTON_W,
                BOOK_BUTTON_H,
                gui.getX(),
                gui.getY(),
                gui.getImageWidth(),
                gui.getImageHeight());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dupeclient$layoutRecipeBookOnInit(CallbackInfo ci) {
        dupeclient$layoutRecipeBookButton();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void dupeclient$layoutRecipeBookBeforeRender(
            GuiGraphics context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci) {
        dupeclient$layoutRecipeBookButton();
    }
}
