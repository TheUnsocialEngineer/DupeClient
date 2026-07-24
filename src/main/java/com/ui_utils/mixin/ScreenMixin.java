package com.ui_utils.mixin;

import com.ui_utils.MainClient;
import com.ui_utils.UiUtilsScreens;
import com.ui_utils.gui.ChatTextFieldWidget;
import com.ui_utils.gui.CustomTextFieldWidget;
import com.ui_utils.mixin.accessor.ScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow
    protected int height;

    @Shadow
    public abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement);

    @Inject(at = @At("TAIL"), method = "init")
    public void uiutils$onInit(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!UiUtilsScreens.shouldAttachWidgets(screen)) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = ((ScreenAccessor) this).getTextRenderer();
        MainClient.createWidgets(mc, screen);

        CustomTextFieldWidget chatField = new ChatTextFieldWidget(textRenderer, 6, this.height - 18, 140, 12, Text.of("Chat..."));
        chatField.setText("");
        chatField.setMaxLength(255);
        this.addDrawableChild(chatField);
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void uiutils$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!UiUtilsScreens.shouldRenderSyncPanel(screen)) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        MainClient.createText(mc, context, ((ScreenAccessor) this).getTextRenderer());
    }
}
