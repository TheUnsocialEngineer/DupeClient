package com.ui_utils;

import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import com.ui_utils.features.PluginScanner;
import com.ui_utils.gui.CustomButtonWidget;
import com.ui_utils.gui.UITheme;
import com.ui_utils.mixin.accessor.ScreenAccessor;
import java.lang.reflect.Method;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainClient implements ClientModInitializer {
    private static boolean initialized;
    private static CustomButtonWidget queueDisplayButton;

    public static Logger LOGGER = LoggerFactory.getLogger("ui-utils");
    public static Minecraft mc = Minecraft.getInstance();

    public static KeyMapping restoreScreenKey;
    public static final KeyMapping.Category UI_UTILS_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ui_utils", "general"));

    @Override
    public void onInitializeClient() {
        if (initialized) {
            LOGGER.info("UI Utils already initialized, skipping duplicate entrypoint call.");
            return;
        }
        initialized = true;

        UpdateUtils.checkForUpdates();

        restoreScreenKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("Restore Screen", GLFW.GLFW_KEY_V, UI_UTILS_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PluginScanner.onTick();
            if (InputFocusGuards.shouldBlockGlobalHotkeys(client)) {
                return;
            }
            while (restoreScreenKey.consumeClick()) {
                if (SharedVariables.storedScreen == null
                        || SharedVariables.storedScreenHandler == null
                        || client.player == null) {
                    continue;
                }
                client.setScreen(SharedVariables.storedScreen);
                client.player.containerMenu = SharedVariables.storedScreenHandler;
            }
        });
    }

    public static void createText(Minecraft mc, GuiGraphicsExtractor context, Font textRenderer) {
        UITheme.drawPanel(context, 153, 4, 56, 24);
        if (mc.player != null && mc.player.containerMenu != null) {
            context.text(
                    textRenderer,
                    "Sync: " + mc.player.containerMenu.containerId,
                    157,
                    7,
                    UITheme.TEXT);
            context.text(
                    textRenderer,
                    "Rev: " + mc.player.containerMenu.getStateId(),
                    157,
                    17,
                    UITheme.TEXT);
        }
    }

    public static void createWidgets(Minecraft mc, Screen screen) {
        int x = 4;
        int y = 4;
        int w = 145;
        int h = 18;
        int spacing = 2;

        ScreenAccessor access = (ScreenAccessor) screen;

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(x, y, w, Component.nullToEmpty("Close without packet"), button -> {
            PacketUtilsManager.INSTANCE.moduleFeedback("UI Utils: close without packet (overlay).");
            mc.setScreen(null);
        }));

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(x, y + h + spacing, w, Component.nullToEmpty("Desync"), button -> {
            if (mc.getConnection() != null && mc.player != null) {
                mc.getConnection()
                        .send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                PacketUtilsManager.INSTANCE.moduleFeedback("UI Utils: sent CloseHandledScreen (desync).");
            }
        }));

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(
                x,
                y + (h + spacing) * 2,
                w,
                Component.nullToEmpty("Send packets: " + SharedVariables.sendUIPackets),
                button -> {
                    SharedVariables.sendUIPackets = !SharedVariables.sendUIPackets;
                    button.setMessage(Component.nullToEmpty("Send packets: " + SharedVariables.sendUIPackets));
                    PacketUtilsManager.INSTANCE.save();
                    PacketUtilsManager.INSTANCE.moduleFeedback(
                            "UI Utils send live packets " + (SharedVariables.sendUIPackets ? "ON" : "OFF"));
                }));

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(
                x,
                y + (h + spacing) * 3,
                w,
                Component.nullToEmpty("Delay packets: " + SharedVariables.delayUIPackets),
                button -> {
                    PacketUtilsManager.INSTANCE.toggleUiUtilsDelay();
                    PacketUtilsManager.INSTANCE.save();
                    button.setMessage(Component.nullToEmpty("Delay packets: " + SharedVariables.delayUIPackets));
                }));

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(x, y + (h + spacing) * 4, w, Component.nullToEmpty("Leave & Send Packets"), button -> {
            PacketUtilsManager.INSTANCE.leaveAndSendUiUtilsPackets(mc);
        }));

        CustomButtonWidget fabricatorButton = CustomButtonWidget.create(
                x,
                y + (h + spacing) * 5,
                w,
                Component.nullToEmpty(clickslotFabricatorButtonLabel()),
                button -> {
                    PacketFabricatorOverlay.INSTANCE.toggleClickslotFabricator();
                    button.setMessage(Component.nullToEmpty(clickslotFabricatorButtonLabel()));
                    PacketUtilsManager.INSTANCE.moduleFeedback(
                            "Clickslot fabricator "
                                    + (PacketFabricatorOverlay.INSTANCE.isModuleEnabled() ? "ON" : "OFF"));
                });
        access.uiUtils$addRenderableWidget(fabricatorButton);

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(
                x,
                y + (h + spacing) * 6,
                w,
                Component.nullToEmpty(slotIdsButtonLabel()),
                button -> {
                    PacketUtilsSettings settings = PacketUtilsManager.INSTANCE.getSettings();
                    settings.slotIdsOverlayEnabled = !settings.slotIdsOverlayEnabled;
                    PacketUtilsManager.INSTANCE.save();
                    button.setMessage(Component.nullToEmpty(slotIdsButtonLabel()));
                    PacketUtilsManager.INSTANCE.moduleFeedback(
                            "Slot ID overlay " + (settings.slotIdsOverlayEnabled ? "ON" : "OFF"));
                }));

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(x, y + (h + spacing) * 7, w, Component.nullToEmpty("Copy GUI Title JSON"), button -> {
            if (mc.screen != null) {
                Component title = mc.screen.getTitle();
                String json = getTextAsJson(title, mc);
                mc.keyboardHandler.setClipboard(json);
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.literal("§7[§c*§7] §7Copied GUI title JSON to clipboard"));
                }
            }
        }));

        int halfW = (w - spacing) / 2;

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(x, y + (h + spacing) * 8, halfW, Component.nullToEmpty("Save GUI"), button -> {
            if (mc.player != null) {
                SharedVariables.storedScreen = mc.screen;
                SharedVariables.storedScreenHandler = mc.player.containerMenu;
                SharedVariables.savedScreens.put("default", mc.screen);
                SharedVariables.savedScreenHandlers.put("default", mc.player.containerMenu);
                mc.player.sendSystemMessage(Component.literal("§7[§c*§7] §7GUI saved"));
            }
        }));

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(
                x + halfW + spacing, y + (h + spacing) * 8, halfW, Component.nullToEmpty("Load GUI"), button -> {
                    if (SharedVariables.storedScreen != null
                            && SharedVariables.storedScreenHandler != null
                            && mc.player != null) {
                        mc.setScreen(SharedVariables.storedScreen);
                        mc.player.containerMenu = SharedVariables.storedScreenHandler;
                    }
                }));

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(x, y + (h + spacing) * 9, halfW, Component.nullToEmpty("Clear Queue"), button -> {
            int count = SharedVariables.delayedUIPackets.size();
            SharedVariables.delayedUIPackets.clear();
            if (mc.player != null) {
                mc.player.sendSystemMessage(
                        Component.literal("§7[§c*§7] §7Cleared §c" + count + " §7packets"));
            }
        }));

        queueDisplayButton = CustomButtonWidget.create(
                x + halfW + spacing,
                y + (h + spacing) * 9,
                halfW,
                Component.nullToEmpty(queueLabel()),
                button -> {});
        access.uiUtils$addRenderableWidget(queueDisplayButton);

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(x, y + (h + spacing) * 10, halfW, Component.nullToEmpty("Resync Inv"), button -> {
            if (mc.getConnection() != null && mc.player != null) {
                mc.player.containerMenu.sendAllDataToRemote();
                mc.player.sendSystemMessage(Component.literal("§7[§c*§7] §7Inventory resynced"));
            }
        }));

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(
                x + halfW + spacing, y + (h + spacing) * 10, halfW, Component.nullToEmpty("Disconnect"), button -> {
                    if (mc.getConnection() != null) {
                        mc.getConnection().getConnection().disconnect(Component.literal("Disconnected"));
                    }
                }));

        CustomButtonWidget spamButton = CustomButtonWidget.create(
                x + 32,
                y + (h + spacing) * 11,
                w - 64,
                Component.nullToEmpty("Spam (x" + SharedVariables.spamCount + ")"),
                button -> {
                    if (mc.getConnection() != null && !SharedVariables.delayedUIPackets.isEmpty()) {
                        int sent = 0;
                        for (int i = 0; i < SharedVariables.spamCount; i++) {
                            for (Packet<?> packet : SharedVariables.delayedUIPackets) {
                                mc.getConnection().send(packet);
                                sent++;
                            }
                        }
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(
                                    Component.literal("§7[§c*§7] §7Spammed §c" + sent + " §7packets"));
                        }
                    } else if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("§7[§c*§7] §cNo packets in queue"));
                    }
                });

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(x, y + (h + spacing) * 11, 30, Component.nullToEmpty("-"), button -> {
            if (SharedVariables.spamCount > 1) {
                SharedVariables.spamCount--;
                spamButton.setMessage(Component.nullToEmpty("Spam (x" + SharedVariables.spamCount + ")"));
            }
        }));

        access.uiUtils$addRenderableWidget(spamButton);

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(x + w - 30, y + (h + spacing) * 11, 30, Component.nullToEmpty("+"), button -> {
            if (SharedVariables.spamCount < 100) {
                SharedVariables.spamCount++;
                spamButton.setMessage(Component.nullToEmpty("Spam (x" + SharedVariables.spamCount + ")"));
            }
        }));

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(x, y + (h + spacing) * 12, halfW, Component.nullToEmpty("Send One"), button -> {
            if (mc.getConnection() != null && !SharedVariables.delayedUIPackets.isEmpty()) {
                Packet<?> packet = SharedVariables.delayedUIPackets.remove(0);
                mc.getConnection().send(packet);
                if (mc.player != null) {
                    mc.player.sendSystemMessage(
                            Component.literal("§7[§c*§7] §7Sent 1 packet §7(§c"
                                    + SharedVariables.delayedUIPackets.size()
                                    + "§7 left)"));
                }
            } else if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§7[§c*§7] §cNo packets in queue"));
            }
        }));

        access.uiUtils$addRenderableWidget(CustomButtonWidget.create(
                x + halfW + spacing, y + (h + spacing) * 12, halfW, Component.nullToEmpty("Pop Last"), button -> {
                    if (!SharedVariables.delayedUIPackets.isEmpty()) {
                        SharedVariables.delayedUIPackets.remove(SharedVariables.delayedUIPackets.size() - 1);
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(
                                    Component.literal("§7[§c*§7] §7Removed last packet §7(§c"
                                            + SharedVariables.delayedUIPackets.size()
                                            + "§7 left)"));
                        }
                    } else if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("§7[§c*§7] §cNo packets in queue"));
                    }
                }));
    }

    private static String queueLabel() {
        return "Queue: " + SharedVariables.delayedUIPackets.size();
    }

    public static void refreshQueueDisplay() {
        if (queueDisplayButton != null) {
            queueDisplayButton.setMessage(Component.nullToEmpty(queueLabel()));
        }
    }

    public static void clearQueueDisplayReference() {
        queueDisplayButton = null;
    }

    private static String clickslotFabricatorButtonLabel() {
        if (!PacketFabricatorOverlay.INSTANCE.isModuleEnabled()) {
            return "Clickslot fabricator";
        }
        return "Clickslot: ON";
    }

    private static String slotIdsButtonLabel() {
        PacketUtilsSettings settings = PacketUtilsManager.INSTANCE.getSettings();
        return settings.slotIdsOverlayEnabled ? "Slot IDs: ON" : "Slot IDs: OFF";
    }

    private static String getTextAsJson(Component text, Minecraft mc) {
        try {
            Class<?> serializationClass = Class.forName("net.minecraft.text.Text$Serialization");
            Method toJsonMethod =
                    serializationClass.getMethod("toJsonString", Component.class, HolderLookup.Provider.class);
            HolderLookup.Provider registries =
                    mc.level != null ? mc.level.registryAccess() : mc.getConnection().registryAccess();
            return (String) toJsonMethod.invoke(null, text, registries);
        } catch (ClassNotFoundException e) {
            try {
                Class<?> serializerClass = Class.forName("net.minecraft.text.Text$Serializer");
                Method toJsonMethod =
                        serializerClass.getMethod("toJson", Component.class, HolderLookup.Provider.class);
                HolderLookup.Provider registries =
                        mc.level != null ? mc.level.registryAccess() : mc.getConnection().registryAccess();
                Object result = toJsonMethod.invoke(null, text, registries);
                return result.toString();
            } catch (Exception ex) {
                return text.getString();
            }
        } catch (Exception e) {
            return text.getString();
        }
    }

    public static String getModVersion(String modId) {
        ModMetadata modMetadata = FabricLoader.getInstance().getModContainer(modId).isPresent()
                ? FabricLoader.getInstance().getModContainer(modId).get().getMetadata()
                : null;
        return modMetadata != null ? modMetadata.getVersion().getFriendlyString() : "null";
    }
}
