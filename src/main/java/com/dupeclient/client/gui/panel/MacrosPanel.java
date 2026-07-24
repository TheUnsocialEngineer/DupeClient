package com.dupeclient.client.gui.panel;

import com.dupeclient.client.gui.MacroEditorScreen;
import com.dupeclient.client.gui.MacroPromptScreen;
import com.dupeclient.client.gui.MacroShareScreen;
import com.dupeclient.client.gui.macro.MacroPanelHitLayout;
import com.dupeclient.client.gui.modern.UiComponents;
import com.dupeclient.client.gui.modern.UiTokens;
import com.dupeclient.client.module.macro.MacroDefinition;
import com.dupeclient.client.module.macro.MacroEngine;
import com.dupeclient.client.module.macro.MacroHotkeyConflicts;
import com.dupeclient.client.module.macro.MacroScheduler;
import com.dupeclient.client.module.macro.MacroShare;
import com.dupeclient.client.module.macro.MacroStorage;
import com.dupeclient.client.core.KeybindManager;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Macros hub: run macros, bind hotkeys, open editor / prompt generator.
 */
public class MacrosPanel extends Panel {
    private static final int ROW_H = 18;
    private static final int MAX_VISIBLE = 6;
    private static final int TAB_H = 18;
    private static final int BTN_H = 18;
    private static final int GAP = 6;
    private static final int CARD_H = 218;
    private static final int BIND_W = 72;
    private static final int ACTION_W = 34;
    private static final int LIST_REFRESH_INTERVAL = 40;
    private static final int HOTKEY_REFRESH_INTERVAL = 200;

    private enum Tab {
        PLAY,
        STUDIO
    }

    private Tab tab = Tab.PLAY;
    private List<String> macroIds = List.of();
    private int playListScroll;
    private int studioListScroll;
    private int studioSelectedIndex = -1;
    private long lastStudioClickMs;
    private int lastStudioClickGlobalIdx = -1;

    private String captureMacroIdOrNull;
    private final Map<String, Integer> hotkeyCache = new HashMap<>();
    private int listRefreshCooldown;
    private int hotkeyRefreshCooldown;
    private final MacroPanelHitLayout hits = new MacroPanelHitLayout();

    public MacrosPanel(int x, int y) {
        super("macros", Component.literal("Macros"), x, y, 340, 240);
    }

    @Override
    public void tick() {
        if (collapsed) {
            return;
        }
        if (--listRefreshCooldown <= 0) {
            listRefreshCooldown = LIST_REFRESH_INTERVAL;
            refreshMacroList();
        }
        if (--hotkeyRefreshCooldown <= 0) {
            hotkeyRefreshCooldown = HOTKEY_REFRESH_INTERVAL;
            refreshHotkeyCache();
        }
    }

    private void refreshMacroList() {
        macroIds = MacroStorage.listMacroIds();
        if (studioSelectedIndex >= macroIds.size()) {
            studioSelectedIndex = macroIds.isEmpty() ? -1 : macroIds.size() - 1;
        }
    }

    private void refreshHotkeyCache() {
        hotkeyCache.clear();
        for (String id : macroIds) {
            try {
                MacroDefinition d = MacroStorage.load(id);
                hotkeyCache.put(id, d.hotkeyKey);
            } catch (Exception ignored) {
                hotkeyCache.put(id, -1);
            }
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        hits.clear();
        super.render(context, mouseX, mouseY, delta);
        if (collapsed) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font tr = mc.font;
        int cardX = x + 8;
        int cardY = y + bodyTopOffset() + 8;
        int cardW = width - 16;

        UiComponents.drawSurfaceCard(context, cardX, cardY, cardW, CARD_H);
        int cx = cardX + 10;
        int cy = cardY + 10;
        int cw = cardW - 20;

        int tabW = (cw - GAP) / 2;
        UiComponents.drawSegmentTab(tr, context, cx, cy, tabW, TAB_H, "Play", tab == Tab.PLAY);
        hits.add(cx, cy, tabW, TAB_H, () -> {
            tab = Tab.PLAY;
            captureMacroIdOrNull = null;
        });
        UiComponents.drawSegmentTab(tr, context, cx + tabW + GAP, cy, tabW, TAB_H, "Studio", tab == Tab.STUDIO);
        hits.add(cx + tabW + GAP, cy, tabW, TAB_H, () -> {
            tab = Tab.STUDIO;
            captureMacroIdOrNull = null;
        });
        cy += TAB_H + GAP + 2;

        if (tab == Tab.PLAY) {
            renderPlayTab(context, tr, mc, cx, cy, cw, mouseX, mouseY);
        } else {
            renderStudioTab(context, tr, mc, cx, cy, cw, mouseX, mouseY);
        }

        height = bodyTopOffset() + 8 + CARD_H + 14;
    }

    private void renderPlayTab(
            GuiGraphicsExtractor context, Font tr, Minecraft mc, int tx, int ty, int sw, int mouseX, int mouseY) {
        String status = MacroEngine.INSTANCE.isRunning()
                ? "Running: " + MacroEngine.INSTANCE.getRunLabel()
                : "Idle — assign a run key or use Run";
        int statusColor = MacroEngine.INSTANCE.isRunning() ? 0xFF4ADE9A : UiTokens.ACCENT;
        context.text(tr, Component.literal(status), tx, ty, statusColor);
        ty += 12;
        String queue = MacroScheduler.getInstance().statusLine();
        if (!queue.isBlank()) {
            context.text(tr, Component.literal(queue), tx, ty, UiTokens.TEXT_DIM);
            ty += 10;
        }
        String conflict = MacroHotkeyConflicts.summaryLine();
        if (!conflict.isBlank()) {
            context.text(tr, Component.literal(conflict), tx, ty, 0xFFFF9A6A);
            ty += 10;
        }

        UiComponents.drawPillActionButton(tr, context, tx, ty, sw, BTN_H, "Open macro editor",
                UiComponents.PillActionStyle.PRIMARY_BLUE);
        hits.add(tx, ty, sw, BTN_H, () -> {
            captureMacroIdOrNull = null;
            MacroEditorScreen.open(mc, null);
        });
        ty += BTN_H + GAP;

        int stopW = 64;
        UiComponents.drawPillActionButton(tr, context, tx, ty, stopW, BTN_H, "Stop",
                MacroEngine.INSTANCE.isRunning()
                        ? UiComponents.PillActionStyle.PRIMARY_MINT
                        : UiComponents.PillActionStyle.SECONDARY_SLATE);
        if (MacroEngine.INSTANCE.isRunning()) {
            hits.add(tx, ty, stopW, BTN_H, () -> {
                captureMacroIdOrNull = null;
                MacroEngine.INSTANCE.stop(mc);
            });
        }
        ty += BTN_H + GAP + 2;

        context.text(tr, Component.literal("Macros"), tx, ty, UiTokens.TEXT_DIM);
        ty += 10;

        List<String> visible = sliceVisible(playListScroll);
        for (int i = 0; i < visible.size(); i++) {
            String id = visible.get(i);
            int ry = ty + i * (ROW_H + 2);
            if (id.startsWith("(")) {
                context.text(tr, Component.literal(id), tx + 4, ry + 5, UiTokens.TEXT_DIM);
                continue;
            }

            boolean rowHot = mouseX >= tx && mouseX < tx + sw && mouseY >= ry && mouseY < ry + ROW_H;
            UiComponents.drawListRowBack(context, tx, ry, sw, ROW_H, rowHot);
            if (rowHot) {
                context.fill(tx + 1, ry + 1, tx + sw - 1, ry + ROW_H - 1, 0x22FFFFFF);
            }

            int labelMax = sw - BIND_W - ACTION_W * 3 - 24;
            context.text(tr, Component.literal(tr.plainSubstrByWidth(id, Math.max(8, labelMax))), tx + 6, ry + 5, UiTokens.TEXT);

            int bx = tx + sw - BIND_W - ACTION_W * 3 - 8;
            boolean listening = id.equals(captureMacroIdOrNull);
            String bindText = listening ? "Press key…" : keyName(hotkeyCache.getOrDefault(id, -1));
            int fill = listening ? 0xCC5A2E1A : 0xC8121822;
            int edge = listening ? 0xFFFF9A6A : 0xFF3A4A5E;
            UiComponents.drawSlotField(context, bx, ry + 2, BIND_W, ROW_H - 4, fill, edge);
            context.centeredText(tr, Component.literal(bindText), bx + BIND_W / 2, ry + 5, 0xFFE6EEFF);
            hits.add(bx, ry + 2, BIND_W, ROW_H - 4, () -> captureMacroIdOrNull = id);

            int ax = tx + sw - ACTION_W * 3 - 4;
            drawRowAction(tr, context, ax, ry + 2, ACTION_W, ROW_H - 4, "Ed");
            hits.add(ax, ry + 2, ACTION_W, ROW_H - 4, () -> {
                captureMacroIdOrNull = null;
                MacroEditorScreen.open(mc, id);
            });

            drawRowAction(tr, context, ax + ACTION_W + 2, ry + 2, ACTION_W, ROW_H - 4, "Del");
            hits.add(ax + ACTION_W + 2, ry + 2, ACTION_W, ROW_H - 4, () -> {
                captureMacroIdOrNull = null;
                openConfirmDeleteMacro(id);
            });

            drawRowAction(tr, context, ax + (ACTION_W + 2) * 2, ry + 2, ACTION_W, ROW_H - 4, "Run");
            if (!MacroEngine.INSTANCE.isRunning()) {
                hits.add(ax + (ACTION_W + 2) * 2, ry + 2, ACTION_W, ROW_H - 4, () -> {
                    captureMacroIdOrNull = null;
                    MacroScheduler.getInstance().enqueue(id);
                });
            }
        }
    }

    private void renderStudioTab(
            GuiGraphicsExtractor context, Font tr, Minecraft mc, int tx, int ty, int sw, int mouseX, int mouseY) {
        String keyLabel = KeybindManager.OPEN_MACRO_EDITOR_KEY.getTranslatedKeyMessage().getString();
        context.text(tr, Component.literal("Editor key: " + keyLabel), tx, ty, UiTokens.TEXT_DIM);
        ty += 12;

        int third = (sw - GAP * 2) / 3;
        UiComponents.drawPillActionButton(tr, context, tx, ty, third, BTN_H, "New",
                UiComponents.PillActionStyle.PRIMARY_MINT);
        hits.add(tx, ty, third, BTN_H, () -> {
            captureMacroIdOrNull = null;
            MacroEditorScreen.open(mc, null);
        });

        UiComponents.drawPillActionButton(tr, context, tx + third + GAP, ty, third, BTN_H, "Edit",
                studioSelectedIndex >= 0 && studioSelectedIndex < macroIds.size()
                        ? UiComponents.PillActionStyle.PRIMARY_BLUE
                        : UiComponents.PillActionStyle.SECONDARY_SLATE);
        if (studioSelectedIndex >= 0 && studioSelectedIndex < macroIds.size()) {
            int sel = studioSelectedIndex;
            hits.add(tx + third + GAP, ty, third, BTN_H, () -> {
                captureMacroIdOrNull = null;
                MacroEditorScreen.open(mc, macroIds.get(sel));
            });
        }

        UiComponents.drawPillActionButton(tr, context, tx + (third + GAP) * 2, ty, third, BTN_H, "Delete",
                UiComponents.PillActionStyle.SECONDARY_SLATE);
        if (studioSelectedIndex >= 0 && studioSelectedIndex < macroIds.size()) {
            int sel = studioSelectedIndex;
            hits.add(tx + (third + GAP) * 2, ty, third, BTN_H, () -> {
                captureMacroIdOrNull = null;
                openConfirmDeleteMacro(macroIds.get(sel));
            });
        }
        ty += BTN_H + GAP;

        UiComponents.drawPillActionButton(tr, context, tx, ty, sw, BTN_H, "Generate from prompt ✦",
                UiComponents.PillActionStyle.PRIMARY_BLUE);
        hits.add(tx, ty, sw, BTN_H, () -> {
            captureMacroIdOrNull = null;
            MacroPromptScreen.open(mc, mc.screen);
        });
        ty += BTN_H + GAP;

        int half = (sw - GAP) / 2;
        UiComponents.drawPillActionButton(tr, context, tx, ty, half, BTN_H, "Import",
                UiComponents.PillActionStyle.SECONDARY_SLATE);
        hits.add(tx, ty, half, BTN_H, () -> {
            captureMacroIdOrNull = null;
            String hint = studioSelectedIndex >= 0 && studioSelectedIndex < macroIds.size()
                    ? macroIds.get(studioSelectedIndex) : null;
            MacroShareScreen.open(mc, mc.screen, hint);
        });

        UiComponents.drawPillActionButton(tr, context, tx + half + GAP, ty, half, BTN_H, "Export",
                studioSelectedIndex >= 0 && studioSelectedIndex < macroIds.size()
                        ? UiComponents.PillActionStyle.PRIMARY_MINT
                        : UiComponents.PillActionStyle.SECONDARY_SLATE);
        if (studioSelectedIndex >= 0 && studioSelectedIndex < macroIds.size()) {
            int sel = studioSelectedIndex;
            hits.add(tx + half + GAP, ty, half, BTN_H, () -> {
                captureMacroIdOrNull = null;
                MacroShare.exportMacroToClipboard(mc, macroIds.get(sel));
            });
        }
        ty += BTN_H + GAP + 2;

        context.text(tr, Component.literal("Click to select · double-click to edit"), tx, ty, UiTokens.TEXT_DIM);
        ty += 10;

        List<String> visible = sliceVisible(studioListScroll);
        for (int i = 0; i < visible.size(); i++) {
            int global = studioListScroll + i;
            int ry = ty + i * (ROW_H + 2);
            String id = visible.get(i);
            if (id.startsWith("(")) {
                context.text(tr, Component.literal(id), tx + 4, ry + 5, UiTokens.TEXT_DIM);
                continue;
            }

            boolean sel = global == studioSelectedIndex;
            boolean rowHot = mouseX >= tx && mouseX < tx + sw && mouseY >= ry && mouseY < ry + ROW_H;
            UiComponents.drawListRowBack(context, tx, ry, sw, ROW_H, sel);
            if (rowHot && !sel) {
                context.fill(tx + 1, ry + 1, tx + sw - 1, ry + ROW_H - 1, 0x18FFFFFF);
            }
            context.text(tr, Component.literal(id), tx + 8, ry + 5, sel ? UiTokens.ACCENT : UiTokens.TEXT);
            if (sel) {
                context.text(tr, Component.literal("▸"), tx + sw - 12, ry + 5, UiTokens.MINT_300);
            }

            final int g = global;
            hits.add(tx, ry, sw, ROW_H, () -> handleStudioRowClick(mc, g));
        }
    }

    private void handleStudioRowClick(Minecraft mc, int global) {
        if (global < 0 || global >= macroIds.size()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (global == lastStudioClickGlobalIdx && now - lastStudioClickMs < 450L) {
            MacroEditorScreen.open(mc, macroIds.get(global));
            lastStudioClickGlobalIdx = -1;
        } else {
            studioSelectedIndex = global;
            lastStudioClickMs = now;
            lastStudioClickGlobalIdx = global;
        }
    }

    private static void drawRowAction(Font tr, GuiGraphicsExtractor context, int x, int y, int w, int h, String label) {
        UiComponents.drawPillActionButton(tr, context, x, y, w, h, label, UiComponents.PillActionStyle.SECONDARY_SLATE);
    }

    private List<String> sliceVisible(int scroll) {
        if (macroIds.isEmpty()) {
            return tab == Tab.PLAY ? List.of("(no macros yet)") : List.of("(no macros — use New)");
        }
        int maxScroll = Math.max(0, macroIds.size() - MAX_VISIBLE);
        if (tab == Tab.PLAY) {
            playListScroll = Math.max(0, Math.min(scroll, maxScroll));
            scroll = playListScroll;
        } else {
            studioListScroll = Math.max(0, Math.min(scroll, maxScroll));
            scroll = studioListScroll;
        }
        return macroIds.subList(scroll, Math.min(macroIds.size(), scroll + MAX_VISIBLE));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (collapsed || button != 0) {
            return false;
        }
        return hits.dispatch(mouseX, mouseY);
    }

    private void openConfirmDeleteMacro(String macroId) {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen;
        final String target = macroId;
        mc.setScreen(new ConfirmScreen(yes -> {
            mc.setScreen(parent);
            if (!yes) {
                return;
            }
            int idx = -1;
            for (int i = 0; i < macroIds.size(); i++) {
                if (macroIds.get(i).equalsIgnoreCase(MacroStorage.filenameId(target))) {
                    idx = i;
                    break;
                }
            }
            try {
                MacroStorage.deleteMacro(target);
                if (idx >= 0) {
                    if (studioSelectedIndex == idx) {
                        studioSelectedIndex = -1;
                    } else if (studioSelectedIndex > idx) {
                        studioSelectedIndex--;
                    }
                }
                String active = MacroEngine.INSTANCE.getActiveMacroId();
                if (active != null && !active.isEmpty()
                        && MacroStorage.filenameId(active).equalsIgnoreCase(MacroStorage.filenameId(target))) {
                    MacroEngine.INSTANCE.stop(mc);
                }
                hotkeyRefreshCooldown = 0;
                listRefreshCooldown = 0;
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.literal("[Macro] Deleted \"" + MacroStorage.filenameId(target) + "\".")
                            .withStyle(ChatFormatting.GREEN));
                }
            } catch (Exception e) {
                if (mc.player != null) {
                    String msg = e.getMessage() == null ? "Delete failed" : e.getMessage();
                    mc.player.sendSystemMessage(Component.literal("[Macro] ").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(msg).withStyle(ChatFormatting.RED)));
                }
            }
        }, Component.literal("Delete macro?"),
                Component.literal("Removes " + MacroStorage.filenameId(macroId) + ".json from disk. This cannot be undone.")));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (collapsed || macroIds.size() <= MAX_VISIBLE || !containsPoint(mouseX, mouseY)) {
            return false;
        }
        int delta = (int) Math.signum(verticalAmount) * Math.max(1, (int) Math.ceil(Math.abs(verticalAmount)));
        int maxScroll = Math.max(0, macroIds.size() - MAX_VISIBLE);
        if (tab == Tab.PLAY) {
            playListScroll = Math.max(0, Math.min(maxScroll, playListScroll - delta));
        } else {
            studioListScroll = Math.max(0, Math.min(maxScroll, studioListScroll - delta));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (captureMacroIdOrNull == null) {
            return false;
        }
        int key = keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
        String err = MacroStorage.setRunHotkey(captureMacroIdOrNull, key);
        captureMacroIdOrNull = null;
        hotkeyRefreshCooldown = 0;
        Minecraft c = Minecraft.getInstance();
        if (c.player != null) {
            if (err != null) {
                c.player.sendSystemMessage(Component.literal("[Macro] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(err).withStyle(ChatFormatting.RED)));
            } else {
                c.player.sendSystemMessage(Component.literal("[Macro] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("Run key saved (" + keyName(key) + ").").withStyle(ChatFormatting.GREEN)));
            }
        }
        return true;
    }

    @Override
    public void onModuleHidden() {
        captureMacroIdOrNull = null;
    }

    @Override
    public boolean hasFocusedTextInput() {
        return isVisible() && captureMacroIdOrNull != null;
    }

    private static String keyName(int keyCode) {
        if (keyCode < 0 || keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return "UNBOUND";
        }
        String glfw = GLFW.glfwGetKeyName(keyCode, 0);
        if (glfw != null) {
            return glfw.toUpperCase(Locale.ROOT);
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            default -> "KEY_" + keyCode;
        };
    }
}
