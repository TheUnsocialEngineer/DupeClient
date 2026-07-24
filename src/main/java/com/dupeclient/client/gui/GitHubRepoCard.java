package com.dupeclient.client.gui;

import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.modern.theme.MidnightShapes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.Objects;

/** Compact attribution chip linking to an upstream GitHub repository. */
public final class GitHubRepoCard {
    public static final GitHubRepoCard AC_AUDIT =
            new GitHubRepoCard("ACAudit", URI.create("https://github.com/Broshan1337/ACAudit"));
    public static final GitHubRepoCard OPSEC =
            new GitHubRepoCard("OpSec", URI.create("https://github.com/aurickk/OpSec"));
    public static final GitHubRepoCard PAY_EVERYONE =
            new GitHubRepoCard("Pay Everyone", URI.create("https://github.com/aurickk/Pay-Everyone"));
    public static final GitHubRepoCard MCP_TOOL_FILES =
            new GitHubRepoCard("MCPTool Files", URI.create("https://github.com/TheUnsocialEngineer/MCPTool-Files"));

    private static final int CARD_W = 132;
    private static final int CARD_H = 36;
    private static final int ICON = 16;
    private static final int ICON_BG_PAD = 1;
    private static final int ICON_BG = 0xFFFFFFFF;
    private static final int CARD_RADIUS = 6;
    private static final int CARD_BG = 0xFF000000;

    private final String title;
    private final URI repoUrl;

    public GitHubRepoCard(String title, URI repoUrl) {
        this.title = Objects.requireNonNull(title, "title");
        this.repoUrl = Objects.requireNonNull(repoUrl, "repoUrl");
    }

    public static int width() {
        return CARD_W;
    }

    public static int height() {
        return CARD_H;
    }

    public URI repoUrl() {
        return repoUrl;
    }

    public void render(DrawContext context, TextRenderer tr, int x, int y, int mouseX, int mouseY) {
        boolean hover = contains(mouseX, mouseY, x, y);
        int border = hover ? UiTokens.argb(0xAA, UiTokens.MINT_500) : 0xFF27272A;
        MidnightShapes.fillRoundedFrame(context, x, y, CARD_W, CARD_H, CARD_RADIUS, CARD_BG, border);

        MinecraftClient client = MinecraftClient.getInstance();
        GitHubMarkTexture.ensureLoaded(client);

        int iconX = x + 8;
        int iconY = y + (CARD_H - ICON) / 2;
        MidnightShapes.fillRoundedRect(
                context,
                iconX - ICON_BG_PAD,
                iconY - ICON_BG_PAD,
                ICON + ICON_BG_PAD * 2,
                ICON + ICON_BG_PAD * 2,
                3,
                ICON_BG);
        if (client != null && GitHubMarkTexture.isReady()) {
            int texSize = GitHubMarkTexture.iconSize();
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    GitHubMarkTexture.texture(),
                    iconX,
                    iconY,
                    0.0F,
                    0.0F,
                    ICON,
                    ICON,
                    texSize,
                    texSize);
        }

        int textX = x + 8 + ICON + 6;
        String shown = tr.trimToWidth(title, CARD_W - ICON - 22);
        context.drawTextWithShadow(tr, shown, textX, y + 8, UiTokens.MINT_300);
        context.drawTextWithShadow(tr, "View on GitHub", textX, y + 20, hover ? 0xFF93C5FD : 0xFF60A5FA);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int button) {
        if (button != 0 || !contains(mouseX, mouseY, x, y)) {
            return false;
        }
        openRepo();
        return true;
    }

    public void openRepo() {
        try {
            Util.getOperatingSystem().open(repoUrl);
        } catch (Exception ignored) {
        }
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
    }
}
