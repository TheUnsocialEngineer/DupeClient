package com.dupeclient.client.gui;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.mojang.realmsclient.RealmsMainScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class DupeMainMenuScreen extends TitleScreen {
    private final List<Star> stars = new ArrayList<>();
    private final long seed = 90210L;
    private int appliedParticleCount = -1;
    private long appliedSettingsRevision = -1L;

    public DupeMainMenuScreen() {
        super(false);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        disableSplashText();
        ensureStars();

        int buttonWidth = 303; // ~1% larger than previous 300
        int buttonHeight = 21; // ~1% larger than previous 20
        int spacing = 4;
        int totalHeight = (buttonHeight + spacing) * 6 - spacing;
        int startY = (this.height / 2) - (totalHeight / 2);
        int x = (this.width - buttonWidth) / 2;

        addRenderableWidget(new StylishButtonWidget(x, startY, buttonWidth, buttonHeight, Component.literal("Singleplayer"),
                () -> this.minecraft.setScreen(new SelectWorldScreen(this))));
        addRenderableWidget(new StylishButtonWidget(x, startY + 24, buttonWidth, buttonHeight, Component.literal("Multiplayer"),
                () -> this.minecraft.setScreen(new JoinMultiplayerScreen(this))));
        addRenderableWidget(new StylishButtonWidget(x, startY + 48, buttonWidth, buttonHeight, Component.literal("Minecraft Realms"),
                () -> this.minecraft.setScreen(new RealmsMainScreen(this))));
        addRenderableWidget(new StylishButtonWidget(x, startY + 72, buttonWidth, buttonHeight, Component.literal("Mods"),
                this::openModsScreen));
        addRenderableWidget(new StylishButtonWidget(x, startY + 96, buttonWidth, buttonHeight, Component.literal("DupeClient settings"),
                () -> this.minecraft.setScreen(new DupeClientSettingsScreen(this))));

        int smallWidth = 150;
        int smallGap = 4;
        addRenderableWidget(new StylishButtonWidget(x, startY + 120, smallWidth, buttonHeight, Component.literal("Options"),
                () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options))));
        addRenderableWidget(new StylishButtonWidget(x + smallWidth + smallGap, startY + 120, smallWidth, buttonHeight, Component.literal("Quit Game"),
                () -> this.minecraft.stop()));
    }

    private void openModsScreen() {
        try {
            Class<?> clazz = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            this.minecraft.setScreen((Screen) clazz.getConstructor(Screen.class).newInstance(this));
        } catch (Exception e) {
            DupeClient.LOGGER.warn("ModMenu not available, Mods button ignored.");
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        disableSplashText();
        super.render(context, mouseX, mouseY, delta);
        context.drawString(this.font, Component.literal("DupeClient " + DupeClient.BUILD_TAG), 6, 6, 0xFFAED0FF);
    }

    private void ensureStars() {
        int targetCount = Math.max(20, DupeClient.getVisualSettings().particleCount);
        if (stars.size() == targetCount && appliedParticleCount == targetCount) {
            return;
        }
        stars.clear();
        appliedParticleCount = targetCount;
        Random random = new Random(seed);
        for (int i = 0; i < targetCount; i++) {
            stars.add(new Star(
                    random.nextFloat(),
                    random.nextFloat(),
                    0.1F + random.nextFloat() * 0.9F,
                    0.2F + random.nextFloat() * 0.8F,
                    random.nextFloat() * 360.0F
            ));
        }
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (appliedSettingsRevision != DupeClient.getVisualSettingsRevision()) {
            invalidateVisualCache();
            appliedSettingsRevision = DupeClient.getVisualSettingsRevision();
        }

        ensureStars();
        context.fill(0, 0, this.width, this.height, 0xFF040714);

        float time = (System.currentTimeMillis() % 100000L) / 1000.0F;
        float motion = DupeClient.getVisualSettings().motionIntensity;
        float twinkle = DupeClient.getVisualSettings().twinkleSpeed;
        boolean animated = DupeClient.getVisualSettings().animatedBackground;

        int nebulaAlpha = (int) (24 + 30 * (0.5F + 0.5F * Mth.sin(time * 0.38F * (animated ? twinkle : 1.0F))));
        int nebulaColor = (nebulaAlpha << 24) | 0x122746;
        context.fillGradient(0, 0, this.width, this.height, 0x26000000, nebulaColor);
        context.fillGradient(0, 0, this.width, this.height, 0x1E081A34, 0x12000000);

        for (Star star : stars) {
            float driftX = animated ? Mth.sin(time * 0.16F * motion + star.phase) * 0.0042F * motion : 0.0F;
            float driftY = animated ? Mth.cos(time * 0.13F * motion + star.phase) * 0.0032F * motion : 0.0F;
            int x = (int) ((star.xNorm + driftX) * this.width);
            int y = (int) ((star.yNorm + driftY) * this.height);
            float pulse = animated
                    ? 0.42F + 0.58F * Mth.sin(time * (0.85F + star.pulseSpeed) * twinkle + star.phase)
                    : 0.8F;
            int alpha = (int) (255 * Mth.clamp(star.brightness * pulse, 0.15F, 1.0F));
            int color = (alpha << 24) | 0xFFFFFF;
            int size = star.brightness > 0.75F ? 2 : 1;
            context.fill(x, y, x + size, y + size, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Star(float xNorm, float yNorm, float brightness, float pulseSpeed, float phase) {
    }

    public void invalidateVisualCache() {
        this.appliedParticleCount = -1;
        this.stars.clear();
        this.appliedSettingsRevision = DupeClient.getVisualSettingsRevision();
    }

    private void disableSplashText() {
        try {
            java.lang.reflect.Field splashField = TitleScreen.class.getDeclaredField("splashText");
            splashField.setAccessible(true);
            splashField.set(this, null);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
