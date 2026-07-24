package com.ui_utils.mixin;

import com.ui_utils.SessionUtils;
import com.ui_utils.SharedVariables;
import com.ui_utils.gui.CustomButtonWidget;
import com.ui_utils.mixin.accessor.ScreenAccessor;
import java.lang.reflect.Field;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MultiplayerScreenMixin {
    @Inject(at = @At("RETURN"), method = "init", require = 0)
    private void replaceViaFabricPlusButton(CallbackInfo ci) {
        if (!SharedVariables.enabled) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded("viafabricplus")) {
            return;
        }
        JoinMultiplayerScreen self = (JoinMultiplayerScreen) (Object) this;
        Button vfpButton = null;
        for (GuiEventListener child : self.children()) {
            if (!(child instanceof Button button)) {
                continue;
            }
            String msg = button.getMessage().getString();
            if (!msg.contains("ViaFabricPlus") && !msg.equals("ViaFabricPlus")) {
                continue;
            }
            vfpButton = button;
            break;
        }
        if (vfpButton != null) {
            Button originalButton = vfpButton;
            ((ScreenAccessor) self).uiUtils$remove(vfpButton);
            Screen screen = self;
            int margin = 5;
            int spacing = 4;
            int buttonWidth = 50;
            int buttonHeight = 14;
            int rightX = screen.width - margin - buttonWidth;
            int bottomY = screen.height - 60;
            ((ScreenAccessor) self).uiUtils$addRenderableWidget(CustomButtonWidget.createSmall(
                    rightX,
                    bottomY + buttonHeight + spacing,
                    buttonWidth,
                    Component.nullToEmpty("Via+"),
                    button -> originalButton.onClick(
                            new MouseButtonEvent(
                                    originalButton.getX() + 1,
                                    originalButton.getY() + 1,
                                    new MouseButtonInfo(0, 0)),
                            false)));
        }
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (!SharedVariables.enabled) {
            return;
        }
        JoinMultiplayerScreen self = (JoinMultiplayerScreen) (Object) this;
        Screen screen = self;
        Minecraft mc = Minecraft.getInstance();
        int margin = 5;
        int spacing = 4;
        int buttonWidth = 50;
        int buttonHeight = 14;
        int bottomY = screen.height - 60;

        ((ScreenAccessor) self).uiUtils$addRenderableWidget(CustomButtonWidget.createSmall(margin, bottomY, buttonWidth, Component.nullToEmpty("Bypass"), button -> {
            SharedVariables.bypassResourcePack = !SharedVariables.bypassResourcePack;
            button.setMessage(Component.literal(SharedVariables.bypassResourcePack ? "§aBypass" : "Bypass"));
        }));

        ((ScreenAccessor) self).uiUtils$addRenderableWidget(CustomButtonWidget.createSmall(
                margin, bottomY + buttonHeight + spacing, buttonWidth, Component.nullToEmpty("Deny"), button -> {
                    SharedVariables.resourcePackForceDeny = !SharedVariables.resourcePackForceDeny;
                    button.setMessage(Component.literal(SharedVariables.resourcePackForceDeny ? "§aDeny" : "Deny"));
                }));

        int rightX = screen.width - margin - buttonWidth;
        ((ScreenAccessor) self).uiUtils$addRenderableWidget(CustomButtonWidget.createSmall(
                rightX, bottomY, buttonWidth, Component.nullToEmpty("User"), button -> mc.setScreen(new UsernameScreen(self, mc))));

        if (!FabricLoader.getInstance().isModLoaded("viafabricplus")) {
            ((ScreenAccessor) self).uiUtils$addRenderableWidget(CustomButtonWidget.createSmall(
                    rightX,
                    bottomY + buttonHeight + spacing,
                    buttonWidth,
                    Component.nullToEmpty("Via+"),
                    button -> {
                        if (FabricLoader.getInstance().isModLoaded("viafabric")) {
                            try {
                                Class<?> viaScreenClass =
                                        Class.forName("com.viaversion.fabric.mc121.gui.ViaConfigScreen");
                                Screen viaScreen = (Screen)
                                        viaScreenClass.getConstructor(Screen.class).newInstance(self);
                                mc.setScreen(viaScreen);
                            } catch (Exception e1) {
                                try {
                                    Class<?> viaScreenClass = Class.forName(
                                            "com.github.creeper123123321.viafabric.gui.ViaConfigScreen");
                                    Screen viaScreen = (Screen)
                                            viaScreenClass.getConstructor(Screen.class).newInstance(self);
                                    mc.setScreen(viaScreen);
                                } catch (Exception e2) {
                                    mc.setScreen(new ViaNotFoundScreen(self, "Error opening ViaFabric"));
                                }
                            }
                        } else {
                            mc.setScreen(new ViaNotFoundScreen(self, "ViaFabric(Plus) not installed!"));
                        }
                    }));
        }
    }

    private static final class ViaNotFoundScreen extends Screen {
        private final Screen parent;
        private final String message;

        private ViaNotFoundScreen(Screen parent, String message) {
            super(Component.literal("ViaFabricPlus"));
            this.parent = parent;
            this.message = message;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            this.addRenderableWidget(Button.builder(Component.literal("OK"), button -> Minecraft.getInstance()
                            .setScreen(this.parent))
                    .bounds(centerX - 50, centerY + 20, 100, 20)
                    .build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            super.extractRenderState(context, mouseX, mouseY, delta);
            context.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
            context.centeredText(
                    this.font, Component.literal(this.message), this.width / 2, this.height / 2, 0xFF5555);
        }
    }

    private static final class UsernameScreen extends Screen {
        private final Screen parent;
        private final Minecraft mc;
        private EditBox usernameField;

        private UsernameScreen(Screen parent, Minecraft mc) {
            super(Component.literal("Set Username"));
            this.parent = parent;
            this.mc = mc;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            this.usernameField = new EditBox(this.font, centerX - 100, centerY - 20, 200, 20, Component.literal("Username"));
            this.usernameField.setValue(this.mc.getUser().getName());
            this.usernameField.setMaxLength(16);
            this.addWidget(this.usernameField);
            this.addRenderableWidget(Button.builder(Component.literal("Apply"), button -> {
                        String newName = this.usernameField.getValue().trim();
                        if (!newName.isEmpty()) {
                            try {
                                User oldSession = this.mc.getUser();
                                User newSession = SessionUtils.copyWith(oldSession, newName, null);
                                Field sessionField = Minecraft.class.getDeclaredField("session");
                                sessionField.setAccessible(true);
                                sessionField.set(this.mc, newSession);
                            } catch (Exception ignored) {
                            }
                        }
                        this.mc.setScreen(this.parent);
                    })
                    .bounds(centerX - 100, centerY + 10, 95, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.mc.setScreen(this.parent))
                    .bounds(centerX + 5, centerY + 10, 95, 20)
                    .build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            super.extractRenderState(context, mouseX, mouseY, delta);
            context.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
            this.usernameField.extractRenderState(context, mouseX, mouseY, delta);
        }
    }
}
