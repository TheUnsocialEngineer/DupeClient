package com.dupeclient.client.gui;

import com.dupeclient.client.core.session.SessionBootstrap;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StartupBlockedScreen extends Screen {
    private static final URI THREAT_RIP = URI.create("https://jlab.threat.rip/");
    private static final URI VIRUS_TOTAL = URI.create("https://www.virustotal.com/");
    private static final URI RATTER_SCANNER = URI.create("https://ratterscanner.com/");

    private int contentScroll;

    public StartupBlockedScreen() {
        super(Component.literal("DupeClient Startup Check Failed"));
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelW = Math.min(520, this.width - 32);
        int x = (this.width - panelW) / 2;
        int y = Math.max(UiTokens.SP_4, (this.height - 360) / 2) - contentScroll;
        int btnH = 22;
        int gap = 6;

        y += 188;

        addRenderableWidget(new StylishButtonWidget(x, y, panelW, btnH, Component.literal("Open flagged jar (placeholder)"),
                () -> openPath(SessionBootstrap.INSTANCE.selfJarPath())));
        y += btnH + gap;
        addRenderableWidget(new StylishButtonWidget(x, y, panelW, btnH, Component.literal("Open baseline file (placeholder)"),
                () -> openPath(SessionBootstrap.INSTANCE.baselineHashPath())));
        y += btnH + gap;
        addRenderableWidget(new StylishButtonWidget(x, y, panelW, btnH, Component.literal("Open config folder (placeholder)"),
                () -> openPath(SessionBootstrap.INSTANCE.configRootPath())));

        y += btnH + UiTokens.SP_4;
        int third = (panelW - gap * 2) / 3;
        addRenderableWidget(new StylishButtonWidget(x, y, third, btnH, Component.literal("jlab.threat.rip"),
                () -> openUrl(THREAT_RIP)));
        addRenderableWidget(new StylishButtonWidget(x + third + gap, y, third, btnH, Component.literal("VirusTotal"),
                () -> openUrl(VIRUS_TOTAL)));
        addRenderableWidget(new StylishButtonWidget(x + (third + gap) * 2, y, third, btnH, Component.literal("RatterScanner"),
                () -> openUrl(RATTER_SCANNER)));

        y += btnH + UiTokens.SP_4;
        addRenderableWidget(new StylishButtonWidget(x, y, panelW, btnH, Component.literal("Quit Game"),
                () -> this.minecraft.stop()));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int panelW = Math.min(520, this.width - 32);
        int x = (this.width - panelW) / 2;
        int y = Math.max(UiTokens.SP_4, (this.height - 360) / 2) - contentScroll;

        context.drawCenteredString(this.font, Component.literal("STARTUP CHECK FAILED"), this.width / 2, y, 0xFFFF6B6B);
        y += 16;
        context.drawCenteredString(this.font, Component.literal("DupeClient cannot run until this is resolved."), this.width / 2, y, UiTokens.SLATE_200);
        y += 18;

        String reason = SessionBootstrap.INSTANCE.lastReason();
        drawWrapped(context, "Reason: " + reason, x, y, panelW, 0xFFFFB4B4);
        y += wrappedHeight(reason, panelW) + UiTokens.SP_3;

        context.drawString(this.font, Component.literal("Related files:"), x, y, UiTokens.SLATE_200);
        y += 12;
        for (String fileLine : fileLines()) {
            drawWrapped(context, "• " + fileLine, x + UiTokens.SP_2, y, panelW - UiTokens.SP_2, 0xFF94A3B8);
            y += wrappedHeight(fileLine, panelW - UiTokens.SP_2) + 2;
        }

        y += UiTokens.SP_2;
        context.drawString(this.font, Component.literal("Verify the mod jar is clean:"), x, y, UiTokens.SLATE_200);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        UiDraw.fillMidnightBackground(context, this.width, this.height);
        int overlay = UiTokens.argb(0xCC, 0x3A0A0A);
        context.fill(0, 0, this.width, this.height, overlay);
        context.fillGradient(0, 0, this.width, this.height / 2, UiTokens.argb(0x55, 0x7F1D1D), 0x00000000);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        contentScroll = Math.max(0, contentScroll - (int) (verticalAmount * 16));
        init();
        return true;
    }

    private List<String> fileLines() {
        List<String> lines = new ArrayList<>(SessionBootstrap.INSTANCE.relatedFiles());
        if (lines.isEmpty()) {
            lines.add("(placeholder) flagged entry inside DupeClient jar");
            lines.add("(placeholder) config/dupeclient/session_mod.jar.sha256");
        }
        return lines;
    }

    private void drawWrapped(GuiGraphics context, String text, int x, int y, int maxWidth, int color) {
        for (var line : this.font.split(Component.literal(text), maxWidth)) {
            context.drawString(this.font, line, x, y, color);
            y += 10;
        }
    }

    private int wrappedHeight(String text, int maxWidth) {
        return this.font.split(Component.literal(text), maxWidth).size() * 10;
    }

    private static void openUrl(URI uri) {
        try {
            Util.getPlatform().openUri(uri);
        } catch (Exception ignored) {
        }
    }

    private static void openPath(Path path) {
        if (path == null) {
            return;
        }
        try {
            Path target = path.toAbsolutePath();
            if (java.nio.file.Files.isRegularFile(target)) {
                Util.getPlatform().openPath(target.getParent());
            } else if (java.nio.file.Files.isDirectory(target)) {
                Util.getPlatform().openPath(target);
            } else {
                Path parent = target.getParent();
                if (parent != null) {
                    Util.getPlatform().openPath(parent);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
