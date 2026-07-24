package com.ui_utils.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.ui_utils.UpdateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "init")
    private void onInitWidgetsNormal(CallbackInfo ci) {
        if (UpdateUtils.isOutdated) {
            if (!UpdateUtils.messageShown) {
                Minecraft client = Minecraft.getInstance();
                ToastManager toastManager = client.getToastManager();
                Component title = Component.nullToEmpty("UI-Utils " + UpdateUtils.version + " is out for " + UpdateUtils.mcVersion + "!");
                Component description = Component.nullToEmpty("Download it from the top left corner!");
                SystemToast.add(toastManager, new SystemToast.SystemToastId(30000L), title, description);
                UpdateUtils.messageShown = true;
            }

            Component message = Component.nullToEmpty("Download UI-Utils " + UpdateUtils.version + "!");

            this.addRenderableWidget(new StringWidget(40 - 15, 5, font.width(message), font.lineHeight, message, font));

            Button downloadUpdateButton = new ImageButton(5, 5 - 3,
                    15, 15,
                    new WidgetSprites(
                            Identifier.fromNamespaceAndPath("ui_utils", "update"),
                            Identifier.fromNamespaceAndPath("ui_utils", "update_selected")
                    ),
                    (button) -> UpdateUtils.downloadUpdate(),
                    Component.nullToEmpty("Download Update"));
            this.addRenderableWidget(downloadUpdateButton);

        }
    }
}
