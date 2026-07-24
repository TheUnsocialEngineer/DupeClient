package com.ui_utils;

import com.dupeclient.client.core.InputFocusGuards;
import com.dupeclient.client.module.packet.PacketUtilsManager;
import com.dupeclient.client.module.packet.PacketUtilsSettings;
import com.dupeclient.client.module.packet.fabricator.PacketFabricatorOverlay;
import com.ui_utils.features.PluginScanner;
import com.ui_utils.gui.CustomButtonWidget;
import com.ui_utils.gui.UITheme;
import java.lang.reflect.Method;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainClient implements ClientModInitializer {
    private static boolean initialized;

    public static Logger LOGGER = LoggerFactory.getLogger("ui-utils");
    public static MinecraftClient mc = MinecraftClient.getInstance();

    public static KeyBinding restoreScreenKey;
    public static final KeyBinding.Category UI_UTILS_CATEGORY =
            KeyBinding.Category.create(Identifier.of("ui_utils", "general"));

    @Override
    public void onInitializeClient() {
        if (initialized) {
            LOGGER.info("UI Utils already initialized, skipping duplicate entrypoint call.");
            return;
        }
        initialized = true;

        UpdateUtils.checkForUpdates();

        restoreScreenKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("Restore Screen", GLFW.GLFW_KEY_V, UI_UTILS_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PluginScanner.onTick();
            if (InputFocusGuards.shouldBlockGlobalHotkeys(client)) {
                return;
            }
            while (restoreScreenKey.wasPressed()) {
                if (SharedVariables.storedScreen == null
                        || SharedVariables.storedScreenHandler == null
                        || client.player == null) {
                    continue;
                }
                client.setScreen(SharedVariables.storedScreen);
                client.player.currentScreenHandler = SharedVariables.storedScreenHandler;
            }
        });
    }

    public static void createText(MinecraftClient mc, DrawContext context, TextRenderer textRenderer) {
        UITheme.drawPanel(context, 153, 4, 56, 24);
        if (mc.player != null && mc.player.currentScreenHandler != null) {
            context.drawText(
                    textRenderer,
                    "Sync: " + mc.player.currentScreenHandler.syncId,
                    157,
                    7,
                    UITheme.TEXT,
                    false);
            context.drawText(
                    textRenderer,
                    "Rev: " + mc.player.currentScreenHandler.getRevision(),
                    157,
                    17,
                    UITheme.TEXT,
                    false);
        }
    }

    public static void createWidgets(MinecraftClient mc, Screen screen) {
        int x = 4;
        int y = 4;
        int w = 145;
        int h = 18;
        int spacing = 2;

        screen.addDrawableChild(CustomButtonWidget.create(x, y, w, Text.of("Close without packet"), button -> {
            PacketUtilsManager.INSTANCE.moduleFeedback("UI Utils: close without packet (overlay).");
            mc.setScreen(null);
        }));

        screen.addDrawableChild(CustomButtonWidget.create(x, y + h + spacing, w, Text.of("Desync"), button -> {
            if (mc.getNetworkHandler() != null && mc.player != null) {
                mc.getNetworkHandler()
                        .sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                PacketUtilsManager.INSTANCE.moduleFeedback("UI Utils: sent CloseHandledScreen (desync).");
            }
        }));

        screen.addDrawableChild(CustomButtonWidget.create(
                x,
                y + (h + spacing) * 2,
                w,
                Text.of("Send packets: " + SharedVariables.sendUIPackets),
                button -> {
                    SharedVariables.sendUIPackets = !SharedVariables.sendUIPackets;
                    button.setMessage(Text.of("Send packets: " + SharedVariables.sendUIPackets));
                    PacketUtilsManager.INSTANCE.save();
                    PacketUtilsManager.INSTANCE.moduleFeedback(
                            "UI Utils send live packets " + (SharedVariables.sendUIPackets ? "ON" : "OFF"));
                }));

        screen.addDrawableChild(CustomButtonWidget.create(
                x,
                y + (h + spacing) * 3,
                w,
                Text.of("Delay packets: " + SharedVariables.delayUIPackets),
                button -> {
                    PacketUtilsManager.INSTANCE.toggleUiUtilsDelay();
                    PacketUtilsManager.INSTANCE.save();
                    button.setMessage(Text.of("Delay packets: " + SharedVariables.delayUIPackets));
                }));

        screen.addDrawableChild(CustomButtonWidget.create(x, y + (h + spacing) * 4, w, Text.of("Leave & Send Packets"), button -> {
            int queued = SharedVariables.delayedUIPackets.size();
            SharedVariables.delayUIPackets = false;
            if (mc.getNetworkHandler() != null) {
                for (Packet<?> packet : SharedVariables.delayedUIPackets) {
                    mc.getNetworkHandler().sendPacket(packet);
                }
            }
            SharedVariables.delayedUIPackets.clear();
            PacketUtilsManager.INSTANCE.moduleFeedback("UI Utils: leave and sent " + queued + " queued packet(s).");
            mc.setScreen(null);
        }));

        CustomButtonWidget fabricatorButton = CustomButtonWidget.create(
                x,
                y + (h + spacing) * 5,
                w,
                Text.of(clickslotFabricatorButtonLabel()),
                button -> {
                    PacketFabricatorOverlay.INSTANCE.toggleClickslotFabricator();
                    button.setMessage(Text.of(clickslotFabricatorButtonLabel()));
                    PacketUtilsManager.INSTANCE.moduleFeedback(
                            "Clickslot fabricator "
                                    + (PacketFabricatorOverlay.INSTANCE.isModuleEnabled() ? "ON" : "OFF"));
                });
        screen.addDrawableChild(fabricatorButton);

        screen.addDrawableChild(CustomButtonWidget.create(
                x,
                y + (h + spacing) * 6,
                w,
                Text.of(slotIdsButtonLabel()),
                button -> {
                    PacketUtilsSettings settings = PacketUtilsManager.INSTANCE.getSettings();
                    settings.slotIdsOverlayEnabled = !settings.slotIdsOverlayEnabled;
                    PacketUtilsManager.INSTANCE.save();
                    button.setMessage(Text.of(slotIdsButtonLabel()));
                    PacketUtilsManager.INSTANCE.moduleFeedback(
                            "Slot ID overlay " + (settings.slotIdsOverlayEnabled ? "ON" : "OFF"));
                }));

        screen.addDrawableChild(CustomButtonWidget.create(x, y + (h + spacing) * 7, w, Text.of("Copy GUI Title JSON"), button -> {
            if (mc.currentScreen != null) {
                Text title = mc.currentScreen.getTitle();
                String json = getTextAsJson(title, mc);
                mc.keyboard.setClipboard(json);
                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("§7[§c*§7] §7Copied GUI title JSON to clipboard"), false);
                }
            }
        }));

        int halfW = (w - spacing) / 2;

        screen.addDrawableChild(CustomButtonWidget.create(x, y + (h + spacing) * 8, halfW, Text.of("Save GUI"), button -> {
            if (mc.player != null) {
                SharedVariables.storedScreen = mc.currentScreen;
                SharedVariables.storedScreenHandler = mc.player.currentScreenHandler;
                SharedVariables.savedScreens.put("default", mc.currentScreen);
                SharedVariables.savedScreenHandlers.put("default", mc.player.currentScreenHandler);
                mc.player.sendMessage(Text.literal("§7[§c*§7] §7GUI saved"), false);
            }
        }));

        screen.addDrawableChild(CustomButtonWidget.create(
                x + halfW + spacing, y + (h + spacing) * 8, halfW, Text.of("Load GUI"), button -> {
                    if (SharedVariables.storedScreen != null
                            && SharedVariables.storedScreenHandler != null
                            && mc.player != null) {
                        mc.setScreen(SharedVariables.storedScreen);
                        mc.player.currentScreenHandler = SharedVariables.storedScreenHandler;
                    }
                }));

        screen.addDrawableChild(CustomButtonWidget.create(x, y + (h + spacing) * 9, halfW, Text.of("Clear Queue"), button -> {
            int count = SharedVariables.delayedUIPackets.size();
            SharedVariables.delayedUIPackets.clear();
            if (mc.player != null) {
                mc.player.sendMessage(
                        Text.literal("§7[§c*§7] §7Cleared §c" + count + " §7packets"), false);
            }
        }));

        screen.addDrawableChild(CustomButtonWidget.create(
                x + halfW + spacing,
                y + (h + spacing) * 9,
                halfW,
                Text.of("Queue: 0"),
                button -> button.setMessage(Text.of("Queue: " + SharedVariables.delayedUIPackets.size()))));

        screen.addDrawableChild(CustomButtonWidget.create(x, y + (h + spacing) * 10, halfW, Text.of("Resync Inv"), button -> {
            if (mc.getNetworkHandler() != null && mc.player != null) {
                mc.player.currentScreenHandler.syncState();
                mc.player.sendMessage(Text.literal("§7[§c*§7] §7Inventory resynced"), false);
            }
        }));

        screen.addDrawableChild(CustomButtonWidget.create(
                x + halfW + spacing, y + (h + spacing) * 10, halfW, Text.of("Disconnect"), button -> {
                    if (mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().getConnection().disconnect(Text.literal("Disconnected"));
                    }
                }));

        CustomButtonWidget spamButton = CustomButtonWidget.create(
                x + 32,
                y + (h + spacing) * 11,
                w - 64,
                Text.of("Spam (x" + SharedVariables.spamCount + ")"),
                button -> {
                    if (mc.getNetworkHandler() != null && !SharedVariables.delayedUIPackets.isEmpty()) {
                        int sent = 0;
                        for (int i = 0; i < SharedVariables.spamCount; i++) {
                            for (Packet<?> packet : SharedVariables.delayedUIPackets) {
                                mc.getNetworkHandler().sendPacket(packet);
                                sent++;
                            }
                        }
                        if (mc.player != null) {
                            mc.player.sendMessage(
                                    Text.literal("§7[§c*§7] §7Spammed §c" + sent + " §7packets"), false);
                        }
                    } else if (mc.player != null) {
                        mc.player.sendMessage(Text.literal("§7[§c*§7] §cNo packets in queue"), false);
                    }
                });

        screen.addDrawableChild(CustomButtonWidget.create(x, y + (h + spacing) * 11, 30, Text.of("-"), button -> {
            if (SharedVariables.spamCount > 1) {
                SharedVariables.spamCount--;
                spamButton.setMessage(Text.of("Spam (x" + SharedVariables.spamCount + ")"));
            }
        }));

        screen.addDrawableChild(spamButton);

        screen.addDrawableChild(CustomButtonWidget.create(x + w - 30, y + (h + spacing) * 11, 30, Text.of("+"), button -> {
            if (SharedVariables.spamCount < 100) {
                SharedVariables.spamCount++;
                spamButton.setMessage(Text.of("Spam (x" + SharedVariables.spamCount + ")"));
            }
        }));

        screen.addDrawableChild(CustomButtonWidget.create(x, y + (h + spacing) * 12, halfW, Text.of("Send One"), button -> {
            if (mc.getNetworkHandler() != null && !SharedVariables.delayedUIPackets.isEmpty()) {
                Packet<?> packet = SharedVariables.delayedUIPackets.remove(0);
                mc.getNetworkHandler().sendPacket(packet);
                if (mc.player != null) {
                    mc.player.sendMessage(
                            Text.literal("§7[§c*§7] §7Sent 1 packet §7(§c"
                                    + SharedVariables.delayedUIPackets.size()
                                    + "§7 left)"),
                            false);
                }
            } else if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§7[§c*§7] §cNo packets in queue"), false);
            }
        }));

        screen.addDrawableChild(CustomButtonWidget.create(
                x + halfW + spacing, y + (h + spacing) * 12, halfW, Text.of("Pop Last"), button -> {
                    if (!SharedVariables.delayedUIPackets.isEmpty()) {
                        SharedVariables.delayedUIPackets.remove(SharedVariables.delayedUIPackets.size() - 1);
                        if (mc.player != null) {
                            mc.player.sendMessage(
                                    Text.literal("§7[§c*§7] §7Removed last packet §7(§c"
                                            + SharedVariables.delayedUIPackets.size()
                                            + "§7 left)"),
                                    false);
                        }
                    } else if (mc.player != null) {
                        mc.player.sendMessage(Text.literal("§7[§c*§7] §cNo packets in queue"), false);
                    }
                }));
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

    private static String getTextAsJson(Text text, MinecraftClient mc) {
        try {
            Class<?> serializationClass = Class.forName("net.minecraft.text.Text$Serialization");
            Method toJsonMethod =
                    serializationClass.getMethod("toJsonString", Text.class, RegistryWrapper.WrapperLookup.class);
            RegistryWrapper.WrapperLookup registries =
                    mc.world != null ? mc.world.getRegistryManager() : mc.getNetworkHandler().getRegistryManager();
            return (String) toJsonMethod.invoke(null, text, registries);
        } catch (ClassNotFoundException e) {
            try {
                Class<?> serializerClass = Class.forName("net.minecraft.text.Text$Serializer");
                Method toJsonMethod =
                        serializerClass.getMethod("toJson", Text.class, RegistryWrapper.WrapperLookup.class);
                RegistryWrapper.WrapperLookup registries =
                        mc.world != null ? mc.world.getRegistryManager() : mc.getNetworkHandler().getRegistryManager();
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
