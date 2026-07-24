package com.dupeclient.client.gui.widget;

import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.modern.theme.MidnightShapes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Cycling string picker with the same mint/slate look as {@link StylishButtonWidget}.
 */
public class StylishCyclingButtonWidget extends AbstractWidget {
    private final List<String> values;
    private int index;
    private final Component caption;
    @Nullable
    private final BiConsumer<StylishCyclingButtonWidget, String> onChange;

    public StylishCyclingButtonWidget(
            int x,
            int y,
            int w,
            int h,
            Component caption,
            List<String> values,
            String initialValue,
            @Nullable BiConsumer<StylishCyclingButtonWidget, String> onChange) {
        super(x, y, w, h, Component.empty());
        this.caption = caption;
        this.values = new ArrayList<>(values);
        this.onChange = onChange;
        this.index = 0;
        int i = this.values.indexOf(initialValue);
        if (i >= 0) {
            this.index = i;
        }
        refreshMessage();
    }

    public String getValue() {
        return values.get(index);
    }

    public void setValue(String v) {
        int i = values.indexOf(v);
        if (i >= 0) {
            index = i;
            refreshMessage();
        }
    }

    private void refreshMessage() {
        setMessage(Component.literal("").append(caption).append(Component.literal(": " + getValue())));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        boolean hoveredNow = this.isHovered() && this.active;
        int x1 = getX();
        int y1 = getY();
        int w = getWidth();
        int h = getHeight();
        int rr = MidnightShapes.controlRadius(h);
        int fill = !active ? 0xFF151A22 : (hoveredNow ? 0xFF243044 : 0xFF1A2030);
        int border = !active ? 0xFF52525B : (hoveredNow ? UiTokens.BLUE_400 : 0xFF64748B);
        MidnightShapes.fillRoundedFrame(context, x1, y1, w, h, rr, fill, border);

        int textColor = this.active ? UiTokens.TEXT : UiTokens.SLATE_500;
        context.centeredText(
                Minecraft.getInstance().font,
                getMessage(),
                x1 + w / 2,
                y1 + (h - 8) / 2,
                textColor);
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubleClick) {
        if (!this.active) {
            return;
        }
        index = (index + 1) % values.size();
        refreshMessage();
        if (onChange != null) {
            onChange.accept(this, getValue());
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }
}
