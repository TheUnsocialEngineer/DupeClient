package com.dupeclient.client.module.dupedb.p2w;

import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.dupeclient.client.gui.widget.StylishButtonWidget;

/** Full-screen disclaimer for community-marked non-P2W servers; modules stay locked until disconnect. */
public final class NonP2wDisclaimerScreen extends Screen {
    private final String server;

    public NonP2wDisclaimerScreen(String server) {
        super(Component.literal("Non-P2W Server"));
        this.server = server == null ? "" : server;
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int btnW = 200;
        this.addRenderableWidget(new StylishButtonWidget(cx - btnW / 2, this.height - 56, btnW, 20,
                Component.literal("I understand — modules stay disabled"), () -> {
                    P2wServerPolicy.INSTANCE.onPolicyUiDismissed(this.server);
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(null);
                    }
                }));
    }

    /** Solid overlay — vanilla blur is already applied once per frame by {@link Screen#render}. */
    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xE0101018);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        drawDisclaimerText(context);
    }

    private void drawDisclaimerText(GuiGraphicsExtractor context) {
        int cx = this.width / 2;
        context.centeredText(this.font, Component.literal("Non-P2W Server"), cx, 48, 0xFF34D399);
        context.centeredText(this.font, Component.literal(this.server), cx, 68, 0xFFE5E7EB);
        int y = 96;
        for (Component line : new Component[]{
                Component.literal("This server is marked non pay-to-win by the DupeClient community."),
                Component.literal("DupeClient does not condone duping, exploiting, or crashing on this server."),
                Component.literal("All exploit modules have been disabled and will remain locked"),
                Component.literal("until you disconnect or leave this server."),
                Component.literal("Re-enabling modules while connected is blocked.")
        }) {
            context.centeredText(this.font, line, cx, y, 0xFFF3F4F6);
            y += 14;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
