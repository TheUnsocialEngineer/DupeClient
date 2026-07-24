package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class Panel {
    protected static final int HEADER_HEIGHT = 24;

    protected final String id;
    protected final Component title;
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    protected boolean visible = true;
    protected boolean collapsed = false;

    private boolean dragging = false;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean allowDrag = true;
    /**
     * When embedded in {@link com.dupeclient.client.gui.modern.HubShell}, height of the scissored content area.
     */
    private int embedViewportH;
    private int embedStretchMinH;

    /** Toggle knob position in [0,1] for smooth animation (Midnight UI). */
    private final Map<String, Float> toggleKnobSmooth = new HashMap<>();

    public Panel(String id, Component title, int x, int y, int width, int height) {
        this.id = id;
        this.title = title;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!visible) {
            return;
        }

        if (embedViewportH > 0) {
            return;
        }

        int renderHeight = getRenderHeight();
        int body = UiTokens.argb(0xEE, UiTokens.SLATE_850);
        context.fill(x, y, x + width, y + renderHeight, body);
        UiDraw.dropShadow(context, x, y, width, renderHeight, 3);

        int hx = x;
        int hy = y;
        int hw = width;
        int hh = HEADER_HEIGHT;
        context.fillGradient(hx, hy, hx + hw, hy + hh, UiTokens.argb(0xCC, UiTokens.SLATE_800), UiTokens.argb(0xAA, UiTokens.SLATE_900));
        context.fill(hx, hy + hh - 1, hx + hw, hy + hh, UiTokens.argb(0x88, UiTokens.MINT_500));
        UiDraw.ring(context, x, y, width, renderHeight, UiTokens.argb(0x66, UiTokens.SLATE_600));

        context.text(
                Minecraft.getInstance().font,
                title, x + UiTokens.SP_3, y + 8, UiTokens.TEXT
        );

        String marker = collapsed ? "›" : "⌄";
        context.text(
                Minecraft.getInstance().font,
                Component.literal(marker), x + width - UiTokens.SP_4, y + 8, UiTokens.MINT_300
        );
    }

    public boolean containsPoint(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + getRenderHeight();
    }

    public boolean isInsideHeader(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width &&
               mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = Math.max(0, width);
    }

    public void setBaseHeight(int height) {
        this.height = Math.max(0, height);
    }

    public void setEmbedViewportH(int h) {
        this.embedViewportH = Math.max(0, h);
    }

    public void setEmbedStretchMinH(int h) {
        this.embedStretchMinH = Math.max(0, h);
    }

    /** Vertical offset from {@link #y} to first body row (no title bar when embedded in hub). */
    protected int bodyTopOffset() {
        return embedViewportH > 0 ? UiTokens.SP_2 : HEADER_HEIGHT + 4;
    }

    /**
     * Smooth toggle thumb position for {@link com.dupeclient.client.gui.modern.UiComponents#drawOptionToggle}.
     * Call each frame with stable {@code key} per control.
     */
    protected float smoothToggle(String key, boolean on, float delta) {
        float target = on ? 1f : 0f;
        float cur = toggleKnobSmooth.getOrDefault(key, target);
        float dt = Math.min(Math.max(delta, 0.001f), 0.25f);
        float k = 1f - (float) Math.exp(-18f * dt);
        float next = cur + (target - cur) * k;
        if (Math.abs(next - target) < 0.002f) {
            next = target;
        }
        toggleKnobSmooth.put(key, next);
        return next;
    }

    public int getEmbedViewportH() {
        return embedViewportH;
    }

    public void setDraggable(boolean allowDrag) {
        this.allowDrag = allowDrag;
        if (!allowDrag) {
            dragging = false;
        }
    }

    public String getId() {
        return id;
    }

    public Component getTitle() {
        return title;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return getRenderHeight();
    }

    public int getLayoutContentHeight() {
        if (collapsed) {
            return embedViewportH > 0 ? bodyTopOffset() : HEADER_HEIGHT;
        }
        return height;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public void tick() {
    }

    /** Called when this module is deselected in the hub (sidebar / pill nav). */
    public void onModuleHidden() {
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        if (embedViewportH > 0) {
            return false;
        }
        if (button == 0 && isInsideHeader(mouseX, mouseY) && mouseX >= x + width - 20) {
            collapsed = !collapsed;
            return true;
        }
        if (allowDrag && button == 0 && isInsideHeader(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!allowDrag || !visible || !dragging || button != 0) {
            return false;
        }
        this.x = (int) (mouseX - dragOffsetX);
        this.y = (int) (mouseY - dragOffsetY);
        clampToViewport();
        return true;
    }

    public void clampToViewport() {
        if (!allowDrag) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int edge = 8;
        this.x = Mth.clamp(x, -width + 48, sw - edge);
        int h = getRenderHeight();
        int minY = Math.min(edge, sh - h - edge);
        int maxY = Math.max(edge, sh - h - edge);
        this.y = Mth.clamp(y, minY, maxY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(int codePoint, int modifiers) {
        return false;
    }

    public boolean hasFocusedTextInput() {
        return false;
    }

    protected int getRenderHeight() {
        if (embedViewportH > 0) {
            if (collapsed) {
                return bodyTopOffset();
            }
            return height;
        }
        if (collapsed) {
            return HEADER_HEIGHT;
        }
        if (embedStretchMinH > 0) {
            return Math.max(height, embedStretchMinH);
        }
        return height;
    }
}
