package com.dupeclient.client.module.dupedb.search;

import com.dupeclient.client.DupeClient;
import com.dupeclient.client.gui.DupeClientToasts;
import com.dupeclient.client.multiplayer.MultiplayerNavigable;
import com.dupeclient.client.multiplayer.MultiplayerScreens;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.dupeclient.client.module.dupedb.search.api.ApiClient;
import com.dupeclient.client.module.dupedb.search.api.ApiException;
import com.dupeclient.client.mixin.MultiplayerScreenAccessor;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import com.dupeclient.client.gui.widget.StylishTextFieldWidget;

public class ServerScannerScreen extends Screen implements MultiplayerNavigable {
   private static final String API_SERVERS_URL = "https://minecraftserversearch.com/api/addon/v1/servers";
   private static final String API_STATS_URL = "https://minecraftserversearch.com/api/addon/v1/stats";
   private static final String API_LIVE_URL = "https://minecraftserversearch.com/api/addon/v1/live";
   private static final String API_FILTER_OPTIONS_URL = "https://minecraftserversearch.com/api/addon/v1/filter-options";
   private static final String API_ME_URL = "https://minecraftserversearch.com/api/addon/me";
   private static final int API_PAGE_SIZE = 50;
   private static final int ROW_HEIGHT = 30;
   private static final int COL_GAP = 10;
   private static final int COMPACT_WIDTH = 560;
   private static final int DETAILS_MODAL_W = 680;
   private static final int DETAILS_MODAL_H = 370;
   private static final int LIVE_CARD_HEIGHT = 58;
   private static final int TOP_TAB_ROW_BOTTOM = 24;
   private static final int LIVE_GAP_BELOW_TOP_BAR = 5;
   private static final int LIVE_SECTION_HEADER_SPACE = 11;
   private static final int LIVE_HEADER_H = 20;
   private static final int ACTION_JOIN_BTN_W = 56;
   private static final int ACTION_ADD_BTN_W = 56;
   private static final int ACTION_COPY_BTN_W = 40;
   private static final int ACTION_BTN_H = 18;
   private static final int ACTION_BTN_GAP = 4;
   private static final int ACTION_EDGE_INSET = 10;
   private static final int LIVE_CARD_INSET = 10;
   private static final ServerScannerScreen.UiState LAST_UI_STATE = new ServerScannerScreen.UiState();
   private static final ServerScannerScreen.SearchResultCache LAST_SEARCH_CACHE = new ServerScannerScreen.SearchResultCache();
   private static final ServerScannerScreen.LiveResultCache LAST_LIVE_CACHE = new ServerScannerScreen.LiveResultCache();
   private final Screen parent;
   private final ApiClient apiClient;
   private final List<ServerScannerScreen.ServerEntry> servers = new ArrayList<>();
   private final Map<String, Identifier> faviconIds = new HashMap<>();
   private final Map<String, DynamicTexture> faviconTextures = new HashMap<>();
   private final Set<String> faviconLoadFailed = new HashSet<>();
   private StylishTextFieldWidget serverTypeField;
   private StylishTextFieldWidget versionField;
   private StylishTextFieldWidget softwareField;
   private StylishTextFieldWidget countryField;
   private StylishTextFieldWidget addressField;
   private StylishTextFieldWidget searchField;
   private StylishTextFieldWidget modNameField;
   private StylishTextFieldWidget pluginNameField;
   private StylishTextFieldWidget playerNameField;
   private StylishTextFieldWidget minPlayersField;
   private StylishTextFieldWidget maxPlayersField;
   private StylishTextFieldWidget pageField;
   private Button crackedToggleButton;
   private Button hasPlayersToggleButton;
   private Button moddedToggleButton;
   private Button isFullToggleButton;
   private Button hasFaviconToggleButton;
   private Button searchButton;
   private Button clearButton;
   private Button prevPageButton;
   private Button nextPageButton;
   private Button dashboardTabButton;
   private Button liveTabButton;
   private Button signOutButton;
   private Button addAllButton;
   private Button addBulkButton;
   private StylishTextFieldWidget bulkAddCountField;
   private final List<SuggestionDropdown> dropdowns = new ArrayList<>();
   private SuggestionDropdown typeDrop;
   private SuggestionDropdown versionDrop;
   private SuggestionDropdown softwareDrop;
   private SuggestionDropdown countryDrop;
   private SuggestionDropdown modDrop;
   private SuggestionDropdown pluginDrop;
   private List<SuggestionDropdown.Item> optTypes = List.of();
   private List<SuggestionDropdown.Item> optVersions = List.of();
   private List<SuggestionDropdown.Item> optSoftware = List.of();
   private List<SuggestionDropdown.Item> optCountries = List.of();
   private List<SuggestionDropdown.Item> optMods = List.of();
   private List<SuggestionDropdown.Item> optPlugins = List.of();
   private Map<String, List<SuggestionDropdown.Item>> optSoftwareByVersion = Map.of();
   private boolean filterOptionsFetchInFlight;
   private int filterOptionsRefreshTicks;
   private String filterOptionsStatus = "";
   private final List<ServerScannerScreen.ChipRegion> chipRegions = new ArrayList<>();
   private final List<ServerScannerScreen.FieldLabel> fieldLabels = new ArrayList<>();
   private final Set<StylishTextFieldWidget> suppressedFields = new HashSet<>();
   private final Set<Button> suppressedToggles = new HashSet<>();
   private int controlsBottomY = 150;
   private int listTopY = 168;
   private int chipBandY = -1;
   private String accountStatus = "Linked";
   private boolean sessionValidated;
   private boolean sessionValidateInFlight;
   private int sessionValidateTicks;
   private final List<ScannerActionButton> joinButtons = new ArrayList<>();
   private final List<ScannerActionButton> addButtons = new ArrayList<>();
   private final List<ScannerActionButton> copyButtons = new ArrayList<>();
   private final List<ScannerActionButton> liveJoinButtons = new ArrayList<>();
   private final List<ScannerActionButton> liveAddButtons = new ArrayList<>();
   private final List<ScannerActionButton> liveCopyButtons = new ArrayList<>();
   private final List<ServerScannerScreen.LiveLinkRegion> liveLinkRegions = new ArrayList<>();
   private final List<ServerScannerScreen.LiveEntry> liveEntries = new ArrayList<>();
   private int visibleRows;
   private int scrollOffset;
   private int liveVisibleCards;
   private int liveScrollOffset;
   private int currentPage = 1;
   private int searchGeneration;
   private ServerScannerScreen.TriState crackedFilter = ServerScannerScreen.TriState.ANY;
   private ServerScannerScreen.TriState hasPlayersFilter = ServerScannerScreen.TriState.ANY;
   private ServerScannerScreen.TriState moddedFilter = ServerScannerScreen.TriState.ANY;
   private ServerScannerScreen.TriState isFullFilter = ServerScannerScreen.TriState.ANY;
   private ServerScannerScreen.TriState hasFaviconFilter = ServerScannerScreen.TriState.ANY;
   private String statusText = "Ready";
   private boolean loading;
   private String liveStatusText = "Live feed ready";
   private boolean liveLoading;
   private int statsOnlineServers = -1;
   private int statsTotalServers = -1;
   private int statsTotalCountries = -1;
   private int statsRefreshTicks;
   private boolean statsFetchInFlight;
   private boolean detailsModalOpen;
   private boolean detailsLoading;
   private String detailsStatus = "";
   private ServerScannerScreen.ServerEntry detailsTarget;
   private ServerScannerScreen.ServerDetails detailsData;
   private boolean detailsDescriptionExpanded;
   private boolean detailsPlayersExpanded;
   private boolean detailsPluginsExpanded;
   private int descToggleX;
   private int descToggleY;
   private int descToggleW;
   private int descToggleH;
   private boolean descToggleVisible;
   private int playersToggleX;
   private int playersToggleY;
   private int playersToggleW;
   private int playersToggleH;
   private boolean playersToggleVisible;
   private int pluginsToggleX;
   private int pluginsToggleY;
   private int pluginsToggleW;
   private int pluginsToggleH;
   private boolean pluginsToggleVisible;
   private int detailsCopyIpX;
   private int detailsCopyIpY;
   private int detailsCopyIpW;
   private int detailsCopyIpH;
   private ServerScannerScreen.Tab activeTab = ServerScannerScreen.Tab.DASHBOARD;

   public ServerScannerScreen(Screen parent, ApiClient apiClient) {
      super(Component.literal("Minecraft Server Scanner"));
      this.parent = parent;
      this.apiClient = apiClient;
   }

   @Override
   public Screen getNavigationParent() {
      return this.parent;
   }

   private void goBack() {
      JoinMultiplayerScreen mp = this.findUnderlyingMultiplayerScreen();
      MultiplayerScreens.returnToMultiplayer(this.minecraft, mp != null ? mp : this.parent);
   }

   @Override
   public void onClose() {
      this.goBack();
   }

   protected void init() {
      super.init();
      this.joinButtons.clear();
      this.addButtons.clear();
      this.copyButtons.clear();
      this.liveJoinButtons.clear();
      this.liveAddButtons.clear();
      this.liveCopyButtons.clear();
      this.clearWidgets();
      this.dashboardTabButton = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Dashboard"), b -> this.setActiveTab(ServerScannerScreen.Tab.DASHBOARD))
            .bounds(10, 4, 92, 20)
            .build()
      );
      this.liveTabButton = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Live"), b -> this.setActiveTab(ServerScannerScreen.Tab.LIVE))
            .bounds(106, 4, 64, 20)
            .build()
      );
      this.serverTypeField = this.addField(110, "Server Type", "Java");
      this.versionField = this.addField(95, "Version", "1.21.11");
      this.softwareField = this.addField(100, "Software", "Paper");
      this.countryField = this.addField(95, "Country", "US");
      this.addressField = this.addField(105, "IP Address", "127.0.0.1");
      this.searchField = this.addField(150, "Search MOTD", "hello");
      this.modNameField = this.addField(110, "Mod", "FML");
      this.pluginNameField = this.addField(120, "Plugin", "luckperms");
      this.playerNameField = this.addField(110, "Player Name", "Notch");
      this.minPlayersField = this.addField(75, "Min Players", "1");
      this.maxPlayersField = this.addField(75, "Max Players", "100");
      this.pageField = this.addField(60, "Page", "1");
      this.fieldLabels.clear();
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.serverTypeField, "Server type"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.versionField, "Version"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.softwareField, "Software"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.countryField, "Country"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.addressField, "IP Address"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.searchField, "Search MOTD"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.modNameField, "Mod"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.pluginNameField, "Plugin"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.playerNameField, "Player name"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.minPlayersField, "Min players"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.maxPlayersField, "Max players"));
      this.fieldLabels.add(new ServerScannerScreen.FieldLabel(this.pageField, "Page"));
      this.crackedToggleButton = (Button)this.addRenderableWidget(Button.builder(buttonLabel("Cracked", this.crackedFilter), b -> {
         this.crackedFilter = this.crackedFilter.next();
         b.setMessage(buttonLabel("Cracked", this.crackedFilter));
      }).bounds(0, 0, 108, 20).build());
      this.hasPlayersToggleButton = (Button)this.addRenderableWidget(Button.builder(buttonLabel("Has Players", this.hasPlayersFilter), b -> {
         this.hasPlayersFilter = this.hasPlayersFilter.next();
         b.setMessage(buttonLabel("Has Players", this.hasPlayersFilter));
      }).bounds(0, 0, 124, 20).build());
      this.moddedToggleButton = (Button)this.addRenderableWidget(Button.builder(buttonLabel("Modded", this.moddedFilter), b -> {
         this.moddedFilter = this.moddedFilter.next();
         b.setMessage(buttonLabel("Modded", this.moddedFilter));
      }).bounds(0, 0, 100, 20).build());
      this.isFullToggleButton = (Button)this.addRenderableWidget(Button.builder(buttonLabel("Is Full", this.isFullFilter), b -> {
         this.isFullFilter = this.isFullFilter.next();
         b.setMessage(buttonLabel("Is Full", this.isFullFilter));
      }).bounds(0, 0, 95, 20).build());
      this.hasFaviconToggleButton = (Button)this.addRenderableWidget(Button.builder(buttonLabel("Has Icon", this.hasFaviconFilter), b -> {
         this.hasFaviconFilter = this.hasFaviconFilter.next();
         b.setMessage(buttonLabel("Has Icon", this.hasFaviconFilter));
      }).bounds(0, 0, 105, 20).build());
      this.prevPageButton = (Button)this.addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
         this.currentPage = Math.max(1, parseIntOrDefault(this.pageField.getValue(), this.currentPage) - 1);
         this.pageField.setValue(Integer.toString(this.currentPage));
         this.startSearch();
      }).bounds(0, 0, 56, 20).build());
      this.nextPageButton = (Button)this.addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
         this.currentPage = parseIntOrDefault(this.pageField.getValue(), this.currentPage) + 1;
         this.pageField.setValue(Integer.toString(this.currentPage));
         this.startSearch();
      }).bounds(0, 0, 56, 20).build());
      this.searchButton = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Search"), b -> this.startSearch()).bounds(0, 0, 70, 20).build()
      );
      this.clearButton = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Clear"), b -> this.clearFilters()).bounds(0, 0, 60, 20).build()
      );
      this.bulkAddCountField = this.addField(36, "Add count", "10");
      this.addAllButton = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Add all"), b -> this.addAllVisible()).bounds(0, 0, 56, 20).build()
      );
      this.addBulkButton = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Add #"), b -> this.addBulkVisible()).bounds(0, 0, 48, 20).build()
      );
      this.signOutButton = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("Sign out"), b -> this.signOut()).bounds(this.width - 164, 4, 80, 20).build()
      );
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.goBack()).bounds(this.width - 80, 4, 70, 20).build());
      this.layoutDashboard();
      int listTop = this.getListTop();
      int listBottom = this.height - 26;
      this.visibleRows = Math.max(1, (listBottom - listTop) / 30);

      for (int i = 0; i < this.visibleRows; i++) {
         int localIndex = i;
         int rowY = listTop + localIndex * 30 + 2;
         int addX = this.actionAddButtonX();
         int copyX = this.actionCopyButtonX();
         int joinX = this.actionJoinButtonX();
         this.joinButtons
            .add(
               (ScannerActionButton)this.addRenderableWidget(new ScannerActionButton(joinX, rowY, 56, 18, Component.literal("Join"), () -> this.joinVisible(localIndex)))
            );
         this.copyButtons
            .add((ScannerActionButton)this.addRenderableWidget(new ScannerActionButton(copyX, rowY, 40, 18, Component.literal("Copy"), () -> this.copyVisible(localIndex))));
         this.addButtons
            .add((ScannerActionButton)this.addRenderableWidget(new ScannerActionButton(addX, rowY, 56, 18, Component.literal("Add"), () -> this.addVisible(localIndex))));
      }

      int liveCardsTop = this.getLiveCardsTop();
      int liveBottom = this.height - 26;
      this.liveVisibleCards = Math.max(1, (liveBottom - liveCardsTop) / 58);

      for (int i = 0; i < this.liveVisibleCards; i++) {
         int localIndex = i;
         int rowY = liveCardsTop + 1 + i * 58;
         int addX = this.actionAddButtonX();
         int copyX = this.actionCopyButtonX();
         int joinX = this.actionJoinButtonX();
         this.liveJoinButtons
            .add(
               (ScannerActionButton)this.addRenderableWidget(
                  new ScannerActionButton(joinX, rowY, 56, 18, Component.literal("Join"), () -> this.joinLive(localIndex))
               )
            );
         this.liveCopyButtons
            .add(
               (ScannerActionButton)this.addRenderableWidget(
                  new ScannerActionButton(copyX, rowY, 40, 18, Component.literal("Copy"), () -> this.copyLive(localIndex))
               )
            );
         this.liveAddButtons
            .add(
               (ScannerActionButton)this.addRenderableWidget(
                  new ScannerActionButton(addX, rowY, 56, 18, Component.literal("Add"), () -> this.addLive(localIndex))
               )
            );
      }

      this.setupDropdowns();
      this.applyUiState();
      this.validateSession();
      this.fetchStats();
      this.fetchFilterOptions();
      if (!this.tryRestoreSearchCache()) {
         this.startSearch();
      }
      this.tryRestoreLiveCache();
      this.updateTabVisibility();
   }

   private void setupDropdowns() {
      this.dropdowns.clear();
      this.typeDrop = new SuggestionDropdown(this.serverTypeField, this.font, () -> this.optTypes);
      this.versionDrop = new SuggestionDropdown(this.versionField, this.font, () -> this.optVersions);
      this.softwareDrop = new SuggestionDropdown(this.softwareField, this.font, this::softwareItemsForVersion);
      this.countryDrop = new SuggestionDropdown(this.countryField, this.font, () -> this.optCountries);
      this.modDrop = new SuggestionDropdown(this.modNameField, this.font, () -> this.optMods);
      this.pluginDrop = new SuggestionDropdown(this.pluginNameField, this.font, () -> this.optPlugins);
      this.dropdowns.add(this.typeDrop);
      this.dropdowns.add(this.versionDrop);
      this.dropdowns.add(this.softwareDrop);
      this.dropdowns.add(this.countryDrop);
      this.dropdowns.add(this.modDrop);
      this.dropdowns.add(this.pluginDrop);
   }

   private List<SuggestionDropdown.Item> softwareItemsForVersion() {
      String version = this.versionField != null ? this.versionField.getValue().trim() : "";
      if (version.isEmpty()) {
         return this.optSoftware;
      } else {
         List<SuggestionDropdown.Item> narrowed = this.optSoftwareByVersion.get(version);
         return narrowed != null && !narrowed.isEmpty() ? narrowed : this.optSoftware;
      }
   }

   private SuggestionDropdown activeDropdown() {
      if (this.activeTab != ServerScannerScreen.Tab.DASHBOARD) {
         return null;
      } else {
         for (SuggestionDropdown d : this.dropdowns) {
            if (d != null && d.isOpen()) {
               return d;
            }
         }

         return null;
      }
   }

   public void tick() {
      super.tick();
      this.statsRefreshTicks++;
      if (this.statsRefreshTicks >= 600) {
         this.statsRefreshTicks = 0;
         this.fetchStats();
      }

      this.filterOptionsRefreshTicks++;
      if (this.filterOptionsRefreshTicks >= 1200) {
         this.filterOptionsRefreshTicks = 0;
         this.fetchFilterOptions();
      }

      if (!this.sessionValidated && !this.sessionValidateInFlight) {
         this.sessionValidateTicks++;
         if (this.sessionValidateTicks >= 100) {
            this.sessionValidateTicks = 0;
            this.validateSession();
         }
      }
   }

   private StylishTextFieldWidget addField(int w, String label, String placeholder) {
      StylishTextFieldWidget field = (StylishTextFieldWidget)this.addRenderableWidget(
              StylishTextFieldWidget.create(this.font, 0, 0, w, Component.literal(label)));
      field.setPlaceholder(placeholder);
      return field;
   }

   private static Component buttonLabel(String name, ServerScannerScreen.TriState value) {
      return Component.literal(name + ": " + value.display);
   }

   private boolean isCompact() {
      return this.width < 560;
   }

   private int getListTop() {
      return this.listTopY;
   }

   private void layoutDashboard() {
      boolean compact = this.isCompact();
      this.suppressedFields.clear();
      this.suppressedToggles.clear();
      List<StylishTextFieldWidget> fieldOrder = List.of(
         this.serverTypeField,
         this.versionField,
         this.countryField,
         this.searchField,
         this.softwareField,
         this.addressField,
         this.pluginNameField,
         this.modNameField,
         this.playerNameField,
         this.minPlayersField,
         this.maxPlayersField
      );
      if (compact) {
         Set<StylishTextFieldWidget> keep = Set.of(this.serverTypeField, this.versionField, this.countryField, this.searchField);

         for (StylishTextFieldWidget f : fieldOrder) {
            if (!keep.contains(f)) {
               this.suppressedFields.add(f);
            }
         }
      }

      List<Button> toggleOrder = List.of(
         this.crackedToggleButton, this.hasPlayersToggleButton, this.moddedToggleButton, this.isFullToggleButton, this.hasFaviconToggleButton
      );
      if (compact) {
         this.suppressedToggles.addAll(toggleOrder);
      }

      for (StylishTextFieldWidget fx : fieldOrder) {
         fx.visible = !this.suppressedFields.contains(fx);
      }

      this.pageField.visible = true;

      for (Button b : toggleOrder) {
         b.visible = !this.suppressedToggles.contains(b);
      }

      this.prevPageButton.visible = true;
      this.nextPageButton.visible = true;
      this.searchButton.visible = true;
      this.clearButton.visible = true;
      int y = 32;
      List<AbstractWidget> flowFields = new ArrayList<>();

      for (StylishTextFieldWidget fx : fieldOrder) {
         if (fx.visible) {
            flowFields.add(fx);
         }
      }

      y = this.flow(flowFields, y, 35, 9);
      List<AbstractWidget> flowToggles = new ArrayList<>();

      for (Button b : toggleOrder) {
         if (b.visible) {
            flowToggles.add(b);
         }
      }

      if (!flowToggles.isEmpty()) {
         y = this.flow(flowToggles, y, 26, 0);
      }

      List<AbstractWidget> actionRow = List.of(
         this.pageField,
         this.prevPageButton,
         this.nextPageButton,
         this.searchButton,
         this.clearButton,
         this.bulkAddCountField,
         this.addBulkButton,
         this.addAllButton
      );
      y = this.flow(actionRow, y, 35, 9);
      this.chipBandY = -1;
      if (!compact) {
         this.chipBandY = y + 2;
         y += 16;
      }

      this.controlsBottomY = y;
      this.listTopY = this.controlsBottomY + 22;
   }

   private int flow(List<? extends AbstractWidget> widgets, int startY, int rowStride, int widgetYOffset) {
      int margin = 8;
      int gap = 6;
      int maxX = this.width - margin;
      int x = margin;
      int y = startY;
      boolean placedAny = false;

      for (AbstractWidget w : widgets) {
         if (w != null && w.visible) {
            int ww = Math.min(w.getWidth(), Math.max(40, maxX - margin));
            w.setWidth(ww);
            if (placedAny && x + ww > maxX) {
               x = margin;
               y += rowStride;
            }

            w.setX(x);
            w.setY(y + widgetYOffset);
            x += ww + gap;
            placedAny = true;
         }
      }

      return placedAny ? y + rowStride : startY;
   }

   private ServerScannerScreen.DashColumns computeColumns() {
      int leftX = 12;
      int actionsX = this.actionJoinButtonX();
      int wVersion = 78;
      int wPlayers = 46;
      int wCountry = 40;
      int wSoftware = 84;
      int wLastSeen = 64;
      int minIp = 96;
      boolean showSoftware = true;
      boolean showLastSeen = true;

      for (int guard = 0; guard < 3; guard++) {
         int columns = 2 + (showSoftware ? 1 : 0) + 2 + (showLastSeen ? 1 : 0);
         int fixed = wVersion + wPlayers + wCountry + (showSoftware ? wSoftware : 0) + (showLastSeen ? wLastSeen : 0);
         int gaps = 10 * columns;
         int ipW = actionsX - leftX - fixed - gaps;
         if (ipW >= minIp || !showSoftware && !showLastSeen) {
            ipW = Math.max(60, ipW);
            int versionX = leftX + ipW + 10;
            int x = versionX + wVersion + 10;
            int softwareX = -1;
            if (showSoftware) {
               softwareX = x;
               x += wSoftware + 10;
            }

            int playersX = x;
            x += wPlayers + 10;
            int countryX = x;
            x += wCountry + 10;
            int lastSeenX = -1;
            if (showLastSeen) {
               lastSeenX = x;
            }

            return new ServerScannerScreen.DashColumns(
               leftX,
               ipW,
               versionX,
               wVersion,
               softwareX,
               wSoftware,
               playersX,
               countryX,
               lastSeenX,
               wLastSeen,
               actionsX,
               showSoftware,
               showLastSeen
            );
         }

         if (showLastSeen) {
            showLastSeen = false;
         } else {
            showSoftware = false;
         }
      }

      return new ServerScannerScreen.DashColumns(
         leftX, 80, leftX + 90, wVersion, -1, wSoftware, leftX + 170, leftX + 220, -1, wLastSeen, actionsX, false, false
      );
   }

   private void signOut() {
      this.apiClient.auth().clear();
      invalidateSearchCache();
      invalidateLiveCache();
      if (this.minecraft != null) {
         this.minecraft.gui.setScreen(new ServerSearchAuthScreen(this.parent));
      }
   }

   private void validateSession() {
      if (com.dupeclient.client.docs.ScreenshotCaptureMode.isActive()) {
         return;
      }
      if (!this.sessionValidateInFlight) {
         if (!this.apiClient.isAuthenticated()) {
            this.returnToLogin("no stored session");
         } else {
            this.sessionValidateInFlight = true;
            Thread.startVirtualThread(() -> {
               try {
                  JsonObject o = this.apiClient.getAuthedJson("https://minecraftserversearch.com/api/addon/me").getAsJsonObject();
                  String username = getString(o, "username", "");
                  String userId = getString(o, "user_id", "");
                  boolean isAdmin = getBoolean(o, "is_admin", false);
                  String name = !username.isEmpty() ? username : userId;
                  if (this.minecraft != null) {
                     this.minecraft.execute(() -> {
                        this.sessionValidated = true;
                        this.sessionValidateInFlight = false;
                        this.accountStatus = name.isEmpty() ? "Linked" : "Linked as " + name + (isAdmin ? " (admin)" : "");
                     });
                  }
               } catch (ApiException var6) {
                  if (this.minecraft != null) {
                     this.minecraft.execute(() -> {
                        this.sessionValidateInFlight = false;
                        if (var6.isAuthFailure()) {
                           this.apiClient.auth().clear();
                           this.returnToLogin(var6.getMessage());
                        } else {
                           this.accountStatus = "Could not verify session, retrying…";
                        }
                     });
                  } else {
                     this.sessionValidateInFlight = false;
                  }
               } catch (Exception var7) {
                  if (this.minecraft != null) {
                     this.minecraft.execute(() -> {
                        this.sessionValidateInFlight = false;
                        this.accountStatus = "Could not verify session, retrying…";
                     });
                  } else {
                     this.sessionValidateInFlight = false;
                  }
               }
            });
         }
      }
   }

   private int getLiveSectionTitleY() {
      return 29;
   }

   private int getLiveCardsTop() {
      return this.getLiveSectionTitleY() + 11;
   }

   private void clearFilters() {
      this.serverTypeField.setValue("");
      this.versionField.setValue("");
      this.softwareField.setValue("");
      this.countryField.setValue("US");
      this.addressField.setValue("");
      this.searchField.setValue("");
      this.modNameField.setValue("");
      this.pluginNameField.setValue("");
      this.playerNameField.setValue("");
      this.minPlayersField.setValue("");
      this.maxPlayersField.setValue("");
      this.pageField.setValue("1");
      this.crackedFilter = ServerScannerScreen.TriState.ANY;
      this.hasPlayersFilter = ServerScannerScreen.TriState.ANY;
      this.moddedFilter = ServerScannerScreen.TriState.ANY;
      this.isFullFilter = ServerScannerScreen.TriState.ANY;
      this.hasFaviconFilter = ServerScannerScreen.TriState.ANY;
      this.crackedToggleButton.setMessage(buttonLabel("Cracked", this.crackedFilter));
      this.hasPlayersToggleButton.setMessage(buttonLabel("Has Players", this.hasPlayersFilter));
      this.moddedToggleButton.setMessage(buttonLabel("Modded", this.moddedFilter));
      this.isFullToggleButton.setMessage(buttonLabel("Is Full", this.isFullFilter));
      this.hasFaviconToggleButton.setMessage(buttonLabel("Has Icon", this.hasFaviconFilter));
      this.liveScrollOffset = 0;
      invalidateSearchCache();
      this.startSearch();
   }

   private void startSearch() {
      this.currentPage = parseIntOrDefault(this.pageField.getValue(), 1);
      this.pageField.setValue(Integer.toString(this.currentPage));
      this.loading = true;
      this.statusText = "Loading...";
      this.searchButton.active = false;
      this.prevPageButton.active = false;
      this.nextPageButton.active = false;
      this.servers.clear();
      this.scrollOffset = 0;
      this.refreshActionButtons();
      String url = this.buildSearchUrl();
      int generation = ++this.searchGeneration;
      Thread.startVirtualThread(() -> this.fetchServers(url, generation));
   }

   private void fetchStats() {
      if (!this.statsFetchInFlight) {
         this.statsFetchInFlight = true;
         Thread.startVirtualThread(() -> {
            try {
               String body = this.apiClient.getAuthed("https://minecraftserversearch.com/api/addon/v1/stats");
               JsonObject o = JsonParser.parseString(body).getAsJsonObject();
               int online = getInt(o, "online_servers", -1);
               int totalServers = getInt(o, "total_servers", -1);
               int totalCountries = getInt(o, "total_countries", -1);
               if (this.minecraft != null) {
                  this.minecraft.execute(() -> {
                     this.statsOnlineServers = online;
                     this.statsTotalServers = totalServers;
                     this.statsTotalCountries = totalCountries;
                     this.statsFetchInFlight = false;
                  });
               }
            } catch (ApiException var6) {
               if (this.minecraft != null) {
                  this.minecraft.execute(() -> {
                     this.statsFetchInFlight = false;
                     if (var6.isAuthFailure()) {
                        this.returnToLogin(var6.getMessage());
                     }
                  });
               }
            } catch (Exception var7) {
               if (this.minecraft != null) {
                  this.minecraft.execute(() -> this.statsFetchInFlight = false);
               } else {
                  this.statsFetchInFlight = false;
               }
            }
         });
      }
   }

   private void fetchFilterOptions() {
      if (!this.filterOptionsFetchInFlight) {
         this.filterOptionsFetchInFlight = true;
         Thread.startVirtualThread(() -> {
            try {
               JsonObject o = this.apiClient.getAuthedJson("https://minecraftserversearch.com/api/addon/v1/filter-options").getAsJsonObject();
               List<SuggestionDropdown.Item> types = new ArrayList<>();

               for (JsonElement e : jsonArray(o, "types")) {
                  if (e.isJsonObject()) {
                     JsonObject t = e.getAsJsonObject();
                     String name = getString(t, "name", "");
                     if (!name.isEmpty()) {
                        types.add(new SuggestionDropdown.Item(name, name, getLong(t, "count", 0L), name));
                     }
                  }
               }

               List<SuggestionDropdown.Item> versions = new ArrayList<>();

               for (JsonElement ex : jsonArray(o, "versions")) {
                  if (ex.isJsonObject()) {
                     JsonObject v = ex.getAsJsonObject();
                     String label = getString(v, "label", "");
                     if (!label.isEmpty()) {
                        versions.add(new SuggestionDropdown.Item(label, label, getLong(v, "count", 0L), label));
                     }
                  }
               }

               List<SuggestionDropdown.Item> software = new ArrayList<>();

               for (JsonElement exx : jsonArray(o, "software")) {
                  if (exx.isJsonObject()) {
                     JsonObject s = exx.getAsJsonObject();
                     String name = getString(s, "name", "");
                     if (!name.isEmpty()) {
                        software.add(new SuggestionDropdown.Item(name, name, getLong(s, "count", 0L), name));
                     }
                  }
               }

               List<SuggestionDropdown.Item> countries = new ArrayList<>();

               for (JsonElement exxx : jsonArray(o, "countries")) {
                  if (exxx.isJsonObject()) {
                     JsonObject c = exxx.getAsJsonObject();
                     String code = getString(c, "code", "");
                     if (!code.isEmpty()) {
                        String name = countryLabel(code);
                        String label = name.equalsIgnoreCase(code) ? code : name + " (" + code + ")";
                        countries.add(new SuggestionDropdown.Item(code, label, getLong(c, "count", 0L), label + " " + code));
                     }
                  }
               }

               List<SuggestionDropdown.Item> mods = new ArrayList<>();

               for (JsonElement exxxx : jsonArray(o, "mods")) {
                  if (exxxx.isJsonObject()) {
                     JsonObject m = exxxx.getAsJsonObject();
                     String id = getString(m, "id", "");
                     if (!id.isEmpty()) {
                        mods.add(new SuggestionDropdown.Item(id, id, getLong(m, "count", 0L), id));
                     }
                  }
               }

               List<SuggestionDropdown.Item> plugins = new ArrayList<>();

               for (JsonElement exxxxx : jsonArray(o, "plugins")) {
                  if (exxxxx.isJsonObject()) {
                     JsonObject p = exxxxx.getAsJsonObject();
                     String name = getString(p, "name", "");
                     String key = getString(p, "key", "");
                     String value = !key.isEmpty() ? key : name;
                     if (!value.isEmpty()) {
                        StringBuilder search = new StringBuilder(name).append(' ').append(key);

                        for (JsonElement a : jsonArray(p, "aliases")) {
                           if (!a.isJsonNull()) {
                              search.append(' ').append(a.getAsString());
                           }
                        }

                        plugins.add(new SuggestionDropdown.Item(value, name.isEmpty() ? value : name, getLong(p, "count", 0L), search.toString()));
                     }
                  }
               }

               Map<String, List<SuggestionDropdown.Item>> byVersion = new HashMap<>();

               for (JsonElement exxxxxx : jsonArray(o, "software_by_version")) {
                  if (exxxxxx.isJsonObject()) {
                     JsonObject s = exxxxxx.getAsJsonObject();
                     String version = getString(s, "version", "");
                     String name = getString(s, "name", "");
                     if (!version.isEmpty() && !name.isEmpty()) {
                        byVersion.computeIfAbsent(version, k -> new ArrayList<>()).add(new SuggestionDropdown.Item(name, name, getLong(s, "count", 0L), name));
                     }
                  }
               }

               if (this.minecraft != null) {
                  this.minecraft.execute(() -> {
                     this.optTypes = types;
                     this.optVersions = versions;
                     this.optSoftware = software;
                     this.optCountries = countries;
                     this.optMods = mods;
                     this.optPlugins = plugins;
                     this.optSoftwareByVersion = byVersion;
                     this.filterOptionsStatus = "";
                     this.filterOptionsFetchInFlight = false;
                  });
               } else {
                  this.filterOptionsFetchInFlight = false;
               }
            } catch (ApiException var17) {
               if (this.minecraft != null) {
                  this.minecraft.execute(() -> {
                     this.filterOptionsFetchInFlight = false;
                     this.filterOptionsStatus = "Filter suggestions unavailable";
                     if (var17.isAuthFailure()) {
                        this.returnToLogin(var17.getMessage());
                     }
                  });
               } else {
                  this.filterOptionsFetchInFlight = false;
               }
            } catch (Exception var18) {
               if (this.minecraft != null) {
                  this.minecraft.execute(() -> {
                     this.filterOptionsFetchInFlight = false;
                     this.filterOptionsStatus = "Filter suggestions unavailable";
                  });
               } else {
                  this.filterOptionsFetchInFlight = false;
               }
            }
         });
      }
   }

   private static JsonArray jsonArray(JsonObject obj, String key) {
      return obj.has(key) && obj.get(key).isJsonArray() ? obj.getAsJsonArray(key) : new JsonArray();
   }

   private static String countryLabel(String code) {
      try {
         String name = Locale.of("", code).getDisplayCountry(Locale.ENGLISH);
         return name != null && !name.isBlank() ? name : code;
      } catch (Exception var2) {
         return code;
      }
   }

   private void setActiveTab(ServerScannerScreen.Tab tab) {
      this.activeTab = tab;
      if (tab == ServerScannerScreen.Tab.LIVE && this.liveEntries.isEmpty() && !this.liveLoading && !LAST_LIVE_CACHE.valid) {
         this.fetchLive();
      }

      this.saveUiState();
      this.updateTabVisibility();
   }

   private void applyUiState() {
      this.serverTypeField.setValue(LAST_UI_STATE.serverType);
      this.versionField.setValue(LAST_UI_STATE.version);
      this.softwareField.setValue(LAST_UI_STATE.software);
      this.countryField.setValue(LAST_UI_STATE.country);
      this.addressField.setValue(LAST_UI_STATE.address);
      this.searchField.setValue(LAST_UI_STATE.search);
      this.modNameField.setValue(LAST_UI_STATE.modName);
      this.pluginNameField.setValue(LAST_UI_STATE.pluginName);
      this.playerNameField.setValue(LAST_UI_STATE.playerName);
      this.minPlayersField.setValue(LAST_UI_STATE.minPlayers);
      this.maxPlayersField.setValue(LAST_UI_STATE.maxPlayers);
      this.pageField.setValue(Integer.toString(Math.max(1, LAST_UI_STATE.currentPage)));
      this.currentPage = Math.max(1, LAST_UI_STATE.currentPage);
      this.crackedFilter = LAST_UI_STATE.crackedFilter;
      this.hasPlayersFilter = LAST_UI_STATE.hasPlayersFilter;
      this.moddedFilter = LAST_UI_STATE.moddedFilter;
      this.isFullFilter = LAST_UI_STATE.isFullFilter;
      this.hasFaviconFilter = LAST_UI_STATE.hasFaviconFilter;
      this.crackedToggleButton.setMessage(buttonLabel("Cracked", this.crackedFilter));
      this.hasPlayersToggleButton.setMessage(buttonLabel("Has Players", this.hasPlayersFilter));
      this.moddedToggleButton.setMessage(buttonLabel("Modded", this.moddedFilter));
      this.isFullToggleButton.setMessage(buttonLabel("Is Full", this.isFullFilter));
      this.hasFaviconToggleButton.setMessage(buttonLabel("Has Icon", this.hasFaviconFilter));
      this.activeTab = LAST_UI_STATE.activeTab;
      this.scrollOffset = Math.max(0, LAST_UI_STATE.scrollOffset);
      this.liveScrollOffset = Math.max(0, LAST_UI_STATE.liveScrollOffset);
   }

   private boolean tryRestoreSearchCache() {
      if (!LAST_SEARCH_CACHE.valid) {
         return false;
      }
      String url = this.buildSearchUrl();
      if (!url.equals(LAST_SEARCH_CACHE.searchUrl)) {
         return false;
      }
      this.servers.clear();
      this.servers.addAll(LAST_SEARCH_CACHE.servers);
      this.loading = false;
      this.statusText = LAST_SEARCH_CACHE.statusText;
      this.searchButton.active = true;
      this.prevPageButton.active = LAST_SEARCH_CACHE.prevPageActive;
      this.nextPageButton.active = LAST_SEARCH_CACHE.nextPageActive;
      this.refreshActionButtons();
      return true;
   }

   private void tryRestoreLiveCache() {
      if (!LAST_LIVE_CACHE.valid) {
         return;
      }
      this.liveEntries.clear();
      this.liveEntries.addAll(LAST_LIVE_CACHE.entries);
      this.liveStatusText = LAST_LIVE_CACHE.statusText;
      this.liveLoading = false;
      this.refreshLiveActionButtons();
   }

   private void saveSearchCache(String url) {
      LAST_SEARCH_CACHE.valid = true;
      LAST_SEARCH_CACHE.searchUrl = url;
      LAST_SEARCH_CACHE.servers = List.copyOf(this.servers);
      LAST_SEARCH_CACHE.statusText = this.statusText;
      LAST_SEARCH_CACHE.prevPageActive = this.prevPageButton.active;
      LAST_SEARCH_CACHE.nextPageActive = this.nextPageButton.active;
   }

   private void saveSearchCacheFromScreen() {
      if (this.loading || this.serverTypeField == null || !this.statusText.startsWith("Loaded")) {
         return;
      }
      this.saveSearchCache(this.buildSearchUrl());
   }

   private void saveLiveCache() {
      LAST_LIVE_CACHE.valid = true;
      LAST_LIVE_CACHE.entries = List.copyOf(this.liveEntries);
      LAST_LIVE_CACHE.statusText = this.liveStatusText;
   }

   private static void invalidateSearchCache() {
      LAST_SEARCH_CACHE.valid = false;
      LAST_SEARCH_CACHE.servers = List.of();
   }

   private static void invalidateLiveCache() {
      LAST_LIVE_CACHE.valid = false;
      LAST_LIVE_CACHE.entries = List.of();
   }

   private void saveUiState() {
      if (this.serverTypeField != null) {
         LAST_UI_STATE.serverType = this.serverTypeField.getValue();
      }

      if (this.versionField != null) {
         LAST_UI_STATE.version = this.versionField.getValue();
      }

      if (this.softwareField != null) {
         LAST_UI_STATE.software = this.softwareField.getValue();
      }

      if (this.countryField != null) {
         LAST_UI_STATE.country = this.countryField.getValue();
      }

      if (this.addressField != null) {
         LAST_UI_STATE.address = this.addressField.getValue();
      }

      if (this.searchField != null) {
         LAST_UI_STATE.search = this.searchField.getValue();
      }

      if (this.modNameField != null) {
         LAST_UI_STATE.modName = this.modNameField.getValue();
      }

      if (this.pluginNameField != null) {
         LAST_UI_STATE.pluginName = this.pluginNameField.getValue();
      }

      if (this.playerNameField != null) {
         LAST_UI_STATE.playerName = this.playerNameField.getValue();
      }

      if (this.minPlayersField != null) {
         LAST_UI_STATE.minPlayers = this.minPlayersField.getValue();
      }

      if (this.maxPlayersField != null) {
         LAST_UI_STATE.maxPlayers = this.maxPlayersField.getValue();
      }

      LAST_UI_STATE.currentPage = parseIntOrDefault(
         this.pageField != null ? this.pageField.getValue() : Integer.toString(this.currentPage), this.currentPage
      );
      LAST_UI_STATE.crackedFilter = this.crackedFilter;
      LAST_UI_STATE.hasPlayersFilter = this.hasPlayersFilter;
      LAST_UI_STATE.moddedFilter = this.moddedFilter;
      LAST_UI_STATE.isFullFilter = this.isFullFilter;
      LAST_UI_STATE.hasFaviconFilter = this.hasFaviconFilter;
      LAST_UI_STATE.activeTab = this.activeTab;
      LAST_UI_STATE.scrollOffset = this.scrollOffset;
      LAST_UI_STATE.liveScrollOffset = this.liveScrollOffset;
   }

   private void updateTabVisibility() {
      boolean dashboard = this.activeTab == ServerScannerScreen.Tab.DASHBOARD;

      for (ServerScannerScreen.FieldLabel fl : this.fieldLabels) {
         fl.field().visible = dashboard && !this.suppressedFields.contains(fl.field());
      }

      this.crackedToggleButton.visible = dashboard && !this.suppressedToggles.contains(this.crackedToggleButton);
      this.hasPlayersToggleButton.visible = dashboard && !this.suppressedToggles.contains(this.hasPlayersToggleButton);
      this.moddedToggleButton.visible = dashboard && !this.suppressedToggles.contains(this.moddedToggleButton);
      this.isFullToggleButton.visible = dashboard && !this.suppressedToggles.contains(this.isFullToggleButton);
      this.hasFaviconToggleButton.visible = dashboard && !this.suppressedToggles.contains(this.hasFaviconToggleButton);
      this.prevPageButton.visible = dashboard;
      this.nextPageButton.visible = dashboard;
      this.searchButton.visible = dashboard;
      this.clearButton.visible = dashboard;

      for (ScannerActionButton b : this.joinButtons) {
         b.visible = dashboard && b.active;
      }

      for (ScannerActionButton b : this.addButtons) {
         b.visible = dashboard && b.active;
      }

      for (ScannerActionButton b : this.copyButtons) {
         b.visible = dashboard && b.active;
      }

      for (ScannerActionButton b : this.liveJoinButtons) {
         b.visible = !dashboard && b.active;
      }

      for (ScannerActionButton b : this.liveAddButtons) {
         b.visible = !dashboard && b.active;
      }

      for (ScannerActionButton b : this.liveCopyButtons) {
         b.visible = !dashboard && b.active;
      }

      this.dashboardTabButton.active = !dashboard;
      this.liveTabButton.active = dashboard;
   }

   private void fetchLive() {
      this.liveLoading = true;
      this.liveStatusText = "Loading live sightings...";
      Thread.startVirtualThread(
         () -> {
            String body;
            try {
               body = this.apiClient.getAuthed("https://minecraftserversearch.com/api/addon/v1/live");
            } catch (ApiException var7) {
               if (this.minecraft != null) {
                  this.minecraft.execute(() -> {
                     this.liveLoading = false;
                     this.liveStatusText = var7.isAuthFailure() ? "Session ended — please sign in again" : "Live fetch failed: " + var7.getMessage();
                     if (var7.isAuthFailure()) {
                        this.returnToLogin(var7.getMessage());
                     }
                  });
               }

               return;
            }

            try {
               JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
               List<ServerScannerScreen.LiveEntry> parsed = new ArrayList<>();

               for (JsonElement e : arr) {
                  if (e.isJsonObject()) {
                     JsonObject o = e.getAsJsonObject();
                     parsed.add(
                        new ServerScannerScreen.LiveEntry(
                           getString(o, "user_login", ""),
                           getString(o, "user_name", ""),
                           getString(o, "stream_title", ""),
                           getString(o, "stream_started_at", ""),
                           getString(o, "server_ip", ""),
                           getInt(o, "server_port", 25565),
                           getString(o, "server_type", "-"),
                           getString(o, "country_code", "-"),
                           getString(o, "platform", "twitch"),
                           getInt(o, "confidence", 0),
                           getInt(o, "sightings_today", 0),
                           getBoolean(o, "is_live_now", false),
                           getString(o, "last_seen_at", ""),
                           getString(o, "first_seen_at", ""),
                           getBooleanNullable(o, "enforces_secure_chat")
                        )
                     );
                  }
               }

               if (this.minecraft != null) {
                  this.minecraft.execute(() -> {
                     this.liveEntries.clear();
                     this.liveEntries.addAll(parsed);
                     this.liveScrollOffset = 0;
                     this.liveLoading = false;
                     this.liveStatusText = "Loaded " + parsed.size() + " live streamers";
                     this.refreshLiveActionButtons();
                     this.saveLiveCache();
                  });
               }
            } catch (Exception var8) {
               if (this.minecraft != null) {
                  this.minecraft.execute(() -> {
                     this.liveLoading = false;
                     this.liveStatusText = "Live fetch failed: " + var8.getMessage();
                  });
               }
            }
         }
      );
   }

   private String buildSearchUrl() {
      Map<String, String> params = new LinkedHashMap<>();
      params.put("page", Integer.toString(this.currentPage));
      putIfPresent(params, "server_type", this.serverTypeField.getValue());
      putIfPresent(params, "version", this.versionField.getValue());
      putIfPresent(params, "software", this.softwareField.getValue());
      putIfPresent(params, "search", this.searchField.getValue());
      putIfPresent(params, "address", this.addressField.getValue());
      putIfPresent(params, "country", this.countryField.getValue().toUpperCase());
      putIfPresent(params, "mod_name", this.modNameField.getValue());
      putIfPresent(params, "plugin_name", this.pluginNameField.getValue());
      putIfPresent(params, "player_name", this.playerNameField.getValue());
      Integer minPlayers = parseOptionalInt(this.minPlayersField.getValue());
      Integer maxPlayers = parseOptionalInt(this.maxPlayersField.getValue());
      if (minPlayers != null) {
         params.put("min_players", Integer.toString(minPlayers));
      }

      if (maxPlayers != null) {
         params.put("max_players", Integer.toString(maxPlayers));
      }

      putTriState(params, "cracked", this.crackedFilter);
      putTriState(params, "has_players", this.hasPlayersFilter);
      putTriState(params, "modded", this.moddedFilter);
      putTriState(params, "is_full", this.isFullFilter);
      putTriState(params, "has_favicon", this.hasFaviconFilter);
      StringBuilder url = new StringBuilder("https://minecraftserversearch.com/api/addon/v1/servers").append("?");
      boolean first = true;

      for (Entry<String, String> e : params.entrySet()) {
         if (!first) {
            url.append("&");
         }

         first = false;
         url.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
         url.append("=").append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
      }

      return url.toString();
   }

   private static void putIfPresent(Map<String, String> params, String key, String value) {
      String clean = value == null ? "" : value.trim();
      if (!clean.isEmpty()) {
         params.put(key, clean);
      }
   }

   private static void putTriState(Map<String, String> params, String key, ServerScannerScreen.TriState value) {
      if (value == ServerScannerScreen.TriState.TRUE) {
         params.put(key, "true");
      } else if (value == ServerScannerScreen.TriState.FALSE) {
         params.put(key, "false");
      }
   }

   private void fetchServers(String url, int generation) {
      String body;
      try {
         body = this.apiClient.getAuthed(url);
      } catch (ApiException var10) {
         if (var10.isAuthFailure()) {
            if (this.minecraft != null) {
               this.minecraft.execute(() -> {
                  this.loading = false;
                  this.statusText = "Session ended — please sign in again";
                  this.returnToLogin(var10.getMessage());
               });
            }
         } else {
            this.setError(generation, "Search failed: " + var10.getMessage());
         }

         return;
      }

      try {
         JsonObject root = JsonParser.parseString(body).getAsJsonObject();
         JsonArray arr = root.has("servers") && root.get("servers").isJsonArray() ? root.getAsJsonArray("servers") : new JsonArray();
         List<ServerScannerScreen.ServerEntry> parsed = new ArrayList<>();

         for (JsonElement e : arr) {
            if (e.isJsonObject()) {
               JsonObject o = e.getAsJsonObject();
               parsed.add(
                  new ServerScannerScreen.ServerEntry(
                     getString(o, "ip", ""),
                     getInt(o, "port", 25565),
                     getString(o, "version_name", "Unknown"),
                     getString(o, "server_type", "Unknown"),
                     getString(o, "description", ""),
                     getString(o, "country_code", "--"),
                     getInt(o, "players_online", 0),
                     getInt(o, "players_max", 0),
                     getString(o, "last_seen", ""),
                     getString(o, "favicon", "")
                  )
               );
            }
         }

         if (this.minecraft != null) {
            this.minecraft.execute(() -> {
               if (generation == this.searchGeneration) {
                  this.servers.clear();
                  this.servers.addAll(parsed);
                  this.loading = false;
                  this.scrollOffset = 0;
                  this.statusText = "Loaded " + parsed.size() + " servers from page " + this.currentPage + " (up to 50)";
                  this.searchButton.active = true;
                  this.prevPageButton.active = this.currentPage > 1;
                  this.nextPageButton.active = parsed.size() >= 50;
                  this.refreshActionButtons();
                  this.saveSearchCache(url);
               }
            });
         }
      } catch (Exception var11) {
         this.setError(generation, "Search failed: " + var11.getMessage());
      }
   }

   private void setError(int generation, String msg) {
      if (this.minecraft != null) {
         this.minecraft.execute(() -> {
            if (generation == this.searchGeneration) {
               this.loading = false;
               this.statusText = msg;
               this.searchButton.active = true;
               this.prevPageButton.active = this.currentPage > 1;
               this.nextPageButton.active = true;
               this.refreshActionButtons();
               invalidateSearchCache();
            }
         });
      }
   }

   private void returnToLogin(String reason) {
      if (this.minecraft != null) {
         if (reason != null && !reason.isEmpty()) {
            DupeClient.LOGGER.info("Returning to login screen: {}", reason);
         }

         this.minecraft.gui.setScreen(new ServerSearchAuthScreen(this.parent));
      }
   }

   private void joinVisible(int localIndex) {
      int idx = this.scrollOffset + localIndex;
      if (idx >= 0 && idx < this.servers.size() && this.minecraft != null) {
         ServerScannerScreen.ServerEntry s = this.servers.get(idx);
         String address = s.ip + ":" + s.port;
         ServerAddress parsed = ServerAddress.parseString(address);
         ServerData info = new ServerData(s.ip, address, ServerData.Type.OTHER);
         ConnectScreen.startConnecting(this, this.minecraft, parsed, info, false, null);
      }
   }

   private void addVisible(int localIndex) {
      int idx = this.scrollOffset + localIndex;
      if (idx >= 0 && idx < this.servers.size() && this.minecraft != null) {
         ServerScannerScreen.ServerEntry s = this.servers.get(idx);
         String address = s.ip + ":" + s.port;
         ServerData info = new ServerData(s.ip, address, ServerData.Type.OTHER);
         if (this.addToSavedServerList(info)) {
            this.statusText = "Added " + address + " to server list";
            DupeClientToasts.show("Server list", "Added " + address);
         } else {
            this.statusText = address + " is already on your server list";
         }
      }
   }

   private void copyVisible(int localIndex) {
      int idx = this.scrollOffset + localIndex;
      if (idx >= 0 && idx < this.servers.size() && this.minecraft != null) {
         ServerScannerScreen.ServerEntry s = this.servers.get(idx);
         this.copyServerAddress(s.ip + ":" + s.port);
      }
   }

   private void addAllVisible() {
      if (this.minecraft == null || this.servers.isEmpty()) {
         this.statusText = "No servers to add";
         return;
      }
      int added = 0;
      int skipped = 0;
      for (ServerScannerScreen.ServerEntry s : this.servers) {
         String address = s.ip + ":" + s.port;
         ServerData info = new ServerData(s.ip, address, ServerData.Type.OTHER);
         if (this.addToSavedServerList(info)) {
            added++;
         } else {
            skipped++;
         }
      }
      this.statusText = "Added " + added + " server(s)" + (skipped > 0 ? " · " + skipped + " already listed" : "");
      DupeClientToasts.show("Server list", "Added " + added + " server(s)");
   }

   private void addBulkVisible() {
      if (this.minecraft == null || this.servers.isEmpty()) {
         this.statusText = "No servers to add";
         return;
      }
      int count = parseIntOrDefault(this.bulkAddCountField.getValue(), 1);
      count = Math.max(1, Math.min(this.servers.size(), count));
      int added = 0;
      int skipped = 0;
      for (int i = 0; i < count; i++) {
         ServerScannerScreen.ServerEntry s = this.servers.get(i);
         String address = s.ip + ":" + s.port;
         ServerData info = new ServerData(s.ip, address, ServerData.Type.OTHER);
         if (this.addToSavedServerList(info)) {
            added++;
         } else {
            skipped++;
         }
      }
      this.statusText = "Added " + added + " of " + count + " server(s)"
         + (skipped > 0 ? " · " + skipped + " already listed" : "");
      DupeClientToasts.show("Server list", "Added " + added + " server(s)");
   }

   private void copyServerAddress(String address) {
      if (this.minecraft != null && this.minecraft.keyboardHandler != null && address != null && !address.isBlank()) {
         this.minecraft.keyboardHandler.setClipboard(address);
         this.statusText = "Copied " + address;
         DupeClientToasts.show("Copied", address);
      }
   }

   private void joinLive(int localIndex) {
      int idx = this.liveScrollOffset + localIndex;
      if (idx >= 0 && idx < this.liveEntries.size() && this.minecraft != null) {
         ServerScannerScreen.LiveEntry e = this.liveEntries.get(idx);
         String host = this.normalizeServerIp(e.serverIp);
         String address = host + ":" + e.serverPort;
         ServerAddress parsed = ServerAddress.parseString(address);
         ServerData info = new ServerData(host, address, ServerData.Type.OTHER);
         ConnectScreen.startConnecting(this, this.minecraft, parsed, info, false, null);
      }
   }

   private void addLive(int localIndex) {
      int idx = this.liveScrollOffset + localIndex;
      if (idx >= 0 && idx < this.liveEntries.size() && this.minecraft != null) {
         ServerScannerScreen.LiveEntry e = this.liveEntries.get(idx);
         String host = this.normalizeServerIp(e.serverIp);
         String address = host + ":" + e.serverPort;
         ServerData info = new ServerData(host, address, ServerData.Type.OTHER);
         if (this.addToSavedServerList(info)) {
            this.liveStatusText = "Added " + address + " to server list";
            DupeClientToasts.show("Server list", "Added " + address);
         } else {
            this.liveStatusText = address + " is already on your server list";
         }
      }
   }

   private void copyLive(int localIndex) {
      int idx = this.liveScrollOffset + localIndex;
      if (idx >= 0 && idx < this.liveEntries.size() && this.minecraft != null) {
         ServerScannerScreen.LiveEntry e = this.liveEntries.get(idx);
         String host = this.normalizeServerIp(e.serverIp);
         this.copyServerAddress(host + ":" + e.serverPort);
      }
   }

   private boolean addToSavedServerList(ServerData info) {
      if (this.minecraft == null) {
         return false;
      }
      ServerList list;
      JoinMultiplayerScreen mp = this.findUnderlyingMultiplayerScreen();
      if (mp != null) {
         list = mp.getServers();
      } else {
         list = new ServerList(this.minecraft);
      }
      list.load();
      for (int i = 0; i < list.size(); i++) {
         ServerData existing = list.get(i);
         if (existing != null && info.ip.equals(existing.ip)) {
            return false;
         }
      }
      list.add(info, false);
      list.save();
      if (mp != null) {
         ((MultiplayerScreenAccessor)mp).getServerListWidget().updateOnlineServers(list);
      }
      return true;
   }

   private JoinMultiplayerScreen findUnderlyingMultiplayerScreen() {
      if (this.parent instanceof JoinMultiplayerScreen mp) {
         return mp;
      } else {
         return this.parent instanceof ServerSearchAuthScreen auth && auth.getNavigationParent() instanceof JoinMultiplayerScreen mp ? mp : null;
      }
   }

   private void refreshActionButtons() {
      for (int i = 0; i < this.joinButtons.size(); i++) {
         int idx = this.scrollOffset + i;
         boolean active = idx < this.servers.size();
         ScannerActionButton join = this.joinButtons.get(i);
         ScannerActionButton add = this.addButtons.get(i);
         ScannerActionButton copy = this.copyButtons.get(i);
         join.visible = active && this.activeTab == ServerScannerScreen.Tab.DASHBOARD;
         join.active = active;
         add.visible = active && this.activeTab == ServerScannerScreen.Tab.DASHBOARD;
         add.active = active;
         copy.visible = active && this.activeTab == ServerScannerScreen.Tab.DASHBOARD;
         copy.active = active;
      }
   }

   private void refreshLiveActionButtons() {
      for (int i = 0; i < this.liveJoinButtons.size(); i++) {
         int idx = this.liveScrollOffset + i;
         boolean active = idx < this.liveEntries.size();
         ScannerActionButton lj = this.liveJoinButtons.get(i);
         ScannerActionButton la = this.liveAddButtons.get(i);
         ScannerActionButton lc = this.liveCopyButtons.get(i);
         lj.active = active;
         la.active = active;
         lc.active = active;
         if (this.activeTab == ServerScannerScreen.Tab.LIVE) {
            lj.visible = active;
            la.visible = active;
            lc.visible = active;
         } else {
            lj.visible = false;
            la.visible = false;
            lc.visible = false;
         }
      }
   }

   private String normalizeServerIp(String serverIp) {
      if (serverIp == null) {
         return "";
      } else {
         int slash = serverIp.indexOf(47);
         return slash > 0 ? serverIp.substring(0, slash) : serverIp;
      }
   }

   private void openDetails(ServerScannerScreen.ServerEntry entry) {
      this.detailsModalOpen = true;
      this.detailsLoading = true;
      this.detailsStatus = "Loading server details...";
      this.detailsTarget = entry;
      this.detailsData = null;
      this.detailsDescriptionExpanded = false;
      this.detailsPlayersExpanded = false;
      this.detailsPluginsExpanded = false;
      this.descToggleVisible = false;
      this.playersToggleVisible = false;
      this.pluginsToggleVisible = false;
      String url = "https://minecraftserversearch.com/api/addon/v1/servers/" + entry.ip + "/" + entry.port;
      Thread.startVirtualThread(() -> this.fetchServerDetails(url, entry));
   }

   private void fetchServerDetails(String url, ServerScannerScreen.ServerEntry entry) {
      String body;
      try {
         body = this.apiClient.getAuthed(url);
      } catch (ApiException var11) {
         if (this.minecraft != null) {
            this.minecraft.execute(() -> {
               this.detailsLoading = false;
               this.detailsStatus = var11.isAuthFailure() ? "Session ended — please sign in again" : "Failed to load details: " + var11.getMessage();
               if (var11.isAuthFailure()) {
                  this.returnToLogin(var11.getMessage());
               }
            });
         }

         return;
      }

      try {
         JsonObject o = JsonParser.parseString(body).getAsJsonObject();
         List<String> recentPlayers = new ArrayList<>();
         if (o.has("recent_players") && o.get("recent_players").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("recent_players")) {
               if (e.isJsonObject()) {
                  JsonObject p = e.getAsJsonObject();
                  String name = getString(p, "name", "");
                  if (!name.isBlank()) {
                     recentPlayers.add(name);
                  }
               }
            }
         }

         List<String> plugins = new ArrayList<>();
         if (o.has("detected") && o.get("detected").isJsonObject()) {
            JsonObject detected = o.getAsJsonObject("detected");
            if (detected.has("plugins") && detected.get("plugins").isJsonArray()) {
               for (JsonElement ex : detected.getAsJsonArray("plugins")) {
                  if (!ex.isJsonNull()) {
                     plugins.add(ex.getAsString());
                  }
               }
            }
         }

         ServerScannerScreen.ServerDetails parsed = new ServerScannerScreen.ServerDetails(
            getString(o, "ip", entry.ip),
            getInt(o, "port", entry.port),
            getString(o, "version_name", entry.version),
            getInt(o, "version_protocol", 0),
            this.sanitizeMotd(getString(o, "description", entry.description)),
            getString(o, "server_type", entry.software),
            getInt(o, "players_online", entry.playersOnline),
            getInt(o, "players_max", entry.playersMax),
            getString(o, "country_code", entry.country),
            getString(o, "city_name", "-"),
            getString(o, "asn", "-"),
            getString(o, "last_seen", entry.lastSeenIso),
            getString(o, "favicon", entry.faviconDataUrl),
            getString(o, "software", "-"),
            recentPlayers,
            plugins
         );
         if (this.minecraft != null) {
            this.minecraft.execute(() -> {
               this.detailsLoading = false;
               this.detailsStatus = "";
               this.detailsData = parsed;
            });
         }
      } catch (Exception var12) {
         if (this.minecraft != null) {
            this.minecraft.execute(() -> {
               this.detailsLoading = false;
               this.detailsStatus = "Details request failed: " + var12.getMessage();
            });
         }
      }
   }

   public boolean keyPressed(KeyEvent input) {
      if (!this.detailsModalOpen && this.activeTab == ServerScannerScreen.Tab.DASHBOARD) {
         SuggestionDropdown d = this.activeDropdown();
         if (d != null) {
            d.refresh();
            int keyCode = input.key();
            if (d.hasSuggestions()) {
               switch (keyCode) {
                  case 256:
                     d.close();
                     return true;
                  case 257:
                  case 335:
                     if (d.selectHighlighted()) {
                        return true;
                     }
                     break;
                  case 264:
                     d.moveHighlight(1);
                     return true;
                  case 265:
                     d.moveHighlight(-1);
                     return true;
               }
            } else if (keyCode == 256) {
               d.close();
               return true;
            }
         }
      }

      return super.keyPressed(input);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.detailsModalOpen) {
         return true;
      } else {
         if (this.activeTab == ServerScannerScreen.Tab.DASHBOARD) {
            SuggestionDropdown d = this.activeDropdown();
            if (d != null && d.isMouseOverPopup(mouseX, mouseY)) {
               d.scroll(verticalAmount);
               return true;
            }
         }

         if (this.activeTab == ServerScannerScreen.Tab.LIVE) {
            int maxOffset = Math.max(0, this.liveEntries.size() - this.liveVisibleCards);
            if (maxOffset == 0) {
               return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            } else {
               if (verticalAmount < 0.0) {
                  this.liveScrollOffset = Math.min(maxOffset, this.liveScrollOffset + 1);
               } else if (verticalAmount > 0.0) {
                  this.liveScrollOffset = Math.max(0, this.liveScrollOffset - 1);
               }

               this.refreshLiveActionButtons();
               return true;
            }
         } else if (this.servers.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
         } else {
            int maxOffset = Math.max(0, this.servers.size() - this.visibleRows);
            if (maxOffset == 0) {
               return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            } else {
               if (verticalAmount < 0.0) {
                  this.scrollOffset = Math.min(maxOffset, this.scrollOffset + 1);
               } else if (verticalAmount > 0.0) {
                  this.scrollOffset = Math.max(0, this.scrollOffset - 1);
               }

               this.refreshActionButtons();
               return true;
            }
         }
      }
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      double mouseX = click.x();
      double mouseY = click.y();
      if (this.detailsModalOpen) {
         int modalW = 680;
         int modalH = this.getDetailsModalHeight();
         int modalX = (this.width - modalW) / 2;
         int modalY = (this.height - modalH) / 2;
         if (mouseX >= modalX + modalW - 26 && mouseX <= modalX + modalW - 6 && mouseY >= modalY + 6 && mouseY <= modalY + 24) {
            this.detailsModalOpen = false;
            this.detailsTarget = null;
            this.detailsData = null;
            this.detailsStatus = "";
            return true;
         } else if (this.detailsData != null
            && this.isInRect(mouseX, mouseY, this.detailsCopyIpX, this.detailsCopyIpY, this.detailsCopyIpW, this.detailsCopyIpH)) {
            this.copyServerAddress(this.detailsData.ip + ":" + this.detailsData.port);
            return true;
         } else if (this.descToggleVisible && this.isInRect(mouseX, mouseY, this.descToggleX, this.descToggleY, this.descToggleW, this.descToggleH)) {
            this.detailsDescriptionExpanded = !this.detailsDescriptionExpanded;
            return true;
         } else if (this.playersToggleVisible
            && this.isInRect(mouseX, mouseY, this.playersToggleX, this.playersToggleY, this.playersToggleW, this.playersToggleH)) {
            this.detailsPlayersExpanded = !this.detailsPlayersExpanded;
            return true;
         } else if (this.pluginsToggleVisible
            && this.isInRect(mouseX, mouseY, this.pluginsToggleX, this.pluginsToggleY, this.pluginsToggleW, this.pluginsToggleH)) {
            this.detailsPluginsExpanded = !this.detailsPluginsExpanded;
            return true;
         } else {
            return true;
         }
      } else {
         if (this.activeTab == ServerScannerScreen.Tab.DASHBOARD) {
            SuggestionDropdown d = this.activeDropdown();
            if (d != null && d.isMouseOverPopup(mouseX, mouseY)) {
               d.onClick(mouseX, mouseY);
               return true;
            }
         }

         boolean handled = super.mouseClicked(click, doubled);
         if (handled) {
            return true;
         } else {
            if (this.activeTab == ServerScannerScreen.Tab.DASHBOARD) {
               for (ServerScannerScreen.ChipRegion c : this.chipRegions) {
                  if (this.isInRect(mouseX, mouseY, c.x(), c.y(), c.w(), c.h())) {
                     c.field().setValue("");
                     this.startSearch();
                     return true;
                  }
               }
            }

            if (this.activeTab == ServerScannerScreen.Tab.LIVE) {
               for (ServerScannerScreen.LiveLinkRegion link : this.liveLinkRegions) {
                  if (this.isInRect(mouseX, mouseY, link.x, link.y, link.w, link.h)) {
                     Util.getPlatform().openUri(link.url);
                     return true;
                  }
               }

               return false;
            } else {
               int listTop = this.getListTop();
               int listBottom = this.height - 26;
               if (mouseX >= 10.0 && mouseX <= this.width - 10 && mouseY >= listTop && mouseY <= listBottom) {
                  int row = (int)((mouseY - listTop) / 30.0);
                  int idx = this.scrollOffset + row;
                  if (idx >= 0 && idx < this.servers.size()) {
                     this.openDetails(this.servers.get(idx));
                     return true;
                  }
               }

               return false;
            }
         }
      }
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
      if (this.activeTab == ServerScannerScreen.Tab.LIVE) {
         this.renderTopStats(context);
         this.renderLivePanelBackgrounds(context);
         this.positionLiveActionButtons();
         this.refreshLiveActionButtons();
         super.extractRenderState(context, mouseX, mouseY, deltaTicks);
         this.renderLivePanelForeground(context);
         int liveColor = this.liveLoading ? -1261205 : (this.liveStatusText.startsWith("Live fetch failed") ? -32640 : -7937906);
         context.text(this.font, Component.literal(this.liveStatusText), 10, this.height - 14, liveColor);
         this.renderAccountLabel(context);
         if (this.detailsModalOpen) {
            this.renderDetailsModal(context);
         }
      } else {
         this.renderTopStats(context);
         int listTop = this.getListTop();
         int listBottom = this.height - 26;
         context.fill(8, 28, this.width - 8, this.controlsBottomY, 1427380774);
         if (this.chipBandY >= 0) {
            context.fill(8, this.chipBandY - 2, this.width - 8, this.chipBandY + 12, 1713516098);
         }

         for (ServerScannerScreen.FieldLabel fl : this.fieldLabels) {
            if (fl.field().visible) {
               this.drawLabel(context, fl.label(), fl.field().getX(), fl.field().getY() - 9);
            }
         }

         this.renderFilterChips(context);
         ServerScannerScreen.DashColumns cols = this.computeColumns();
         context.fill(8, listTop - 18, this.width - 8, listTop - 2, -2011157958);
         context.fill(8, listTop - 2, this.width - 8, listBottom, 1712066586);
         int headerY = listTop - 15;
         context.text(this.font, Component.literal("IP / MOTD"), cols.ipX() + 20, headerY, -5257217);
         context.text(this.font, Component.literal("Version"), cols.versionX(), headerY, -5257217);
         if (cols.showSoftware()) {
            context.text(this.font, Component.literal("Software"), cols.softwareX(), headerY, -5257217);
         }

         context.text(this.font, Component.literal("Players"), cols.playersX(), headerY, -5257217);
         context.text(this.font, Component.literal("Cty"), cols.countryX(), headerY, -5257217);
         if (cols.showLastSeen()) {
            context.text(this.font, Component.literal("Last Seen"), cols.lastSeenX(), headerY, -5257217);
         }

         context.text(this.font, Component.literal("Actions"), cols.actionsX() + 2, headerY, -5257217);

         this.positionDashboardActionButtons();
         this.refreshActionButtons();

         for (int row = 0; row < this.visibleRows; row++) {
            int idx = this.scrollOffset + row;
            if (idx >= this.servers.size()) {
               break;
            }

            int y = listTop + row * 30;
            int bg = row % 2 == 0 ? 1141839910 : 1142958907;
            context.fill(10, y, cols.actionsX() - 2, y + 26, bg);
         }

         super.extractRenderState(context, mouseX, mouseY, deltaTicks);

         for (int row = 0; row < this.visibleRows; row++) {
            int idx = this.scrollOffset + row;
            if (idx >= this.servers.size()) {
               break;
            }

            ServerScannerScreen.ServerEntry s = this.servers.get(idx);
            int y = listTop + row * 30;
            String ipPort = s.ip + ":" + s.port;
            String players = s.playersOnline + "/" + s.playersMax;
            int iconX = cols.ipX();
            int iconY = y + 2;
            Identifier favicon = this.getOrLoadFavicon(s);
            if (favicon != null) {
               context.blit(RenderPipelines.GUI_TEXTURED, favicon, iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
            } else {
               context.fill(iconX, iconY, iconX + 16, iconY + 16, 1714698822);
            }

            int textX = cols.ipX() + 20;
            int ipTextW = Math.max(0, cols.ipW() - 22);
            context.text(this.font, this.truncateToWidth(ipPort, ipTextW), textX, y + 5, -1, false);
            String desc = this.truncateToWidth(this.sanitizeMotd(s.description), cols.ipW());
            context.text(this.font, desc, cols.ipX(), y + 19, -8355712, false);
            context.text(this.font, this.truncateToWidth(s.version, cols.versionW()), cols.versionX(), y + 5, -4729345, false);
            if (cols.showSoftware()) {
               context.text(this.font, this.truncateToWidth(s.software, cols.softwareW()), cols.softwareX(), y + 5, -8398721, false);
            }

            context.text(this.font, players, cols.playersX(), y + 5, -1261205, false);
            context.text(this.font, s.country, cols.countryX(), y + 5, -3815995, false);
            if (cols.showLastSeen()) {
               context.text(
                  this.font, this.truncateToWidth(formatLastSeen(s.lastSeenIso), cols.lastSeenW()), cols.lastSeenX(), y + 5, -6250336, false
               );
            }
         }

         int from = this.servers.isEmpty() ? 0 : this.scrollOffset + 1;
         int to = Math.min(this.servers.size(), this.scrollOffset + this.visibleRows);
         context.text(
            this.font, Component.literal("Showing " + from + "-" + to + " of " + this.servers.size()), 10, this.height - 24, -7686913
         );
         int statusColor = this.loading ? -1261205 : (!this.statusText.startsWith("Search failed") && !this.statusText.startsWith("HTTP ") ? -7937906 : -32640);
         context.text(this.font, Component.literal(this.statusText), 10, this.height - 14, statusColor);
         this.renderAccountLabel(context);
         if (!this.detailsModalOpen) {
            SuggestionDropdown active = this.activeDropdown();
            if (active != null) {
               active.render(context);
            }
         }

         if (this.detailsModalOpen) {
            this.renderDetailsModal(context);
         }
      }
   }

   private void renderFilterChips(GuiGraphicsExtractor context) {
      this.chipRegions.clear();
      if (this.chipBandY >= 0) {
         int x = 12;
         int y = this.chipBandY;
         int maxX = this.width - 12;
         x = this.drawFilterChip(context, x, y, maxX, "Type", this.serverTypeField);
         x = this.drawFilterChip(context, x, y, maxX, "Version", this.versionField);
         x = this.drawFilterChip(context, x, y, maxX, "Software", this.softwareField);
         x = this.drawFilterChip(context, x, y, maxX, "Country", this.countryField);
         x = this.drawFilterChip(context, x, y, maxX, "Mod", this.modNameField);
         this.drawFilterChip(context, x, y, maxX, "Plugin", this.pluginNameField);
         if (!this.filterOptionsStatus.isEmpty()) {
            int w = this.font.width(this.filterOptionsStatus);
            context.text(this.font, this.filterOptionsStatus, maxX - w, y, -2054054, false);
         }
      }
   }

   private int drawFilterChip(GuiGraphicsExtractor context, int x, int y, int maxX, String label, StylishTextFieldWidget field) {
      String value = field != null && field.getValue() != null ? field.getValue().trim() : "";
      if (value.isEmpty()) {
         return x;
      } else {
         String text = this.truncateToWidth(label + ": " + value, 150);
         int textW = this.font.width(text);
         int xMarkW = this.font.width("x");
         int chipW = textW + 6 + xMarkW + 8;
         if (x + chipW > maxX) {
            return x;
         } else {
            context.fill(x, y - 1, x + chipW, y + 10, -2010035078);
            context.text(this.font, text, x + 4, y + 1, -1642753, false);
            int xMarkX = x + 4 + textW + 4;
            context.text(this.font, "x", xMarkX, y + 1, -28528, false);
            this.chipRegions.add(new ServerScannerScreen.ChipRegion(xMarkX - 2, y - 2, xMarkW + 6, 13, field));
            return x + chipW + 5;
         }
      }
   }

   private int actionAddButtonX() {
      return this.width - ACTION_EDGE_INSET - ACTION_ADD_BTN_W;
   }

   private int actionCopyButtonX() {
      return this.actionAddButtonX() - ACTION_BTN_GAP - ACTION_COPY_BTN_W;
   }

   private int actionJoinButtonX() {
      return this.actionCopyButtonX() - ACTION_BTN_GAP - ACTION_JOIN_BTN_W;
   }

   private void renderLivePanelBackgrounds(GuiGraphicsExtractor context) {
      int titleY = this.getLiveSectionTitleY();
      int listTop = this.getLiveCardsTop();
      int listBottom = this.height - 26;
      int cardH = 58;
      context.fill(8, titleY - 2, this.width - 8, listBottom, 1712066586);
      context.text(this.font, Component.literal("Live Streamer Sightings"), 12, titleY, -6360130);
      int cardW = this.width - 20;
      int joinX = this.actionJoinButtonX();

      for (int i = 0; i < this.liveVisibleCards; i++) {
         int idx = this.liveScrollOffset + i;
         int y = listTop + i * cardH;
         if (idx >= this.liveEntries.size()) {
            break;
         }

         ServerScannerScreen.LiveEntry e = this.liveEntries.get(idx);
         context.fill(10, y, 10 + cardW, y + cardH - 2, 1714437464);
         context.fill(10, y, 10 + cardW, y + 20, -2009050000);
         String badge = e.isLiveNow ? "LIVE" : "EARLIER";
         int badgeW = this.font.width(badge) + 8;
         int bx = joinX - badgeW - 36;
         context.fill(bx, y + 4, bx + badgeW, y + 16, e.isLiveNow ? -1441830342 : -1439350694);
      }
   }

   private void positionLiveActionButtons() {
      int listTop = this.getLiveCardsTop();
      int joinX = this.actionJoinButtonX();
      int copyX = this.actionCopyButtonX();
      int addX = this.actionAddButtonX();

      for (int i = 0; i < this.liveVisibleCards; i++) {
         int y = listTop + i * 58;
         this.liveJoinButtons.get(i).setPosition(joinX, y + 1);
         this.liveCopyButtons.get(i).setPosition(copyX, y + 1);
         this.liveAddButtons.get(i).setPosition(addX, y + 1);
      }
   }

   private void positionDashboardActionButtons() {
      int listTop = this.getListTop();
      int joinX = this.actionJoinButtonX();
      int copyX = this.actionCopyButtonX();
      int addX = this.actionAddButtonX();

      for (int i = 0; i < this.visibleRows; i++) {
         int y = listTop + i * 30 + 2;
         this.joinButtons.get(i).setPosition(joinX, y);
         this.copyButtons.get(i).setPosition(copyX, y);
         this.addButtons.get(i).setPosition(addX, y);
      }
   }

   private void renderLivePanelForeground(GuiGraphicsExtractor context) {
      this.liveLinkRegions.clear();
      int listTop = this.getLiveCardsTop();
      int cardH = 58;
      int joinX = this.actionJoinButtonX();
      int titleW = joinX - 20;

      for (int i = 0; i < this.liveVisibleCards; i++) {
         int idx = this.liveScrollOffset + i;
         int y = listTop + i * cardH;
         if (idx >= this.liveEntries.size()) {
            break;
         }

         ServerScannerScreen.LiveEntry e = this.liveEntries.get(idx);
         String displayName = !e.userName.isBlank() ? e.userName : e.userLogin;
         int nameMaxW = joinX - 24 - 16;
         String username = this.truncateToWidth(displayName, Math.max(40, nameMaxW));
         int ux = 14;
         int uy = y + 6;
         context.text(this.font, Component.literal(username), ux, uy, -11672441);
         String url = "https://www.twitch.tv/" + e.userLogin;
         this.liveLinkRegions.add(new ServerScannerScreen.LiveLinkRegion(ux, uy, this.font.width(username), 9, url));
         String badge = e.isLiveNow ? "LIVE" : "EARLIER";
         int badgeW = this.font.width(badge) + 8;
         int bx = joinX - badgeW - 36;
         context.text(this.font, badge, bx + 4, y + 5, -1903873, false);
         context.text(this.font, e.confidence + "%", bx + badgeW + 4, y + 5, -1261205, false);
         int bodyY = y + 20 + 2;
         context.text(this.font, this.truncateToWidth(this.sanitizeMotd(e.streamTitle), titleW), 14, bodyY, -1, false);
         String srv = this.normalizeServerIp(e.serverIp) + ":" + e.serverPort;
         context.text(this.font, this.truncateToWidth(srv, 140), 14, bodyY + 11, -4204289, false);
         context.text(this.font, this.truncateToWidth(e.serverType, 72), 158, bodyY + 11, -4204289, false);
         context.text(this.font, e.countryCode, 236, bodyY + 11, -4204289, false);
         String chat = e.enforcesSecureChat == null ? "?" : (e.enforcesSecureChat ? "Secure" : "Off");
         context.text(this.font, chat, 268, bodyY + 11, -7351391, false);
         String meta = e.sightingsToday + " today · " + formatLastSeen(e.lastSeenAt) + " · " + formatTimeAgo(e.streamStartedAt);
         context.text(this.font, this.truncateToWidth(meta, titleW), 14, bodyY + 22, -7303024, false);
      }

      this.refreshLiveActionButtons();
   }

   private void drawLabel(GuiGraphicsExtractor context, String text, int x, int y) {
      context.text(this.font, Component.literal(text), x, y, 2141233312);
   }

   private void renderTopStats(GuiGraphicsExtractor context) {
      int limit = (this.signOutButton != null ? this.signOutButton.getX() : this.width - 84) - 8;
      int x = 190;
      x = this.drawStatChip(context, x, 6, limit, "Online " + this.formatStat(this.statsOnlineServers), -13787046, -16437990);
      x = this.drawStatChip(context, x + 6, 6, limit, "Total " + this.formatStat(this.statsTotalServers), -12158775, -16048576);
      this.drawStatChip(context, x + 6, 6, limit, "Countries " + this.formatStat(this.statsTotalCountries), -7706169, -14347457);
   }

   private int drawStatChip(GuiGraphicsExtractor context, int x, int y, int limit, String text, int borderColor, int fillColor) {
      int w = this.font.width(text) + 12;
      if (x + w > limit) {
         return x;
      } else {
         context.fill(x, y, x + w, y + 14, borderColor);
         context.fill(x + 1, y + 1, x + w - 1, y + 13, fillColor);
         context.text(this.font, text, x + 6, y + 3, -1, false);
         return x + w;
      }
   }

   private void renderAccountLabel(GuiGraphicsExtractor context) {
      String label = this.accountStatus;
      int w = this.font.width(label);
      int color = this.accountStatus.startsWith("Could not verify") ? -2054054 : -7364424;
      context.text(this.font, Component.literal(label), this.width - 10 - w, this.height - 14, color);
   }

   private String formatStat(int value) {
      return value < 0 ? "-" : String.format("%,d", value);
   }

   private String sanitizeMotd(String motd) {
      if (motd == null) {
         return "";
      } else {
         String cleaned = motd.replaceAll("§.", "");
         cleaned = cleaned.replace('\n', ' ').replace('\r', ' ');
         return cleaned.trim();
      }
   }

   private String truncateToWidth(String value, int maxWidth) {
      if (value.isEmpty()) {
         return value;
      } else if (this.font.width(value) <= maxWidth) {
         return value;
      } else {
         String ellipsis = "...";
         String trimmed = this.font.plainSubstrByWidth(value, Math.max(0, maxWidth - this.font.width(ellipsis)));
         return trimmed + ellipsis;
      }
   }

   private Identifier getOrLoadFavicon(ServerScannerScreen.ServerEntry entry) {
      return this.getOrLoadFavicon(entry.ip + ":" + entry.port, entry.faviconDataUrl);
   }

   private Identifier getOrLoadFavicon(String key, String data) {
      if (this.minecraft == null) {
         return null;
      } else {
         Identifier existing = this.faviconIds.get(key);
         if (existing != null) {
            return existing;
         } else if (this.faviconLoadFailed.contains(key)) {
            return null;
         } else if (data != null && !data.isBlank() && data.startsWith("data:image") && data.contains("base64,")) {
            try {
               String b64 = data.substring(data.indexOf("base64,") + "base64,".length());
               byte[] bytes = Base64.getDecoder().decode(b64);
               NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
               DynamicTexture texture = new DynamicTexture(() -> "server-scanner-favicon-" + key, image);
               Identifier id = Identifier.fromNamespaceAndPath("minecraft-server-scanner-addon", "favicon/" + Integer.toHexString(key.hashCode()));
               this.minecraft.getTextureManager().register(id, texture);
               this.faviconTextures.put(key, texture);
               this.faviconIds.put(key, id);
               return id;
            } catch (Exception var9) {
               this.faviconLoadFailed.add(key);
               return null;
            }
         } else {
            this.faviconLoadFailed.add(key);
            return null;
         }
      }
   }

   private void renderDetailsModal(GuiGraphicsExtractor context) {
      int modalW = 680;
      int modalH = this.getDetailsModalHeight();
      int x = (this.width - modalW) / 2;
      int y = (this.height - modalH) / 2;
      context.fill(0, 0, this.width, this.height, -2013265920);
      context.fill(x, y, x + modalW, y + modalH, -267381470);
      context.fill(x + 1, y + 1, x + modalW - 1, y + modalH - 1, -266788557);
      context.fill(x, y, x + modalW, y + 30, -14731944);
      context.text(this.font, Component.literal("Server Details"), x + 8, y + 8, -1);
      context.text(this.font, Component.literal("X"), x + modalW - 16, y + 9, -28528);
      if (this.detailsLoading) {
         context.centeredText(this.font, Component.literal(this.detailsStatus), x + modalW / 2, y + modalH / 2, -1261205);
      } else if (this.detailsData == null) {
         String msg = this.detailsStatus.isBlank() ? "No details loaded." : this.detailsStatus;
         context.centeredText(this.font, Component.literal(msg), x + modalW / 2, y + modalH / 2, -32640);
      } else {
         int left = x + 14;
         int right = x + 250;
         int lineY = y + 42;
         this.descToggleVisible = false;
         this.playersToggleVisible = false;
         this.pluginsToggleVisible = false;
         context.fill(left - 6, lineY - 6, right - 10, y + modalH - 14, 1714571108);
         context.fill(right - 8, lineY - 6, x + modalW - 14, y + modalH - 14, 1429225050);
         Identifier icon = this.getOrLoadFavicon("detail:" + this.detailsData.ip + ":" + this.detailsData.port, this.detailsData.faviconDataUrl);
         if (icon != null) {
            context.blit(RenderPipelines.GUI_TEXTURED, icon, left, lineY, 0.0F, 0.0F, 56, 56, 56, 56);
         } else {
            context.fill(left, lineY, left + 56, lineY + 56, 1714698822);
         }

         int summaryTextX = left + 66;
         int summaryTextW = Math.max(0, right - 16 - summaryTextX);
         int summaryY = lineY + 6;

         for (String line : this.wrapText(this.detailsData.ip + ":" + this.detailsData.port, summaryTextW, 2)) {
            context.text(this.font, Component.literal(line), summaryTextX, summaryY, -1);
            summaryY += 11;
         }

         String ipPort = this.detailsData.ip + ":" + this.detailsData.port;
         this.detailsCopyIpW = 52;
         this.detailsCopyIpH = 14;
         this.detailsCopyIpX = summaryTextX + summaryTextW - this.detailsCopyIpW;
         this.detailsCopyIpY = lineY + 4;
         context.fill(this.detailsCopyIpX, this.detailsCopyIpY, this.detailsCopyIpX + this.detailsCopyIpW, this.detailsCopyIpY + this.detailsCopyIpH, -2010035078);
         context.centeredText(this.font, Component.literal("Copy IP"), this.detailsCopyIpX + this.detailsCopyIpW / 2, this.detailsCopyIpY + 3, -6371585);

         for (String line : this.wrapText(this.detailsData.versionName, summaryTextW, 2)) {
            context.text(this.font, Component.literal(line), summaryTextX, summaryY, -4729345);
            summaryY += 11;
         }

         context.text(
            this.font, Component.literal(this.detailsData.playersOnline + "/" + this.detailsData.playersMax), summaryTextX, summaryY + 1, -1261205
         );
         int yInfo = y + 112;
         yInfo = this.drawInfoLine(context, "Type", this.detailsData.serverType, left, yInfo);
         yInfo = this.drawInfoLine(context, "Detected", this.detailsData.detectedSoftware, left, yInfo);
         yInfo = this.drawInfoLine(context, "Country", this.detailsData.countryCode + " (" + this.detailsData.cityName + ")", left, yInfo);
         yInfo = this.drawInfoLine(context, "ASN", this.detailsData.asn, left, yInfo);
         yInfo = this.drawInfoLine(context, "Protocol", Integer.toString(this.detailsData.versionProtocol), left, yInfo);
         this.drawInfoLine(context, "Last seen", formatLastSeen(this.detailsData.lastSeenIso), left, yInfo);
         int descX = right;
         int descW = modalW - (right - x) - 14;
         String fullDescription = this.detailsData.description == null ? "-" : this.detailsData.description;
         int descContentWidth = Math.max(0, descW - 16);
         List<String> descLines = this.wrapText(fullDescription, descContentWidth, this.detailsDescriptionExpanded ? 7 : 3);
         int descH = 24 + descLines.size() * 11;
         context.fill(right, y + 42, right + descW, y + 42 + descH, 1714438232);
         context.text(this.font, Component.literal("Description"), right + 8, y + 47, -7686913);
         int dy = y + 61;

         for (String line : descLines) {
            context.text(this.font, line, descX + 8, dy, -1, false);
            dy += 11;
         }

         List<String> allDescLines = this.wrapText(fullDescription, descContentWidth, Integer.MAX_VALUE);
         if (allDescLines.size() > descLines.size()) {
            String toggle = this.detailsDescriptionExpanded ? "Show less" : "Show more";
            int tw = this.font.width(toggle);
            this.descToggleX = descX + descW - tw - 10;
            this.descToggleY = y + 46;
            this.descToggleW = tw + 4;
            this.descToggleH = 10;
            this.descToggleVisible = true;
            context.text(this.font, toggle, this.descToggleX + 2, this.descToggleY + 1, -6371585, false);
         }

         int sectionY = y + 50 + descH;
         context.text(this.font, Component.literal("Recent Players"), descX, sectionY, -7686913);
         if (this.detailsData.recentPlayers.size() > 18) {
            String t = this.detailsPlayersExpanded ? "Collapse" : "Expand";
            int tw = this.font.width(t);
            this.playersToggleX = descX + descW - tw - 4;
            this.playersToggleY = sectionY;
            this.playersToggleW = tw + 4;
            this.playersToggleH = 10;
            this.playersToggleVisible = true;
            context.text(this.font, t, this.playersToggleX + 2, this.playersToggleY + 1, -6371585, false);
         }

         sectionY += 16;
         sectionY = this.drawChips(context, this.detailsData.recentPlayers, "-", descX, sectionY, descW, this.detailsPlayersExpanded ? Integer.MAX_VALUE : 18);
         sectionY += 8;
         context.text(this.font, Component.literal("Detected Plugins"), descX, sectionY, -7686913);
         if (this.detailsData.detectedPlugins.size() > 24) {
            String t = this.detailsPluginsExpanded ? "Collapse" : "Expand";
            int tw = this.font.width(t);
            this.pluginsToggleX = descX + descW - tw - 4;
            this.pluginsToggleY = sectionY;
            this.pluginsToggleW = tw + 4;
            this.pluginsToggleH = 10;
            this.pluginsToggleVisible = true;
            context.text(this.font, t, this.pluginsToggleX + 2, this.pluginsToggleY + 1, -6371585, false);
         }

         sectionY += 16;
         this.drawChips(context, this.detailsData.detectedPlugins, "-", descX, sectionY, descW, this.detailsPluginsExpanded ? Integer.MAX_VALUE : 24);
      }
   }

   private int drawInfoLine(GuiGraphicsExtractor context, String label, String value, int x, int y) {
      context.text(this.font, label + ":", x, y, -7686913, false);
      context.text(this.font, this.truncateToWidth(value == null ? "-" : value, 180), x + 62, y, -3811340, false);
      return y + 14;
   }

   private int drawChips(GuiGraphicsExtractor context, List<String> values, String fallback, int x, int y, int width, int maxChips) {
      List<String> chips = values != null && !values.isEmpty() ? values : List.of(fallback);
      int cursorX = x;
      int maxX = x + width;
      int rowY = y;
      int rendered = 0;

      for (String raw : chips) {
         if (rendered >= maxChips) {
            break;
         }

         String chipText = this.truncateToWidth(raw, 140);
         int chipW = this.font.width(chipText) + 10;
         if (cursorX + chipW > maxX) {
            cursorX = x;
            rowY += 16;
         }

         context.fill(cursorX, rowY - 1, cursorX + chipW, rowY + 10, 2000703600);
         context.text(this.font, chipText, cursorX + 5, rowY + 1, -1642753, false);
         cursorX += chipW + 6;
         rendered++;
      }

      if (chips.size() > rendered) {
         String more = "+" + (chips.size() - rendered) + " more";
         int chipW = this.font.width(more) + 10;
         if (cursorX + chipW > maxX) {
            cursorX = x;
            rowY += 16;
         }

         context.fill(cursorX, rowY - 1, cursorX + chipW, rowY + 10, 1716546191);
         context.text(this.font, more, cursorX + 5, rowY + 1, -3153153, false);
      }

      return rowY + 14;
   }

   private int getDetailsModalHeight() {
      int base = 370;
      if (this.detailsModalOpen && !this.detailsLoading && this.detailsData != null) {
         int modalW = 680;
         int right = 250;
         int descW = modalW - right - 14;
         int descContentWidth = Math.max(0, descW - 16);
         String fullDescription = this.detailsData.description == null ? "-" : this.detailsData.description;
         int descLines = this.wrapText(fullDescription, descContentWidth, this.detailsDescriptionExpanded ? 7 : 3).size();
         int descH = 24 + descLines * 11;
         int playersMax = this.detailsPlayersExpanded ? Integer.MAX_VALUE : 18;
         int pluginsMax = this.detailsPluginsExpanded ? Integer.MAX_VALUE : 24;
         int playersH = this.measureChipsHeight(this.detailsData.recentPlayers, "-", descW, playersMax);
         int pluginsH = this.measureChipsHeight(this.detailsData.detectedPlugins, "-", descW, pluginsMax);
         int contentH = 50 + descH + 16 + playersH + 8 + 16 + pluginsH + 24;
         int desired = Math.max(base, contentH);
         int maxAllowed = Math.max(base, this.height - 24);
         return Math.min(desired, maxAllowed);
      } else {
         return base;
      }
   }

   private int measureChipsHeight(List<String> values, String fallback, int width, int maxChips) {
      List<String> chips = values != null && !values.isEmpty() ? values : List.of(fallback);
      int maxX = width;
      int cursorX = 0;
      int rowY = 0;
      int rendered = 0;

      for (String raw : chips) {
         if (rendered >= maxChips) {
            break;
         }

         String chipText = this.truncateToWidth(raw, 140);
         int chipW = this.font.width(chipText) + 10;
         if (cursorX + chipW > maxX) {
            cursorX = 0;
            rowY += 16;
         }

         cursorX += chipW + 6;
         rendered++;
      }

      if (chips.size() > rendered) {
         String more = "+" + (chips.size() - rendered) + " more";
         int chipW = this.font.width(more) + 10;
         if (cursorX + chipW > maxX) {
            rowY += 16;
         }
      }

      return rowY + 14;
   }

   private boolean isInRect(double mx, double my, int x, int y, int w, int h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   private List<String> wrapText(String value, int maxWidth, int maxLines) {
      List<String> lines = new ArrayList<>();
      if (value != null && !value.isBlank() && maxWidth > 0) {
         String remaining = value.replace('\n', ' ').replace('\r', ' ').trim();

         while (!remaining.isBlank() && lines.size() < maxLines) {
            String line = this.font.plainSubstrByWidth(remaining, maxWidth);
            if (line.isEmpty()) {
               break;
            }

            lines.add(line);
            if (line.length() >= remaining.length()) {
               break;
            }

            remaining = remaining.substring(line.length()).trim();
         }

         if (lines.isEmpty()) {
            lines.add("-");
         }

         if (!remaining.isBlank() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, this.truncateToWidth(lines.get(last), Math.max(0, maxWidth - this.font.width("..."))) + "...");
         }

         return lines;
      } else {
         return List.of("-");
      }
   }

   public void removed() {
      this.saveUiState();
      this.saveSearchCacheFromScreen();
      if (!this.liveEntries.isEmpty() && !this.liveLoading) {
         this.saveLiveCache();
      }
      super.removed();
      if (this.minecraft != null) {
         for (Identifier id : this.faviconIds.values()) {
            this.minecraft.getTextureManager().release(id);
         }
      }

      for (DynamicTexture texture : this.faviconTextures.values()) {
         texture.close();
      }

      this.faviconIds.clear();
      this.faviconTextures.clear();
      this.faviconLoadFailed.clear();
   }

   private static String getString(JsonObject obj, String key, String fallback) {
      return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
   }

   private static int getInt(JsonObject obj, String key, int fallback) {
      if (obj.has(key) && !obj.get(key).isJsonNull()) {
         try {
            return obj.get(key).getAsInt();
         } catch (Exception var4) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   private static long getLong(JsonObject obj, String key, long fallback) {
      if (obj.has(key) && !obj.get(key).isJsonNull()) {
         try {
            return obj.get(key).getAsLong();
         } catch (Exception var5) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
      if (obj.has(key) && !obj.get(key).isJsonNull()) {
         try {
            return obj.get(key).getAsBoolean();
         } catch (Exception var4) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   private static Boolean getBooleanNullable(JsonObject obj, String key) {
      if (obj.has(key) && !obj.get(key).isJsonNull()) {
         try {
            return obj.get(key).getAsBoolean();
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static int parseIntOrDefault(String text, int fallback) {
      try {
         int v = Integer.parseInt(text.trim());
         return Math.max(1, v);
      } catch (Exception var3) {
         return fallback;
      }
   }

   private static Integer parseOptionalInt(String text) {
      String cleaned = text == null ? "" : text.trim();
      if (cleaned.isEmpty()) {
         return null;
      } else {
         try {
            return Integer.parseInt(cleaned);
         } catch (Exception var3) {
            return null;
         }
      }
   }

   private static String formatLastSeen(String isoTime) {
      if (isoTime != null && !isoTime.isBlank()) {
         try {
            return OffsetDateTime.parse(isoTime).toLocalDate().toString();
         } catch (Exception var2) {
            return isoTime;
         }
      } else {
         return "-";
      }
   }

   private static String formatTimeAgo(String isoTime) {
      if (isoTime != null && !isoTime.isBlank()) {
         try {
            Duration d = Duration.between(OffsetDateTime.parse(isoTime), OffsetDateTime.now());
            long mins = Math.max(0L, d.toMinutes());
            if (mins < 60L) {
               return mins + "m ago";
            } else {
               long hrs = mins / 60L;
               return hrs < 48L ? hrs + "h ago" : hrs / 24L + "d ago";
            }
         } catch (Exception var6) {
            return "-";
         }
      } else {
         return "-";
      }
   }

   private record ChipRegion(int x, int y, int w, int h, StylishTextFieldWidget field) {
   }

   private record DashColumns(
      int ipX,
      int ipW,
      int versionX,
      int versionW,
      int softwareX,
      int softwareW,
      int playersX,
      int countryX,
      int lastSeenX,
      int lastSeenW,
      int actionsX,
      boolean showSoftware,
      boolean showLastSeen
   ) {
   }

   private record FieldLabel(StylishTextFieldWidget field, String label) {
   }

   private record LiveEntry(
      String userLogin,
      String userName,
      String streamTitle,
      String streamStartedAt,
      String serverIp,
      int serverPort,
      String serverType,
      String countryCode,
      String platform,
      int confidence,
      int sightingsToday,
      boolean isLiveNow,
      String lastSeenAt,
      String firstSeenAt,
      Boolean enforcesSecureChat
   ) {
   }

   private record LiveLinkRegion(int x, int y, int w, int h, String url) {
   }

   private record ServerDetails(
      String ip,
      int port,
      String versionName,
      int versionProtocol,
      String description,
      String serverType,
      int playersOnline,
      int playersMax,
      String countryCode,
      String cityName,
      String asn,
      String lastSeenIso,
      String faviconDataUrl,
      String detectedSoftware,
      List<String> recentPlayers,
      List<String> detectedPlugins
   ) {
   }

   private record ServerEntry(
      String ip,
      int port,
      String version,
      String software,
      String description,
      String country,
      int playersOnline,
      int playersMax,
      String lastSeenIso,
      String faviconDataUrl
   ) {
   }

   private static enum Tab {
      DASHBOARD,
      LIVE;
   }

   private static enum TriState {
      ANY("Any"),
      TRUE("Yes"),
      FALSE("No");

      private final String display;

      private TriState(String display) {
         this.display = display;
      }

      private ServerScannerScreen.TriState next() {
         return switch (this) {
            case ANY -> TRUE;
            case TRUE -> FALSE;
            case FALSE -> ANY;
         };
      }
   }

   private static final class SearchResultCache {
      boolean valid;
      String searchUrl = "";
      List<ServerScannerScreen.ServerEntry> servers = List.of();
      String statusText = "";
      boolean prevPageActive;
      boolean nextPageActive;
   }

   private static final class LiveResultCache {
      boolean valid;
      List<ServerScannerScreen.LiveEntry> entries = List.of();
      String statusText = "";
   }

   private static final class UiState {
      String serverType = "";
      String version = "";
      String software = "";
      String country = "US";
      String address = "";
      String search = "";
      String modName = "";
      String pluginName = "";
      String playerName = "";
      String minPlayers = "";
      String maxPlayers = "";
      int currentPage = 1;
      ServerScannerScreen.TriState crackedFilter = ServerScannerScreen.TriState.ANY;
      ServerScannerScreen.TriState hasPlayersFilter = ServerScannerScreen.TriState.ANY;
      ServerScannerScreen.TriState moddedFilter = ServerScannerScreen.TriState.ANY;
      ServerScannerScreen.TriState isFullFilter = ServerScannerScreen.TriState.ANY;
      ServerScannerScreen.TriState hasFaviconFilter = ServerScannerScreen.TriState.ANY;
      ServerScannerScreen.Tab activeTab = ServerScannerScreen.Tab.DASHBOARD;
      int scrollOffset = 0;
      int liveScrollOffset = 0;
   }
}
