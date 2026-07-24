package com.dupeclient.client.module.acaudit;

import com.dupeclient.client.gui.GitHubRepoCard;
import java.net.URI;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** AC Audit upstream attribution — delegates to shared {@link GitHubRepoCard}. */
public final class AcAuditGitHubCard {
    public static final URI REPO_URL = GitHubRepoCard.AC_AUDIT.repoUrl();

    private AcAuditGitHubCard() {
    }

    public static int width() {
        return GitHubRepoCard.width();
    }

    public static int height() {
        return GitHubRepoCard.height();
    }

    public static void render(GuiGraphics context, Font tr, int x, int y, int mouseX, int mouseY) {
        GitHubRepoCard.AC_AUDIT.render(context, tr, x, y, mouseX, mouseY);
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int x, int y, int button) {
        return GitHubRepoCard.AC_AUDIT.mouseClicked(mouseX, mouseY, x, y, button);
    }

    public static void openRepo() {
        GitHubRepoCard.AC_AUDIT.openRepo();
    }
}
