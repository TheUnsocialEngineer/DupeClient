package com.dupeclient.client.gui;

import com.dupeclient.client.gui.GuiContextMenu;
import com.dupeclient.client.gui.WaypointsScreen;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiDraw;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.gui.modern.theme.MidnightPalette;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.gui.widget.StylishButtonWidget;
import com.dupeclient.client.core.session.SocialHubRules;
import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import com.dupeclient.client.module.cape.DupeClientPresenceConfigManager;
import com.dupeclient.client.module.cape.DupeClientPresenceSettings;
import com.dupeclient.client.module.social.DupeClientSocialFriendsManager;
import com.dupeclient.client.module.social.DupeClientSocialListFetcher;
import com.dupeclient.client.module.social.OnlineDupeClientUser;
import com.dupeclient.client.module.mcptools.McpToolsManager;
import com.dupeclient.client.module.waypoint.DupeClientWaypointManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class SocialScreen
extends Screen {
    private static final int CARD_H = 54;
    private static final int CARD_GAP = 8;
    private static final int LIST_INSET = 10;
    private static final int LIST_HEADER_H = 34;
    private static final int JOIN_BTN_W = 52;
    private static final int JOIN_BTN_H = 20;
    private static final int SETTINGS_BLOCK_H = UiTokens.CARD_CONTENT_TOP + UiTokens.ROW_STEP * 9 + UiTokens.SP_4;
    private static final int TOOLBAR_Y = 8;
    private static final int TOOLBAR_H = 22;
    private static final int TAB_ALL_X = -100;
    private static final int TAB_ALL_W = 44;
    private static final int TAB_SERVER_X = -54;
    private static final int TAB_SERVER_W = 70;
    private static final int TAB_SETTINGS_X = 18;
    private static final int TAB_SETTINGS_W = 58;
    private final Screen parent;
    private final List<OnlineDupeClientUser> allRows = new ArrayList<OnlineDupeClientUser>();
    private final List<OnlineDupeClientUser> rows = new ArrayList<OnlineDupeClientUser>();
    private boolean loading = true;
    private String statusHint;
    private int listScrollY;
    private int ticksUntilAutoRefresh = 60;
    private ScreenTab screenTab = ScreenTab.ALL;
    private final GuiContextMenu contextMenu = new GuiContextMenu();

    public SocialScreen(Screen parent) {
        super(Text.literal("DupeClient Social"));
        this.parent = parent;
    }

    protected void init() {
        if (!SocialHubRules.socialUiAllowed()) {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
            return;
        }
        DupeClientPresenceConfigManager.reload();
        DupeClientSocialFriendsManager.reload();
        P2wServerPolicy.INSTANCE.refreshRegistryAsync();
        this.clearChildren();
        int cx = this.width / 2;
        this.addDrawableChild(new StylishButtonWidget(cx - 172, TOOLBAR_Y, 72, TOOLBAR_H, Text.literal("Refresh"), this::requestList));
        this.addDrawableChild(new StylishButtonWidget(cx + 82, TOOLBAR_Y, 72, TOOLBAR_H, Text.literal("Waypoints"), () -> {
            if (this.client != null) {
                this.client.setScreen(new WaypointsScreen(this));
            }
        }));
        this.addDrawableChild(new StylishButtonWidget(cx + 158, TOOLBAR_Y, 72, TOOLBAR_H, Text.literal("Done"), () -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }));
        this.requestList();
    }

    private void requestList() {
        this.loading = true;
        this.statusHint = null;
        DupeClientSocialListFetcher.fetchAsync(this.client, (list, errorHint) -> {
            this.allRows.clear();
            MinecraftClient mc = this.client;
            UUID self = mc != null && mc.player != null ? mc.player.getUuid() : null;
            DupeClientPresenceSettings cfg = DupeClientPresenceConfigManager.get();
            boolean hideSelf = Boolean.TRUE.equals(cfg.hideSelfInSocial);
            boolean friendsOnlyView = Boolean.TRUE.equals(cfg.socialListFriendsOnlyView);
            Set<UUID> friends = DupeClientSocialFriendsManager.friendUuidSet();
            for (OnlineDupeClientUser u : list) {
                if (hideSelf && self != null && u.minecraftUuid().equals(self)) continue;
                boolean inFriends = friends.contains(u.minecraftUuid());
                if (friendsOnlyView && !inFriends) continue;
                this.allRows.add(u);
            }
            this.statusHint = errorHint;
            this.loading = false;
            this.listScrollY = 0;
            this.ticksUntilAutoRefresh = 200;
            this.rebuildRows();
        });
    }

    private void rebuildRows() {
        this.rows.clear();
        String currentServer = SocialScreen.currentServerKey(this.client);
        for (OnlineDupeClientUser u : this.allRows) {
            String userServer;
            if (this.screenTab == ScreenTab.CURRENT_SERVER && (currentServer == null || (userServer = SocialScreen.normalizeServerKey(u.server())) == null || !userServer.equals(currentServer))) continue;
            this.rows.add(u);
        }
        this.listScrollY = 0;
    }

    private int contentAreaTop() {
        return TOOLBAR_Y + TOOLBAR_H + UiTokens.SP_3;
    }

    private int contentAreaBottom() {
        return this.height - UiTokens.SP_2;
    }

    private int listAreaTop() {
        return this.contentAreaTop();
    }

    private int listAreaBottom() {
        return this.contentAreaBottom();
    }

    private int listContentTop() {
        return this.listAreaTop() + LIST_HEADER_H;
    }

    private int cardWidth(int panelW) {
        int scroll = this.maxListScrollY() > 0 ? 10 : 0;
        return panelW - LIST_INSET * 2 - scroll;
    }

    private int cardX(int panelX) {
        return panelX + LIST_INSET;
    }

    private int listAreaHeight() {
        return Math.max(24, this.listAreaBottom() - this.listAreaTop());
    }

    private int listContentHeight() {
        if (this.loading) {
            return 40;
        }
        if (this.rows.isEmpty()) {
            return 48;
        }
        return UiTokens.SP_2 + this.rows.size() * (CARD_H + CARD_GAP);
    }

    private void drawToolbarTabs(DrawContext context, int mouseX, int mouseY) {
        int cx = this.width / 2;
        int ty = TOOLBAR_Y;
        UiComponents.drawSegmentTab(this.textRenderer, context, cx + TAB_ALL_X, ty, TAB_ALL_W, TOOLBAR_H, "All", this.screenTab == ScreenTab.ALL);
        UiComponents.drawSegmentTab(
                this.textRenderer, context, cx + TAB_SERVER_X, ty, TAB_SERVER_W, TOOLBAR_H, "This Server", this.screenTab == ScreenTab.CURRENT_SERVER);
        UiComponents.drawSegmentTab(
                this.textRenderer, context, cx + TAB_SETTINGS_X, ty, TAB_SETTINGS_W, TOOLBAR_H, "Settings", this.screenTab == ScreenTab.SETTINGS);
    }

    private boolean clickToolbarTab(double mx, double my) {
        if (my < TOOLBAR_Y || my >= TOOLBAR_Y + TOOLBAR_H) {
            return false;
        }
        int cx = this.width / 2;
        if (mx >= cx + TAB_ALL_X && mx < cx + TAB_ALL_X + TAB_ALL_W) {
            this.screenTab = ScreenTab.ALL;
            this.rebuildRows();
            return true;
        }
        if (mx >= cx + TAB_SERVER_X && mx < cx + TAB_SERVER_X + TAB_SERVER_W) {
            this.screenTab = ScreenTab.CURRENT_SERVER;
            this.rebuildRows();
            return true;
        }
        if (mx >= cx + TAB_SETTINGS_X && mx < cx + TAB_SETTINGS_X + TAB_SETTINGS_W) {
            this.screenTab = ScreenTab.SETTINGS;
            return true;
        }
        return false;
    }

    private void drawPlayerCard(
            DrawContext context,
            int cardX,
            int cardY,
            int cardW,
            int rowIndex,
            OnlineDupeClientUser user,
            boolean friend,
            String currentServer,
            boolean showSrv,
            boolean showCoords,
            int mouseX,
            int mouseY) {
        boolean hovered = mouseX >= cardX && mouseX < cardX + cardW && mouseY >= cardY && mouseY < cardY + CARD_H;
        String serverLine = null;
        String coordsLine = null;
        if (showSrv) {
            String srv = user.server() != null ? user.server() : "—";
            String p2w = P2wServerPolicy.INSTANCE.registryStatusForServer(srv);
            serverLine = p2w.isBlank() ? "Server · " + srv : "Server · " + srv + " · " + p2w;
        }
        if (showCoords) {
            String coords = user.coords() != null ? user.coords() : "—";
            coordsLine = "Coords · " + coords;
        }
        boolean showJoin = showSrv && this.canJoinServer(user.server(), currentServer);
        SkinTextures skin = this.client != null ? SocialScreen.skinForListRow(this.client, user.minecraftUuid()) : null;
        String uuidShort = user.minecraftUuid().toString();
        if (uuidShort.length() > 22) {
            uuidShort = uuidShort.substring(0, 8) + "…" + uuidShort.substring(uuidShort.length() - 6);
        }
        UiComponents.drawPresenceUserCard(
                this.textRenderer,
                context,
                this.client,
                cardX,
                cardY,
                cardW,
                CARD_H,
                friend ? SocialScreen.displayUsername(user) + " ★" : SocialScreen.displayUsername(user),
                uuidShort,
                serverLine,
                coordsLine,
                friend,
                hovered,
                showJoin,
                skin);
    }

    private int maxListScrollY() {
        return Math.max(0, this.listContentHeight() - this.listAreaHeight());
    }

    private void clampListScrollY() {
        this.listScrollY = MathHelper.clamp((int)this.listScrollY, (int)0, (int)this.maxListScrollY());
    }

    public void tick() {
        super.tick();
        if (this.loading) {
            return;
        }
        --this.ticksUntilAutoRefresh;
        if (this.ticksUntilAutoRefresh <= 0) {
            this.ticksUntilAutoRefresh = 200;
            this.requestList();
        }
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        UiDraw.fillMidnightBackground(context, this.width, this.height);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawToolbarTabs(context, mouseX, mouseY);

        int lx = UiTokens.SP_2;
        int lw = this.width - UiTokens.SP_4;
        int contentTop = this.contentAreaTop();
        int contentBottom = this.contentAreaBottom();

        if (this.screenTab == ScreenTab.SETTINGS) {
            this.drawSettingsPanel(context, lx, contentTop, lw, contentBottom - contentTop);
            this.contextMenu.render(context, this.textRenderer, mouseX, mouseY);
            return;
        }

        int listTop = this.listAreaTop();
        int listBottom = this.listAreaBottom();
        this.clampListScrollY();

        DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
        boolean showSrv = Boolean.TRUE.equals(s.showServersInSocial);
        boolean showCoords = Boolean.TRUE.equals(s.showCoordsInSocial);

        UiDraw.cardElevated(context, lx, listTop, lw, listBottom - listTop, UiTokens.R_XL);

        String listTitle = this.screenTab == ScreenTab.ALL
                ? "Online DupeClient users"
                : "Users on your server";
        context.drawTextWithShadow(this.textRenderer, Text.literal(listTitle), lx + LIST_INSET, listTop + UiTokens.SP_2 + 2, MidnightPalette.PATH_GREEN);
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Right-click a card for actions"),
                lx + LIST_INSET,
                listTop + UiTokens.SP_2 + 13,
                MidnightPalette.TEXT_MUTED);

        if (!this.loading && !this.rows.isEmpty()) {
            String count = this.rows.size() + (this.rows.size() == 1 ? " online" : " online");
            int countW = this.textRenderer.getWidth(count);
            context.drawTextWithShadow(
                    this.textRenderer, Text.literal(count), lx + lw - countW - LIST_INSET, listTop + UiTokens.SP_2 + 2, MidnightPalette.TEXT_SECONDARY);
        }

        int listContentTop = this.listContentTop();
        int cardW = this.cardWidth(lw);
        int cardX = this.cardX(lx);
        int scrollRight = this.maxListScrollY() > 0 ? lx + lw - 6 : lx + lw - LIST_INSET;

        context.enableScissor(lx + 1, listContentTop, lx + lw - 1, listBottom - 1);
        if (this.loading) {
            int cy = listContentTop + UiTokens.SP_3 - this.listScrollY;
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Loading…"), this.width / 2, cy, MidnightPalette.TEXT_SECONDARY);
        } else if (this.rows.isEmpty()) {
            String msg = this.statusHint != null && !this.statusHint.isBlank()
                    ? this.statusHint
                    : (this.screenTab == ScreenTab.CURRENT_SERVER
                            ? "No DupeClient users are sharing this same server right now."
                            : "API OK but list empty — enable presence + broadcastPresence, stay in-world so heartbeats run.");
            int ey = listContentTop + UiTokens.SP_3 - this.listScrollY;
            for (OrderedText line : this.textRenderer.wrapLines(Text.literal(msg), lw - LIST_INSET * 4)) {
                context.drawCenteredTextWithShadow(this.textRenderer, line, this.width / 2, ey, UiTokens.MINT_400);
                ey += 12;
            }
        } else {
            String currentServer = SocialScreen.currentServerKey(this.client);
            for (int i = 0; i < this.rows.size(); ++i) {
                int cardY = listContentTop + UiTokens.SP_2 + i * (CARD_H + CARD_GAP) - this.listScrollY;
                OnlineDupeClientUser u = this.rows.get(i);
                boolean friend = DupeClientSocialFriendsManager.isFriend(u.minecraftUuid());
                this.drawPlayerCard(context, cardX, cardY, cardW, i, u, friend, currentServer, showSrv, showCoords, mouseX, mouseY);
            }
        }
        if (this.maxListScrollY() > 0) {
            UiDraw.drawScrollbar(context, scrollRight, listContentTop + 2, listBottom - 2, this.listScrollY, this.maxListScrollY());
        }
        context.disableScissor();

        this.contextMenu.render(context, this.textRenderer, mouseX, mouseY);
    }

    private void drawSettingsPanel(DrawContext context, int tx, int top, int rowW, int areaH) {
        int cardH = Math.min(SETTINGS_BLOCK_H, areaH);
        int cardY = top + Math.max(0, (areaH - cardH) / 2);
        UiComponents.drawSectionCard(this.textRenderer, context, tx, cardY, rowW, cardH, "Social settings", false);
        int innerX = tx + UiTokens.SP_3;
        int innerW = rowW - UiTokens.SP_6;
        DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
        int baseY = UiComponents.titledCardBodyY(cardY);
        UiComponents.drawOptionToggle(this.textRenderer, context, innerX, baseY, innerW, "Show server on cards (when others share)", Boolean.TRUE.equals(s.showServersInSocial));
        UiComponents.drawOptionToggle(this.textRenderer, context, innerX, baseY + UiTokens.ROW_STEP, innerW, "Show coords on cards (when others share)", Boolean.TRUE.equals(s.showCoordsInSocial));
        UiComponents.drawOptionToggle(this.textRenderer, context, innerX, baseY + UiTokens.ROW_STEP * 2, innerW, "Hide myself in this list", Boolean.TRUE.equals(s.hideSelfInSocial));
        boolean everyoneSeesMe = "everyone".equalsIgnoreCase(s.presenceListAudience);
        UiComponents.drawOptionToggle(this.textRenderer, context, innerX, baseY + UiTokens.ROW_STEP * 3, innerW, everyoneSeesMe ? "My presence row: visible to everyone" : "My presence row: friends only", everyoneSeesMe);
        UiComponents.drawOptionToggle(this.textRenderer, context, innerX, baseY + UiTokens.ROW_STEP * 4, innerW, "Social list view: friends only", Boolean.TRUE.equals(s.socialListFriendsOnlyView));
        UiComponents.drawOptionToggle(this.textRenderer, context, innerX, baseY + UiTokens.ROW_STEP * 5, innerW, "Share my current server", Boolean.TRUE.equals(s.shareCurrentServer));
        UiComponents.drawOptionToggle(this.textRenderer, context, innerX, baseY + UiTokens.ROW_STEP * 6, innerW, "Share my current coords", Boolean.TRUE.equals(s.shareCurrentCoords));
        UiComponents.drawOptionToggle(this.textRenderer, context, innerX, baseY + UiTokens.ROW_STEP * 7, innerW, "Share my waypoints", Boolean.TRUE.equals(s.shareWaypoints));
        UiComponents.drawOptionToggle(this.textRenderer, context, innerX, baseY + UiTokens.ROW_STEP * 8, innerW, "Show waypoints in world", Boolean.TRUE.equals(s.showSharedWaypointsInWorld));
    }

    private static SkinTextures skinForListRow(MinecraftClient client, UUID uuid) {
        PlayerListEntry entry;
        if (client.getNetworkHandler() != null && (entry = client.getNetworkHandler().getPlayerListEntry(uuid)) != null) {
            return entry.getSkinTextures();
        }
        return DefaultSkinHelper.getSkinTextures((UUID)uuid);
    }

    private static String displayUsername(OnlineDupeClientUser u) {
        String n = u.minecraftUsername();
        return n.isEmpty() ? "Unknown" : n;
    }

    private static String normalizeServerKey(String server) {
        if (server == null) {
            return null;
        }
        String s = server.trim();
        if (s.isEmpty()) {
            return null;
        }
        return s.toLowerCase(Locale.ROOT);
    }

    private static String currentServerKey(MinecraftClient mc) {
        if (mc == null) {
            return null;
        }
        if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
            return SocialScreen.normalizeServerKey(mc.getCurrentServerEntry().address);
        }
        if (mc.world != null && mc.player != null) {
            return SocialScreen.normalizeServerKey("Singleplayer");
        }
        return null;
    }

    private boolean canJoinServer(String sharedServer, String currentServerKey) {
        String key = SocialScreen.normalizeServerKey(sharedServer);
        if (key == null || "singleplayer".equals(key)) {
            return false;
        }
        return !key.equals(currentServerKey);
    }

    private void joinSharedServer(String address) {
        if (this.client == null || address == null || address.isBlank()) {
            return;
        }
        String trimmed = address.trim();
        if ("Singleplayer".equalsIgnoreCase(trimmed)) {
            return;
        }
        ServerAddress parsed = ServerAddress.parse(trimmed);
        ServerInfo info = new ServerInfo(trimmed, trimmed, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect((Screen)this, (MinecraftClient)this.client, (ServerAddress)parsed, (ServerInfo)info, (boolean)false, null);
    }

    private int rowIndexAt(double mx, double my) {
        if (this.loading || this.rows.isEmpty()) {
            return -1;
        }
        int lx = UiTokens.SP_2;
        int lw = this.width - UiTokens.SP_4;
        if (mx < lx || mx > lx + lw) {
            return -1;
        }
        int listTop = this.listAreaTop();
        int listBottom = this.listAreaBottom();
        if (my < listTop || my >= listBottom) {
            return -1;
        }
        int contentTop = this.listContentTop();
        int vLocal = (int) (my - contentTop + this.listScrollY) - UiTokens.SP_2;
        if (vLocal < 0) {
            return -1;
        }
        int stride = CARD_H + CARD_GAP;
        int idx = vLocal / stride;
        if (idx < 0 || idx >= this.rows.size()) {
            return -1;
        }
        int inCard = vLocal % stride;
        if (inCard >= CARD_H) {
            return -1;
        }
        return idx;
    }

    private boolean clickJoinButton(double mx, double my) {
        DupeClientPresenceSettings s = DupeClientPresenceConfigManager.get();
        if (!Boolean.TRUE.equals(s.showServersInSocial) || this.rows.isEmpty()) {
            return false;
        }
        int idx = this.rowIndexAt(mx, my);
        if (idx < 0 || idx >= this.rows.size()) {
            return false;
        }
        OnlineDupeClientUser user = this.rows.get(idx);
        String currentServer = SocialScreen.currentServerKey(this.client);
        if (!this.canJoinServer(user.server(), currentServer)) {
            return false;
        }
        int lx = UiTokens.SP_2;
        int lw = this.width - UiTokens.SP_4;
        int cardW = this.cardWidth(lw);
        int cardX = this.cardX(lx);
        int cardY = this.listContentTop() + UiTokens.SP_2 + idx * (CARD_H + CARD_GAP) - this.listScrollY;
        int joinX = cardX + cardW - JOIN_BTN_W - UiTokens.SP_2;
        int joinY = cardY + (CARD_H - JOIN_BTN_H) / 2;
        if (mx >= joinX && mx <= joinX + JOIN_BTN_W && my >= joinY && my <= joinY + JOIN_BTN_H) {
            this.joinSharedServer(user.server());
            return true;
        }
        return false;
    }

    private void openRowContextMenu(int idx, int anchorX, int anchorY) {
        if (idx < 0 || idx >= this.rows.size()) {
            return;
        }
        OnlineDupeClientUser user = this.rows.get(idx);
        UUID uuid = user.minecraftUuid();
        boolean friend = DupeClientSocialFriendsManager.isFriend(uuid);
        ArrayList<GuiContextMenu.Entry> items = new ArrayList<GuiContextMenu.Entry>();
        items.add(new GuiContextMenu.Entry(friend ? "Remove friend" : "Add friend", () -> {
            DupeClientSocialFriendsManager.toggleFriend(uuid);
            this.requestList();
        }));
        String currentServer = SocialScreen.currentServerKey(this.client);
        if (this.canJoinServer(user.server(), currentServer)) {
            items.add(new GuiContextMenu.Entry("Join server", () -> this.joinSharedServer(user.server())));
        }
        if (friend) {
            items.add(new GuiContextMenu.Entry("Send bots to me", () -> {
                if (this.client != null) {
                    McpToolsManager.INSTANCE.sendBotsToLocalPlayer(this.client);
                }
            }));
        }
        this.contextMenu.open(anchorX, anchorY, this.width, this.height, this.listAreaTop(), items, this.textRenderer);
    }

    public boolean mouseClicked(Click click, boolean doubleClick) {
        int idx;
        if (this.contextMenu.isOpen()) {
            if (this.contextMenu.handleClick(click.x(), click.y(), click.button())) {
                return true;
            }
            if (click.button() == 0) {
                this.contextMenu.close();
                return true;
            }
        }
        if (IngameOverlayHost.onScreenOverlayMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        double mx = click.x();
        double my = click.y();
        if (click.button() == 0 && this.clickToolbarTab(mx, my)) {
            return true;
        }
        if (this.screenTab == ScreenTab.SETTINGS) {
            if (click.button() == 0 && this.clickSettingsToggle(mx, my)) {
                return true;
            }
            return super.mouseClicked(click, doubleClick);
        }
        if (click.button() == 1 && !this.loading && !this.rows.isEmpty() && (idx = this.rowIndexAt(mx, my)) >= 0) {
            this.openRowContextMenu(idx, (int)mx, (int)my);
            return true;
        }
        if (click.button() == 0 && !this.loading && this.clickJoinButton(mx, my)) {
            return true;
        }
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        return false;
    }

    private boolean clickSettingsToggle(double mx, double my) {
        int tx = UiTokens.SP_2;
        int lw = this.width - UiTokens.SP_4;
        int contentTop = this.contentAreaTop();
        int areaH = this.contentAreaBottom() - contentTop;
        int cardH = Math.min(SETTINGS_BLOCK_H, areaH);
        int cardY = contentTop + Math.max(0, (areaH - cardH) / 2);
        int innerX = tx + UiTokens.SP_3;
        int innerW = lw - UiTokens.SP_6;
        int baseY = UiComponents.titledCardBodyY(cardY);
        if (mx < innerX || mx > innerX + innerW || my < cardY || my >= cardY + cardH) {
            return false;
        }
        int relY = (int) (my - baseY);
        if (relY < 0) {
            return false;
        }
        int toggleIndex = relY / UiTokens.ROW_STEP;
        if (toggleIndex < 0 || toggleIndex >= 9) {
            return false;
        }
        DupeClientPresenceSettings cfg = DupeClientPresenceConfigManager.get();
        switch (toggleIndex) {
            case 0 -> cfg.showServersInSocial = !Boolean.TRUE.equals(cfg.showServersInSocial);
            case 1 -> cfg.showCoordsInSocial = !Boolean.TRUE.equals(cfg.showCoordsInSocial);
            case 2 -> {
                cfg.hideSelfInSocial = !Boolean.TRUE.equals(cfg.hideSelfInSocial);
                DupeClientPresenceConfigManager.save(cfg);
                this.requestList();
                return true;
            }
            case 3 -> cfg.presenceListAudience = "friends_only".equalsIgnoreCase(cfg.presenceListAudience) ? "everyone" : "friends_only";
            case 4 -> {
                cfg.socialListFriendsOnlyView = !Boolean.TRUE.equals(cfg.socialListFriendsOnlyView);
                DupeClientPresenceConfigManager.save(cfg);
                this.requestList();
                return true;
            }
            case 5 -> cfg.shareCurrentServer = !Boolean.TRUE.equals(cfg.shareCurrentServer);
            case 6 -> cfg.shareCurrentCoords = !Boolean.TRUE.equals(cfg.shareCurrentCoords);
            case 7 -> {
                cfg.shareWaypoints = !Boolean.TRUE.equals(cfg.shareWaypoints);
                DupeClientPresenceConfigManager.save(cfg);
                DupeClientWaypointManager.INSTANCE.markSyncDirty();
                return true;
            }
            case 8 -> cfg.showSharedWaypointsInWorld = !Boolean.TRUE.equals(cfg.showSharedWaypointsInWorld);
            default -> {
                return false;
            }
        }
        DupeClientPresenceConfigManager.save(cfg);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int lx = UiTokens.SP_2;
        if (mouseX >= lx && mouseX < this.width - lx && mouseY >= this.listAreaTop() && mouseY <= this.listAreaBottom() && verticalAmount != 0.0) {
            this.listScrollY -= (int)(verticalAmount * 10.0);
            this.clampListScrollY();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean shouldPause() {
        return false;
    }

    private static enum ScreenTab {
        ALL,
        CURRENT_SERVER,
        SETTINGS
    }
}

