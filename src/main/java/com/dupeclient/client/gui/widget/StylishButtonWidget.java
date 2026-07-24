package com.dupeclient.client.gui.widget;

import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.modern.theme.MidnightPalette;
import com.dupeclient.client.gui.modern.theme.MidnightShapes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class StylishButtonWidget extends ClickableWidget {
    private final Runnable onPress;
    private boolean selected;

    public StylishButtonWidget(int x, int y, int width, int height, Text text, Runnable onPress) {
        super(x, y, width, height, text);
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        int x1 = getX();
        int y1 = getY();
        int w = getWidth();
        int h = getHeight();
        int rr = MidnightShapes.controlRadius(h);

        if (selected) {
            int fill = blend(MidnightPalette.BLUE_L, MidnightPalette.BLUE_R, 0.45f);
            MidnightShapes.fillRoundedFrame(context, x1, y1, w, h, rr, fill, MidnightPalette.alphaRgb(0x77, 0x3B82F6));
        } else if (hovered) {
            MidnightShapes.fillRoundedFrame(
                    context, x1, y1, w, h, rr,
                    0xFF243044,
                    UiTokens.BLUE_400);
        } else {
            MidnightShapes.fillRoundedFrame(
                    context, x1, y1, w, h, rr,
                    0xFF1A2030,
                    0xFF64748B);
        }

        int textColor = !this.active
                ? MidnightPalette.TEXT_MUTED
                : (selected ? MidnightPalette.TEXT_PRIMARY : (hovered ? MidnightPalette.TEXT_PRIMARY : MidnightPalette.TEXT_SECONDARY));
        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                getMessage(),
                x1 + w / 2,
                y1 + (h - 8) / 2,
                textColor
        );
    }

    private static int blend(int c0, int c1, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int a0 = (c0 >>> 24) & 0xFF;
        int r0 = (c0 >> 16) & 0xFF;
        int g0 = (c0 >> 8) & 0xFF;
        int b0 = c0 & 0xFF;
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a = (int) (a0 + (a1 - a0) * t);
        int r = (int) (r0 + (r1 - r0) * t);
        int g = (int) (g0 + (g1 - g0) * t);
        int b = (int) (b0 + (b1 - b0) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void onClick(Click click, boolean doubleClick) {
        if (this.active) {
            this.onPress.run();
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
