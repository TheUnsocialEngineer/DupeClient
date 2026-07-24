package com.ui_utils.mixin;

import com.ui_utils.MainClient;
import com.ui_utils.UiUtilsScreens;
import com.ui_utils.gui.ChatTextFieldWidget;
import com.ui_utils.gui.CustomTextFieldWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
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
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
    private HandledScreenMixin() {
        super(null);
    }

    @Shadow
    protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Unique
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Unique
    private CustomTextFieldWidget uiutils$chatField;

    @Inject(at = @At("TAIL"), method = "init")
    public void uiutils$findChatField(CallbackInfo ci) {
        if (!UiUtilsScreens.shouldAttachWidgets((Screen) (Object) this)) {
            uiutils$chatField = null;
            return;
        }
        uiutils$chatField = null;
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        for (var child : screen.children()) {
            if (child instanceof ChatTextFieldWidget field) {
                uiutils$chatField = field;
                break;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    public void uiutils$onKeyPressed(KeyInput keyInput, CallbackInfoReturnable<Boolean> cir) {
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
        } else if (MainClient.mc.options.inventoryKey.matchesKey(keyInput)) {
            this.close();
            cir.setReturnValue(true);
            cir.cancel();
        } else if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
            if (mc.options.pickItemKey.matchesKey(keyInput)) {
                this.onMouseClick(this.focusedSlot, this.focusedSlot.id, 0, SlotActionType.CLONE);
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
            if (mc.options.dropKey.matchesKey(keyInput)) {
                boolean controlDown = (keyInput.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
                this.onMouseClick(this.focusedSlot, this.focusedSlot.id, controlDown ? 1 : 0, SlotActionType.THROW);
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }
}
