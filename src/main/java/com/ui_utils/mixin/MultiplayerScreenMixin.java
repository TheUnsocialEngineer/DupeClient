package com.ui_utils.mixin;

import com.ui_utils.SessionUtils;
import com.ui_utils.SharedVariables;
import com.ui_utils.gui.CustomButtonWidget;
import com.ui_utils.mixin.accessor.ScreenAccessor;
import java.lang.reflect.Field;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin {
    @Inject(at = @At("RETURN"), method = "init", require = 0)
    private void replaceViaFabricPlusButton(CallbackInfo ci) {
        if (!SharedVariables.enabled) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded("viafabricplus")) {
            return;
        }
        MultiplayerScreen self = (MultiplayerScreen) (Object) this;
        ButtonWidget vfpButton = null;
        for (Element child : self.children()) {
            if (!(child instanceof ButtonWidget button)) {
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
            ButtonWidget originalButton = vfpButton;
            ((ScreenAccessor) self).uiUtils$remove(vfpButton);
            Screen screen = self;
            int margin = 5;
            int spacing = 4;
            int buttonWidth = 50;
            int buttonHeight = 14;
            int rightX = screen.width - margin - buttonWidth;
            int bottomY = screen.height - 60;
            self.addDrawableChild(CustomButtonWidget.createSmall(
                    rightX,
                    bottomY + buttonHeight + spacing,
                    buttonWidth,
                    Text.of("Via+"),
                    button -> originalButton.onClick(
                            new Click(
                                    originalButton.getX() + 1,
                                    originalButton.getY() + 1,
                                    new MouseInput(0, 0)),
                            false)));
        }
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (!SharedVariables.enabled) {
            return;
        }
        MultiplayerScreen self = (MultiplayerScreen) (Object) this;
        Screen screen = self;
        MinecraftClient mc = MinecraftClient.getInstance();
        int margin = 5;
        int spacing = 4;
        int buttonWidth = 50;
        int buttonHeight = 14;
        int bottomY = screen.height - 60;

        self.addDrawableChild(CustomButtonWidget.createSmall(margin, bottomY, buttonWidth, Text.of("Bypass"), button -> {
            SharedVariables.bypassResourcePack = !SharedVariables.bypassResourcePack;
            button.setMessage(Text.literal(SharedVariables.bypassResourcePack ? "§aBypass" : "Bypass"));
        }));

        self.addDrawableChild(CustomButtonWidget.createSmall(
                margin, bottomY + buttonHeight + spacing, buttonWidth, Text.of("Deny"), button -> {
                    SharedVariables.resourcePackForceDeny = !SharedVariables.resourcePackForceDeny;
                    button.setMessage(Text.literal(SharedVariables.resourcePackForceDeny ? "§aDeny" : "Deny"));
                }));

        int rightX = screen.width - margin - buttonWidth;
        self.addDrawableChild(CustomButtonWidget.createSmall(
                rightX, bottomY, buttonWidth, Text.of("User"), button -> mc.setScreen(new UsernameScreen(self, mc))));

        if (!FabricLoader.getInstance().isModLoaded("viafabricplus")) {
            self.addDrawableChild(CustomButtonWidget.createSmall(
                    rightX,
                    bottomY + buttonHeight + spacing,
                    buttonWidth,
                    Text.of("Via+"),
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
            super(Text.literal("ViaFabricPlus"));
            this.parent = parent;
            this.message = message;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            this.addDrawableChild(ButtonWidget.builder(Text.literal("OK"), button -> MinecraftClient.getInstance()
                            .setScreen(this.parent))
                    .dimensions(centerX - 50, centerY + 20, 100, 20)
                    .build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
            context.drawCenteredTextWithShadow(
                    this.textRenderer, Text.literal(this.message), this.width / 2, this.height / 2, 0xFF5555);
        }
    }

    private static final class UsernameScreen extends Screen {
        private final Screen parent;
        private final MinecraftClient mc;
        private TextFieldWidget usernameField;

        private UsernameScreen(Screen parent, MinecraftClient mc) {
            super(Text.literal("Set Username"));
            this.parent = parent;
            this.mc = mc;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            this.usernameField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 20, 200, 20, Text.literal("Username"));
            this.usernameField.setText(this.mc.getSession().getUsername());
            this.usernameField.setMaxLength(16);
            this.addSelectableChild(this.usernameField);
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), button -> {
                        String newName = this.usernameField.getText().trim();
                        if (!newName.isEmpty()) {
                            try {
                                Session oldSession = this.mc.getSession();
                                Session newSession = SessionUtils.copyWith(oldSession, newName, null);
                                Field sessionField = MinecraftClient.class.getDeclaredField("session");
                                sessionField.setAccessible(true);
                                sessionField.set(this.mc, newSession);
                            } catch (Exception ignored) {
                            }
                        }
                        this.mc.setScreen(this.parent);
                    })
                    .dimensions(centerX - 100, centerY + 10, 95, 20)
                    .build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> this.mc.setScreen(this.parent))
                    .dimensions(centerX + 5, centerY + 10, 95, 20)
                    .build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
            this.usernameField.render(context, mouseX, mouseY, delta);
        }
    }
}
