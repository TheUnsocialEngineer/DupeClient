package com.dupeclient.client.mixin;

import com.dupeclient.client.gui.HandledScreenGuiScale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
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
@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookScreenGuiScaleMixin {
    private static final int BOOK_BUTTON_W = 20;
    private static final int BOOK_BUTTON_H = 18;

    @Shadow
    protected abstract ScreenPos getRecipeBookButtonPos();

    @Unique
    private TexturedButtonWidget dupeclient$recipeBookButton() {
        RecipeBookScreen self = (RecipeBookScreen) (Object) this;
        for (Element child : self.children()) {
            if (child instanceof TexturedButtonWidget button) {
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
        TexturedButtonWidget button = dupeclient$recipeBookButton();
        if (button == null) {
            return;
        }
        HandledScreenAccessor gui = (HandledScreenAccessor) this;
        ScreenPos pos = getRecipeBookButtonPos();
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
                gui.getBackgroundWidth(),
                gui.getBackgroundHeight());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dupeclient$layoutRecipeBookOnInit(CallbackInfo ci) {
        dupeclient$layoutRecipeBookButton();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void dupeclient$layoutRecipeBookBeforeRender(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci) {
        dupeclient$layoutRecipeBookButton();
    }
}
