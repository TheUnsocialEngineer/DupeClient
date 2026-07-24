package com.ui_utils.mixin;

import com.ui_utils.UiUtilsScreens;
import com.ui_utils.gui.CustomTextFieldWidget;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HandledScreen no longer exposes {@code charTyped} for mixin injection in 1.21.11; route typed
 * characters to the UI Utils chat field from {@link KeyboardHandler#charTyped} instead.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void uiutils$onChar(long window, CharacterEvent input, CallbackInfo ci) {
        if (!(this.minecraft.screen instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        if (!UiUtilsScreens.shouldAttachWidgets(screen)) {
            return;
        }
        for (var child : screen.children()) {
            if (child instanceof CustomTextFieldWidget field && field.isFocused()) {
                if (field.charTyped(input)) {
                    ci.cancel();
                }
                return;
            }
        }
    }
}
