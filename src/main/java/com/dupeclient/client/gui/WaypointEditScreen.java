package com.dupeclient.client.gui;

import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.module.waypoint.DupeClientWaypoint;
import com.dupeclient.client.module.waypoint.DupeClientWaypointManager;
import com.dupeclient.client.module.waypoint.WaypointColors;
import com.dupeclient.client.module.waypoint.WaypointShape;
import com.dupeclient.client.module.waypoint.WaypointShareAudience;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;

public final class WaypointEditScreen extends Screen {
    private final Screen parent;
    private final DupeClientWaypoint original;
    private final boolean creating;

    private StylishTextFieldWidget nameField;
    private StylishTextFieldWidget xField;
    private StylishTextFieldWidget yField;
    private StylishTextFieldWidget zField;
    private int colorArgb;
    private WaypointShape shape;
    private WaypointShareAudience shareAudience;
    private String status = "";
    private int panelX;
    private int panelW;
    private int innerX;
    private int innerW;

    public WaypointEditScreen(Screen parent, DupeClientWaypoint waypoint, boolean creating) {
        super(Component.literal(creating ? "Create Waypoint" : "Edit Waypoint"));
        this.parent = parent;
        this.original = waypoint;
        this.creating = creating;
        this.colorArgb = waypoint.colorArgb();
        this.shape = waypoint.shape();
        this.shareAudience = waypoint.shareAudience();
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        panelW = Math.min(420, width - 40);
        panelX = (width - panelW) / 2;
        innerX = panelX + 16;
        innerW = panelW - 32;

        int y = PANEL_TOP() + 36;
        nameField = field(innerX, y, innerW, "Name", original.name());
        y += 28;
        int third = (innerW - 12) / 3;
        xField = field(innerX, y, third, "X", String.valueOf(original.x()));
        yField = field(innerX + third + 6, y, third, "Y", String.valueOf(original.y()));
        zField = field(innerX + (third + 6) * 2, y, third, "Z", String.valueOf(original.z()));
        y += 28;
        addRenderableWidget(new StylishButtonWidget(innerX, y, innerW, 20, Component.literal("Use my position"), () -> {
            if (minecraft == null || minecraft.player == null) {
                status = "Join a world first.";
                return;
            }
            xField.setValue(String.valueOf((int) Math.floor(minecraft.player.getX())));
            yField.setValue(String.valueOf((int) Math.floor(minecraft.player.getY())));
            zField.setValue(String.valueOf((int) Math.floor(minecraft.player.getZ())));
        }));
        y += 26;
        addRenderableWidget(new StylishButtonWidget(innerX, y, innerW, 20,
            Component.literal("Color: " + WaypointColors.label(colorArgb)), () -> {
            colorArgb = WaypointColors.cycle(colorArgb);
            init();
        }));
        y += 26;
        addRenderableWidget(new StylishButtonWidget(innerX, y, innerW, 20,
            Component.literal("Shape: " + shape.label()), () -> {
            shape = shape.next();
            init();
        }));
        y += 26;
        addRenderableWidget(new StylishButtonWidget(innerX, y, innerW, 20,
            Component.literal("Share: " + shareAudience.label()), () -> {
            shareAudience = shareAudience.next();
            init();
        }));
        y += 34;
        addRenderableWidget(new StylishButtonWidget(innerX, y, innerW / 2 - 4, 20, Component.literal("Save"), this::save));
        addRenderableWidget(new StylishButtonWidget(innerX + innerW / 2 + 4, y, innerW / 2 - 4, 20, CommonComponents.GUI_CANCEL, () -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }));
        if (!creating) {
            addRenderableWidget(new StylishButtonWidget(innerX, y + 26, innerW, 20, Component.literal("Delete waypoint"), () -> {
                DupeClientWaypointManager.INSTANCE.delete(original.id());
                if (minecraft != null) {
                    minecraft.setScreen(parent);
                }
            }));
        }
        addRenderableWidget(new StylishButtonWidget(width / 2 - 100, height - 28, 200, 20, CommonComponents.GUI_BACK, () -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }));
        setInitialFocus(nameField);
    }

    private static int PANEL_TOP() {
        return 24;
    }

    private StylishTextFieldWidget field(int x, int y, int w, String label, String value) {
        StylishTextFieldWidget field = StylishTextFieldWidget.create(font, x, y, w, Component.literal(label));
        field.setMaxLength(label.equals("Name") ? 48 : 8);
        field.setValue(value);
        field.setPlaceholder(label);
        addRenderableWidget(field);
        return field;
    }

    private void save() {
        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(xField.getValue().trim());
            y = Integer.parseInt(yField.getValue().trim());
            z = Integer.parseInt(zField.getValue().trim());
        } catch (NumberFormatException ex) {
            status = "Coordinates must be whole numbers.";
            return;
        }
        String dim = DupeClientWaypointManager.currentDimensionKey(minecraft);
        DupeClientWaypoint updated = original.withEdits(nameField.getValue(), x, y, z, dim, colorArgb, shape, shareAudience);
        if (creating) {
            DupeClientWaypointManager.INSTANCE.add(updated);
        } else {
            DupeClientWaypointManager.INSTANCE.update(updated);
        }
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        UiDraw.fillMidnightBackground(context, width, height);
        int panelH = height - 48;
        UiDraw.cardElevated(context, panelX, PANEL_TOP(), panelW, panelH, 10);
        context.drawCenteredString(font, title, width / 2, PANEL_TOP() + 10, 0xFFE8EEF8);
        context.drawString(font, Component.literal("Name"), innerX, PANEL_TOP() + 26, 0xFFAFC7FF);
        context.drawString(font, Component.literal("Coordinates"), innerX, PANEL_TOP() + 54, 0xFFAFC7FF);
        context.fill(innerX + innerW - 28, PANEL_TOP() + 118, innerX + innerW - 8, PANEL_TOP() + 138, 0xFF000000 | (colorArgb & 0x00FFFFFF));
        if (!status.isEmpty()) {
            context.drawCenteredString(font, Component.literal(status), width / 2, height - 44, 0xFFF87171);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
    }
}
