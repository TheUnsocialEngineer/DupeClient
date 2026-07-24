package com.dupeclient.client.module.dupedb.p2w;

import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Dismissible alert when joining a community-marked P2W server. */
public final class P2wAlertScreen extends Screen {
    private final String server;
    private final int scorePercent;

    public P2wAlertScreen(String server, int scorePercent) {
        super(Text.literal("P2W Server"));
        this.server = server == null ? "" : server;
        this.scorePercent = scorePercent;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int btnW = 160;
        this.addDrawableChild(new StylishButtonWidget(cx - btnW / 2, this.height / 2 + 36, btnW, 20,
                Text.literal("Acknowledge"), () -> {
                    P2wServerPolicy.INSTANCE.onPolicyUiDismissed(this.server);
                    if (this.client != null) {
                        this.client.setScreen(null);
                    }
                }));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xB0101018);
        drawPanel(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext context) {
        int cx = this.width / 2;
        int panelW = Math.min(420, this.width - 40);
        int panelH = 150;
        int px = cx - panelW / 2;
        int py = this.height / 2 - panelH / 2 - 10;
        UiDraw.cardElevated(context, px, py, panelW, panelH, 8);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Community P2W Server"), cx, py + 14, 0xFFFBBF24);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(this.server), cx, py + 34, 0xFFE5E7EB);
        String scoreLine = scorePercent >= 0
                ? "Community P2W score: " + scorePercent + "%"
                : "This server is marked pay-to-win by the community.";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(scoreLine), cx, py + 50, 0xFF93C5FD);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Exploit modules remain available — use responsibly."), cx, py + 68, 0xFF9CA3AF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
