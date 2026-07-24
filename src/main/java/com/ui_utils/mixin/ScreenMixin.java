package com.ui_utils.mixin;

import com.ui_utils.MainClient;
import com.ui_utils.UiUtilsScreens;
import com.ui_utils.gui.ChatTextFieldWidget;
import com.ui_utils.gui.CustomTextFieldWidget;
import com.ui_utils.mixin.accessor.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
    public abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T drawableElement);

    @Inject(at = @At("TAIL"), method = "init")
    public void uiutils$onInit(CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!UiUtilsScreens.shouldAttachWidgets(screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font textRenderer = ((ScreenAccessor) this).getFont();
        MainClient.createWidgets(mc, screen);

        CustomTextFieldWidget chatField = new ChatTextFieldWidget(textRenderer, 6, this.height - 18, 140, 12, Component.nullToEmpty("Chat..."));
        chatField.setValue("");
        chatField.setMaxLength(255);
        this.addRenderableWidget(chatField);
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void uiutils$onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!UiUtilsScreens.shouldRenderSyncPanel(screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        MainClient.createText(mc, context, ((ScreenAccessor) this).getFont());
    }
}
