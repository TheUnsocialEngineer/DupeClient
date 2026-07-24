package com.dupeclient.client.gui;

import com.dupeclient.client.core.session.SessionBootstrap;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
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
        super(Text.literal("DupeClient Startup Check Failed"));
    }

    @Override
    protected void init() {
        clearChildren();
        int panelW = Math.min(520, this.width - 32);
        int x = (this.width - panelW) / 2;
        int y = Math.max(UiTokens.SP_4, (this.height - 360) / 2) - contentScroll;
        int btnH = 22;
        int gap = 6;

        y += 188;

        addDrawableChild(new StylishButtonWidget(x, y, panelW, btnH, Text.literal("Open flagged jar (placeholder)"),
                () -> openPath(SessionBootstrap.INSTANCE.selfJarPath())));
        y += btnH + gap;
        addDrawableChild(new StylishButtonWidget(x, y, panelW, btnH, Text.literal("Open baseline file (placeholder)"),
                () -> openPath(SessionBootstrap.INSTANCE.baselineHashPath())));
        y += btnH + gap;
        addDrawableChild(new StylishButtonWidget(x, y, panelW, btnH, Text.literal("Open config folder (placeholder)"),
                () -> openPath(SessionBootstrap.INSTANCE.configRootPath())));

        y += btnH + UiTokens.SP_4;
        int third = (panelW - gap * 2) / 3;
        addDrawableChild(new StylishButtonWidget(x, y, third, btnH, Text.literal("jlab.threat.rip"),
                () -> openUrl(THREAT_RIP)));
        addDrawableChild(new StylishButtonWidget(x + third + gap, y, third, btnH, Text.literal("VirusTotal"),
                () -> openUrl(VIRUS_TOTAL)));
        addDrawableChild(new StylishButtonWidget(x + (third + gap) * 2, y, third, btnH, Text.literal("RatterScanner"),
                () -> openUrl(RATTER_SCANNER)));

        y += btnH + UiTokens.SP_4;
        addDrawableChild(new StylishButtonWidget(x, y, panelW, btnH, Text.literal("Quit Game"),
                () -> this.client.scheduleStop()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int panelW = Math.min(520, this.width - 32);
        int x = (this.width - panelW) / 2;
        int y = Math.max(UiTokens.SP_4, (this.height - 360) / 2) - contentScroll;

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("STARTUP CHECK FAILED"), this.width / 2, y, 0xFFFF6B6B);
        y += 16;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("DupeClient cannot run until this is resolved."), this.width / 2, y, UiTokens.SLATE_200);
        y += 18;

        String reason = SessionBootstrap.INSTANCE.lastReason();
        drawWrapped(context, "Reason: " + reason, x, y, panelW, 0xFFFFB4B4);
        y += wrappedHeight(reason, panelW) + UiTokens.SP_3;

        context.drawTextWithShadow(this.textRenderer, Text.literal("Related files:"), x, y, UiTokens.SLATE_200);
        y += 12;
        for (String fileLine : fileLines()) {
            drawWrapped(context, "• " + fileLine, x + UiTokens.SP_2, y, panelW - UiTokens.SP_2, 0xFF94A3B8);
            y += wrappedHeight(fileLine, panelW - UiTokens.SP_2) + 2;
        }

        y += UiTokens.SP_2;
        context.drawTextWithShadow(this.textRenderer, Text.literal("Verify the mod jar is clean:"), x, y, UiTokens.SLATE_200);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
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
    public void close() {
    }

    @Override
    public boolean shouldPause() {
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

    private void drawWrapped(DrawContext context, String text, int x, int y, int maxWidth, int color) {
        for (var line : this.textRenderer.wrapLines(Text.literal(text), maxWidth)) {
            context.drawTextWithShadow(this.textRenderer, line, x, y, color);
            y += 10;
        }
    }

    private int wrappedHeight(String text, int maxWidth) {
        return this.textRenderer.wrapLines(Text.literal(text), maxWidth).size() * 10;
    }

    private static void openUrl(URI uri) {
        try {
            Util.getOperatingSystem().open(uri);
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
                Util.getOperatingSystem().open(target.getParent());
            } else if (java.nio.file.Files.isDirectory(target)) {
                Util.getOperatingSystem().open(target);
            } else {
                Path parent = target.getParent();
                if (parent != null) {
                    Util.getOperatingSystem().open(parent);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
