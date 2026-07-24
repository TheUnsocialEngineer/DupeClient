package com.dupeclient.client.gui.widget;

import com.dupeclient.client.gui.overlay.SearchableDropdown;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Midnight-styled searchable dropdown for use on full screens (e.g. NBT enchant picker).
 */
public final class StylishSearchableDropdownWidget extends AbstractWidget {
    private final SearchableDropdown dropdown;
    private final List<String> options;
    @Nullable
    private final Consumer<String> onChange;

    public StylishSearchableDropdownWidget(
            int x,
            int y,
            int width,
            int height,
            String placeholder,
            List<String> options,
            String initialValue,
            @Nullable Consumer<String> onChange) {
        super(x, y, width, height, Component.empty());
        this.options = options;
        this.onChange = onChange;
        this.dropdown = new SearchableDropdown(placeholder, 6);
        this.dropdown.setModernChrome(true);
        this.dropdown.setDisplayValue(initialValue == null ? "" : initialValue);
    }

    public String getValue() {
        String value = dropdown.displayValue();
        return value == null ? "" : value;
    }

    public void setValue(String value) {
        dropdown.setDisplayValue(value == null ? "" : value);
    }

    public boolean isOpen() {
        return dropdown.isOpen();
    }

    public boolean hasTextFocus() {
        return dropdown.hasTextFocus();
    }

    public void close() {
        dropdown.close();
    }

    public int extraHeight() {
        return dropdown.extraHeight();
    }

    public boolean hitsInteractive(double mouseX, double mouseY) {
        return dropdown.hitsInteractive(mouseX, mouseY);
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        var tr = Minecraft.getInstance().font;
        dropdown.render(context, tr, getX(), getY(), getWidth(), getHeight(), options, mouseX, mouseY);
    }

    public void renderPopupLayer(GuiGraphics context) {
        var tr = Minecraft.getInstance().font;
        dropdown.renderPopupLayer(context, tr, options, 0, 0);
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubleClick) {
        handleMouseClick(click.x(), click.y(), click.button());
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        return dropdown.mouseClicked(mouseX, mouseY, button, options, this::applySelection);
    }

    public boolean handleMouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        return dropdown.mouseScrolled(mouseX, mouseY, verticalAmount, options);
    }

    public boolean handleKeyPressed(int keyCode) {
        return dropdown.keyPressed(keyCode);
    }

    public boolean handleCharTyped(int codePoint) {
        return dropdown.charTyped(codePoint);
    }

    private void applySelection(String value) {
        dropdown.setDisplayValue(value);
        if (onChange != null) {
            onChange.accept(value);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }
}
