package com.dupeclient.client.module.dupedb.search;

import com.dupeclient.client.gui.modern.UiTokens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Compact Join/Add control for scanner rows — matches {@link com.dupeclient.client.gui.widget.StylishButtonWidget} look.
 */
public class ScannerActionButton extends AbstractWidget {
    private final Runnable onPress;

    public ScannerActionButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        setAlpha(1f);
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        if (!this.active) {
            return;
        }
        playDownSound(Minecraft.getInstance().getSoundManager());
        onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        int x1 = getX();
        int y1 = getY();
        int x2 = x1 + getWidth();
        int y2 = y1 + getHeight();
        boolean hovered = active && isMouseOver(mouseX, mouseY);

        if (!active) {
            context.fillGradient(x1, y1, x2, y2, UiTokens.argb(0x77, UiTokens.SLATE_900), UiTokens.argb(0x66, UiTokens.SLATE_950));
        } else if (hovered) {
            context.fillGradient(x1, y1, x2, y2, UiTokens.argb(0xEE, UiTokens.MINT_500), UiTokens.argb(0xEE, UiTokens.MINT_600));
        } else {
            context.fillGradient(x1, y1, x2, y2, UiTokens.argb(0xDD, UiTokens.SLATE_700), UiTokens.argb(0xCC, UiTokens.SLATE_800));
        }
        context.fill(x1, y1, x2, y1 + 1, UiTokens.argb(0x66, UiTokens.SLATE_200));
        context.fill(x1, y1, x1 + 1, y2, UiTokens.argb(0x44, UiTokens.MINT_300));
        context.fill(x2 - 1, y1, x2, y2, UiTokens.argb(0x55, UiTokens.SLATE_950));
        context.fill(x1, y2 - 1, x2, y2, UiTokens.argb(0x77, UiTokens.SLATE_950));

        Font tr = Minecraft.getInstance().font;
        int color = active ? 0xFFF8FAFC : UiTokens.SLATE_500;
        int ty = y1 + (getHeight() - 8) / 2;
        context.drawCenteredString(tr, getMessage(), x1 + getWidth() / 2, ty, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, getMessage());
    }
}
