package com.dupeclient.client.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Compact +/- integer control with a directly editable center field.
 */
public final class IntegerStepperWidget {
    private static final int BTN_MIN_W = 18;
    private static final int BTN_MAX_W = 24;

    private final int min;
    private final int max;
    private final StylishButtonWidget minusButton;
    private final StylishTextFieldWidget valueField;
    private final StylishButtonWidget plusButton;

    public IntegerStepperWidget(
            TextRenderer textRenderer,
            int x,
            int y,
            int width,
            int height,
            int min,
            int max,
            int initial,
            @Nullable IntConsumer onChange) {
        this.min = min;
        this.max = max;
        int buttonWidth = Math.max(BTN_MIN_W, Math.min(BTN_MAX_W, height + 2));
        int valueX = x + buttonWidth + 3;
        int valueW = Math.max(24, width - (buttonWidth + 3) * 2);

        minusButton = new StylishButtonWidget(x, y, buttonWidth, height, Text.literal("−"), () -> bump(-1, onChange));
        valueField = new StylishTextFieldWidget(textRenderer, valueX, y, valueW, height, Text.empty());
        valueField.setMaxLength(11);
        valueField.setCentered(true);
        valueField.setTextPredicate(IntegerStepperWidget::isPartialIntInput);
        valueField.setText(Integer.toString(clamp(initial)));
        valueField.setChangedListener(text -> {
            if (isCompleteInt(text)) {
                int parsed = clamp(Integer.parseInt(text));
                String shown = Integer.toString(parsed);
                if (!shown.equals(text)) {
                    valueField.setText(shown);
                }
                if (onChange != null) {
                    onChange.accept(parsed);
                }
            }
        });
        plusButton = new StylishButtonWidget(
                x + width - buttonWidth, y, buttonWidth, height, Text.literal("+"), () -> bump(1, onChange));
    }

    public List<ClickableWidget> widgets() {
        return List.of(minusButton, valueField, plusButton);
    }

    public StylishTextFieldWidget valueField() {
        return valueField;
    }

    public int getValue() {
        String text = valueField.getText().trim();
        if (!isCompleteInt(text)) {
            return clamp(0);
        }
        try {
            return clamp(Integer.parseInt(text));
        } catch (NumberFormatException ignored) {
            return clamp(0);
        }
    }

    public void setValue(int next) {
        valueField.setText(Integer.toString(clamp(next)));
    }

    private void bump(int delta, @Nullable IntConsumer onChange) {
        int next = clamp(getValue() + delta);
        setValue(next);
        if (onChange != null) {
            onChange.accept(next);
        }
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }

    private static boolean isPartialIntInput(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        if (text.equals("-")) {
            return true;
        }
        return text.matches("-?\\d*");
    }

    private static boolean isCompleteInt(String text) {
        return text != null && !text.isEmpty() && !text.equals("-") && text.matches("-?\\d+");
    }
}
