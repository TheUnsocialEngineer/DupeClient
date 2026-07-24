package com.ui_utils.mixin;

import com.ui_utils.MainClient;
import com.ui_utils.UiUtilsScreens;
import com.ui_utils.gui.ChatTextFieldWidget;
import com.ui_utils.gui.CustomTextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inventory / container screens: widgets from {@link ScreenMixin}; keyboard routing for the overlay chat field.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {
    private HandledScreenMixin() {
        super(null);
    }

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int button, ContainerInput actionType);

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Unique
    private static final Minecraft mc = Minecraft.getInstance();

    @Unique
    private CustomTextFieldWidget uiutils$chatField;

    @Inject(at = @At("TAIL"), method = "init")
    public void uiutils$findChatField(CallbackInfo ci) {
        if (!UiUtilsScreens.shouldAttachWidgets((Screen) (Object) this)) {
            uiutils$chatField = null;
            return;
        }
        uiutils$chatField = null;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        for (var child : screen.children()) {
            if (child instanceof ChatTextFieldWidget field) {
                uiutils$chatField = field;
                break;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    public void uiutils$onKeyPressed(KeyEvent keyInput, CallbackInfoReturnable<Boolean> cir) {
        if (uiutils$chatField != null && uiutils$chatField.isFocused()) {
            if (uiutils$chatField.keyPressed(keyInput)) {
                cir.setReturnValue(true);
                return;
            }
            if (keyInput.key() == GLFW.GLFW_KEY_ESCAPE) {
                uiutils$chatField.setFocused(false);
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(true);
            return;
        }

        if (super.keyPressed(keyInput)) {
            cir.setReturnValue(true);
            cir.cancel();
        } else if (MainClient.mc.options.keyInventory.matches(keyInput)) {
            this.onClose();
            cir.setReturnValue(true);
            cir.cancel();
        } else if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            if (mc.options.keyPickItem.matches(keyInput)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ContainerInput.CLONE);
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
            if (mc.options.keyDrop.matches(keyInput)) {
                boolean controlDown = (keyInput.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, controlDown ? 1 : 0, ContainerInput.THROW);
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }
}
