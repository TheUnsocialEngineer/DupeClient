package com.dupeclient.client.multiplayer;

import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

public class OfflineAccountsScreen extends Screen implements MultiplayerNavigable {
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
        super(Component.literal("Accounts"));
        this.parent = parent;
    }

    @Override
    public Screen getNavigationParent() {
        return parent;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        accounts.clear();
        accounts.addAll(OfflineAccountStore.load());
        if (minecraft != null) {
            OfflineAccountSkins.prefetchAll(minecraft, accounts);
        }

        panelW = Math.min(440, width - 40);
        panelX = (width - panelW) / 2;
        innerX = panelX + 16;
        innerW = panelW - 32;

        int fieldY = PANEL_TOP + 58;
        usernameField = StylishTextFieldWidget.create(font, innerX, fieldY, innerW - 208, Component.literal("Username"));
        usernameField.setMaxLength(16);
        usernameField.setPlaceholder("Username");
        addRenderableWidget(usernameField);
        setInitialFocus(usernameField);

        addRenderableWidget(new StylishButtonWidget(innerX + innerW - 200, fieldY, 96, 20, Component.literal("Add"), () -> {
            String name = usernameField.getValue().trim();
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
            if (minecraft != null) {
                OfflineAccountSkins.prefetchAll(minecraft, List.of(added));
            }
            usernameField.setValue("");
            status = "Saved " + name;
            init();
        }));
        addRenderableWidget(new StylishButtonWidget(innerX + innerW - 100, fieldY, 100, 20, Component.literal("SSID Login"), () -> {
            if (minecraft != null) {
                minecraft.gui.setScreen(new SsidLoginScreen(this));
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

            addRenderableWidget(new StylishButtonWidget(useX, rowY + 4, actionW, 20, Component.literal("Use"), () -> {
                OfflineAccountManager.apply(minecraft, account);
                status = "Logged in as " + account.username();
                init();
            }));
            addRenderableWidget(new StylishButtonWidget(delX, rowY + 4, actionW, 20, Component.literal("Delete"), () -> {
                accounts.removeIf(entry -> entry.username().equalsIgnoreCase(account.username()));
                OfflineAccountStore.save(accounts);
                status = "Removed " + account.username();
                init();
            }));
        }

        addRenderableWidget(new StylishButtonWidget(width / 2 - 100, height - 28, 200, 20, CommonComponents.GUI_BACK, () ->
            MultiplayerScreens.returnToMultiplayer(minecraft, parent)));
    }

    @Override
    public void onClose() {
        MultiplayerScreens.returnToMultiplayer(minecraft, parent);
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        UiDraw.fillMidnightBackground(context, width, height);
        int panelH = height - 48;
        UiDraw.cardElevated(context, panelX, PANEL_TOP, panelW, panelH, 10);

        context.centeredText(font, title, width / 2, PANEL_TOP + 10, 0xFFE8EEF8);

        String current = minecraft != null ? minecraft.getUser().getName() : "";
        context.text(font, Component.literal("Signed in as "), innerX, PANEL_TOP + 28, 0xFF8FA3B8);
        context.text(font, Component.literal(current), innerX + font.width("Signed in as "), PANEL_TOP + 28, 0xFF4ADE80);

        context.text(font, Component.literal("Add account"), innerX, PANEL_TOP + 48, 0xFFAFC7FF);

        int visibleRows = Math.max(1, (listBottom - listTop) / ROW_H);
        if (!accounts.isEmpty()) {
            context.text(font, Component.literal("Saved accounts"), innerX, listTop - 12, 0xFFAFC7FF);
        }

        Minecraft mc = minecraft;
        for (int i = 0; i < visibleRows; i++) {
            int idx = scroll + i;
            if (idx >= accounts.size()) {
                break;
            }
            OfflineAccount account = accounts.get(idx);
            int rowY = listTop + i * ROW_H;
            boolean active = account.username().equalsIgnoreCase(OfflineAccountManager.getActiveUsername())
                || (mc != null && account.username().equalsIgnoreCase(mc.getUser().getName()));

            int rowBg = active ? UiTokens.argb(0x55, 0x22C55E) : UiTokens.argb(0x35, 0x1A2236);
            context.fill(innerX, rowY, innerX + innerW, rowY + ROW_H - 2, rowBg);
            if (active) {
                context.fill(innerX, rowY, innerX + 2, rowY + ROW_H - 2, 0xFF4ADE80);
            }

            if (mc != null) {
                PlayerSkin skin = OfflineAccountSkins.texturesFor(mc, account);
                PlayerFaceExtractor.extractRenderState(context, skin, innerX + 4, rowY + 5, HEAD_SIZE);
            }

            int nameX = innerX + 28;
            int nameColor = active ? 0xFF4ADE80 : 0xFFE8EEF8;
            context.text(font, Component.literal(account.username()), nameX, rowY + 6, nameColor);
            String sub = active ? "Active" : "Offline";
            context.text(font, Component.literal(sub), nameX, rowY + 16, 0xFF8FA3B8);
        }

        if (accounts.isEmpty()) {
            context.centeredText(
                font,
                Component.literal("No saved accounts yet"),
                width / 2,
                listTop + 24,
                0xFF8FA3B8
            );
        }

        if (!status.isEmpty()) {
            context.centeredText(font, Component.literal(status), width / 2, height - 44, 0xFF00E676);
        }

        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
    }
}
