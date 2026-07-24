package com.dupeclient.client.multiplayer;

import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class OfflineAccountsScreen extends Screen {
    private static final int ROW_H = 28;
    private static final int HEAD_SIZE = 18;
    private static final int PANEL_TOP = 24;

    private final Screen parent;
    private final List<OfflineAccount> accounts = new ArrayList<>();
    private StylishTextFieldWidget usernameField;
    private int scroll;
    private String status = "";
    private int panelX;
    private int panelW;
    private int innerX;
    private int innerW;
    private int listTop;
    private int listBottom;

    public OfflineAccountsScreen(Screen parent) {
        super(Text.literal("Accounts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        accounts.clear();
        accounts.addAll(OfflineAccountStore.load());
        if (client != null) {
            OfflineAccountSkins.prefetchAll(client, accounts);
        }

        panelW = Math.min(440, width - 40);
        panelX = (width - panelW) / 2;
        innerX = panelX + 16;
        innerW = panelW - 32;

        int fieldY = PANEL_TOP + 58;
        usernameField = StylishTextFieldWidget.create(textRenderer, innerX, fieldY, innerW - 208, Text.literal("Username"));
        usernameField.setMaxLength(16);
        usernameField.setPlaceholder("Username");
        addDrawableChild(usernameField);
        setInitialFocus(usernameField);

        addDrawableChild(new StylishButtonWidget(innerX + innerW - 200, fieldY, 96, 20, Text.literal("Add"), () -> {
            String name = usernameField.getText().trim();
            if (name.isEmpty()) {
                status = "Enter a username.";
                return;
            }
            for (OfflineAccount existing : accounts) {
                if (existing.username().equalsIgnoreCase(name)) {
                    status = "Account already saved.";
                    return;
                }
            }
            OfflineAccount added = OfflineAccount.ofUsername(name);
            accounts.add(added);
            OfflineAccountStore.save(accounts);
            if (client != null) {
                OfflineAccountSkins.prefetchAll(client, List.of(added));
            }
            usernameField.setText("");
            status = "Saved " + name;
            init();
        }));
        addDrawableChild(new StylishButtonWidget(innerX + innerW - 100, fieldY, 100, 20, Text.literal("SSID Login"), () -> {
            if (client != null) {
                client.setScreen(new SsidLoginScreen(this));
            }
        }));

        listTop = fieldY + 34;
        listBottom = height - 56;
        int visibleRows = Math.max(1, (listBottom - listTop) / ROW_H);
        scroll = Math.min(scroll, Math.max(0, accounts.size() - visibleRows));

        for (int i = 0; i < visibleRows; i++) {
            int idx = scroll + i;
            if (idx >= accounts.size()) {
                break;
            }
            OfflineAccount account = accounts.get(idx);
            int rowY = listTop + i * ROW_H;
            int actionW = 58;
            int gap = 6;
            int useX = innerX + innerW - actionW * 2 - gap;
            int delX = innerX + innerW - actionW;

            addDrawableChild(new StylishButtonWidget(useX, rowY + 4, actionW, 20, Text.literal("Use"), () -> {
                OfflineAccountManager.apply(client, account);
                status = "Logged in as " + account.username();
                init();
            }));
            addDrawableChild(new StylishButtonWidget(delX, rowY + 4, actionW, 20, Text.literal("Delete"), () -> {
                accounts.removeIf(entry -> entry.username().equalsIgnoreCase(account.username()));
                OfflineAccountStore.save(accounts);
                status = "Removed " + account.username();
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            int maxScroll = Math.max(0, accounts.size() - Math.max(1, (listBottom - listTop) / ROW_H));
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

        String current = client != null ? client.getSession().getUsername() : "";
        context.drawTextWithShadow(textRenderer, Text.literal("Signed in as "), innerX, PANEL_TOP + 28, 0xFF8FA3B8);
        context.drawTextWithShadow(textRenderer, Text.literal(current), innerX + textRenderer.getWidth("Signed in as "), PANEL_TOP + 28, 0xFF4ADE80);

        context.drawTextWithShadow(textRenderer, Text.literal("Add account"), innerX, PANEL_TOP + 48, 0xFFAFC7FF);

        int visibleRows = Math.max(1, (listBottom - listTop) / ROW_H);
        if (!accounts.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal("Saved accounts"), innerX, listTop - 12, 0xFFAFC7FF);
        }

        MinecraftClient mc = client;
        for (int i = 0; i < visibleRows; i++) {
            int idx = scroll + i;
            if (idx >= accounts.size()) {
                break;
            }
            OfflineAccount account = accounts.get(idx);
            int rowY = listTop + i * ROW_H;
            boolean active = account.username().equalsIgnoreCase(OfflineAccountManager.getActiveUsername())
                || (mc != null && account.username().equalsIgnoreCase(mc.getSession().getUsername()));

            int rowBg = active ? UiTokens.argb(0x55, 0x22C55E) : UiTokens.argb(0x35, 0x1A2236);
            context.fill(innerX, rowY, innerX + innerW, rowY + ROW_H - 2, rowBg);
            if (active) {
                context.fill(innerX, rowY, innerX + 2, rowY + ROW_H - 2, 0xFF4ADE80);
            }

            if (mc != null) {
                SkinTextures skin = OfflineAccountSkins.texturesFor(mc, account);
                PlayerSkinDrawer.draw(context, skin, innerX + 4, rowY + 5, HEAD_SIZE);
            }

            int nameX = innerX + 28;
            int nameColor = active ? 0xFF4ADE80 : 0xFFE8EEF8;
            context.drawTextWithShadow(textRenderer, Text.literal(account.username()), nameX, rowY + 6, nameColor);
            String sub = active ? "Active" : "Offline";
            context.drawTextWithShadow(textRenderer, Text.literal(sub), nameX, rowY + 16, 0xFF8FA3B8);
        }

        if (accounts.isEmpty()) {
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("No saved accounts yet"),
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
}
