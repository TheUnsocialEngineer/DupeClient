package com.dupeclient.client.gui;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.config.VisualSettings;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class DupeClientSettingsScreen extends Screen {
    private final Screen parent;

    public DupeClientSettingsScreen(Screen parent) {
        super(Component.literal("DupeClient Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int width = Math.min(280, this.width - 32);
        int left = centerX - width / 2;
        int y = Mth.clamp(this.height / 2 - 72, 16, Math.max(16, this.height - 192));

        final StylishButtonWidget[] animatedRef = new StylishButtonWidget[1];
        animatedRef[0] = new StylishButtonWidget(left, y, width, 20, animatedLabel(), () -> {
            VisualSettings settings = DupeClient.getVisualSettings();
            settings.animatedBackground = !settings.animatedBackground;
            animatedRef[0].setMessage(animatedLabel());
            DupeClient.saveVisualSettings();
            notifyParentVisualsChanged();
        });
        addRenderableWidget(animatedRef[0]);

        final StylishButtonWidget[] particleRef = new StylishButtonWidget[1];
        particleRef[0] = new StylishButtonWidget(left, y + 24, width, 20, particleLabel(), () -> {
            VisualSettings settings = DupeClient.getVisualSettings();
            int[] options = {80, 120, 180, 240, 320, 420};
            settings.particleCount = nextIntOption(settings.particleCount, options);
            particleRef[0].setMessage(particleLabel());
            DupeClient.saveVisualSettings();
            notifyParentVisualsChanged();
        });
        addRenderableWidget(particleRef[0]);

        final StylishButtonWidget[] motionRef = new StylishButtonWidget[1];
        motionRef[0] = new StylishButtonWidget(left, y + 48, width, 20, motionLabel(), () -> {
            VisualSettings settings = DupeClient.getVisualSettings();
            float[] options = {0.4F, 0.7F, 1.0F, 1.3F, 1.7F};
            settings.motionIntensity = nextFloatOption(settings.motionIntensity, options);
            motionRef[0].setMessage(motionLabel());
            DupeClient.saveVisualSettings();
            notifyParentVisualsChanged();
        });
        addRenderableWidget(motionRef[0]);

        final StylishButtonWidget[] twinkleRef = new StylishButtonWidget[1];
        twinkleRef[0] = new StylishButtonWidget(left, y + 72, width, 20, twinkleLabel(), () -> {
            VisualSettings settings = DupeClient.getVisualSettings();
            float[] options = {0.6F, 0.9F, 1.0F, 1.3F, 1.6F};
            settings.twinkleSpeed = nextFloatOption(settings.twinkleSpeed, options);
            twinkleRef[0].setMessage(twinkleLabel());
            DupeClient.saveVisualSettings();
            notifyParentVisualsChanged();
        });
        addRenderableWidget(twinkleRef[0]);

        addRenderableWidget(new StylishButtonWidget(left, y + 96, width, 20, Component.literal("Reset to defaults"), () -> {
            VisualSettings settings = DupeClient.getVisualSettings();
            settings.animatedBackground = true;
            settings.particleCount = 180;
            settings.motionIntensity = 1.0F;
            settings.twinkleSpeed = 1.0F;
            DupeClient.saveVisualSettings();
            notifyParentVisualsChanged();
            this.init();
        }));

        addRenderableWidget(new StylishButtonWidget(centerX - 100, y + 128, 200, 20, Component.literal("Back"), this::onClose));
    }

    private Component animatedLabel() {
        return Component.literal("Animated Background: " + (DupeClient.getVisualSettings().animatedBackground ? "ON" : "OFF"));
    }

    private Component particleLabel() {
        return Component.literal("Particle Count: " + DupeClient.getVisualSettings().particleCount);
    }

    private Component motionLabel() {
        int percent = Math.round(DupeClient.getVisualSettings().motionIntensity * 100.0F);
        return Component.literal("Motion Intensity: " + percent + "%");
    }

    private Component twinkleLabel() {
        int percent = Math.round(DupeClient.getVisualSettings().twinkleSpeed * 100.0F);
        return Component.literal("Twinkle Speed: " + percent + "%");
    }

    private int nextIntOption(int current, int[] options) {
        for (int i = 0; i < options.length; i++) {
            if (options[i] == current) {
                return options[(i + 1) % options.length];
            }
        }
        return options[0];
    }

    private float nextFloatOption(float current, float[] options) {
        for (int i = 0; i < options.length; i++) {
            if (Math.abs(options[i] - current) < 0.001F) {
                return options[(i + 1) % options.length];
            }
        }
        return options[0];
    }

    private void notifyParentVisualsChanged() {
        if (parent instanceof DupeMainMenuScreen menu) {
            menu.invalidateVisualCache();
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (parent instanceof DupeMainMenuScreen menu) {
            menu.renderBackground(context, mouseX, mouseY, delta);
        } else {
            UiDraw.fillMidnightBackground(context, this.width, this.height);
        }
        context.fill(0, 0, this.width, this.height, UiTokens.argb(0x66, UiTokens.SLATE_950));
        int tY = Mth.clamp(this.height / 2 - 120, 8, 80);
        context.drawCenteredString(this.font, this.title, this.width / 2, tY, UiTokens.TEXT);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
