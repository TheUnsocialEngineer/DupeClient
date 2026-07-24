package com.dupeclient.client;

import com.dupeclient.DupeBuildConstants;
import com.dupeclient.client.compat.ModCompat;
import com.dupeclient.client.config.DupeClientConfigDir;
import com.dupeclient.client.multiplayer.SessionManager;
import com.dupeclient.client.config.VisualSettings;
import com.dupeclient.client.config.VisualSettingsConfigManager;
import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.core.KeybindManager;
import com.dupeclient.client.gui.ClientGuiManager;
import com.dupeclient.client.gui.DupeMainMenuScreen;
import com.dupeclient.client.gui.ClientGuiScreen;
import com.dupeclient.client.gui.IngameUiRouter;
import com.dupeclient.client.gui.StartupBlockedScreen;
import com.dupeclient.client.core.session.SessionBootstrap;
import com.dupeclient.client.core.session.SessionGate;
import com.dupeclient.client.core.session.SlashCommandGate;
import com.dupeclient.client.core.session.HubModuleRules;
import com.dupeclient.client.core.session.HubModuleSuppressor;
import com.dupeclient.client.core.notify.ClientNotificationHub;
import com.dupeclient.client.gui.MacroEditorScreen;
import com.dupeclient.client.gui.overlay.IngameOverlayHost;
import com.dupeclient.client.gui.overlay.IngameOverlayScreens;
import com.dupeclient.client.gui.render.UiNativeRenderer;
import com.dupeclient.client.module.acaudit.AcAuditManager;
import com.dupeclient.client.module.cape.DupeClientCapePresence;
import com.dupeclient.client.module.cape.DupeClientPresenceConfigManager;
import com.dupeclient.client.module.cape.DupeClientPresenceSettings;
import com.dupeclient.client.module.dupedb.DupedbManager;
import com.dupeclient.client.module.dupedb.DupedbMode;
import com.dupeclient.client.module.dupedb.P2wCommands;
import com.dupeclient.client.module.dupedb.P2wServerPolicy;
import com.dupeclient.client.docs.ScreenshotCaptureMode;
import com.dupeclient.client.docs.ScreenshotCaptureService;
import com.dupeclient.client.module.dupedb.search.ServerSearchAuthScreen;
import com.dupeclient.client.module.fuzzer.economy.EconomyFuzzerManager;
import com.dupeclient.client.module.hud.HudCommands;
import com.dupeclient.client.module.hud.HudManager;
import com.dupeclient.client.module.hud.HudSettings;
import com.dupeclient.client.gui.overlay.ServerProfileCard;
import com.dupeclient.client.module.macro.MacroBaritoneSupport;
import com.dupeclient.client.module.macro.MacroScheduler;
import com.dupeclient.client.module.macro.MacroCommands;
import com.dupeclient.client.module.macro.MacroEngine;
import com.dupeclient.client.module.macro.MacroQuickPlay;
import com.dupeclient.client.module.macro.MacroStorage;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.command.CommandPacketSender;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import com.dupeclient.client.module.packet.sniffer.MappingLabelResolver;
import com.dupeclient.client.module.packet.sniffer.PacketReplayScheduler;
import com.dupeclient.client.module.packet.sniffer.PacketSnifferManager;
import com.dupeclient.client.module.payall.PayAllManager;
import com.dupeclient.client.module.mcptools.McpToolsManager;
import com.dupeclient.client.module.security.SecurityManager;
import com.dupeclient.client.module.social.DupeClientSocialFriendsManager;
import com.dupeclient.client.module.serverpassword.ServerPasswordCommands;
import com.dupeclient.client.module.serverpassword.ServerPasswordManager;
import com.dupeclient.client.module.utility.ChatGamesManager;
import com.dupeclient.client.module.utility.DupeTrollCommand;
import com.dupeclient.client.module.utility.LookNbtCommand;
import com.dupeclient.client.module.utility.nbtedit.NbtEditCommand;
import com.dupeclient.client.module.utility.crashes.CrashesManager;
import com.dupeclient.client.module.waypoint.DupeClientWaypointManager;
import com.dupeclient.client.module.waypoint.WaypointWorldRenderer;
import net.fabricmc.api.ClientModInitializer;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;

public final class DupeClient implements ClientModInitializer {
    public static final String MOD_ID = "dupeclient";
    public static final String CLIENT_NAME = "DupeClient";
    public static final String BUILD_TAG = DupeBuildConstants.BUILD_TAG;
    public static final String MOD_VERSION = DupeBuildConstants.MOD_VERSION;
    public static final Logger LOGGER = LoggerFactory.getLogger(CLIENT_NAME);

    private static final ClientGuiManager GUI_MANAGER = new ClientGuiManager();
    private static VisualSettings VISUAL_SETTINGS;
    private static boolean rightCtrlWasDown;
    private static boolean macroEditorKeyEdgeLatch;
    private static boolean hudEditorKeyEdgeLatch;
    private static boolean socialScreenKeyEdgeLatch;
    private static boolean waypointsScreenKeyEdgeLatch;
    private static long visualSettingsRevision;

    @Override
    public void onInitializeClient() {
        DupeClientConfigDir.migrateFromLegacyLocations();
        ModCompat.resolve();
        GUI_MANAGER.initializeDefaults();
        VISUAL_SETTINGS = VisualSettingsConfigManager.load();
        MacroStorage.prepare();
        HudManager.INSTANCE.initialize();
        PacketUtilsManager.INSTANCE.initialize();
        PacketSnifferManager.INSTANCE.initialize();
        MappingLabelResolver.startBackgroundLoad();
        SecurityManager.INSTANCE.initialize();
        AcAuditManager.INSTANCE.initialize();
        DupedbManager.INSTANCE.initialize();
        CrashesManager.INSTANCE.initialize();
        ChatGamesManager.INSTANCE.initialize();
        EconomyFuzzerManager.INSTANCE.initialize();
        PayAllManager.INSTANCE.initialize();
        McpToolsManager.INSTANCE.initialize();
        DupeClientPresenceConfigManager.initialize();
        DupeClientSocialFriendsManager.initialize();
        DupeClientWaypointManager.INSTANCE.initialize();
        ServerPasswordManager.INSTANCE.initialize();
        WaypointWorldRenderer.register();
        SessionBootstrap.INSTANCE.initialize();
        if (ScreenshotCaptureMode.isActive()) {
            ScreenshotCaptureService.INSTANCE.initialize();
        }
        registerPlayConnectionNetworking();
        KeybindManager.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            MacroCommands.register(dispatcher);
            HudCommands.register(dispatcher);
            P2wCommands.register(dispatcher);
            DupeTrollCommand.register(dispatcher);
            LookNbtCommand.register(dispatcher);
            NbtEditCommand.register(dispatcher);
            ServerPasswordCommands.register(dispatcher);
            dispatcher.register(ClientCommands.literal("serversearch").executes(ctx -> {
                if (SlashCommandGate.blockExploit(ctx.getSource())) {
                    return 0;
                }
                Minecraft c = Minecraft.getInstance();
                if (c != null) {
                    c.setScreen(new ServerSearchAuthScreen(c.screen));
                }
                return 1;
            }));
            dispatcher.register(ClientCommands.literal("server")
                    .then(ClientCommands.literal("plugins").executes(ctx -> {
                        if (SlashCommandGate.blockExploit(ctx.getSource())) {
                            return 0;
                        }
                        DupedbManager.INSTANCE.startPluginListScan();
                        return 1;
                    })));
            registerDupedbCommands(dispatcher);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SessionManager.initialize();
            CommandPacketSender.INSTANCE.beginTick();
            boolean guiText = InputFocusGuards.hasAnyTextInputFocus(client);
            PacketUtilsManager.INSTANCE.setTextInputFocused(guiText);
            CrashesManager.INSTANCE.setTextInputFocused(guiText);
            ChatGamesManager.INSTANCE.setTextInputFocused(guiText);
            EconomyFuzzerManager.INSTANCE.setTextInputFocused(guiText);
            PacketUtilsManager.INSTANCE.tick(client);
            PacketSnifferManager.INSTANCE.tick(client);
            PacketReplayScheduler.INSTANCE.tick(client);
            SecurityManager.INSTANCE.tick(client);
            AcAuditManager.INSTANCE.tick(client);
            DupedbManager.INSTANCE.tick(client);
            CrashesManager.INSTANCE.tick(client);
            ChatGamesManager.INSTANCE.tick(client);
            EconomyFuzzerManager.INSTANCE.tick(client);
            PayAllManager.INSTANCE.tick(client);
            McpToolsManager.INSTANCE.tick(client);
            ServerPasswordManager.INSTANCE.tick(client);
            SessionBootstrap.INSTANCE.tick();
            SessionGate.tick(client);
            HubModuleSuppressor.tick(client);
            ClientNotificationHub.tick();
            IngameOverlayScreens.tick(client);
            P2wServerPolicy.INSTANCE.enforce(client);
            DupeClientCapePresence.tick(client);
            DupeClientWaypointManager.INSTANCE.tick(client);
            MacroEngine.INSTANCE.tick(client);
            MacroQuickPlay.tick(client);
            MacroScheduler.getInstance().tick(client);
            HudManager.INSTANCE.tick(client);
            if (ScreenshotCaptureMode.isActive()) {
                ScreenshotCaptureService.INSTANCE.tick(client);
            }

            if (!InputFocusGuards.shouldBlockGlobalHotkeys(client)) {
                handlePanelHotkeys(client);
                if (HubModuleRules.exploitFeaturesAllowed() && macroEditorKeyPressedThisTick(client)) {
                    MacroEditorScreen.open(client, null);
                } else if (KeybindManager.OPEN_GUI_KEY.consumeClick() || consumeRightCtrlPress(client)) {
                    openClientGui(client);
                }
            } else {
                hudEditorKeyEdgeLatch = false;
                socialScreenKeyEdgeLatch = false;
                waypointsScreenKeyEdgeLatch = false;
                macroEditorKeyEdgeLatch = false;
                rightCtrlWasDown = client != null && client.getWindow() != null
                        && InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "main"), (graphics, deltaTracker) -> {
            HudManager.INSTANCE.render(graphics);
            IngameOverlayHost.renderOnHud(graphics, deltaTracker);
            WaypointWorldRenderer.renderHud(graphics, deltaTracker);
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> UiNativeRenderer.ensureReady());

        LOGGER.info("{} initialized ({})", CLIENT_NAME, BUILD_TAG);
    }

    private static void registerDupedbCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        var dupedb = ClientCommands.literal("dupedb")
                .executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    DupedbManager.INSTANCE.startScan(false);
                    return 1;
                })
                .then(ClientCommands.literal("scan").executes(ctx -> {
                    if (SlashCommandGate.blockExploit(ctx.getSource())) {
                        return 0;
                    }
                    DupedbManager.INSTANCE.startScan(false);
                    return 1;
                }))
                .then(ClientCommands.literal("login").executes(ctx -> {
                    DupedbManager.INSTANCE.startLoginFlow();
                    return 1;
                }))
                .then(ClientCommands.literal("token")
                        .then(ClientCommands.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    DupedbManager.INSTANCE.setPersonalAccessToken(
                                            StringArgumentType.getString(ctx, "value"));
                                    return 1;
                                })))
                .then(ClientCommands.literal("appid")
                        .then(ClientCommands.argument("slug", StringArgumentType.word())
                                .executes(ctx -> {
                                    DupedbManager.INSTANCE.setOAuthAppId(
                                            StringArgumentType.getString(ctx, "slug"));
                                    return 1;
                                })))
                .then(ClientCommands.literal("developer").executes(ctx -> {
                    DupedbManager.INSTANCE.openDeveloperSettingsPage();
                    ctx.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("[DupeDB] Opening developer settings"));
                    return 1;
                }))
                .then(ClientCommands.literal("plugins").executes(ctx -> {
                    DupedbManager.INSTANCE.startScan(false);
                    return 1;
                }))
                .then(ClientCommands.literal("search").executes(ctx -> {
                    DupedbManager.INSTANCE.startScan(false);
                    return 1;
                }))
                .then(ClientCommands.literal("status").executes(ctx -> {
                    if (DupedbManager.INSTANCE.isAuthenticated()) {
                        ctx.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("[DupeDB] Authenticated"));
                    } else {
                        ctx.getSource().sendFeedback(net.minecraft.network.chat.Component.literal(
                                "[DupeDB] Not authenticated. Use /dupedb login or /dupedb token dupe_pat_..."));
                    }
                    return 1;
                }))
                .then(ClientCommands.literal("revoke").executes(ctx -> {
                    DupedbManager.INSTANCE.clearToken();
                    DupedbManager.INSTANCE.openSettingsPage();
                    ctx.getSource().sendFeedback(net.minecraft.network.chat.Component.literal("[DupeDB] Token cleared"));
                    return 1;
                }))
                .then(ClientCommands.literal("mode")
                        .then(ClientCommands.literal("auto").executes(ctx -> {
                            DupedbManager.INSTANCE.getSettings().mode = DupedbMode.AUTO;
                            DupedbManager.INSTANCE.save();
                            return 1;
                        }))
                        .then(ClientCommands.literal("command").executes(ctx -> {
                            DupedbManager.INSTANCE.getSettings().mode = DupedbMode.COMMAND;
                            DupedbManager.INSTANCE.save();
                            return 1;
                        })));
        dispatcher.register(dupedb);
    }

    private static void registerPlayConnectionNetworking() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            DupeClientCapePresence.onPlaySessionJoined();
            MacroBaritoneSupport.onWorldJoin(client);
            ServerPasswordManager.INSTANCE.onSessionJoined(client);
            DupedbManager.INSTANCE.onPlaySessionJoin(client);
            P2wServerPolicy.INSTANCE.onPlaySessionJoin();
            SecurityManager.INSTANCE.onPlaySessionJoin(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            IngameOverlayHost.onPlaySessionLeave();
            IngameUiRouter.closeClientScreensOnLeave(client);
            DupeClientCapePresence.onDisconnected();
            ServerPasswordManager.INSTANCE.onSessionLeave();
            PacketUtilsManager.INSTANCE.onSessionLeave();
            PacketSnifferManager.INSTANCE.onSessionLeave();
            DupedbManager.INSTANCE.onPlaySessionLeave();
            P2wServerPolicy.INSTANCE.onDisconnected();
            PayAllManager.INSTANCE.onSessionLeave();
            CrashesManager.INSTANCE.onDisconnected();
            AcAuditManager.INSTANCE.onSessionLeave();
            MacroEngine.INSTANCE.stop(client);
            MacroScheduler.getInstance().clear();
            ServerProfileCard.dismiss();
        });
    }

    private static void handlePanelHotkeys(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            hudEditorKeyEdgeLatch = false;
            socialScreenKeyEdgeLatch = false;
            waypointsScreenKeyEdgeLatch = false;
            return;
        }
        HudSettings hudSettings = HudManager.INSTANCE.settings();
        if (consumeGlfwKeyPress(client, hudSettings.editorOpenKey, true, false)) {
            IngameUiRouter.openHudEditor(client.screen);
        }
        DupeClientPresenceSettings presenceSettings = DupeClientPresenceConfigManager.get();
        if (consumeGlfwKeyPress(client, presenceSettings.openSocialKey, false, true)
                && HubModuleRules.socialFeaturesAllowed()) {
            IngameUiRouter.openSocial(client.screen);
        }
        if (consumeGlfwKeyPress(client, presenceSettings.openWaypointsKey, false, false)
                && HubModuleRules.socialFeaturesAllowed()) {
            IngameUiRouter.openWaypoints(client.screen);
        }
    }

    private static boolean consumeGlfwKeyPress(Minecraft client, int keyCode, boolean hudEditor, boolean socialKey) {
        if (keyCode < 0 || keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            if (hudEditor) {
                hudEditorKeyEdgeLatch = false;
            } else if (socialKey) {
                socialScreenKeyEdgeLatch = false;
            } else {
                waypointsScreenKeyEdgeLatch = false;
            }
            return false;
        }
        boolean down = InputConstants.isKeyDown(client.getWindow(), keyCode);
        boolean last = hudEditor ? hudEditorKeyEdgeLatch : (socialKey ? socialScreenKeyEdgeLatch : waypointsScreenKeyEdgeLatch);
        boolean pressed = down && !last;
        if (hudEditor) {
            hudEditorKeyEdgeLatch = down;
        } else if (socialKey) {
            socialScreenKeyEdgeLatch = down;
        } else {
            waypointsScreenKeyEdgeLatch = down;
        }
        return pressed;
    }

    private static boolean macroEditorKeyPressedThisTick(Minecraft client) {
        if (KeybindManager.OPEN_MACRO_EDITOR_KEY.consumeClick()) {
            return true;
        }
        if (client == null || client.getWindow() == null) {
            macroEditorKeyEdgeLatch = false;
            return false;
        }
        int code = KeybindManager.OPEN_MACRO_EDITOR_KEY.getDefaultKey().getValue();
        boolean down = InputConstants.isKeyDown(client.getWindow(), code);
        boolean pressed = down && !macroEditorKeyEdgeLatch;
        macroEditorKeyEdgeLatch = down;
        return pressed;
    }

    private static boolean consumeRightCtrlPress(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            rightCtrlWasDown = false;
            return false;
        }
        boolean down = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean pressed = down && !rightCtrlWasDown;
        rightCtrlWasDown = down;
        return pressed;
    }

    public static ClientGuiManager getGuiManager() {
        return GUI_MANAGER;
    }

    public static Screen createMainMenu() {
        if (SessionGate.isGameBlocked()) {
            return new StartupBlockedScreen();
        }
        return new DupeMainMenuScreen();
    }

    public static VisualSettings getVisualSettings() {
        return VISUAL_SETTINGS;
    }

    public static void saveVisualSettings() {
        VisualSettingsConfigManager.save(VISUAL_SETTINGS);
        visualSettingsRevision++;
    }

    public static long getVisualSettingsRevision() {
        return visualSettingsRevision;
    }

    public static void openClientGui(Minecraft client) {
        if (client == null) {
            return;
        }
        if (client.screen instanceof ClientGuiScreen hub) {
            hub.onClose();
            return;
        }
        IngameOverlayHost.hideAllOverlays();
        PacketFabricatorOverlay.INSTANCE.setVisible(false);
        IngameUiRouter.openClientGui(client);
    }

    public static void openModsGui(Minecraft client, Screen parent) {
        try {
            Class<?> clazz = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            client.setScreen((Screen) clazz.getConstructor(Screen.class).newInstance(parent));
        } catch (Exception e) {
            LOGGER.warn("ModMenu not available, keybind ignored.");
        }
    }
}
