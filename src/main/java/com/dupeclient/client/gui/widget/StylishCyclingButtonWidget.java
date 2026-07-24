package com.dupeclient.client.gui.widget;

import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.modern.theme.MidnightShapes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Cycling string picker with the same mint/slate look as {@link StylishButtonWidget}.
 */
public class StylishCyclingButtonWidget extends ClickableWidget {
    private final List<String> values;
    private int index;
    private final Text caption;
    @Nullable
    private final BiConsumer<StylishCyclingButtonWidget, String> onChange;

    public StylishCyclingButtonWidget(
            int x,
            int y,
            int w,
            int h,
            Text caption,
            List<String> values,
            String initialValue,
            @Nullable BiConsumer<StylishCyclingButtonWidget, String> onChange) {
        super(x, y, w, h, Text.empty());
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
        setMessage(Text.literal("").append(caption).append(Text.literal(": " + getValue())));
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
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
        context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                getMessage(),
                x1 + w / 2,
                y1 + (h - 8) / 2,
                textColor);
    }

    @Override
    public void onClick(Click click, boolean doubleClick) {
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
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
