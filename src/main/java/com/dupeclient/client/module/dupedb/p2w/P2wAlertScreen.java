package com.dupeclient.client.module.dupedb.p2w;

import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.widget.StylishButtonWidget;

/** Dismissible alert when joining a community-marked P2W server. */
public final class P2wAlertScreen extends Screen {
    private final String server;
    private final int scorePercent;

    public P2wAlertScreen(String server, int scorePercent) {
        super(Component.literal("P2W Server"));
        this.server = server == null ? "" : server;
        this.scorePercent = scorePercent;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int btnW = 160;
        this.addRenderableWidget(new StylishButtonWidget(cx - btnW / 2, this.height / 2 + 36, btnW, 20,
                Component.literal("Acknowledge"), () -> {
                    P2wServerPolicy.INSTANCE.onPolicyUiDismissed(this.server);
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(null);
                    }
                }));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xB0101018);
        drawPanel(context);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void drawPanel(GuiGraphicsExtractor context) {
        int cx = this.width / 2;
        int panelW = Math.min(420, this.width - 40);
        int panelH = 150;
        int px = cx - panelW / 2;
        int py = this.height / 2 - panelH / 2 - 10;
        UiDraw.cardElevated(context, px, py, panelW, panelH, 8);
        context.centeredText(this.font, Component.literal("Community P2W Server"), cx, py + 14, 0xFFFBBF24);
        context.centeredText(this.font, Component.literal(this.server), cx, py + 34, 0xFFE5E7EB);
        String scoreLine = scorePercent >= 0
                ? "Community P2W score: " + scorePercent + "%"
                : "This server is marked pay-to-win by the community.";
        context.centeredText(this.font, Component.literal(scoreLine), cx, py + 50, 0xFF93C5FD);
        context.centeredText(this.font,
                Component.literal("Exploit modules remain available — use responsibly."), cx, py + 68, 0xFF9CA3AF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
