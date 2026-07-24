package com.dupeclient.client.multiplayer;

import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ProxiesScreen extends Screen {
    private static final int ROW_H = 30;
    private static final int PANEL_TOP = 24;
    private static final int LABEL_H = 10;
    private static final int FIELD_H = 20;
    private static final int FIELD_GAP = 8;

    private final Screen parent;
    private final List<ProxyProfile> proxies = new ArrayList<>();
    private StylishTextFieldWidget nameField;
    private StylishTextFieldWidget hostField;
    private StylishTextFieldWidget portField;
    private StylishTextFieldWidget userField;
    private StylishTextFieldWidget passField;
    private ProxyType draftType = ProxyType.SOCKS5;
    private int scroll;
    private String status = "";
    private int panelX;
    private int panelW;
    private int innerX;
    private int innerW;
    private int listTop;
    private int listBottom;

    public ProxiesScreen(Screen parent) {
        super(Text.literal("Proxies"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        String savedName = nameField != null ? nameField.getText() : "";
        String savedHost = hostField != null ? hostField.getText() : "";
        String savedPort = portField != null ? portField.getText() : "";
        String savedUser = userField != null ? userField.getText() : "";
        String savedPass = passField != null ? passField.getText() : "";

        clearChildren();
        ProxyManager.INSTANCE.reload();
        proxies.clear();
        proxies.addAll(ProxyManager.INSTANCE.getProxies());
        ProxyHealthChecker.requestCheckAll(proxies, false);

        panelW = Math.min(500, width - 40);
        panelX = (width - panelW) / 2;
        innerX = panelX + 16;
        innerW = panelW - 32;

        int row1Y = PANEL_TOP + 56;
        int row2Y = row1Y + LABEL_H + FIELD_H + 10;
        int row3Y = row2Y + LABEL_H + FIELD_H + 10;
        int halfW = (innerW - FIELD_GAP) / 2;

        nameField = addField(innerX, row1Y, halfW, "My proxy");
        hostField = addField(innerX + halfW + FIELD_GAP, row1Y, halfW, "127.0.0.1");

        portField = addField(innerX, row2Y, 72, "1080");
        if (portField.getText().isBlank()) {
            portField.setText("1080");
        }
        userField = addField(innerX + 80, row2Y, halfW - 36, "Username");
        passField = addField(innerX + halfW + FIELD_GAP + 44, row2Y, halfW - 44, "Password");
        passField.setMaxLength(128);
        nameField.setText(savedName);
        hostField.setText(savedHost);
        if (!savedPort.isBlank()) {
            portField.setText(savedPort);
        }
        userField.setText(savedUser);
        passField.setText(savedPass);

        addDrawableChild(new StylishButtonWidget(innerX, row3Y, 88, 20, Text.literal(draftType.label), () -> {
            draftType = draftType.next();
            init();
        }));
        addDrawableChild(new StylishButtonWidget(innerX + 96, row3Y, 96, 20, Text.literal("Add proxy"), this::addProxy));
        addDrawableChild(new StylishButtonWidget(innerX + 200, row3Y, 104, 20, Text.literal("Test all"), () -> {
            ProxyHealthChecker.requestCheckAll(proxies, true);
            status = "Testing proxies...";
        }));
        addDrawableChild(new StylishButtonWidget(innerX + 312, row3Y, 104, 20, Text.literal("Disable"), () -> {
            ProxyManager.INSTANCE.clearActive();
            status = "Proxy disabled";
            init();
        }));

        listTop = row3Y + 34;
        listBottom = height - 56;
        int visibleRows = Math.max(1, (listBottom - listTop) / ROW_H);
        scroll = Math.min(scroll, Math.max(0, proxies.size() - visibleRows));

        for (int i = 0; i < visibleRows; i++) {
            int idx = scroll + i;
            if (idx >= proxies.size()) {
                break;
            }
            ProxyProfile proxy = proxies.get(idx);
            int rowY = listTop + i * ROW_H;
            int actionW = 58;
            int gap = 6;
            int useX = innerX + innerW - actionW * 2 - gap;
            int delX = innerX + innerW - actionW;

            addDrawableChild(new StylishButtonWidget(useX, rowY + 5, actionW, 20, Text.literal("Use"), () -> {
                ProxyManager.INSTANCE.setActiveId(proxy.id());
                ProxyHealthChecker.requestCheck(proxy, true);
                status = "Active: " + proxy.displayLabel();
                init();
            }));
            addDrawableChild(new StylishButtonWidget(delX, rowY + 5, actionW, 20, Text.literal("Delete"), () -> {
                ProxyManager.INSTANCE.remove(proxy.id());
                status = "Removed " + proxy.name();
                init();
            }));
        }

        addDrawableChild(new StylishButtonWidget(width / 2 - 100, height - 28, 200, 20, ScreenTexts.BACK, () ->
            MultiplayerScreens.returnToMultiplayer(client, parent)));
    }

    @Override
    public void close() {
        MultiplayerScreens.returnToMultiplayer(client, parent);
    }

    private StylishTextFieldWidget addField(int x, int y, int w, String placeholder) {
        StylishTextFieldWidget field = StylishTextFieldWidget.create(textRenderer, x, y, w, FIELD_H, Text.literal(placeholder));
        field.setPlaceholder(placeholder);
        field.setMaxLength(128);
        addDrawableChild(field);
        return field;
    }

    private void addProxy() {
        String host = hostField.getText().trim();
        int port = parsePort(portField.getText());
        if (host.isEmpty() || port <= 0) {
            status = "Enter a valid host and port.";
            return;
        }
        ProxyProfile profile = ProxyProfile.create(
            nameField.getText(),
            host,
            port,
            draftType,
            userField.getText(),
            passField.getText()
        );
        ProxyManager.INSTANCE.add(profile);
        ProxyHealthChecker.requestCheck(profile, true);
        status = "Added " + profile.displayLabel();
        init();
    }

    private static int parsePort(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ex) {
            return -1;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            int maxScroll = Math.max(0, proxies.size() - Math.max(1, (listBottom - listTop) / ROW_H));
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) verticalAmount));
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        UiDraw.fillMidnightBackground(context, width, height);
        int panelH = height - 48;
        UiDraw.cardElevated(context, panelX, PANEL_TOP, panelW, panelH, 10);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, PANEL_TOP + 10, 0xFFE8EEF8);

        ProxyManager.INSTANCE.getActive().ifPresentOrElse(
            proxy -> {
                context.drawTextWithShadow(textRenderer, Text.literal("Active "), innerX, PANEL_TOP + 28, 0xFF8FA3B8);
                context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(proxy.displayLabel()),
                    innerX + textRenderer.getWidth("Active "),
                    PANEL_TOP + 28,
                    0xFF4ADE80
                );
            },
            () -> context.drawTextWithShadow(textRenderer, Text.literal("Active: none"), innerX, PANEL_TOP + 28, 0xFF8FA3B8)
        );

        int row1Y = PANEL_TOP + 56;
        int row2Y = row1Y + LABEL_H + FIELD_H + 10;
        drawFieldLabel(context, "Label", innerX, row1Y - LABEL_H);
        drawFieldLabel(context, "Host", innerX + (innerW - FIELD_GAP) / 2 + FIELD_GAP, row1Y - LABEL_H);
        drawFieldLabel(context, "Port", innerX, row2Y - LABEL_H);
        drawFieldLabel(context, "Username", innerX + 80, row2Y - LABEL_H);
        drawFieldLabel(context, "Password", innerX + (innerW - FIELD_GAP) / 2 + FIELD_GAP + 44, row2Y - LABEL_H);

        if (!proxies.isEmpty()) {
            int headerY = listTop - 12;
            context.drawTextWithShadow(textRenderer, Text.literal("Status"), innerX + 2, headerY, 0xFF8899BB);
            context.drawTextWithShadow(textRenderer, Text.literal("Proxy"), innerX + 52, headerY, 0xFF8899BB);
            context.drawTextWithShadow(textRenderer, Text.literal("Ping"), innerX + innerW - 200, headerY, 0xFF8899BB);
            context.drawTextWithShadow(textRenderer, Text.literal("Region"), innerX + innerW - 128, headerY, 0xFF8899BB);
        }

        int visibleRows = Math.max(1, (listBottom - listTop) / ROW_H);
        for (int i = 0; i < visibleRows; i++) {
            int idx = scroll + i;
            if (idx >= proxies.size()) {
                break;
            }
            ProxyProfile proxy = proxies.get(idx);
            int rowY = listTop + i * ROW_H;
            boolean active = proxy.id().equals(ProxyManager.INSTANCE.getActiveId());
            ProxyHealth health = ProxyHealthChecker.healthFor(proxy);

            int rowBg = active ? UiTokens.argb(0x55, 0x3B82F6) : UiTokens.argb(0x35, 0x1A2236);
            context.fill(innerX, rowY, innerX + innerW, rowY + ROW_H - 2, rowBg);
            if (active) {
                context.fill(innerX, rowY, innerX + 2, rowY + ROW_H - 2, 0xFF60A5FA);
            }

            drawStatusDot(context, innerX + 8, rowY + 12, health.statusColor());
            context.drawTextWithShadow(textRenderer, Text.literal(health.statusLabel()), innerX + 18, rowY + 6, health.statusColor());

            String title = proxy.name();
            if (title.length() > 18) {
                title = title.substring(0, 17) + "…";
            }
            context.drawTextWithShadow(textRenderer, Text.literal(title), innerX + 52, rowY + 5, 0xFFE8EEF8);
            String endpoint = proxy.type().label + " " + proxy.host() + ":" + proxy.port();
            if (endpoint.length() > 28) {
                endpoint = endpoint.substring(0, 27) + "…";
            }
            context.drawTextWithShadow(textRenderer, Text.literal(endpoint), innerX + 52, rowY + 16, 0xFF8FA3B8);

            context.drawTextWithShadow(
                textRenderer,
                Text.literal(health.pingLabel()),
                innerX + innerW - 200,
                rowY + 10,
                health.pingColor()
            );

            String region = health.region();
            if (region.isBlank()) {
                region = "—";
            }
            if (region.length() > 16) {
                region = region.substring(0, 15) + "…";
            }
            context.drawTextWithShadow(textRenderer, Text.literal(region), innerX + innerW - 128, rowY + 10, 0xFFCAD9FF);
        }

        if (proxies.isEmpty()) {
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("No proxies saved"),
                width / 2,
                listTop + 24,
                0xFF8FA3B8
            );
        }

        if (!status.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(status), width / 2, height - 44, 0xFF00E676);
        }

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void drawFieldLabel(DrawContext context, String label, int x, int y) {
        context.drawTextWithShadow(textRenderer, Text.literal(label), x, y, 0xFF8FA3B8);
    }

    private static void drawStatusDot(DrawContext context, int cx, int cy, int color) {
        context.fill(cx - 3, cy - 3, cx + 3, cy + 3, color);
        context.fill(cx - 2, cy - 2, cx + 2, cy + 2, UiTokens.argb(0xFF, 0x0A1020));
        context.fill(cx - 2, cy - 2, cx + 2, cy + 2, color);
    }
}
